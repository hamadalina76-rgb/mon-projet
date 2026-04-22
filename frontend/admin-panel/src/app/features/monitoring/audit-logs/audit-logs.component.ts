// src/app/features/monitoring/audit-logs/audit-logs.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DataTableComponent, TableColumn } from '@shared/components/data-table/data-table.component';
import { MonitoringService } from '../services/monitoring.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.scss'],
})
export class AuditLogsComponent implements OnInit {
  private monitoringService = inject(MonitoringService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  auditLogs: Record<string, unknown>[] = [];
  loading = false;
  page = 0;
  pageSize = 50;
  total = 0;

  columns: TableColumn[] = [
    { key: 'timestamp', label: 'monitoring.auditColTime', type: 'date' },
    { key: 'action', label: 'monitoring.auditColAction', type: 'text' },
    { key: 'user', label: 'monitoring.auditColUser', type: 'text' },
  ];

  ngOnInit(): void {
    this.loadAuditLogs();
  }

  loadAuditLogs(): void {
    this.loading = true;
    this.monitoringService.getAuditLogs(this.page + 1, this.pageSize).subscribe({
      next: (response: unknown) => {
        const r = response as Record<string, unknown>;
        const data = (r['data'] ?? r['content'] ?? r['items'] ?? []) as Record<string, unknown>[];
        this.auditLogs = Array.isArray(data) ? this.normalizeRows(data) : [];
        const total = r['totalElements'] ?? r['total'] ?? this.auditLogs.length;
        this.total = typeof total === 'number' ? total : Number(total) || this.auditLogs.length;
        this.loading = false;
      },
      error: () => {
        this.auditLogs = [];
        this.loading = false;
      },
    });
  }

  private normalizeRows(rows: Record<string, unknown>[]): Record<string, unknown>[] {
    return rows.map((row) => ({
      ...row,
      timestamp: row['timestamp'] ?? row['time'] ?? row['createdAt'] ?? row['at'],
      action: row['action'] ?? row['type'] ?? row['eventType'] ?? '—',
      user: row['user'] ?? row['userId'] ?? row['actor'] ?? row['email'] ?? '—',
    }));
  }

  exportLogs(): void {
    const blob = new Blob([JSON.stringify(this.auditLogs, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    this.snackBar.open(this.translate.instant('monitoring.auditExportDone'), undefined, { duration: 3000 });
  }
}
