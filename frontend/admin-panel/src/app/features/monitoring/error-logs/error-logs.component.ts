// src/app/features/monitoring/error-logs/error-logs.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent, TableColumn } from '@shared/components/data-table/data-table.component';
import { MonitoringService } from '../services/monitoring.service';

@Component({
  selector: 'app-error-logs',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './error-logs.component.html',
  styleUrls: ['./error-logs.component.scss'],
})
export class ErrorLogsComponent implements OnInit {
  private monitoringService = inject(MonitoringService);

  errors: Record<string, unknown>[] = [];
  loading = false;
  page = 0;
  pageSize = 20;
  total = 0;

  columns: TableColumn[] = [
    { key: 'timestamp', label: 'monitoring.errorColTime', type: 'date' },
    { key: 'message', label: 'monitoring.errorColMessage', type: 'text', tooltip: true },
    { key: 'source', label: 'monitoring.errorColSource', type: 'text' },
  ];

  ngOnInit(): void {
    this.loadErrors();
  }

  loadErrors(): void {
    this.loading = true;
    this.monitoringService.getErrorLogs(this.page + 1, this.pageSize).subscribe({
      next: (response: unknown) => {
        const r = response as Record<string, unknown>;
        const data = (r['data'] ?? r['content'] ?? r['items'] ?? []) as Record<string, unknown>[];
        this.errors = Array.isArray(data) ? this.normalizeRows(data) : [];
        const total = r['totalElements'] ?? r['total'] ?? this.errors.length;
        this.total = typeof total === 'number' ? total : Number(total) || this.errors.length;
        this.loading = false;
      },
      error: () => {
        this.errors = [];
        this.loading = false;
      },
    });
  }

  private normalizeRows(rows: Record<string, unknown>[]): Record<string, unknown>[] {
    return rows.map((row) => ({
      ...row,
      timestamp: row['timestamp'] ?? row['time'] ?? row['createdAt'] ?? row['at'],
      message: row['message'] ?? row['error'] ?? row['detail'] ?? '—',
      source: row['source'] ?? row['service'] ?? row['logger'] ?? '—',
    }));
  }
}
