from __future__ import annotations

from fastapi import FastAPI

from schemas import SolveRequest, SolveResponse
from solvers import solve

app = FastAPI(title="speedline-solver-service", version="1.0.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/solve", response_model=SolveResponse)
def solve_endpoint(request: SolveRequest) -> SolveResponse:
    return solve(request)
