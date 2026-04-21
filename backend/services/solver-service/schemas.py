from __future__ import annotations

from typing import List, Literal, Optional
from pydantic import BaseModel, Field


class Order(BaseModel):
    id: int
    partnerId: Optional[int] = None
    customerId: Optional[int] = None

    pickupLat: Optional[float] = None
    pickupLon: Optional[float] = None
    deliveryLat: Optional[float] = None
    deliveryLon: Optional[float] = None

    guaranteedDeliveryMinutes: int = 30
    createdAt: Optional[str] = None

    urgent: bool = False
    largeOrder: bool = False
    scheduled: bool = False


class Courier(BaseModel):
    id: int
    zoneId: Optional[int] = None

    lat: Optional[float] = None
    lon: Optional[float] = None
    vehicleType: str = "BICYCLE"
    capacity: int = 1


class SolveRequest(BaseModel):
    algorithm: Literal["MCF", "VRPPD"] = "MCF"
    orders: List[Order] = Field(min_length=1)
    couriers: List[Courier] = Field(min_length=1)
    costs: List[List[float]] = Field(default_factory=list)
    timeoutMs: int = 2000


class Assignment(BaseModel):
    orderId: int
    courierId: int
    bundleId: Optional[int] = None
    cost: float
    etaPickupMin: Optional[int] = None
    etaDeliveryMin: Optional[int] = None


class SolveResponse(BaseModel):
    status: Literal["ok", "partial"] = "ok"
    solverUsed: Literal["MCF", "VRPPD"] = "MCF"
    assignments: List[Assignment] = Field(default_factory=list)
