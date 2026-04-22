import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';

import { DispatchApiService } from '../../dispatch/services/dispatch-api.service';
import { DispatchRealtimeService } from '../../dispatch/services/dispatch-realtime.service';
import { ZonesService } from '../../zones/services/zones.service';
import { Zone } from '../../zones/models/zone.model';
import { DispatchCycleEvent, DispatchZoneMetrics } from '../../dispatch/models/dispatch-dashboard.model';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

const MAX_CYCLES = 20;
const PENDING_WARN = 8;
const COURIER_WARN = 2;

@Component({
  selector: 'app-dispatch-monitor',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    FormsModule,
    TranslateModule,
    EmptyStateComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './dispatch-monitor.component.html',
  styleUrls: ['./dispatch-monitor.component.scss'],
})
export class DispatchMonitorComponent implements OnInit, OnDestroy {
  private api = inject(DispatchApiService);
  private realtime = inject(DispatchRealtimeService);
  private zonesService = inject(ZonesService);
  private destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly zones = signal<Zone[]>([]);
  readonly selectedZoneId = signal<number | null>(null);
  readonly metrics = signal<DispatchZoneMetrics | null>(null);
  readonly cycleFeed = signal<DispatchCycleEvent[]>([]);
  /** Synthetic “assignment” feed from approved proposals + cycle summaries */
  readonly activityFeed = signal<string[]>([]);

  readonly alerts = computed(() => {
    const m = this.metrics();
    if (!m) {
      return [] as { key: string; severity: 'warn' | 'danger' }[];
    }
    const out: { key: string; severity: 'warn' | 'danger' }[] = [];
    if (m.pendingOrders >= PENDING_WARN) {
      out.push({ key: 'monitoring.dispatchMonitor.alertPending', severity: m.pendingOrders >= 20 ? 'danger' : 'warn' });
    }
    if (m.onlineCouriers <= COURIER_WARN) {
      out.push({ key: 'monitoring.dispatchMonitor.alertCouriers', severity: 'warn' });
    }
    return out;
  });

  ngOnInit(): void {
    this.realtime.connect();
    this.realtime.zoneCycle$.pipe(takeUntil(this.destroy$)).subscribe((ev) => {
      if (ev?.zoneId != null && ev.zoneId === this.selectedZoneId()) {
        this.cycleFeed.update((list) => [ev, ...list].slice(0, MAX_CYCLES));
        this.activityFeed.update((a) => {
          const line = `${ev.occurredAt}|${ev.zoneId}|${ev.assignedOrders}|${ev.unmatchedOrders}`;
          return [line, ...a].slice(0, 20);
        });
      }
    });
    this.realtime.proposalResolved$.pipe(takeUntil(this.destroy$)).subscribe((r) => {
      if (r?.zoneId === this.selectedZoneId()) {
        this.activityFeed.update((a) => {
          const line = `resolved:${r.orderId}:${r.status}`;
          return [line, ...a].slice(0, 20);
        });
      }
    });

    this.loading.set(true);
    this.zonesService.getActiveZones().subscribe({
      next: (z) => {
        this.zones.set(z);
        if (z.length > 0) {
          this.selectedZoneId.set(z[0].id);
          this.reloadZone();
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  ngOnDestroy(): void {
    this.realtime.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }

  onZoneChange(id: number): void {
    this.selectedZoneId.set(id);
    this.cycleFeed.set([]);
    this.activityFeed.set([]);
    this.realtime.subscribeZoneCycle(id);
    this.reloadZone();
  }

  private reloadZone(): void {
    const id = this.selectedZoneId();
    if (id == null) {
      return;
    }
    this.loading.set(true);
    this.api.getZoneMetrics(id).subscribe({
      next: (m) => {
        this.metrics.set(m);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
