export type DispatchMode = 'AUTO' | 'SEMI_AUTO' | 'MANUAL';

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
  mode?: DispatchMode;
  runtimeModeOverride?: boolean;
  onlineCouriers: number;
  idleCouriers: number;
  onDeliveryCouriers: number;
  pendingOrders: number;
  averageAssignmentDelaySeconds: number;
}

export interface ZoneModeResponse {
  zoneId: number;
  mode: DispatchMode;
  runtimeOverride: boolean;
}

export interface DispatchProposal {
  zoneId: number;
  orderId: number;
  courierId: number;
  bundleId: number | null;
  cost: number | null;
  etaPickupMin: number | null;
  etaDeliveryMin: number | null;
  createdAt: string;
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

/** Objet adresse renvoyé par order-service (ou chaîne côté anciennes API) */
export type DeliveryAddressValue =
  | string
  | {
      formattedAddress?: string;
      fullAddress?: string;
      line1?: string;
      street?: string;
      line2?: string;
      city?: string;
      postalCode?: string;
      state?: string;
      country?: string;
      [key: string]: unknown;
    }
  | null;

export type DispatchPendingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | string;

export interface DispatchPendingOrder {
  id: number;
  orderNumber?: string;
  partnerId?: number;
  customerId?: number;
  deliveryAddress?: DeliveryAddressValue;
  createdAt?: string;
  courierId?: number | null;
  status?: DispatchPendingStatus;
  statusLabel?: string;
}

export interface DispatchCycleEvent {
  zoneId: number;
  totalOrders: number;
  availableCouriers: number;
  assignedOrders: number;
  unmatchedOrders: number;
  occurredAt: string;
}

export interface DispatchProposalResolvedEvent {
  zoneId: number;
  orderId: number;
  status: string;
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
