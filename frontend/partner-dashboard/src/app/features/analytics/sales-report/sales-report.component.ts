// src/app/features/analytics/sales-report/sales-report.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { TranslateModule } from '@ngx-translate/core';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-sales-report',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
    MatPaginatorModule,
    TranslateModule,
  ],
  templateUrl: './sales-report.component.html',
  styleUrls: ['./sales-report.component.scss'],
})
export class SalesReportComponent implements OnInit {
  private fb = inject(FormBuilder);
  private analyticsService = inject(AnalyticsService);

  dateRangeForm: FormGroup = this.fb.group({
    startDate: [null],
    endDate: [null],
  });

  reportData: any[] = [];
  displayedColumns = ['date', 'orders', 'revenue', 'averageOrder', 'topProduct'];
  loading = false;

  summary = {
    totalRevenue: 0,
    totalOrders: 0,
    averageOrder: 0,
  };

  ngOnInit(): void {
    this.setDefaultDates();
    this.generateReport();
  }

  setDefaultDates(): void {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);
    
    this.dateRangeForm.patchValue({ startDate, endDate });
  }

  generateReport(): void {
    this.loading = true;
    // TODO: Implement
  }

  exportReport(format: string): void {
    // TODO: Implement
  }
}
