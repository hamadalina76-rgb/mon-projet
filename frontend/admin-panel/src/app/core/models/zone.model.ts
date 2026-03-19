export interface Zone {
  id: number;
  name: string;
  description?: string;
  city?: string;
  type: 'DELIVERY' | 'RESTRICTED' | 'PREMIUM';
  boundaryJson?: string;
  deliveryFee?: number;
  minDeliveryTime?: number;
  maxDeliveryTime?: number;
  isActive: boolean;
  radiusKm?: number;
  assignedAt?: string;
}
