// ==========================================================================
// SpeedLine – Menu feature models
// ==========================================================================

export enum OptionType {
  SINGLE = 'SINGLE',
  MULTIPLE = 'MULTIPLE',
}

// ─── Response interfaces ────────────────────────────────────────────────────

export interface MenuCategory {
  id: number;
  partnerId: number;
  name: string;
  description?: string;
  imageUrl?: string;
  position: number;
  isVisible: boolean;
  productCount?: number;   // fourni par l’API GET /categories
  createdAt?: string;
  updatedAt?: string;
}

export interface Option {
  id: number;
  groupId: number;
  name: string;
  priceModifier: number;
  isDefault: boolean;
  isAvailable: boolean;
  position: number;
}

export interface OptionGroup {
  id: number;
  productId: number;
  name: string;
  type: OptionType;
  isRequired: boolean;
  minSelection: number;
  maxSelection: number;
  position: number;
  options: Option[];
}

export interface Product {
  id: number;
  categoryId?: number;
  partnerId: number;
  name: string;
  description?: string;
  imageUrl?: string;
  image?: string;          // alias used by product-card
  price: number;
  /** Prix avant réduction (affiché barré si discountPercentage > 0). */
  originalPrice?: number;
  /** Pourcentage de réduction (ex. 20 pour -20%). */
  discountPercentage?: number;
  isAvailable: boolean;
  isPopular: boolean;
  preparationTimeMin?: number;
  position: number;
  tags?: string;
  optionGroups: OptionGroup[];
  /** Enrichi par l’API (IN_STOCK, LOW_STOCK, OUT_OF_STOCK). */
  stockStatus?: StockStatus;
  promotionLabel?: string;
  promotionEndDate?: string;  // ISO date
  createdAt?: string;
  updatedAt?: string;
}

export interface CategorySection {
  category: MenuCategory;
  products: Product[];
}

export interface FullMenuResponse {
  partnerId: number;
  partnerName: string;
  categories: CategorySection[];
}

// ─── Request interfaces ─────────────────────────────────────────────────────

export interface ReorderItem {
  id: number;
  position: number;
}

export interface ReorderRequest {
  items: ReorderItem[];
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  imageUrl?: string;
  position?: number;
  isVisible?: boolean;
}

export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  imageUrl?: string;
  position?: number;
  isVisible?: boolean;
}

export interface CreateProductRequest {
  name: string;
  price: number;
  categoryId?: number;
  description?: string;
  imageUrl?: string;
  isAvailable?: boolean;
  isPopular?: boolean;
  preparationTimeMin?: number;
  position?: number;
  tags?: string;
}

export interface UpdateProductRequest {
  name?: string;
  price?: number;
  categoryId?: number;
  description?: string;
  imageUrl?: string;
  isAvailable?: boolean;
  isPopular?: boolean;
  preparationTimeMin?: number;
  position?: number;
  tags?: string;
}

export interface CreateOptionGroupRequest {
  name: string;
  type: OptionType;
  isRequired?: boolean;
  minSelection?: number;
  maxSelection?: number;
  position?: number;
}

export interface UpdateOptionGroupRequest {
  name?: string;
  type?: OptionType;
  isRequired?: boolean;
  minSelection?: number;
  maxSelection?: number;
  position?: number;
}

export interface CreateOptionRequest {
  name: string;
  priceModifier?: number;
  isDefault?: boolean;
  isAvailable?: boolean;
  position?: number;
}

export interface UpdateOptionRequest {
  name?: string;
  priceModifier?: number;
  isDefault?: boolean;
  isAvailable?: boolean;
  position?: number;
}

// ─── Stock ────────────────────────────────────────────────────────────────

export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';

export interface ProductStockDTO {
  productId: number;
  productName: string;
  productImageUrl?: string;
  categoryName?: string;
  quantity: number;
  lowStockThreshold: number;
  isTrackingEnabled: boolean;
  isAvailable: boolean;
  stockStatus: StockStatus;
  updatedAt?: string;
}

export interface UpdateStockRequest {
  quantity?: number;
  lowStockThreshold?: number;
  isTrackingEnabled?: boolean;
}

export interface BulkStockError {
  row: number;
  productId?: number;
  message: string;
}

export interface BulkStockUpdateResult {
  processed: number;
  success: number;
  errors: BulkStockError[];
}

// ─── Import menu CSV (TC-58, TC-59) ────────────────────────────────────────

export interface ImportChangeRow {
  productId: number;
  productName: string;
  field: string;
  oldValue: string;
  newValue: string;
}

export interface ImportParseError {
  row: number;
  message: string;
}

export interface ImportPreviewResponse {
  changes: ImportChangeRow[];
  parseErrors: ImportParseError[];
}

export interface ImportConfirmError {
  row: number;
  productId?: number;
  message: string;
}

export interface ImportConfirmResult {
  processed: number;
  success: number;
  errors: ImportConfirmError[];
}
