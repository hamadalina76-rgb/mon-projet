// src/app/features/orders/models/order.model.ts
export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'PICKED_UP'
  | 'DELIVERED'
  | 'CANCELLED';

export type OrderType = 'DELIVERY' | 'PICKUP' | 'DINE_IN';
export type PaymentMethod = 'CASH' | 'CARD' | 'ONLINE' | string;
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED' | string;

export interface OrderItemOption {
  name: string;
  value: string;
  price: number;
}

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  price: number; // unitPrice × quantity
  options?: OrderItemOption[];
  notes?: string;
  /** Minutes (fiche produit au moment de la commande), si exposées par l’API. */
  preparationTimeMin?: number;
}

/** Max des temps produit par ligne (file cuisine) ; 15 min si aucune donnée. */
export function suggestedPrepMinutesFromItems(items: OrderItem[]): number {
  const mins = (items ?? [])
    .map((it) => it.preparationTimeMin)
    .filter((m): m is number => m != null && m > 0);
  if (mins.length === 0) return 15;
  return Math.max(...mins);
}

export function hasProductPrepOnItems(items: OrderItem[]): boolean {
  return (items ?? []).some((it) => it.preparationTimeMin != null && it.preparationTimeMin > 0);
}

export interface DeliveryAddress {
  street?: string;
  city?: string;
  building?: string;
  floor?: string;
  notes?: string;
  latitude?: number;
  longitude?: number;
}

export interface StatusHistoryEntry {
  status: OrderStatus;
  timestamp: string;
  actorType?: string;
  notes?: string;
  /** Minutes annoncées par le partenaire à l'acceptation (ligne d'historique concernée). */
  estimatedPrepMinutes?: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  orderType?: OrderType;

  customer: {
    id: string;
    name: string;
    phone: string;
    email?: string;
  };

  deliveryAddress?: DeliveryAddress;

  items: OrderItem[];

  subtotal: number;
  deliveryFee: number;
  serviceFee?: number;
  discount?: number;
  /** TVA (montant), exposé par l’API order-service. */
  tax?: number;
  total: number;

  /** Nom du livreur si assigné (API). */
  courierName?: string;

  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;

  prepTime?: number;
  /** Max des temps produit (minutes), renseigné par l’API à la création. */
  suggestedPreparationMinutes?: number;
  notes?: string;

  statusHistory?: StatusHistoryEntry[];

  createdAt: Date | string;
  confirmedAt?: Date | string;
  preparingAt?: Date | string;
  readyAt?: Date | string;
  deliveredAt?: Date | string;
  cancelledAt?: Date | string;
  cancelReason?: string;

  // Computed helpers (may come from API)
  customerName?: string;
  customerPhone?: string;
  partnerId?: string | number;
}

export interface OrderFilters {
  status: OrderStatus | 'all';
  search: string;
}
