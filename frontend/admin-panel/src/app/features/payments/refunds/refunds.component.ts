// src/app/features/payments/refunds/refunds.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { PaymentsService } from '../services/payments.service';

@Component({
  selector: 'app-refunds',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './refunds.component.html',
  styleUrls: ['./refunds.component.scss'],
})
export class RefundsComponent implements OnInit {
  private paymentsService = inject(PaymentsService);

  refunds: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadRefunds();
  }

  loadRefunds(): void {
    // TODO: Implement
  }
}
