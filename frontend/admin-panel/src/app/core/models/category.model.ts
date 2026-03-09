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
