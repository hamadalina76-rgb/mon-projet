// src/app/features/payments/courier-payouts/courier-payouts.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { PaymentsService } from '../services/payments.service';

@Component({
  selector: 'app-courier-payouts',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './courier-payouts.component.html',
  styleUrls: ['./courier-payouts.component.scss'],
})
export class CourierPayoutsComponent implements OnInit {
  private paymentsService = inject(PaymentsService);

  payouts: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadPayouts();
  }

  loadPayouts(): void {
    // TODO: Implement
  }
}
