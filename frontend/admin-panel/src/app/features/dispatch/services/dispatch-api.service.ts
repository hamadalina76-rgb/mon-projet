import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import {
  DispatchCourierPosition,
  DispatchDashboardKpis,
  DispatchMode,
  DispatchPendingOrder,
  DispatchProposal,
  DispatchZoneMetrics,
  ManualAssignPayload,
  ManualBundlePayload,
  ZoneModeResponse,
} from '../models/dispatch-dashboard.model';
import type {
  BundlingConfigDto,
  ExclusivityConfigDto,
  GeneralConfigDto,
  GroupEnvelope,
  InternalExternalConfigDto,
  ReplayResponse,
  ScoringConfigDto,
  SimulateRequest,
  SimulateResponse,
} from '../models/dispatch-config.model';

@Injectable({ providedIn: 'root' })
export class DispatchApiService {
  private api = inject(ApiService);

  getKpis(): Observable<DispatchDashboardKpis> {
    return this.api.get<DispatchDashboardKpis>('dispatch/dashboard/kpis');
  }

  getZoneMetrics(zoneId: number): Observable<DispatchZoneMetrics> {
    return this.api.get<DispatchZoneMetrics>(`dispatch/zones/${zoneId}/metrics`);
  }

  getZoneMode(zoneId: number): Observable<ZoneModeResponse> {
    return this.api.get<ZoneModeResponse>(`dispatch/zones/${zoneId}/mode`);
  }

  putZoneMode(zoneId: number, mode: DispatchMode): Observable<ZoneModeResponse> {
    return this.api.put<ZoneModeResponse>(`dispatch/zones/${zoneId}/mode`, { mode });
  }

  getZoneProposals(zoneId: number): Observable<DispatchProposal[]> {
    return this.api.get<DispatchProposal[]>(`dispatch/zones/${zoneId}/proposals`);
  }

  approveProposal(orderId: number): Observable<Record<string, unknown>> {
    return this.api.post<Record<string, unknown>>(`dispatch/proposals/${orderId}/approve`, {});
  }

  rejectProposal(orderId: number): Observable<Record<string, unknown>> {
    return this.api.post<Record<string, unknown>>(`dispatch/proposals/${orderId}/reject`, {});
  }

  getCourierPositions(): Observable<DispatchCourierPosition[]> {
    return this.api.get<DispatchCourierPosition[]>('dispatch/couriers/positions');
  }

  getPendingOrders(): Observable<DispatchPendingOrder[]> {
    return this.api.get<DispatchPendingOrder[]>('dispatch/orders/pending');
  }

  manualAssign(payload: ManualAssignPayload): Observable<unknown> {
    return this.api.post('dispatch/manual-assign', payload);
  }

  manualBundle(payload: ManualBundlePayload): Observable<unknown> {
    return this.api.post('dispatch/manual-bundle', payload);
  }

  updateZoneStatus(zoneId: number, active: boolean): Observable<unknown> {
    return this.api.put(`dispatch/zones/${zoneId}/status`, { active });
  }

  getDispatchConfigMeta() {
    return this.api.get<{ activeVersion: number; optimisticLock: number; redisConfigVersion: number }>(
      'dispatch/dispatch-config/meta'
    );
  }

  getDispatchConfigGeneral(): Observable<GroupEnvelope<GeneralConfigDto>> {
    return this.api.get<GroupEnvelope<GeneralConfigDto>>('dispatch/dispatch-config/general');
  }

  putDispatchConfigGeneral(body: { baseVersion: number; data: GeneralConfigDto }) {
    return this.api.put<GroupEnvelope<GeneralConfigDto>>('dispatch/dispatch-config/general', body);
  }

  getDispatchConfigScoring(): Observable<GroupEnvelope<ScoringConfigDto>> {
    return this.api.get<GroupEnvelope<ScoringConfigDto>>('dispatch/dispatch-config/scoring');
  }

  putDispatchConfigScoring(body: { baseVersion: number; data: ScoringConfigDto }) {
    return this.api.put<GroupEnvelope<ScoringConfigDto>>('dispatch/dispatch-config/scoring', body);
  }

  getDispatchConfigInternalExternal(): Observable<GroupEnvelope<InternalExternalConfigDto>> {
    return this.api.get<GroupEnvelope<InternalExternalConfigDto>>('dispatch/dispatch-config/internal-external');
  }

  putDispatchConfigInternalExternal(body: { baseVersion: number; data: InternalExternalConfigDto }) {
    return this.api.put<GroupEnvelope<InternalExternalConfigDto>>(
      'dispatch/dispatch-config/internal-external',
      body
    );
  }

  getDispatchConfigBundling(): Observable<GroupEnvelope<BundlingConfigDto>> {
    return this.api.get<GroupEnvelope<BundlingConfigDto>>('dispatch/dispatch-config/bundling');
  }

  putDispatchConfigBundling(body: { baseVersion: number; data: BundlingConfigDto }) {
    return this.api.put<GroupEnvelope<BundlingConfigDto>>('dispatch/dispatch-config/bundling', body);
  }

  getDispatchConfigExclusivity(): Observable<GroupEnvelope<ExclusivityConfigDto>> {
    return this.api.get<GroupEnvelope<ExclusivityConfigDto>>('dispatch/dispatch-config/exclusivity');
  }

  putDispatchConfigExclusivity(body: { baseVersion: number; data: ExclusivityConfigDto }) {
    return this.api.put<GroupEnvelope<ExclusivityConfigDto>>('dispatch/dispatch-config/exclusivity', body);
  }

  simulateDispatch(body: SimulateRequest): Observable<SimulateResponse> {
    return this.api.post<SimulateResponse>('dispatch/dispatch-config/simulate', body);
  }

  replayDispatch(cycleId: string): Observable<ReplayResponse> {
    return this.api.get<ReplayResponse>(`dispatch/dispatch-config/replay/${encodeURIComponent(cycleId)}`);
  }

  exportDispatchConfigAudit(fromUtcIso: string, toUtcExclusiveIso: string) {
    const params = new HttpParams().set('fromUtc', fromUtcIso).set('toUtcExclusive', toUtcExclusiveIso);
    return this.api.get<Blob>('dispatch/dispatch-config/audit/export', params, { responseType: 'blob' });
  }
}
