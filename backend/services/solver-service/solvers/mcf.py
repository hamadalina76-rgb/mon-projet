from __future__ import annotations

from typing import List

from schemas import SolveRequest, SolveResponse, Assignment


INF_COST = 1.0e12
SCALE = 1000


def solve_mcf(request: SolveRequest) -> SolveResponse:
    if not request.orders or not request.couriers:
        return SolveResponse(status="ok", solverUsed="MCF", assignments=[])

    timeout_ms = int(request.timeoutMs or 2000)
    if timeout_ms <= 1:
        return SolveResponse(status="partial", solverUsed="MCF", assignments=[])

    try:
        from ortools.graph.python import min_cost_flow
    except Exception:
        return _greedy_fallback(request)

    n_orders = len(request.orders)
    n_couriers = len(request.couriers)

    # Node indexing:
    # 0 = source
    # 1..n_orders = orders
    # n_orders+1 .. n_orders+n_couriers = couriers
    # last = sink
    source = 0
    order_offset = 1
    courier_offset = order_offset + n_orders
    sink = courier_offset + n_couriers

    mcf = min_cost_flow.SimpleMinCostFlow()

    # source -> orders (cap=1)
    for i in range(n_orders):
        mcf.add_arc_with_capacity_and_unit_cost(source, order_offset + i, 1, 0)

    # orders -> couriers (cap=1, cost)
    for i in range(n_orders):
        for j in range(n_couriers):
            cost = request.costs[i][j] if i < len(request.costs) and j < len(request.costs[i]) else INF_COST
            if cost >= INF_COST / 2:
                continue
            mcf.add_arc_with_capacity_and_unit_cost(order_offset + i, courier_offset + j, 1, int(cost * SCALE))

    # couriers -> sink (cap=1)
    for j in range(n_couriers):
        mcf.add_arc_with_capacity_and_unit_cost(courier_offset + j, sink, 1, 0)

    mcf.set_node_supply(source, min(n_orders, n_couriers))
    mcf.set_node_supply(sink, -min(n_orders, n_couriers))

    status = mcf.solve()
    if status != mcf.OPTIMAL:
        return SolveResponse(status="partial", solverUsed="MCF", assignments=[])

    assignments: List[Assignment] = []
    for arc in range(mcf.num_arcs()):
        tail = mcf.tail(arc)
        head = mcf.head(arc)
        flow = mcf.flow(arc)
        if flow != 1:
            continue
        if not (order_offset <= tail < courier_offset):
            continue
        if not (courier_offset <= head < sink):
            continue

        oi = tail - order_offset
        cj = head - courier_offset
        raw_cost = mcf.unit_cost(arc) / SCALE
        assignments.append(
            Assignment(
                orderId=request.orders[oi].id,
                courierId=request.couriers[cj].id,
                cost=float(raw_cost),
                etaPickupMin=max(1, int(raw_cost / 2)),
                etaDeliveryMin=max(1, int(raw_cost)),
            )
        )

    return SolveResponse(status="ok", solverUsed="MCF", assignments=assignments)


def _greedy_fallback(request: SolveRequest) -> SolveResponse:
    assignments: List[Assignment] = []
    used_orders = set()
    used_couriers = set()

    candidates = []
    for i, order in enumerate(request.orders):
        for j, courier in enumerate(request.couriers):
            cost = request.costs[i][j] if i < len(request.costs) and j < len(request.costs[i]) else INF_COST
            if cost >= INF_COST / 2:
                continue
            candidates.append((float(cost), order.id, courier.id))

    candidates.sort(key=lambda x: x[0])
    for cost, oid, cid in candidates:
        if oid in used_orders or cid in used_couriers:
            continue
        used_orders.add(oid)
        used_couriers.add(cid)
        assignments.append(Assignment(orderId=oid, courierId=cid, cost=cost))

    return SolveResponse(status="partial", solverUsed="MCF", assignments=assignments)
