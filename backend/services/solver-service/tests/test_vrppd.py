from schemas import SolveRequest, Order, Courier
from solvers.vrppd import solve_vrppd


def test_8_orders_time_windows_respected():
    orders = [Order(id=i + 1, guaranteedDeliveryMinutes=30) for i in range(8)]
    couriers = [Courier(id=10, vehicleType="MOTO", capacity=2), Courier(id=11, vehicleType="MOTOTRICYCLE", capacity=4)]

    costs = []
    for i in range(8):
        costs.append([10.0 + i, 12.0 + i])

    req = SolveRequest(algorithm="VRPPD", orders=orders, couriers=couriers, costs=costs, timeoutMs=2000)
    res = solve_vrppd(req)

    assert res.solverUsed == "VRPPD"
    for a in res.assignments:
        assert a.etaDeliveryMin <= 30


def test_vehicle_capacity_enforced():
    orders = [Order(id=i + 1, guaranteedDeliveryMinutes=60) for i in range(5)]
    couriers = [Courier(id=20, vehicleType="MOTO", capacity=2), Courier(id=21, vehicleType="MOTOTRICYCLE", capacity=4)]
    costs = [[10.0, 10.0] for _ in range(5)]

    req = SolveRequest(algorithm="VRPPD", orders=orders, couriers=couriers, costs=costs, timeoutMs=2000)
    res = solve_vrppd(req)

    by_courier = {}
    for a in res.assignments:
        by_courier[a.courierId] = by_courier.get(a.courierId, 0) + 1

    assert by_courier.get(20, 0) <= 2
    assert by_courier.get(21, 0) <= 4
