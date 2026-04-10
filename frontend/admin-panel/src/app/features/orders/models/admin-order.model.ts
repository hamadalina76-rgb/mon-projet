export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | 'PICKED_UP'
  | 'IN_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export type PaymentMethod = 'CASH' | 'CARD' | 'WALLET' | 'CARD_ON_DELIVERY';
export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';

export interface AdminOrder {
  id: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  customerPhone: string;
  partnerId: number;
  partnerName: string;
  courierId: number | null;
  courierName: string | null;
  courierPhone: string | null;
  status: OrderStatus;
  orderType: 'DELIVERY' | 'PICKUP';
  subtotal: number;
  deliveryFee: number;
  serviceFee: number;
  tax: number;
  discount: number;
  promoCode: string | null;
  tip: number;
  total: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  deliveryCity: string | null;
  deliveryAddress: string | null;
  deliveryLatitude: number | null;
  deliveryLongitude: number | null;
  customerNotes: string | null;
  cancellationReason: string | null;
  cancelledBy: string | null;
  estimatedDeliveryTime: string | null;
  orderTime: string;
  createdAt: string;
  updatedAt: string;
  itemCount: number;
}

export interface OrderPageResponse {
  content: AdminOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface OrderFilters {
  status?: OrderStatus | '';
  search?: string;
  partnerId?: number | null;
  courierId?: number | null;
  city?: string;
  startDate?: string;
  endDate?: string;
  paymentMethod?: PaymentMethod | '';
  amountMin?: number | null;
  amountMax?: number | null;
}

export const ORDER_STATUS_CONFIG: Record<OrderStatus, { color: string; bg: string; icon: string }> = {
  PENDING:          { color: '#D97706', bg: 'rgba(245,158,11,.1)',  icon: 'schedule' },
  CONFIRMED:        { color: '#2563EB', bg: 'rgba(59,130,246,.1)',  icon: 'thumb_up' },
  PREPARING:        { color: '#7C3AED', bg: 'rgba(139,92,246,.1)',  icon: 'restaurant' },
  READY_FOR_PICKUP: { color: '#0891B2', bg: 'rgba(6,182,212,.1)',   icon: 'inventory_2' },
  PICKED_UP:        { color: '#0D9488', bg: 'rgba(20,184,166,.1)',  icon: 'local_shipping' },
  IN_DELIVERY:      { color: '#CA8A04', bg: 'rgba(234,179,8,.1)',   icon: 'delivery_dining' },
  DELIVERED:        { color: '#059669', bg: 'rgba(16,185,129,.1)',   icon: 'check_circle' },
  CANCELLED:        { color: '#DC2626', bg: 'rgba(239,68,68,.1)',   icon: 'cancel' },
};
