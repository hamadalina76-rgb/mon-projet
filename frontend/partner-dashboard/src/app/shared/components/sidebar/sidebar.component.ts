// src/app/shared/components/sidebar/sidebar.component.ts - Angular 19
import { Component, input, output, signal, inject, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { PartnerService } from '@core/services/partner.service';

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
export class SidebarComponent implements OnInit {
  private authService = inject(AuthService);
  private partnerService = inject(PartnerService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  // Angular 19 Signal Input/Output
  collapsed = input(false);
  mobileOpen = input(false);
  isMobile = input(false);
  collapsedChange = output<boolean>();

  partnerStatus = signal<string>('PENDING');

  // Computed: check if partner is active
  isPartnerActive = computed(() => {
    const status = this.partnerStatus().toLowerCase();
    // Exclude documents_missing, pending, rejected, suspended, inactive, deactivated
    return status === 'active' || status === 'approved';
  });
  
  // Computed: check if partner status is documents_missing
  isDocumentsMissing = computed(() => {
    return this.partnerStatus().toLowerCase() === 'documents_missing';
  });

  menuItems: MenuItem[] = [
    { label: 'nav.dashboard', icon: 'dashboard', route: '/dashboard', requiresActive: false },
    { label: 'nav.orders', icon: 'shopping_bag', route: '/orders', requiresActive: true },
    { label: 'nav.menu', icon: 'restaurant_menu', route: '/menu', requiresActive: true },
    { label: 'nav.analytics', icon: 'analytics', route: '/analytics', requiresActive: true },
    { label: 'nav.reviews', icon: 'star', route: '/reviews', requiresActive: true },
    { label: 'nav.promotions', icon: 'local_offer', route: '/promotions', requiresActive: true },
    { label: 'nav.profile', icon: 'store', route: '/profile', requiresActive: false },
    { label: 'nav.finance', icon: 'account_balance', route: '/finance', requiresActive: true },
  ];

  ngOnInit(): void {
    this.loadPartnerStatus();
  }

  private loadPartnerStatus(): void {
    const user = this.authService.currentUser();
    const partnerId = user?.partnerId;

    if (partnerId) {
      this.partnerService.getPartner(partnerId).subscribe({
        next: (partner: any) => {
          this.partnerStatus.set(partner.status || 'PENDING');
        },
        error: () => {
          this.partnerStatus.set('PENDING');
        }
      });
    }
  }

  isItemLocked(item: MenuItem): boolean {
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
