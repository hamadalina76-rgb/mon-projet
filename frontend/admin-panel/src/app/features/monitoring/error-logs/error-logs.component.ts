// src/app/features/monitoring/error-logs/error-logs.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
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

  errors: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadErrors();
  }

  loadErrors(): void {
    // TODO: Implement
  }
}
