// src/app/features/orders/models/order.model.ts
export interface Order {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  customer: {
    id: string;
    name: string;
    phone: string;
  };
  items: OrderItem[];
  subtotal: number;
  deliveryFee: number;
  total: number;
  paymentMethod: string;
  paymentStatus: string;
  prepTime?: number;
  notes?: string;
  createdAt: Date;
  confirmedAt?: Date;
  readyAt?: Date;
}

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  price: number;
  options?: OrderItemOption[];
  notes?: string;
}

export interface OrderItemOption {
  name: string;
  value: string;
  price: number;
}

export type OrderStatus = 
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'PICKED_UP'
  | 'CANCELLED';
