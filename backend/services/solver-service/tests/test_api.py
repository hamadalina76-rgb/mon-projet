from fastapi.testclient import TestClient
import time

from main import app


client = TestClient(app)


def test_health():
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_solve_timeout_1ms_returns_partial_or_empty():
    payload = {
        "algorithm": "MCF",
        "timeoutMs": 1,
        "orders": [{"id": 1, "guaranteedDeliveryMinutes": 30}],
        "couriers": [{"id": 10, "vehicleType": "MOTO", "capacity": 2}],
        "costs": [[5.0]],
    }
    res = client.post("/solve", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["status"] in ["ok", "partial"]


def test_solve_invalid_payload_422():
    payload = {"algorithm": "MCF", "couriers": [], "costs": []}
    res = client.post("/solve", json=payload)
    assert res.status_code == 422


def test_solve_20x10_under_2s():
    orders = [{"id": i + 1, "guaranteedDeliveryMinutes": 60} for i in range(20)]
    couriers = [{"id": 100 + j, "vehicleType": "MOTO", "capacity": 2} for j in range(10)]
    costs = [[float(10 + i + j) for j in range(10)] for i in range(20)]

    payload = {
        "algorithm": "MCF",
        "timeoutMs": 2000,
        "orders": orders,
        "couriers": couriers,
        "costs": costs,
    }

    start = time.perf_counter()
    res = client.post("/solve", json=payload)
    elapsed = time.perf_counter() - start

    assert res.status_code == 200
    assert elapsed < 2.0
