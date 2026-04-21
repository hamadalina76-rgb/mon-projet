export interface DispatchDashboardKpis {
  firstCycleDispatchRate: number;
  averageAssignmentDelaySeconds: number;
  deliveriesPerCourierPerHour: number;
  bundlingRate: number;
  failureRate: number;
  onTimeRate: number;
}

export interface DispatchZoneMetrics {
  zoneId: number;
  zoneName?: string | null;
  zoneActive?: boolean | null;
  onlineCouriers: number;
  idleCouriers: number;
  onDeliveryCouriers: number;
  pendingOrders: number;
  averageAssignmentDelaySeconds: number;
}

export interface DispatchCourierPosition {
  courierId: number;
  zoneId: number;
  type: 'INTERNAL' | 'EXTERNAL';
  status: 'IDLE' | 'ON_DELIVERY' | 'PRE_ASSIGNABLE' | string;
  lat: number | null;
  lon: number | null;
  online: boolean;
}

export interface DispatchPendingOrder {
  id: number;
  orderNumber?: string;
  partnerId?: number;
  customerId?: number;
  deliveryAddress?: string;
  createdAt?: string;
  courierId?: number | null;
}

export interface DispatchCycleEvent {
  zoneId: number;
  totalOrders: number;
  availableCouriers: number;
  assignedOrders: number;
  unmatchedOrders: number;
  occurredAt: string;
}

export interface ManualAssignPayload {
  orderId: number;
  courierId: number;
}

export interface ManualBundlePayload {
  zoneId: number;
  orderIds: number[];
}
