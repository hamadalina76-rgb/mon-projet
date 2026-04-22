from __future__ import annotations

from fastapi import FastAPI

from schemas import (
    BundleRouteRequest,
    BundleRouteResponse,
    SolveRequest,
    SolveResponse,
)
from solvers import solve
from solvers.tsp import optimize_bundle_route

app = FastAPI(title="speedline-solver-service", version="1.0.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/solve", response_model=SolveResponse)
def solve_endpoint(request: SolveRequest) -> SolveResponse:
    return solve(request)


@app.post("/optimize-route", response_model=BundleRouteResponse)
def optimize_route_endpoint(request: BundleRouteRequest) -> BundleRouteResponse:
    return optimize_bundle_route(request)
