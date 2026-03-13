import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { CouriersService } from '../services/couriers.service';
import { RejectDialogComponent } from '../../../partners/partner-approval/reject-dialog.component';
import { RequestMoreInfoDialogComponent } from '../../../partners/partner-approval/request-more-info-dialog.component';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { environment } from '@environments/environment';

@Component({
  selector: 'app-courier-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    TranslateModule,
  ],
  templateUrl: './courier-detail.component.html',
  styleUrls: ['./courier-detail.component.scss'],
})
export class CourierDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private couriersService = inject(CouriersService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);

  courier = signal<any>(null);
  loading = signal(true);
  actionLoading = signal(false);
  uploadsBaseUrl = environment.uploadsBaseUrl || '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadCourier(id);
  }

  loadCourier(id: string): void {
    this.loading.set(true);
    this.couriersService.getCourier(id).subscribe({
      next: (data) => {
        this.courier.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  getFullName(c: any): string {
    if (!c) return '-';
    return [c.firstName, c.lastName].filter(Boolean).join(' ') || c.email || '-';
  }

  getStatusLabel(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'PENDING_APPROVAL') return this.translate.instant('users.couriers.status.pending');
    if (s === 'ACTIVE') return this.translate.instant('users.couriers.status.approved');
    if (s === 'REJECTED') return this.translate.instant('users.couriers.status.rejected');
    if (s === 'SUSPENDED') return this.translate.instant('users.couriers.status.blocked');
    if (s === 'DEACTIVATED') return this.translate.instant('users.couriers.status.deactivated');
    return status || '-';
  }

  /** True si le livreur peut être désactivé (Block). */
  canDeactivate(c: any): boolean {
    const s = (c?.status || '').toUpperCase();
    return ['ACTIVE', 'APPROVED', 'AVAILABLE', 'OFFLINE', 'BUSY'].includes(s);
  }

  /** True si le livreur peut être suspendu. */
  canSuspend(c: any): boolean {
    const s = (c?.status || '').toUpperCase();
    return ['ACTIVE', 'APPROVED', 'AVAILABLE', 'OFFLINE', 'BUSY'].includes(s);
  }

  /** True si le livreur est suspendu. */
  isSuspended(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'SUSPENDED';
  }

  /** True si le livreur est désactivé. */
  isDeactivated(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'DEACTIVATED';
  }

  /** True si le livreur est rejeté. */
  isRejected(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'REJECTED';
  }

  /** True si le livreur peut être réactivé (suspendu, désactivé ou rejeté). */
  canActivate(c: any): boolean {
    return this.isSuspended(c) || this.isDeactivated(c) || this.isRejected(c);
  }

  /** True si le livreur est en attente d'approbation. */
  isPendingApproval(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'PENDING_APPROVAL';
  }

  docUrl(url: string | null): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return this.uploadsBaseUrl + url;
  }

  approve(): void {
    const c = this.courier();
    if (!c) return;
    this.actionLoading.set(true);
    this.couriersService.approveCourier(String(c.id)).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('users.couriers.approveSuccess'));
        this.loadCourier(String(c.id));
        this.actionLoading.set(false);
      },
      error: () => {
        this.toastr.error(this.translate.instant('common.error'));
        this.actionLoading.set(false);
      },
    });
  }

  reject(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.rejectCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.rejectSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  requestMoreInfo(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RequestMoreInfoDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((message) => {
      if (message) {
        this.actionLoading.set(true);
        this.couriersService.requestMoreInfo(String(c.id), message).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.requestMoreInfoSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  deactivate(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.deactivateCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.deactivateSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  suspend(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.suspendCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.suspendSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  activate(): void {
    const c = this.courier();
    if (!c) return;
    this.actionLoading.set(true);
    this.couriersService.activateCourier(String(c.id)).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('users.couriers.activateSuccess'));
        this.loadCourier(String(c.id));
        this.actionLoading.set(false);
      },
      error: () => {
        this.toastr.error(this.translate.instant('common.error'));
        this.actionLoading.set(false);
      },
    });
  }

  goToApproval(): void {
    const c = this.courier();
    if (c) this.router.navigate(['/users/couriers', c.id, 'approval']);
  }

  goBack(): void {
    this.router.navigate(['/users/couriers']);
  }

  getCourierIdDisplay(): string {
    const c = this.courier();
    if (!c?.id) return '—';
    return 'SL-' + String(c.id).padStart(5, '0');
  }

  getVehicleTypeLabel(type: string | { name?: string } | null): string {
    if (type == null) return '—';
    const name = typeof type === 'object' && type?.name ? String(type.name) : String(type);
    const t = name.toUpperCase();
    if (t === 'BICYCLE') return 'Vélo';
    if (t === 'MOTORCYCLE') return 'Moto';
    if (t === 'CAR') return 'Voiture';
    return name;
  }
}
