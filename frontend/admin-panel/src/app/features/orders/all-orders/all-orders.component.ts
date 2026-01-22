// src/app/features/orders/all-orders/all-orders.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { OrdersService } from '../services/orders.service';

@Component({
  selector: 'app-all-orders',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'orders.all' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <app-data-table
          [data]="orders"
          [loading]="loading"
          [columns]="columns"
          (rowClick)="viewOrder($event)">
        </app-data-table>
      </mat-card-content>
    </mat-card>
  `,
})
export class AllOrdersComponent implements OnInit {
  private ordersService = inject(OrdersService);
  private router = inject(Router);

  orders: any[] = [];
  loading = false;
  columns = [
    { key: 'id', label: 'ID' },
    { key: 'customer', label: 'Customer' },
    { key: 'total', label: 'Total' },
    { key: 'status', label: 'Status' },
  ];

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.ordersService.getOrders(1, 50).subscribe({
      next: (response: any) => {
        this.orders = response.data || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  viewOrder(order: any): void {
    this.router.navigate(['/orders', order.id]);
  }
}
