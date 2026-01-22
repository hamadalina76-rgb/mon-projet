// src/app/features/payments/payouts/payouts.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { PaymentsService } from '../services/payments.service';

@Component({
  selector: 'app-payouts',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    TranslateModule,
    DataTableComponent,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'payments.payouts' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <mat-tab-group>
          <mat-tab label="Partner Payouts">
            <app-data-table [data]="partnerPayouts" [loading]="loading"></app-data-table>
          </mat-tab>
          <mat-tab label="Courier Payouts">
            <app-data-table [data]="courierPayouts" [loading]="loading"></app-data-table>
          </mat-tab>
        </mat-tab-group>
      </mat-card-content>
    </mat-card>
  `,
})
export class PayoutsComponent implements OnInit {
  private paymentsService = inject(PaymentsService);

  partnerPayouts: any[] = [];
  courierPayouts: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadPayouts();
  }

  loadPayouts(): void {
    this.loading = true;
    // TODO: Implement payouts loading
    this.partnerPayouts = [];
    this.courierPayouts = [];
    this.loading = false;
  }
}
