// Partner Models and Interfaces

export type PartnerType = 'restaurant' | 'grocery' | 'pharmacy' | 'retail' | 'florist' | 'other';
export type LegalStatus = 'auto-entrepreneur' | 'sarl' | 'sas' | 'association' | 'other';
export type CommissionModel = 'standard' | 'premium' | 'subscription' | 'hybrid';
export type PartnerStatus = 'pending' | 'in_review' | 'documents_missing' | 'approved' | 'rejected' | 'active' | 'suspended';
export type DeliveryFeesType = 'customer' | 'shared' | 'partner';

export interface OpeningHours {
  day: string;
  isClosed: boolean;
  slots: {
    open: string;
    close: string;
  }[];
}

/** Exception d'horaires : jour férié ou fermeture exceptionnelle */
export interface ScheduleException {
  date: string; // YYYY-MM-DD
  label?: string;
  type: 'CLOSED';
}

export interface Documents {
  kbis?: File | string;
  idCard?: File | string;
  insurance?: File | string;
  license?: File | string;
  rib?: File | string;
}

export interface RegistrationStep1 {
  partnerType: PartnerType;
  otherType?: string;
  businessName: string;
  brandName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

export interface RegistrationStep2 {
  fullAddress: string;
  postalCode: string;
  city: string;
  country: string;
  latitude: number;
  longitude: number;
  addressComplement?: string;
}

export interface RegistrationStep3 {
  legalStatus: LegalStatus;
  siret: string;
  tva?: string;
  legalRepFirstName: string;
  legalRepLastName: string;
  position: string;
  documents: Documents;
}

export interface RegistrationStep4 {
  accountHolderName: string;
  iban: string;
  bic: string;
  bankName: string;
  currency: string;
}

export interface RegistrationStep5 {
  openingHours: OpeningHours[];
  preparationTime: string;
  deliveryRadius: number;
  acceptOnlinePayment: boolean;
  acceptCashPayment: boolean;
}

export interface RegistrationStep6 {
  commissionModel: CommissionModel;
}

export interface RegistrationStep7 {
  deliveryFeesType: DeliveryFeesType;
  deliveryFees?: number;
  variableDeliveryFees?: boolean;
  deliveryFeesRanges?: {
    min: number;
    max: number;
    fee: number;
  }[];
  minimumOrder?: number;
  smallOrderFee?: boolean;
  smallOrderThreshold?: number;
  smallOrderFeeAmount?: number;
}

export interface RegistrationStep8 {
  logo?: File | string;
  coverImage?: File | string;
  photos?: (File | string)[];
  shortDescription: string;
  fullDescription: string;
  tags: string[];
}

export interface PartnerRegistrationData
  extends RegistrationStep1,
    RegistrationStep2,
    RegistrationStep3,
    RegistrationStep4,
    RegistrationStep5,
    RegistrationStep6,
    RegistrationStep7,
    RegistrationStep8 {
  acceptTerms: boolean;
  acceptPrivacy: boolean;
  acceptCommission: boolean;
  certifyInformation: boolean;
  acceptMarketing?: boolean;
}

export interface Partner {
  id: string;
  businessName: string;
  brandName: string;
  partnerType: PartnerType;
  email: string;
  phone: string;
  status: PartnerStatus;
  commissionModel: CommissionModel;
  commissionRate: number;
  subscriptionFee?: number;
  isActive: boolean;
  rating: number;
  totalOrders: number;
  createdAt: Date;
  updatedAt: Date;
  address: RegistrationStep2;
  legalInfo: RegistrationStep3;
  bankInfo: RegistrationStep4;
  settings: RegistrationStep5 & RegistrationStep7;
  branding: RegistrationStep8;
}

export interface LoginCredentials {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface AdminReviewData {
  partnerId: string;
  status: PartnerStatus;
  notes?: string;
  documentsValidated?: boolean;
  informationVerified?: boolean;
  deliveryZoneAcceptable?: boolean;
  legalCompliance?: boolean;
  rejectionReason?: string;
}

/** DTO retourné par le backend (GET /partners/{id}, GET /partners/by-user/{userId}) */
export interface PartnerProfileDto {
  id: number;
  userId?: number;
  businessName: string;
  brandName?: string;
  slug?: string;
  type?: string;
  description?: string;
  shortDescription?: string;
  logo?: string;
  coverImage?: string;
  phoneNumber?: string;
  email?: string;
  legalStatus?: string;
  tva?: string;
  legalRepFirstName?: string;
  legalRepLastName?: string;
  position?: string;
  accountHolderName?: string;
  iban?: string;
  bankName?: string;
  currency?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  state?: string;
  country?: string;
  latitude?: number;
  longitude?: number;
  deliveryRadius?: number;
  status?: string;
  isActive?: boolean;
  acceptsOrders?: boolean;
  /** Établissement actuellement ouvert (accepte les commandes) – aligné backend */
  isCurrentlyOpen?: boolean;
  isVerified?: boolean;
  isPremium?: boolean;
  isFeatured?: boolean;
  preparationTime?: number;
  deliveryFee?: number;
  minimumOrder?: number;
  freeDeliveryThreshold?: number;
  acceptOnlinePayment?: boolean;
  acceptCashPayment?: boolean;
  rating?: number;
  totalRatings?: number;
  totalOrders?: number;
  kbisUrl?: string;
  idCardUrl?: string;
  insuranceUrl?: string;
  ribUrl?: string;
  photosJson?: string;
  tags?: string[];
  categoryIds?: number[];
  openingHoursDisplay?: string;
  scheduleExceptionsDisplay?: string; // JSON: ScheduleException[]
  createdAt?: string;
}
