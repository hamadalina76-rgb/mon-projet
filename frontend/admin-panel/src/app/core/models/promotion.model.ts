// core/models/promotion.model.ts

export type PromotionType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREE_DELIVERY';
export type PromotionStatus = 'SCHEDULED' | 'ACTIVE' | 'INACTIVE' | 'EXPIRED';

export type RuleType =
  | 'MIN_ORDER'
  | 'PARTNER_RESTRICTION'
  | 'CATEGORY_RESTRICTION'
  | 'FIRST_ORDER'
  | 'USER_QUOTA'
  | 'SPECIFIC_DAY'
  | 'TIME_RANGE'
  | 'MIN_ITEMS';

export interface PromotionRule {
  id?: number;
  ruleType: RuleType;
  operator?: string;
  targetValue?: string;
}

export interface Promotion {
  id: number;
  code: string;
  name: string;
  description?: string;
  type: PromotionType;
  value: number;
  maximumDiscount?: number;
  minimumOrder?: number;
  usageLimitTotal?: number;
  usageCount: number;
  usageLimitPerUser?: number;
  startDate?: string;
  endDate?: string;
  isActive: boolean;
  status: PromotionStatus;
  applicablePartnerIds?: number[];
  applicableCategoryIds?: number[];
  applicableZoneIds?: number[];
  firstOrderOnly: boolean;
  rules?: PromotionRule[];
  createdAt: string;
  updatedAt: string;
}

export interface PromotionPageResponse {
  content: Promotion[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface CreatePromotionRequest {
  code: string;
  name: string;
  description?: string;
  type: PromotionType;
  value: number;
  maximumDiscount?: number;
  minimumOrder?: number;
  usageLimitTotal?: number;
  usageLimitPerUser?: number;
  startDate?: string;
  endDate?: string;
  applicablePartnerIds?: number[];
  applicableCategoryIds?: number[];
  applicableZoneIds?: number[];
  firstOrderOnly?: boolean;
  status?: PromotionStatus;
  rules?: PromotionRule[];
}

export interface UpdatePromotionRequest {
  name?: string;
  description?: string;
  type?: PromotionType;
  value?: number;
  maximumDiscount?: number;
  minimumOrder?: number;
  usageLimitTotal?: number;
  usageLimitPerUser?: number;
  startDate?: string;
  endDate?: string;
  applicablePartnerIds?: number[];
  applicableCategoryIds?: number[];
  applicableZoneIds?: number[];
  firstOrderOnly?: boolean;
  status?: PromotionStatus;
  rules?: PromotionRule[];
}

export interface ValidatePromotionRequest {
  code: string;
  userId: number;
  orderSubtotal: number;
  deliveryFee?: number;
  partnerId?: number;
  categoryIds?: number[];
  productIds?: number[];
  itemCount?: number;
}

export interface ValidatePromotionResponse {
  isValid: boolean;
  discountAmount: number;
  discountType: PromotionType;
  originalSubtotal: number;
  newSubtotal: number;
  deliveryFee: number;
  total: number;
  message: string;
}

export interface ApplyPromotionRequest {
  code: string;
  userId: number;
  orderId: number;
  orderSubtotal: number;
  deliveryFee?: number;
  partnerId?: number;
  categoryIds?: number[];
  itemCount?: number;
}

export interface RevokePromotionRequest {
  code: string;
  userId: number;
  orderId: number;
}

export interface UsageLogEntry {
  userId: number;
  orderId: number;
  discountAmount: number;
  status: 'APPLIED' | 'REVOKED';
  createdAt: string;
}

export interface PromotionAnalytics {
  promotionId: number;
  code: string;
  name: string;
  totalApplied: number;
  totalRevoked: number;
  netUsage: number;
  totalDiscountGiven: number;
  uniqueUsers: number;
  avgDiscountPerUse: number;
  usageLimit?: number;
  usageRatePct: number;
  usageLogs: UsageLogEntry[];
}

/** @deprecated use Promotion */
export type PromotionDto = Promotion;

export interface PromotionDetailResponse {
  promotion: Promotion;
  totalApplied: number;
  totalRevoked: number;
  totalRevenue: number;
  uniqueUsers: number;
}

export interface PromotionStatistics {
  activeCount: number;
  inactiveCount: number;
  expiredCount: number;
  totalPromotions: number;
  totalUsages: number;
  totalDiscount: number;
}
