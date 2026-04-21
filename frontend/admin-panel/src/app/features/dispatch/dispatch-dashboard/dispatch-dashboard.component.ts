import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
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

import { DispatchApiService } from '../services/dispatch-api.service';
import { DispatchRealtimeService } from '../services/dispatch-realtime.service';
import { DispatchStoreService } from '../services/dispatch-store.service';
import { MapboxService } from '../../zones/services/mapbox.service';
import { ZonesService } from '../../zones/services/zones.service';
import { CouriersService } from '../../users/couriers/services/couriers.service';
import { Zone } from '../../zones/models/zone.model';
import { DispatchCourierPosition } from '../models/dispatch-dashboard.model';
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
  private destroy$ = new Subject<void>();

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
      this.renderCouriersOnMap(this.state().couriers);
    });

    this.store.state$.pipe(takeUntil(this.destroy$)).subscribe((s) => {
      this.state.set(s);
      const active = s.selectedZoneMetrics?.zoneActive;
      if (active !== undefined && active !== null) {
        this.zoneActive = active;
      }
    });

    this.loadActiveZones();
    this.loadSnapshot();
    this.realtime.connect();
    this.realtime.courierPositions$.pipe(takeUntil(this.destroy$)).subscribe((positions) => {
      this.store.setCouriers(positions);
      this.renderCouriersOnMap(positions);
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.mapbox.initializeMap('dispatch-map', { center: [10.1815, 36.8065], zoom: 11 });
      this.mapbox.resize();
      this.renderCouriersOnMap(this.state().couriers);
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

  private applyZoneChange(zone: Zone): void {
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
    this.loadCouriersForZone(zone.id);
    this.api.getPendingOrders().subscribe((orders) => this.store.setPendingOrders(orders));
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
      next: () => {
        this.snackBar.open(this.translate.instant('dispatch.assignSuccess'), undefined, { duration: 2500 });
        this.selectedOrderId = null;
        this.selectedCourier.set(null);
        this.courierSearchCtrl.setValue('');
        this.loadSnapshot();
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
      this.renderCouriersOnMap(positions);
    });
    this.api.getPendingOrders().subscribe((orders) => this.store.setPendingOrders(orders));
    if (this.selectedZone()) {
      this.api
        .getZoneMetrics(this.selectedZoneId)
        .subscribe((metrics) => this.store.setZoneMetrics(metrics));
    }
  }

  private renderCouriersOnMap(couriers: DispatchCourierPosition[]): void {
    this.mapbox.clearMarkers();
    couriers.forEach((c) => {
      if (c.lat == null || c.lon == null) return;
      const title = this.translate.instant('dispatch.mapPopupCourier', { id: c.courierId });
      const sub = this.translate.instant('dispatch.mapPopupSub', { type: c.type, status: c.status });
      this.mapbox.addMarker([c.lon, c.lat], {
        color: this.courierColor(c),
        popup: `<b>${title}</b><br/>${sub}`,
      });
    });
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
}
