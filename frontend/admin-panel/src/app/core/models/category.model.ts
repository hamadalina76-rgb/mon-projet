/**
 * Category models aligned with partner-service CategoryDTO and requests.
 */

export enum CategoryBusinessType {
  RESTAURANT = 'RESTAURANT',
  GROCERY = 'GROCERY',
  PHARMACY = 'PHARMACY',
  OTHER = 'OTHER',
}

export interface Category {
  id: number;
  nameI18n: Record<string, string>;
  slug: string | null;
  description: string | null;
  icon: string | null;
  image: string | null;
  parentId: number | null;
  depth: number | null;
  displayOrder: number;
  isActive: boolean;
  isFeatured: boolean;
  categoryBusinessType: CategoryBusinessType;
  categoryType: string | null;
  backgroundColor: string | null;
  textColor: string | null;
  partnerCount: number | null;
  productCount: number | null;
  createdAt: string | null;
  updatedAt: string | null;
  createdBy: number | null;
}

export interface CreateCategoryRequest {
  nameI18n: Record<string, string>;
  description?: string;
  icon?: string;
  image?: string;
  parentId?: number;
  displayOrder: number;
  isFeatured: boolean;
  categoryBusinessType: CategoryBusinessType;
  categoryType?: string;
  backgroundColor?: string;
  textColor?: string;
}

export interface UpdateCategoryRequest {
  nameI18n: Record<string, string>;
  description?: string;
  icon?: string;
  image?: string;
  parentId?: number;
  displayOrder?: number;
  isFeatured?: boolean;
  isActive?: boolean;
  categoryBusinessType: CategoryBusinessType;
  categoryType?: string;
  backgroundColor?: string;
  textColor?: string;
}

// ─── Stats & Audit ────────────────────────────────────────────────────────────

export interface DailyOrderStat {
  day: string;
  orders: number;
}

export interface CategoryStats {
  categoryId: number;
  categoryName: string;
  productCount: number;
  partnerCount: number;
  ordersLast30Days: number;
  orderTrendPercent: number;
  partnerTrendPercent: number;
  productTrendPercent: number;
  topCategory: boolean;
  topCategoryThreshold: number;
  dailyOrders: DailyOrderStat[];
}

export interface AuditLogEntry {
  id: number;
  adminId: number;
  adminName: string;
  adminRole: string;
  action: string;
  timestamp: string;
  changesBefore: string;
  changesAfter: string;
  status: string;
}
