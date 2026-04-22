export interface DispatchConfigMeta {
  activeVersion: number;
  optimisticLock: number;
  redisConfigVersion: number;
}

export interface GroupEnvelope<T> {
  meta: DispatchConfigMeta;
  data: T;
}

export type CostComponentKey =
  | 'ETA_TOTAL_ESTIMATED'
  | 'COURIER_TYPE'
  | 'AVAILABILITY'
  | 'ROUTE_ALIGNMENT'
  | 'TOUR_COMPATIBILITY'
  | 'PERFORMANCE_RATING'
  | 'GUARANTEED_DEADLINE'
  | 'RECENT_REFUSAL'
  | 'WORKLOAD_FAIRNESS'
  | 'MERCHANT_KNOWLEDGE'
  | 'VEHICLE_COMPATIBILITY';

export interface ScoringComponentRow {
  key: CostComponentKey;
  enabled: boolean;
  weight: number;
  order: number;
  mandatory?: boolean;
}

export interface GeneralConfigDto {
  intervalSeconds?: number;
  responseTimeoutSeconds?: number;
  lockTtlSeconds?: number;
  preAssignmentFinishWindowSeconds?: number;
  preAssignmentCostPenalty?: number;
  eligibilityOrderWaitingThresholdSeconds?: number;
  eligibilityInternalShortageThreshold?: number;
}

export interface ScoringConfigDto {
  components?: ScoringComponentRow[];
  legacyComponents?: ScoringComponentRow[];
}

export interface InternalExternalConfigDto {
  internalPriorityOverExternal?: boolean;
  externalScoreDegradationThreshold?: number;
  partnerDelayThresholdMinutes?: number;
  partnerDelaySchedulerIntervalSeconds?: number;
  shiftWindowStartLocal?: string;
  shiftWindowEndLocal?: string;
}

export interface BundlingConfigDto {
  enabled?: boolean;
  maxBundleSize?: number;
  dropoffRadiusMeters?: number;
  merchantRadiusMeters?: number;
  timeWindowSeconds?: number;
  internalOnly?: boolean;
  averageSpeedKmh?: number;
  respectUrgentFlag?: boolean;
  assignmentStrategy?: string;
  compensationEnabled?: boolean;
  compensationThresholdMinutes?: number;
  compensationPercentOfTotal?: number;
  compensationCurrency?: string;
}

export interface ExclusivityCellDto {
  zoneId: number;
  commerceType: string;
  allowed: boolean;
}

export interface ExclusivityPartnerOverrideDto {
  partnerId: number;
  zoneId: number;
  commerceType: string;
  allowed: boolean;
}

export interface ExclusivityConfigDto {
  cells?: ExclusivityCellDto[];
  partnerOverrides?: ExclusivityPartnerOverrideDto[];
}

export interface SimulateRequest {
  zoneId?: number | null;
  scoringOverlay?: ScoringConfigDto;
  generalOverlay?: GeneralConfigDto;
}

export interface ComponentScoreDto {
  key: CostComponentKey;
  weightedCost: number;
  eliminated: boolean;
  detail?: string | null;
}

export interface SimulatePairResultDto {
  orderId: number;
  courierId: number;
  totalCost: number;
  eliminated: boolean;
  eliminationReason?: string | null;
  components?: ComponentScoreDto[];
}

export interface SimulateResponse {
  simulationRunId: number;
  status: string;
  pairResults: SimulatePairResultDto[];
  note?: string | null;
}

export interface ReplayResponse {
  cycleId: string;
  replaySessionId: number;
  pairResults: SimulatePairResultDto[];
}
