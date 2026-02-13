import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { HeaderComponent } from '@shared/components/header/header.component';
import { FooterComponent } from '@shared/components/footer/footer.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { LoadingService } from '@core/services/loading.service';
import { InactivityService } from '@core/services/inactivity.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarComponent,
    HeaderComponent,
    FooterComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss'],
})
export class AdminLayoutComponent {
  private loadingService = inject(LoadingService);
  private inactivityService = inject(InactivityService);

  isLoading = this.loadingService.isLoading;
  sidebarMobileOpen = signal(false);

  constructor() {
    this.inactivityService.start();
  }

  toggleMobileSidebar(): void {
    this.sidebarMobileOpen.update((v) => !v);
  }

  closeMobileSidebar(): void {
    this.sidebarMobileOpen.set(false);
  }
}
