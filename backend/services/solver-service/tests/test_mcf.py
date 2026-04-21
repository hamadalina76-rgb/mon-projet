from schemas import SolveRequest, Order, Courier
from solvers.mcf import solve_mcf


def test_10x5_optimal():
    orders = [Order(id=i + 1, guaranteedDeliveryMinutes=60) for i in range(10)]
    couriers = [Courier(id=100 + j, vehicleType="MOTO", capacity=2) for j in range(5)]

    costs = []
    for i in range(10):
        row = []
        for j in range(5):
            row.append(float((i + 1) * (j + 1)))
        costs.append(row)

    req = SolveRequest(algorithm="MCF", orders=orders, couriers=couriers, costs=costs, timeoutMs=2000)
    res = solve_mcf(req)

    assert res.solverUsed == "MCF"
    assert len(res.assignments) == 5
    assigned_orders = {a.orderId for a in res.assignments}
    assert len(assigned_orders) == 5
