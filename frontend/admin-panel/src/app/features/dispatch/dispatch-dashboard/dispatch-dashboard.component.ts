import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';

import { AuthService } from '@core/services/auth.service';
import { PERMISSIONS } from '@core/models/role.model';
import { DispatchApiService } from '../services/dispatch-api.service';
import { DispatchRealtimeService } from '../services/dispatch-realtime.service';
import { DispatchStoreService } from '../services/dispatch-store.service';
import { MapboxService } from '../../zones/services/mapbox.service';
import { ZonesService } from '../../zones/services/zones.service';
import { CouriersService } from '../../users/couriers/services/couriers.service';
import { Zone } from '../../zones/models/zone.model';
import { DispatchCourierPosition, DispatchMode, DispatchProposal } from '../models/dispatch-dashboard.model';
import { StatsCardComponent } from '../../../shared/components/stats-card/stats-card.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

type KpiColor = 'primary' | 'success' | 'warning' | 'danger' | 'info';

@Component({
  selector: 'app-dispatch-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
    MatDividerModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
    TranslateModule,
    StatsCardComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    RouterLink,
  ],
  templateUrl: './dispatch-dashboard.component.html',
  styleUrls: ['./dispatch-dashboard.component.scss'],
})
export class DispatchDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private api = inject(DispatchApiService);
  private realtime = inject(DispatchRealtimeService);
  private store = inject(DispatchStoreService);
  private mapbox = inject(MapboxService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private zonesService = inject(ZonesService);
  private couriersService = inject(CouriersService);
  private auth = inject(AuthService);
  private destroy$ = new Subject<void>();

  readonly canManageDispatchConfig = computed(() =>
    this.auth.hasAnyPermission([PERMISSIONS.DELIVERY_MANAGE])
  );

  /* ── Language / RTL ─────────────────────────────── */
  private langTick = signal(0);
  readonly currentLang = signal(this.translate.currentLang || 'fr');
  readonly isRtl = computed(() => this.currentLang() === 'ar');

  /* ── Global state ────────────────────────────────── */
  readonly state = signal(this.store.snapshot);

  /* ── Loading flags ───────────────────────────────── */
  readonly loadingZones = signal(false);
  readonly loadingMetrics = signal(false);
  readonly loadingCouriers = signal(false);

  /* ── Zone autocomplete ───────────────────────────── */
  readonly zoneSearchCtrl = new FormControl<string | Zone>('');
  private readonly zoneSearchSig = toSignal(this.zoneSearchCtrl.valueChanges, { initialValue: '' as string | Zone });
  readonly allZones = signal<Zone[]>([]);
  readonly selectedZone = signal<Zone | null>(null);

  readonly filteredZones = computed(() => {
    const q = this.zoneSearchSig();
    if (!q || typeof q !== 'string') return this.allZones();
    const lower = q.toLowerCase();
    return this.allZones().filter(
      (z) =>
        z.name.toLowerCase().includes(lower) ||
        (z.city ?? '').toLowerCase().includes(lower),
    );
  });

  get selectedZoneId(): number {
    return this.selectedZone()?.id ?? 1;
  }

  /* ── Courier autocomplete ────────────────────────── */
  readonly courierSearchCtrl = new FormControl<string>('');
  private readonly courierSearchSig = toSignal(this.courierSearchCtrl.valueChanges, { initialValue: '' });
  readonly couriersInZone = signal<any[]>([]);
  readonly selectedCourier = signal<any | null>(null);

  readonly filteredCouriers = computed(() => {
    const q = (this.courierSearchSig() ?? '').toString().toLowerCase();
    return this.couriersInZone().filter((c) => {
      const name = `${c.firstName ?? ''} ${c.lastName ?? ''}`.toLowerCase();
      return !q || name.includes(q) || String(c.id).includes(q);
    });
  });

  /* ── Assignment form ─────────────────────────────── */
  selectedOrderId: number | null = null;
  bundleOrderIds = '';
  zoneActive = true;
  settingMode = false;

  /** Carte: style navigation = trafic temps réel Mapbox */
  readonly showTrafficOnMap = signal(false);
  private mapLayersReady = false;

  /* ── KPI stats (mapped to shared StatsCard) ──────── */
  readonly kpiStats = computed(() => {
    this.langTick();
    const k = this.state().kpis;
    if (!k) return [] as { label: string; value: string; icon: string; color: KpiColor }[];
    const t = (key: string) => this.translate.instant(key);
    return [
      {
        label: t('dispatch.kpiFirstCycle'),
        value: `${k.firstCycleDispatchRate}%`,
        icon: 'loop',
        color: (k.firstCycleDispatchRate < 85 ? 'danger' : 'success') as KpiColor,
      },
      {
        label: t('dispatch.kpiAssignDelay'),
        value: `${k.averageAssignmentDelaySeconds}s`,
        icon: 'timer',
        color: (k.averageAssignmentDelaySeconds > 90 ? 'danger' : 'success') as KpiColor,
      },
      {
        label: t('dispatch.kpiDelPerCourier'),
        value: `${k.deliveriesPerCourierPerHour}`,
        icon: 'delivery_dining',
        color: (k.deliveriesPerCourierPerHour < 3 ? 'warning' : 'info') as KpiColor,
      },
      {
        label: t('dispatch.kpiBundling'),
        value: `${k.bundlingRate}%`,
        icon: 'inventory_2',
        color: (k.bundlingRate < 40 ? 'warning' : 'info') as KpiColor,
      },
      {
        label: t('dispatch.kpiFailure'),
        value: `${k.failureRate}%`,
        icon: 'error_outline',
        color: (k.failureRate > 2 ? 'danger' : 'success') as KpiColor,
      },
      {
        label: t('dispatch.kpiOnTime'),
        value: `${k.onTimeRate}%`,
        icon: 'schedule_send',
        color: (k.onTimeRate < 92 ? 'warning' : 'success') as KpiColor,
      },
    ];
  });

  /* ── Helpers for mat-autocomplete displayWith ─────── */
  readonly displayZone = (val: Zone | string | null): string => {
    if (!val) return '';
    if (typeof val === 'string') return val;
    return val.name;
  };

  readonly displayCourier = (val: any | null): string => {
    if (!val) return '';
    if (typeof val === 'string') return val;
    const fn = val.firstName ?? '';
    const ln = val.lastName ?? '';
    const full = `${fn} ${ln}`.trim();
    return full || val.phoneNumber || `#${val.id}`;
  };

  /* ── Lifecycle ───────────────────────────────────── */
  ngOnInit(): void {
    this.translate.onLangChange.pipe(takeUntil(this.destroy$)).subscribe((e) => {
      this.currentLang.set(e.lang);
      this.langTick.update((n) => n + 1);
      if (this.mapLayersReady) {
        this.refreshMapLayers();
      }
    });

    this.store.state$.pipe(takeUntil(this.destroy$)).subscribe((s) => {
      this.state.set(s);
      const active = s.selectedZoneMetrics?.zoneActive;
      if (active !== undefined && active !== null) {
        this.zoneActive = active;
      }
      if (this.mapLayersReady) {
        this.refreshMapLayers();
      }
    });

    this.loadActiveZones();
    this.loadSnapshot();
    this.realtime.connect();
    this.realtime.courierPositions$.pipe(takeUntil(this.destroy$)).subscribe((positions) => {
      this.store.setCouriers(positions);
    });
    this.realtime.zoneCycle$.pipe(takeUntil(this.destroy$)).subscribe((ev) => {
      if (ev?.zoneId != null && ev.zoneId === this.selectedZoneId) {
        this.store.pushZoneCycleEvent(ev);
      }
    });
    this.realtime.proposalEvents$.pipe(takeUntil(this.destroy$)).subscribe((p) => {
      if (p?.zoneId != null && p.zoneId === this.selectedZoneId) {
        this.store.upsertProposal(p);
      }
    });
    this.realtime.proposalResolved$.pipe(takeUntil(this.destroy$)).subscribe((r) => {
      if (r?.orderId != null && r.zoneId === this.selectedZoneId) {
        this.store.removeProposal(r.orderId);
        this.store.pushProposalResolved(r);
        this.api.getPendingOrders().subscribe((orders) => this.store.setPendingOrders(orders));
      }
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.mapbox.initializeMap('dispatch-map', { center: [10.1815, 36.8065], zoom: 11 });
      this.mapbox.resize();
      this.mapLayersReady = true;
      this.refreshMapLayers();
    }, 0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.realtime.disconnect();
    this.mapbox.destroy();
  }

  /* ── Zone loading ────────────────────────────────── */
  private loadActiveZones(): void {
    this.loadingZones.set(true);
    this.zonesService
      .getActiveZones()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (zones) => {
          this.allZones.set(zones);
          this.loadingZones.set(false);
          if (zones.length > 0 && !this.selectedZone()) {
            const first = zones[0];
            this.selectedZone.set(first);
            this.zoneSearchCtrl.setValue(first, { emitEvent: false });
            this.applyZoneChange(first);
          }
        },
        error: () => this.loadingZones.set(false),
      });
  }

  onZoneSelected(event: MatAutocompleteSelectedEvent): void {
    const zone = event.option.value as Zone;
    this.selectedZone.set(zone);
    this.applyZoneChange(zone);
  }

  onHeaderZoneSelect(zoneId: number | null | undefined): void {
    if (zoneId == null) {
      return;
    }
    const z = this.allZones().find((zz) => zz.id === zoneId);
    if (z) {
      this.selectedZone.set(z);
      this.zoneSearchCtrl.setValue(z, { emitEvent: true });
      this.applyZoneChange(z);
    }
  }

  private applyZoneChange(zone: Zone): void {
    this.store.clearZoneCycleFeed();
    this.store.clearProposalFeed();
    this.realtime.subscribeZoneCycle(zone.id);
    this.loadingMetrics.set(true);
    this.api.getZoneMetrics(zone.id).subscribe({
      next: (m) => {
        this.store.setZoneMetrics(m);
        this.loadingMetrics.set(false);
        if (m.zoneActive !== null && m.zoneActive !== undefined) {
          this.zoneActive = m.zoneActive;
        }
      },
      error: () => this.loadingMetrics.set(false),
    });
    this.api.getZoneProposals(zone.id).subscribe((list) => this.store.setProposals(list));
    this.loadCouriersForZone(zone.id);
    this.api.getPendingOrders().subscribe((orders) => this.store.setPendingOrders(orders));
    if (this.mapLayersReady) {
      setTimeout(() => this.refreshMapLayers(), 0);
    }
  }

  /* ── Courier loading ─────────────────────────────── */
  private loadCouriersForZone(zoneId: number): void {
    this.loadingCouriers.set(true);
    this.couriersService
      .getCouriers(0, 100, undefined, undefined, undefined, zoneId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          const list: any[] = Array.isArray(res) ? res : (res?.content ?? []);
          this.couriersInZone.set(list);
          this.loadingCouriers.set(false);
        },
        error: () => {
          this.couriersInZone.set([]);
          this.loadingCouriers.set(false);
        },
      });
  }

  onCourierSelected(event: MatAutocompleteSelectedEvent): void {
    this.selectedCourier.set(event.option.value);
  }

  clearCourier(): void {
    this.selectedCourier.set(null);
    this.courierSearchCtrl.setValue('');
  }

  /* ── Actions ─────────────────────────────────────── */
  doManualAssign(): void {
    const orderId = this.selectedOrderId;
    const courierId = this.selectedCourier()?.id;
    if (!orderId || !courierId) return;
    this.api.manualAssign({ orderId, courierId }).subscribe({
      next: (res) => {
        const body = res as { success?: boolean; message?: string; error?: string };
        if (body && body['success'] === false) {
          const extra = (body['message'] as string) || (body['error'] as string) || '';
          this.snackBar.open(
            this.translate.instant('dispatch.assignErrorDetail', { detail: extra }),
            undefined,
            { duration: 6000 },
          );
          return;
        }
        this.snackBar.open(this.translate.instant('dispatch.assignSuccess'), undefined, { duration: 2500 });
        this.selectedOrderId = null;
        this.selectedCourier.set(null);
        this.courierSearchCtrl.setValue('');
        this.loadSnapshot();
        if (this.mapLayersReady) {
          this.refreshMapLayers();
        }
      },
      error: () => {
        this.snackBar.open(this.translate.instant('dispatch.assignError'), undefined, { duration: 4000 });
      },
    });
  }

  doManualBundle(): void {
    const ids = this.bundleOrderIds
      .split(',')
      .map((v) => Number(v.trim()))
      .filter((v) => Number.isFinite(v) && v > 0);
    if (!ids.length) return;
    this.api.manualBundle({ zoneId: this.selectedZoneId, orderIds: ids }).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('dispatch.bundleSuccess'), undefined, { duration: 2500 });
        this.bundleOrderIds = '';
      },
      error: () => {
        this.snackBar.open(this.translate.instant('dispatch.bundleError'), undefined, { duration: 4000 });
      },
    });
  }

  setDispatchMode(mode: DispatchMode): void {
    if (this.settingMode) {
      return;
    }
    this.settingMode = true;
    this.api.putZoneMode(this.selectedZoneId, mode).subscribe({
      next: () => {
        this.api.getZoneMetrics(this.selectedZoneId).subscribe((m) => this.store.setZoneMetrics(m));
        this.snackBar.open(this.translate.instant('dispatch.modeUpdated'), undefined, { duration: 2000 });
        this.settingMode = false;
      },
      error: () => {
        this.snackBar.open(this.translate.instant('dispatch.modeUpdateError'), undefined, { duration: 4000 });
        this.settingMode = false;
      },
    });
  }

  isModeActive(m: DispatchMode): boolean {
    return this.state().selectedZoneMetrics?.mode === m;
  }

  approveProposal(p: DispatchProposal): void {
    this.api.approveProposal(p.orderId).subscribe({
      next: (r) => {
        if ((r as { success?: boolean })['success'] === false) {
          this.snackBar.open(this.translate.instant('dispatch.proposalError'), undefined, { duration: 3000 });
          return;
        }
        this.store.removeProposal(p.orderId);
        this.api.getZoneProposals(this.selectedZoneId).subscribe((list) => this.store.setProposals(list));
        this.api.getPendingOrders().subscribe((orders) => this.store.setPendingOrders(orders));
        this.snackBar.open(this.translate.instant('dispatch.proposalApproved'), undefined, { duration: 2000 });
      },
      error: () =>
        this.snackBar.open(this.translate.instant('dispatch.proposalError'), undefined, { duration: 3000 }),
    });
  }

  rejectProposal(p: DispatchProposal): void {
    this.api.rejectProposal(p.orderId).subscribe({
      next: (r) => {
        if ((r as { success?: boolean })['success'] === false) {
          this.snackBar.open(this.translate.instant('dispatch.proposalError'), undefined, { duration: 3000 });
          return;
        }
        this.store.removeProposal(p.orderId);
        this.api.getZoneProposals(this.selectedZoneId).subscribe((list) => this.store.setProposals(list));
        this.snackBar.open(this.translate.instant('dispatch.proposalRejected'), undefined, { duration: 2000 });
      },
      error: () =>
        this.snackBar.open(this.translate.instant('dispatch.proposalError'), undefined, { duration: 3000 }),
    });
  }

  onTrafficToggle(checked: boolean): void {
    this.showTrafficOnMap.set(checked);
    this.mapbox.setStyleWithTraffic(checked, () => {
      this.refreshMapLayers();
    });
  }

  onZoneStatusToggle(active: boolean): void {
    const previous = this.zoneActive;
    this.zoneActive = active;
    this.api.updateZoneStatus(this.selectedZoneId, active).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('dispatch.zoneUpdated'), undefined, { duration: 2500 });
        this.api.getZoneMetrics(this.selectedZoneId).subscribe((m) => this.store.setZoneMetrics(m));
      },
      error: () => {
        this.zoneActive = previous;
        this.snackBar.open(this.translate.instant('dispatch.zoneUpdateError'), undefined, { duration: 5000 });
      },
    });
  }

  /* ── Helpers ─────────────────────────────────────── */
  private loadSnapshot(): void {
    this.api.getKpis().subscribe((kpis) => this.store.setKpis(kpis));
    this.api.getCourierPositions().subscribe((positions) => {
      this.store.setCouriers(positions);
    });
    this.api.getPendingOrders().subscribe((orders) => this.store.setPendingOrders(orders));
    if (this.selectedZone()) {
      this.api
        .getZoneMetrics(this.selectedZoneId)
        .subscribe((metrics) => this.store.setZoneMetrics(metrics));
    }
  }

  /**
   * Recalcule zone + marqueurs livreurs (filtrés par zone) + commandes en attente (si coords connues)
   */
  refreshMapLayers(): void {
    const map = this.mapbox.getMap();
    if (!map) {
      return;
    }
    if (!map.isStyleLoaded()) {
      map.once('load', () => this.refreshMapLayers());
      return;
    }
    const zone = this.selectedZone();
    if (zone?.boundaryJson) {
      const ring = this.mapbox.drawDispatchZoneFromBoundary(zone.boundaryJson, zone.name);
      if (ring && ring.length > 0) {
        this.mapbox.fitBounds(ring, 48);
      } else {
        this.flyToZoneCenter(zone);
      }
    } else {
      this.mapbox.removeDispatchZoneOverlay();
      if (zone) {
        this.flyToZoneCenter(zone);
      }
    }
    this.mapbox.clearPlacementMarkers();
    const zid = this.selectedZoneId;
    for (const c of this.state().couriers) {
      if (c.lat == null || c.lon == null) {
        continue;
      }
      if (c.zoneId != null && c.zoneId !== zid) {
        continue;
      }
      const title = this.translate.instant('dispatch.mapPopupCourier', { id: c.courierId });
      const sub = this.translate.instant('dispatch.mapPopupSub', { type: c.type, status: c.status });
      this.mapbox.addMarker([c.lon, c.lat], {
        color: this.courierColor(c),
        popup: `<b>${title}</b><br/>${sub}`,
      });
    }
    for (const o of this.state().pendingOrders) {
      const pos = this.pendingOrderLatLon(o);
      if (!pos) {
        continue;
      }
      const label = o.orderNumber ?? `#${o.id}`;
      this.mapbox.addOrderMarker(pos, {
        color: '#e11d48',
        popup: `<b>${this.translate.instant('dispatch.mapPendingOrder')}</b><br/>${label}`,
      });
    }
  }

  private pendingOrderLatLon(o: { deliveryAddress?: unknown; id: number; orderNumber?: string }): [number, number] | null {
    const a = o.deliveryAddress;
    if (a == null) {
      return null;
    }
    if (typeof a === 'object' && !Array.isArray(a)) {
      const m = a as Record<string, unknown>;
      const lat = this.toNum(m['latitude'] ?? m['lat']);
      const lon = this.toNum(m['longitude'] ?? m['lon'] ?? m['lng']);
      if (lat != null && lon != null) {
        return [lon, lat];
      }
    }
    return null;
  }

  private toNum(v: unknown): number | null {
    if (v == null) {
      return null;
    }
    if (typeof v === 'number' && Number.isFinite(v)) {
      return v;
    }
    const n = parseFloat(String(v));
    return Number.isFinite(n) ? n : null;
  }

  /**
   * Fallback de recadrage: si la boundary n'est pas exploitable, utilise `zone.center` ([lat, lon]).
   */
  private flyToZoneCenter(zone: Zone): void {
    const center = zone.center;
    if (!Array.isArray(center) || center.length < 2) {
      return;
    }
    const lat = this.toNum(center[0]);
    const lon = this.toNum(center[1]);
    if (lat == null || lon == null) {
      return;
    }
    this.mapbox.flyTo([lon, lat], 12);
  }

  private courierColor(courier: DispatchCourierPosition): string {
    if (courier.type === 'INTERNAL') {
      if (courier.status === 'IDLE') return '#2563eb';
      if (courier.status === 'ON_DELIVERY') return '#f97316';
      return '#9ca3af';
    }
    if (courier.status === 'IDLE') return '#22c55e';
    if (courier.status === 'ON_DELIVERY') return '#facc15';
    return '#9ca3af';
  }

  /**
   * L'API order renvoie souvent `deliveryAddress` comme objet (formattedAddress, street, city, …).
   * Sans cela, le template affiche "[object Object]".
   */
  formatDeliveryAddress(addr: unknown): string {
    if (addr == null) {
      return '';
    }
    if (typeof addr === 'string') {
      return addr.trim();
    }
    if (typeof addr !== 'object' || Array.isArray(addr)) {
      return '';
    }
    const o = addr as Record<string, unknown>;
    const firstString = (k: string): string | undefined => {
      const v = o[k];
      return typeof v === 'string' && v.trim() ? v.trim() : undefined;
    };
    const direct =
      firstString('formattedAddress') ??
      firstString('fullAddress') ??
      firstString('formatted_address');
    if (direct) {
      return direct;
    }
    const street = firstString('street') ?? firstString('line1') ?? firstString('addressLine1') ?? firstString('deliveryAddress');
    const line2 = firstString('line2') ?? firstString('building') ?? firstString('floor');
    const city = firstString('city');
    const postal = firstString('postalCode') ?? firstString('zip') ?? firstString('postal_code');
    const state = firstString('state');
    const country = firstString('country');
    const parts = [street, line2, city, postal, state, country].filter((p): p is string => Boolean(p));
    return parts.length > 0 ? parts.join(', ') : '';
  }
}
