from __future__ import annotations

from schemas import SolveRequest, SolveResponse
from solvers.mcf import solve_mcf
from solvers.vrppd import solve_vrppd


def select_solver(request: SolveRequest):
    if request.algorithm == "VRPPD":
        return solve_vrppd
    return solve_mcf


def solve(request: SolveRequest) -> SolveResponse:
    return select_solver(request)(request)
