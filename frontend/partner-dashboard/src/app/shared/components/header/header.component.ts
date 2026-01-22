// src/app/shared/components/header/header.component.ts - Angular 19
import { Component, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent {
  authService = inject(AuthService);

  // Angular 19 Signal Input
  sidebarCollapsed = input(false);

  // Angular 19 Signals
  showUserMenu = signal(false);
  showNotifications = signal(false);

  toggleUserMenu(): void {
    this.showUserMenu.update(v => !v);
    this.showNotifications.set(false);
  }

  toggleNotifications(): void {
    this.showNotifications.update(v => !v);
    this.showUserMenu.set(false);
  }

  logout(): void {
    this.authService.logout();
  }
}
