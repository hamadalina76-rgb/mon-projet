package com.speedline.delivery.dispatch.impl.bundling;

import com.speedline.delivery.dispatch.client.SolverServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.contract.engine.BundlingEngine;
import com.speedline.delivery.dispatch.contract.engine.BundlingResult;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleBasedBundlingEngine implements BundlingEngine {

	private final DispatchProperties dispatchProperties;
	private final SolverServiceClient solverServiceClient;

	@Override
	public BundlingResult detectBundles(List<PendingOrder> orders) {
		if (orders == null || orders.size() < 2) {
			return BundlingResult.builder().build();
		}

		List<PendingOrder> candidates = orders.stream()
				.filter(this::hasValidCoordinates)
				.sorted(Comparator.comparing(PendingOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();

		if (candidates.size() < 2) {
			return BundlingResult.builder().build();
		}

		int maxOrders = Math.max(2, dispatchProperties.getBundling().getMaxBundleSize());
		Set<Long> assigned = new HashSet<>();
		List<List<Long>> bundles = new ArrayList<>();
		Map<Long, Long> orderToBundle = new HashMap<>();
		Map<Long, List<Long>> bundleToOrders = new LinkedHashMap<>();
		Map<Long, Integer> orderEtaMinutes = new HashMap<>();
		long nextBundleId = 1L;

		for (PendingOrder seed : candidates) {
			if (seed.getId() == null || assigned.contains(seed.getId())) {
				continue;
			}

			List<PendingOrder> local = candidates.stream()
					.filter(o -> o.getId() != null && !assigned.contains(o.getId()))
					.filter(o -> canBundle(seed, o))
					.sorted(Comparator.comparingDouble(o -> customerDistanceMeters(seed, o)))
					.limit(maxOrders)
					.toList();

			if (local.size() < 2) {
				continue;
			}

			RoutePlan routePlan = computeRoute(local);
			if (!isSlaSafe(local, routePlan.orderEtaMinutes())) {
				continue;
			}

			long bundleId = nextBundleId++;
			List<Long> orderIds = new ArrayList<>(local.size());
			for (PendingOrder order : local) {
				assigned.add(order.getId());
				orderIds.add(order.getId());
				orderToBundle.put(order.getId(), bundleId);
				Integer eta = routePlan.orderEtaMinutes().get(order.getId());
				if (eta != null) {
					orderEtaMinutes.put(order.getId(), eta);
				}
			}

			bundles.add(orderIds);
			bundleToOrders.put(bundleId, orderIds);
		}

		return BundlingResult.builder()
				.bundles(bundles)
				.orderToBundleId(orderToBundle)
				.bundleToOrders(bundleToOrders)
				.orderEtaMinutes(orderEtaMinutes)
				.build();
	}

	private boolean hasValidCoordinates(PendingOrder order) {
		return order != null
				&& order.getId() != null
				&& order.getPartnerLat() != null
				&& order.getPartnerLon() != null
				&& order.getCustomerLat() != null
				&& order.getCustomerLon() != null;
	}

	private boolean canBundle(PendingOrder seed, PendingOrder candidate) {
		if (seed == null || candidate == null || Objects.equals(seed.getId(), candidate.getId())) {
			return false;
		}

		if (!isWithinTimeWindow(seed, candidate)) {
			return false;
		}

		if (customerDistanceMeters(seed, candidate) > dispatchProperties.getBundling().getDropoffRadiusMeters()) {
			return false;
		}

		boolean samePartner = seed.getPartnerId() != null && seed.getPartnerId().equals(candidate.getPartnerId());
		if (!samePartner && partnerDistanceMeters(seed, candidate) > dispatchProperties.getBundling().getMerchantRadiusMeters()) {
			return false;
		}

		return true;
	}

	private boolean isWithinTimeWindow(PendingOrder left, PendingOrder right) {
		if (left.getCreatedAt() == null || right.getCreatedAt() == null) {
			return true;
		}
		long deltaSeconds = Math.abs(left.getCreatedAt().getEpochSecond() - right.getCreatedAt().getEpochSecond());
		return deltaSeconds <= dispatchProperties.getBundling().getTimeWindowSeconds();
	}

	private RoutePlan computeRoute(List<PendingOrder> orders) {
		if (orders.isEmpty()) {
			return new RoutePlan(List.of(), Map.of());
		}

		if (dispatchProperties.getSolver().getOrtools().isEnabled()) {
			try {
				BundleRouteRequest request = BundleRouteRequest.builder()
						.startLat(avgPartnerLat(orders))
						.startLon(avgPartnerLon(orders))
						.stops(orders.stream()
								.map(o -> BundleRouteRequest.Stop.builder()
										.orderId(o.getId())
										.lat(o.getCustomerLat())
										.lon(o.getCustomerLon())
										.build())
								.toList())
						.build();
				BundleRouteResponse response = solverServiceClient.optimizeRoute(request);
				RoutePlan fromOrTools = normalizeResponse(response, orders);
				if (!fromOrTools.orderSequence().isEmpty()) {
					return fromOrTools;
				}
			} catch (Exception ex) {
				log.debug("OR-Tools route optimization unavailable, fallback nearest-neighbor: {}", ex.getMessage());
			}
		}

		return nearestNeighborRoute(orders);
	}

	private RoutePlan normalizeResponse(BundleRouteResponse response, List<PendingOrder> orders) {
		if (response == null || response.getOrderSequence() == null || response.getOrderSequence().isEmpty()) {
			return new RoutePlan(List.of(), Map.of());
		}

		Map<Long, PendingOrder> byId = new HashMap<>();
		for (PendingOrder order : orders) {
			byId.put(order.getId(), order);
		}

		List<Long> sequence = new ArrayList<>();
		for (Long id : response.getOrderSequence()) {
			if (id != null && byId.containsKey(id) && !sequence.contains(id)) {
				sequence.add(id);
			}
		}
		if (sequence.size() != orders.size()) {
			return new RoutePlan(List.of(), Map.of());
		}

		List<Integer> cumulative = response.getCumulativeEtaMinutes() == null ? List.of() : response.getCumulativeEtaMinutes();
		Map<Long, Integer> etaByOrder = new HashMap<>();
		for (int i = 0; i < sequence.size(); i++) {
			int eta = i < cumulative.size() ? Math.max(1, cumulative.get(i)) : (i + 1) * 5;
			etaByOrder.put(sequence.get(i), eta);
		}

		return new RoutePlan(sequence, etaByOrder);
	}

	private RoutePlan nearestNeighborRoute(List<PendingOrder> orders) {
		Map<Long, PendingOrder> byId = new HashMap<>();
		for (PendingOrder order : orders) {
			byId.put(order.getId(), order);
		}

		List<Long> remaining = new ArrayList<>(byId.keySet());
		List<Long> sequence = new ArrayList<>(orders.size());
		Map<Long, Integer> etaByOrder = new HashMap<>();

		double currentLat = avgPartnerLat(orders);
		double currentLon = avgPartnerLon(orders);
		int cumulativeMinutes = 0;

		while (!remaining.isEmpty()) {
			final double refLat = currentLat;
			final double refLon = currentLon;
			Long nearestOrderId = remaining.stream()
					.min(Comparator.comparingDouble(id -> {
						PendingOrder o = byId.get(id);
						return haversineKm(refLat, refLon, o.getCustomerLat(), o.getCustomerLon());
					}))
					.orElse(null);
			if (nearestOrderId == null) {
				break;
			}

			PendingOrder next = byId.get(nearestOrderId);
			double distanceKm = haversineKm(currentLat, currentLon, next.getCustomerLat(), next.getCustomerLon());
			int travelMinutes = (int) Math.ceil((distanceKm / dispatchProperties.getBundling().getAverageSpeedKmh()) * 60.0);
			cumulativeMinutes += Math.max(1, travelMinutes);

			sequence.add(nearestOrderId);
			etaByOrder.put(nearestOrderId, cumulativeMinutes);
			remaining.remove(nearestOrderId);
			currentLat = next.getCustomerLat();
			currentLon = next.getCustomerLon();
		}

		return new RoutePlan(sequence, etaByOrder);
	}

	private boolean isSlaSafe(List<PendingOrder> orders, Map<Long, Integer> etaByOrder) {
		for (PendingOrder order : orders) {
			Integer guaranteed = order.getGuaranteedDeliveryMinutes();
			Integer eta = etaByOrder.get(order.getId());
			if (guaranteed != null && eta != null && eta > guaranteed) {
				return false;
			}
		}
		return true;
	}

	private double avgPartnerLat(List<PendingOrder> orders) {
		return orders.stream().map(PendingOrder::getPartnerLat).filter(Objects::nonNull)
				.mapToDouble(Double::doubleValue)
				.average()
				.orElse(0.0);
	}

	private double avgPartnerLon(List<PendingOrder> orders) {
		return orders.stream().map(PendingOrder::getPartnerLon).filter(Objects::nonNull)
				.mapToDouble(Double::doubleValue)
				.average()
				.orElse(0.0);
	}

	private double customerDistanceMeters(PendingOrder left, PendingOrder right) {
		return haversineKm(left.getCustomerLat(), left.getCustomerLon(), right.getCustomerLat(), right.getCustomerLon()) * 1000.0;
	}

	private double partnerDistanceMeters(PendingOrder left, PendingOrder right) {
		return haversineKm(left.getPartnerLat(), left.getPartnerLon(), right.getPartnerLat(), right.getPartnerLon()) * 1000.0;
	}

	private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		final double earthRadiusKm = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
		double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
		return earthRadiusKm * c;
	}

	private record RoutePlan(List<Long> orderSequence, Map<Long, Integer> orderEtaMinutes) {
	}
}
