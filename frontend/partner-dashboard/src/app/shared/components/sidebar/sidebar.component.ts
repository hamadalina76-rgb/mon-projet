// src/app/shared/components/sidebar/sidebar.component.ts - Angular 19
import { Component, input, output, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
  requiresActive?: boolean; // Only accessible when partner is ACTIVE
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, MatTooltipModule, TranslateModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent {
  private translate = inject(TranslateService);

  // Angular 19 Signal Input/Output
  collapsed = input(false);
  mobileOpen = input(false);
  isMobile = input(false);
  collapsedChange = output<boolean>();

  /**
   * Statut API (synchronisé depuis MainLayout : polling + WebSocket).
   * Source unique pour éviter un menu encore « ouvert » après désactivation admin sans F5.
   */
  partnerStatus = input<string>('');

  // Computed: check if partner is active
  isPartnerActive = computed(() => {
    const status = this.partnerStatus().toLowerCase();
    // Exclude documents_missing, pending, rejected, suspended, inactive, deactivated
    return status === 'active' || status === 'approved';
  });

  /** Compte fermé côté admin : tout le menu doit être verrouillé (y compris tableau de bord et profil). */
  isPartnerAccountRestricted = computed(() => {
    const s = this.partnerStatus().toLowerCase();
    return (
      s === 'suspended' ||
      s === 'inactive' ||
      s === 'deactivated' ||
      s === 'rejected' ||
      s === 'closed'
    );
  });

  menuItems: MenuItem[] = [
    { label: 'nav.dashboard', icon: 'dashboard', route: '/dashboard', requiresActive: false },
    { label: 'nav.notifications', icon: 'notifications', route: '/notifications', requiresActive: false },
    { label: 'nav.orders', icon: 'shopping_bag', route: '/orders', requiresActive: true },
    { label: 'nav.orderHistory', icon: 'history', route: '/orders/history', requiresActive: true },
    { label: 'nav.menu', icon: 'restaurant_menu', route: '/menu', requiresActive: true },
    { label: 'nav.analytics', icon: 'analytics', route: '/analytics', requiresActive: true },
    { label: 'nav.reviews', icon: 'star', route: '/reviews', requiresActive: true },
    { label: 'nav.profile', icon: 'store', route: '/profile', requiresActive: false },
    { label: 'nav.finance', icon: 'account_balance', route: '/finance', requiresActive: true },
  ];

  isItemLocked(item: MenuItem): boolean {
    if (this.isPartnerAccountRestricted()) {
      return true;
    }
    return item.requiresActive === true && !this.isPartnerActive();
  }

  onItemClick(item: MenuItem, event: Event): void {
    if (this.isItemLocked(item)) {
      event.preventDefault();
      event.stopPropagation();
    }
  }

  getTooltip(item: MenuItem): string {
    return this.isItemLocked(item)
      ? this.translate.instant('nav.lockedTooltip')
      : this.translate.instant(item.label);
  }

  toggleCollapse(): void {
    if (this.isMobile() && this.mobileOpen()) {
      this.collapsedChange.emit(true);
    } else {
      this.collapsedChange.emit(!this.collapsed());
    }
  }
}
