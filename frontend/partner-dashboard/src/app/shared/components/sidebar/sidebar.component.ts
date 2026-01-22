// src/app/shared/components/sidebar/sidebar.component.ts - Angular 19
import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent {
  // Angular 19 Signal Input/Output
  collapsed = input(false);
  collapsedChange = output<boolean>();

  menuItems: MenuItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Commandes', icon: 'shopping_bag', route: '/orders' },
    { label: 'Menu', icon: 'restaurant_menu', route: '/menu' },
    { label: 'Analytiques', icon: 'analytics', route: '/analytics' },
    { label: 'Avis', icon: 'star', route: '/reviews' },
    { label: 'Promotions', icon: 'local_offer', route: '/promotions' },
    { label: 'Profil', icon: 'store', route: '/profile' },
    { label: 'Finance', icon: 'account_balance', route: '/finance' },
  ];

  toggleCollapse(): void {
    this.collapsedChange.emit(!this.collapsed());
  }
}
