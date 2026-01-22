// src/app/features/orders/disputes/disputes.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { OrdersService } from '../services/orders.service';

@Component({
  selector: 'app-disputes',
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
  templateUrl: './disputes.component.html',
  styleUrls: ['./disputes.component.scss'],
})
export class DisputesComponent implements OnInit {
  private ordersService = inject(OrdersService);

  disputes: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadDisputes();
  }

  loadDisputes(): void {
    // TODO: Implement
  }
}
