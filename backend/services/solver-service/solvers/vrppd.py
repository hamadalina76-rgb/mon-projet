from __future__ import annotations

from typing import Dict, List

from schemas import SolveRequest, SolveResponse, Assignment

INF_COST = 1.0e12


def solve_vrppd(request: SolveRequest) -> SolveResponse:
    if not request.orders or not request.couriers:
        return SolveResponse(status="ok", solverUsed="VRPPD", assignments=[])

    timeout_ms = int(request.timeoutMs or 2000)
    if timeout_ms <= 1:
        return SolveResponse(status="partial", solverUsed="VRPPD", assignments=[])

    try:
        from ortools.constraint_solver import pywrapcp, routing_enums_pb2
    except Exception:
        return _greedy_capacity_tw_fallback(request)

    n_orders = len(request.orders)
    n_couriers = len(request.couriers)

    # Node layout:
    # 0..(n_couriers-1)                : per-courier starts/ends (same node for start+end)
    # n_couriers..n_couriers+n_orders-1: pickups
    # ... +n_orders                    : deliveries
    pickup_offset = n_couriers
    delivery_offset = pickup_offset + n_orders
    num_nodes = n_couriers + 2 * n_orders

    starts = list(range(n_couriers))
    ends = list(range(n_couriers))
    manager = pywrapcp.RoutingIndexManager(num_nodes, n_couriers, starts, ends)
    routing = pywrapcp.RoutingModel(manager)

    def node_type(node: int) -> str:
        if node < n_couriers:
            return "courier"
        if node < delivery_offset:
            return "pickup"
        return "delivery"

    def node_order_idx(node: int) -> int:
        if node_type(node) == "pickup":
            return node - pickup_offset
        if node_type(node) == "delivery":
            return node - delivery_offset
        return -1

    def travel_cost(from_node: int, to_node: int) -> float:
        if from_node == to_node:
            return 0.0
        to_t = node_type(to_node)
        from_t = node_type(from_node)

        if to_t == "delivery" and from_t in {"courier", "pickup"}:
            # Must go through pickup first.
            return INF_COST

        if from_t == "courier" and to_t == "pickup":
            oi = node_order_idx(to_node)
            cj = from_node
            base = _matrix_cost(request, oi, cj)
            return float(base if base < INF_COST / 2 else INF_COST)

        # Approximate remaining legs from geometry when available.
        from_pt = _node_coord(request, from_node, n_couriers, pickup_offset, delivery_offset)
        to_pt = _node_coord(request, to_node, n_couriers, pickup_offset, delivery_offset)
        if from_pt is None or to_pt is None:
            return 10.0
        return _euclidean_minutes(from_pt[0], from_pt[1], to_pt[0], to_pt[1])

    def transit_callback(from_index: int, to_index: int) -> int:
        from_node = manager.IndexToNode(from_index)
        to_node = manager.IndexToNode(to_index)
        return int(min(INF_COST / 4, max(0.0, travel_cost(from_node, to_node))))

    transit_cb_idx = routing.RegisterTransitCallback(transit_callback)
    routing.SetArcCostEvaluatorOfAllVehicles(transit_cb_idx)

    # Capacity dimension.
    def demand_callback(index: int) -> int:
        node = manager.IndexToNode(index)
        t = node_type(node)
        if t == "pickup":
            return 1
        if t == "delivery":
            return -1
        return 0

    demand_cb_idx = routing.RegisterUnaryTransitCallback(demand_callback)
    capacities = [
        (c.capacity if c.capacity and c.capacity > 0 else _capacity_from_vehicle(c.vehicleType))
        for c in request.couriers
    ]
    routing.AddDimensionWithVehicleCapacity(
        demand_cb_idx,
        0,
        capacities,
        True,
        "Capacity",
    )

    # Time dimension with per-order delivery deadline.
    routing.AddDimension(
        transit_cb_idx,
        10_000,  # waiting slack
        100_000,  # max route time
        True,
        "Time",
    )
    time_dim = routing.GetDimensionOrDie("Time")

    for i, order in enumerate(request.orders):
        p_node = pickup_offset + i
        d_node = delivery_offset + i
        p_idx = manager.NodeToIndex(p_node)
        d_idx = manager.NodeToIndex(d_node)

        routing.AddPickupAndDelivery(p_idx, d_idx)
        routing.solver().Add(routing.VehicleVar(p_idx) == routing.VehicleVar(d_idx))
        routing.solver().Add(time_dim.CumulVar(p_idx) <= time_dim.CumulVar(d_idx))

        max_delivery = max(1, int(order.guaranteedDeliveryMinutes or 30))
        time_dim.CumulVar(d_idx).SetRange(0, max_delivery)

    # Avoid impossible assignments from cost matrix (order-courier forbidden pairs).
    for oi in range(n_orders):
        p_idx = manager.NodeToIndex(pickup_offset + oi)
        for cj in range(n_couriers):
            if _matrix_cost(request, oi, cj) >= INF_COST / 2:
                routing.VehicleVar(p_idx).RemoveValue(cj)

    search = pywrapcp.DefaultRoutingSearchParameters()
    search.first_solution_strategy = routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    search.local_search_metaheuristic = routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    search.time_limit.FromMilliseconds(timeout_ms)

    solution = routing.SolveWithParameters(search)
    if solution is None:
        return SolveResponse(status="partial", solverUsed="VRPPD", assignments=[])

    assignments: List[Assignment] = []
    for oi, order in enumerate(request.orders):
        p_idx = manager.NodeToIndex(pickup_offset + oi)
        d_idx = manager.NodeToIndex(delivery_offset + oi)
        vehicle = solution.Value(routing.VehicleVar(p_idx))
        if vehicle < 0 or vehicle >= n_couriers:
            continue

        delivery_min = int(solution.Value(time_dim.CumulVar(d_idx)))
        pickup_min = int(solution.Value(time_dim.CumulVar(p_idx)))
        courier = request.couriers[vehicle]
        base_cost = _matrix_cost(request, oi, vehicle)
        if base_cost >= INF_COST / 2:
            continue

        assignments.append(
            Assignment(
                orderId=order.id,
                courierId=courier.id,
                cost=float(base_cost),
                etaPickupMin=max(1, pickup_min),
                etaDeliveryMin=max(1, delivery_min),
            )
        )

    status = "ok" if len(assignments) == len(request.orders) else "partial"
    return SolveResponse(status=status, solverUsed="VRPPD", assignments=assignments)


def _capacity_from_vehicle(vehicle_type: str | None) -> int:
    if not vehicle_type:
        return 1
    v = vehicle_type.upper()
    if v == "MOTO":
        return 2
    if v == "MOTOTRICYCLE":
        return 4
    return 1


def _matrix_cost(request: SolveRequest, order_idx: int, courier_idx: int) -> float:
    if order_idx < len(request.costs) and courier_idx < len(request.costs[order_idx]):
        return float(request.costs[order_idx][courier_idx])
    return INF_COST


def _node_coord(request: SolveRequest, node: int, n_couriers: int, pickup_offset: int, delivery_offset: int):
    if node < n_couriers:
        c = request.couriers[node]
        if c.lat is None or c.lon is None:
            return None
        return (float(c.lat), float(c.lon))

    if node < delivery_offset:
        o = request.orders[node - pickup_offset]
        if o.pickupLat is None or o.pickupLon is None:
            return None
        return (float(o.pickupLat), float(o.pickupLon))

    o = request.orders[node - delivery_offset]
    if o.deliveryLat is None or o.deliveryLon is None:
        return None
    return (float(o.deliveryLat), float(o.deliveryLon))


def _euclidean_minutes(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    # Lightweight deterministic proxy in minutes for routing tests.
    dx = lat1 - lat2
    dy = lon1 - lon2
    return max(1.0, (dx * dx + dy * dy) ** 0.5 * 1000.0)


def _greedy_capacity_tw_fallback(request: SolveRequest) -> SolveResponse:
    used_orders = set()
    per_courier_load: Dict[int, int] = {c.id: 0 for c in request.couriers}
    couriers_by_id = {c.id: c for c in request.couriers}
    assignments: List[Assignment] = []

    candidates = []
    for i, order in enumerate(request.orders):
        for j, courier in enumerate(request.couriers):
            cost = _matrix_cost(request, i, j)
            if cost >= INF_COST / 2:
                continue
            eta_delivery = max(1, int(float(cost)))
            if eta_delivery > max(1, int(order.guaranteedDeliveryMinutes or 30)):
                continue
            candidates.append((float(cost), order.id, courier.id, eta_delivery))

    candidates.sort(key=lambda x: x[0])
    for cost, order_id, courier_id, eta_delivery in candidates:
        if order_id in used_orders:
            continue
        courier = couriers_by_id[courier_id]
        capacity = courier.capacity if courier.capacity and courier.capacity > 0 else _capacity_from_vehicle(courier.vehicleType)
        if per_courier_load[courier_id] >= capacity:
            continue
        used_orders.add(order_id)
        per_courier_load[courier_id] += 1
        assignments.append(
            Assignment(
                orderId=order_id,
                courierId=courier_id,
                cost=cost,
                etaPickupMin=max(1, int(cost / 2)),
                etaDeliveryMin=eta_delivery,
            )
        )

    return SolveResponse(status="partial", solverUsed="VRPPD", assignments=assignments)
