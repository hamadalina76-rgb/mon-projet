"""Nearest-neighbor TSP for multi-drop bundle routes (OR-Tools optional later)."""
from __future__ import annotations

import math
from typing import List, Optional, Tuple

from schemas import BundleRouteRequest, BundleRouteResponse, BundleStop


EARTH_R_KM = 6371.0
DEFAULT_AVG_SPEED_KMH = 15.0  # city bicycle / moped default


def _haversine_km(
    a: Tuple[float, float], b: Tuple[float, float]
) -> float:
    lat1, lon1, lat2, lon2 = map(
        math.radians, (a[0], a[1], b[0], b[1])
    )
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    h = (
        math.sin(dlat / 2) ** 2
        + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    )
    return 2 * EARTH_R_KM * math.asin(min(1.0, math.sqrt(h)))


def _coord(stop: BundleStop) -> Optional[Tuple[float, float]]:
    if stop.lat is None or stop.lon is None:
        return None
    return (float(stop.lat), float(stop.lon))


def optimize_bundle_route(
    request: BundleRouteRequest, avg_speed_kmh: float = DEFAULT_AVG_SPEED_KMH
) -> BundleRouteResponse:
    """Greedy nearest-neighbor from start through all valid-coordinate stops."""
    stops = list(request.stops)
    if not stops:
        return BundleRouteResponse()

    # Valid stops: must have lat/lon
    indexed: List[Tuple[int, BundleStop, Tuple[float, float]]] = []
    for i, s in enumerate(stops):
        c = _coord(s)
        if c is not None and s.orderId is not None:
            indexed.append((i, s, c))

    if not indexed:
        return BundleRouteResponse(
            orderSequence=[s.orderId for s in stops if s.orderId is not None],
            cumulativeEtaMinutes=[],
        )

    start: Tuple[float, float]
    if request.startLat is not None and request.startLon is not None:
        start = (float(request.startLat), float(request.startLon))
    else:
        # Use centroid of valid stops
        n = len(indexed)
        start = (
            sum(t[1][0] for t in indexed) / n,
            sum(t[1][1] for t in indexed) / n,
        )

    remaining = list(indexed)
    order_sequence: List[int] = []
    cumulative_min: List[int] = []
    current = start
    total_km = 0.0

    while remaining:
        best_i = 0
        best_d = _haversine_km(current, remaining[0][2])
        for j in range(1, len(remaining)):
            d = _haversine_km(current, remaining[j][2])
            if d < best_d:
                best_d = d
                best_i = j
        _idx, st, coord = remaining.pop(best_i)
        order_sequence.append(int(st.orderId))
        total_km += best_d
        eta_min = max(0, int(round((total_km / max(avg_speed_kmh, 1.0)) * 60.0)))
        cumulative_min.append(eta_min)
        current = coord

    return BundleRouteResponse(
        orderSequence=order_sequence, cumulativeEtaMinutes=cumulative_min
    )
