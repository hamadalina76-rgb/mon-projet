// src/app/features/monitoring/system-health/system-health.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MonitoringService } from '../services/monitoring.service';

export interface SystemHealthSnapshot {
  status: string;
  lastCheck?: string | number | Date | null;
}

export interface ServiceHealthRow {
  name: string;
  status: string;
  uptime: number;
  latency: number;
  healthScore: number;
}

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    TranslateModule,
  ],
  templateUrl: './system-health.component.html',
  styleUrls: ['./system-health.component.scss'],
})
export class SystemHealthComponent implements OnInit {
  private monitoringService = inject(MonitoringService);

  healthData: SystemHealthSnapshot | null = null;
  services: ServiceHealthRow[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadHealth();
  }

  loadHealth(): void {
    this.loading = true;
    forkJoin({
      health: this.monitoringService.getSystemHealth().pipe(catchError(() => of(null))),
      svc: this.monitoringService.getServiceStatus().pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ health, svc }) => {
        const h = (health as Record<string, unknown> | null) ?? null;
        if (h) {
          const status =
            (h['status'] as string) ??
            (h['healthy'] === true || h['ok'] === true ? 'healthy' : h['up'] === false ? 'unhealthy' : 'unknown');
          const rawTime = h['lastCheck'] ?? h['checkedAt'] ?? h['at'];
          this.healthData = {
            status: String(status),
            lastCheck:
              rawTime == null
                ? undefined
                : typeof rawTime === 'string' || typeof rawTime === 'number' || rawTime instanceof Date
                  ? rawTime
                  : String(rawTime),
          };
        } else {
          this.healthData = null;
        }

        const raw = svc as Record<string, unknown> | unknown[] | null;
        let list: Array<Record<string, unknown>> = [];
        if (Array.isArray(raw)) {
          list = raw as Array<Record<string, unknown>>;
        } else if (raw && Array.isArray((raw as Record<string, unknown>)['services'])) {
          list = (raw as { services: Array<Record<string, unknown>> }).services;
        } else if (raw && Array.isArray((raw as Record<string, unknown>)['data'])) {
          list = (raw as { data: Array<Record<string, unknown>> }).data;
        }
        this.services = list.map((s) => ({
          name: String(s['name'] ?? s['service'] ?? s['id'] ?? '—'),
          status: String(s['status'] ?? s['state'] ?? '—'),
          uptime: Number(s['uptime'] ?? s['upTimePercent'] ?? 0),
          latency: Number(s['latency'] ?? s['latencyMs'] ?? 0),
          healthScore: Number(s['healthScore'] ?? s['score'] ?? 0),
        }));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
