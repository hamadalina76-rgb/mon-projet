export enum ZoneType {
  DELIVERY = 'DELIVERY',
  RESTRICTED = 'RESTRICTED',
  PREMIUM = 'PREMIUM',
  EXPRESS = 'EXPRESS'
}

export type WeekDay =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface ZoneInternalCourierAssignment {
  courierId: number;
  workDays: WeekDay[];
  startTime?: string;
  endTime?: string;
}

export interface Zone {
  id: number;
  name: string;
  description?: string;
  city: string;
  type: ZoneType;
  boundaryJson: string; // Format: [[lat,lon],[lat,lon],...]
  deliveryFee?: number;
  serviceFee?: number;
  minDeliveryTime?: number;
  maxDeliveryTime?: number;
  isActive: boolean;
  /**
   * Approximate delivery radius for this zone (in kilometers).
   */
  radiusKm?: number;
  minActiveInternalCouriers?: number;
  maxSimultaneousOrders?: number;
  interZoneExtensionRadiusKm?: number;
  maxInterZoneReassignmentDelayMinutes?: number;
  internalCourierAssignments?: ZoneInternalCourierAssignment[];
  internalAssignedCouriersCount?: number;
  externalAssignedCouriersCount?: number;
  createdAt?: string;
  updatedAt?: string;
  partnersCount?: number;
  areaKm2?: number;
  center?: [number, number]; // [latitude, longitude]
}

export interface ZoneCreateRequest {
  name: string;
  description?: string;
  city: string;
  type: ZoneType;
  boundaryJson: string;
  deliveryFee?: number;
  serviceFee?: number;
  minDeliveryTime?: number;
  maxDeliveryTime?: number;
  isActive?: boolean;
  radiusKm?: number;
  minActiveInternalCouriers?: number;
  maxSimultaneousOrders?: number;
  interZoneExtensionRadiusKm?: number;
  maxInterZoneReassignmentDelayMinutes?: number;
  internalCourierAssignments?: ZoneInternalCourierAssignment[];
}

export interface ZoneUpdateRequest {
  name?: string;
  description?: string;
  city?: string;
  type?: ZoneType;
  boundaryJson?: string;
  deliveryFee?: number;
  serviceFee?: number;
  minDeliveryTime?: number;
  maxDeliveryTime?: number;
  isActive?: boolean;
  radiusKm?: number;
  minActiveInternalCouriers?: number;
  maxSimultaneousOrders?: number;
  interZoneExtensionRadiusKm?: number;
  maxInterZoneReassignmentDelayMinutes?: number;
  internalCourierAssignments?: ZoneInternalCourierAssignment[];
}

export interface PartnerInZone {
  id: number;
  businessName: string;
  address?: string;
  city?: string;
  latitude: number;
  longitude: number;
  isActive: boolean;
  acceptsOrders: boolean;
}
