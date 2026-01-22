// src/app/features/dashboard/components/real-time-orders/real-time-orders.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';

@Component({
  selector: 'app-real-time-orders',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule],
  templateUrl: './real-time-orders.component.html',
  styleUrls: ['./real-time-orders.component.scss'],
})
export class RealTimeOrdersComponent implements OnInit, OnDestroy {
  ngOnInit(): void {
    // TODO: Subscribe to real-time orders
  }

  ngOnDestroy(): void {
    // TODO: Unsubscribe
  }
}
