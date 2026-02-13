// src/app/features/dashboard/dashboard.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

interface StatCard {
  labelKey: string;
  value: string;
  icon: string;
  trend: string;
  trendUp: boolean;
  color: string;
}

interface RecentOrder {
  id: string;
  customer: string;
  partner: string;
  total: string;
  status: string;
  statusClass: string;
  date: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent {
  stats: StatCard[] = [
    {
      labelKey: 'dashboard.totalOrders',
      value: '12,845',
      icon: 'shopping_cart',
      trend: '+12.5%',
      trendUp: true,
      color: '#3b82f6',
    },
    {
      labelKey: 'dashboard.pendingApprovals',
      value: '234',
      icon: 'pending_actions',
      trend: '+3.2%',
      trendUp: true,
      color: '#f59e0b',
    },
    {
      labelKey: 'dashboard.activeCouriers',
      value: '89',
      icon: 'delivery_dining',
      trend: '-2.1%',
      trendUp: false,
      color: '#8b5cf6',
    },
    {
      labelKey: 'dashboard.totalRevenue',
      value: '45,280 TND',
      icon: 'payments',
      trend: '+18.7%',
      trendUp: true,
      color: '#10b981',
    },
  ];

  recentOrders: RecentOrder[] = [
    { id: 'ORD-7841', customer: 'Ahmed Ben Ali', partner: 'Pizza Palace', total: '32.50 TND', status: 'orders.status.delivered', statusClass: 'delivered', date: '2024-01-15' },
    { id: 'ORD-7840', customer: 'Fatma Trabelsi', partner: 'Burger House', total: '28.00 TND', status: 'orders.status.delivering', statusClass: 'delivering', date: '2024-01-15' },
    { id: 'ORD-7839', customer: 'Mohamed Sassi', partner: 'Sushi World', total: '55.00 TND', status: 'orders.status.preparing', statusClass: 'preparing', date: '2024-01-15' },
    { id: 'ORD-7838', customer: 'Amira Khemiri', partner: 'Café Central', total: '15.50 TND', status: 'orders.status.pending', statusClass: 'pending', date: '2024-01-15' },
    { id: 'ORD-7837', customer: 'Youssef Hamdi', partner: 'Pasta Corner', total: '42.00 TND', status: 'orders.status.delivered', statusClass: 'delivered', date: '2024-01-14' },
    { id: 'ORD-7836', customer: 'Nour Mejri', partner: 'Pizza Palace', total: '19.90 TND', status: 'orders.status.cancelled', statusClass: 'cancelled', date: '2024-01-14' },
  ];

  selectedStatus = 'all';

  statuses = ['all', 'pending', 'preparing', 'delivering', 'delivered', 'cancelled'];
}
