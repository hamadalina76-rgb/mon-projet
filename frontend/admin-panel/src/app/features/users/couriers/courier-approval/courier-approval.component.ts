import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { CouriersService } from '../services/couriers.service';
import { CourierScheduleService } from '../services/courier-schedule.service';
import { RejectDialogComponent } from '../../../partners/partner-approval/reject-dialog.component';
import { RequestMoreInfoDialogComponent } from '../../../partners/partner-approval/request-more-info-dialog.component';
import { ApproveTypeDialogComponent, ApproveTypeResult } from './approve-type-dialog.component';
import { environment } from '@environments/environment';

@Component({
  selector: 'app-courier-approval',
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
    TranslateModule,
  ],
  templateUrl: './courier-approval.component.html',
  styleUrls: ['./courier-approval.component.scss'],
})
export class CourierApprovalComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private couriersService = inject(CouriersService);
  private scheduleSvc = inject(CourierScheduleService);
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
    return status || '-';
  }

  docUrl(url: string | null): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return this.uploadsBaseUrl + url;
  }

  approveCourier(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(ApproveTypeDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      panelClass: 'approve-type-panel',
    });
    dialogRef.afterClosed().subscribe((result: ApproveTypeResult | null) => {
      if (!result) return;
      this.actionLoading.set(true);
      this.couriersService.approveCourier(String(c.id), result.courierType, result.zoneIds ?? []).subscribe({
        next: () => {
          if (result.courierType === 'INTERNAL' && result.templateId) {
            this.scheduleSvc.applyTemplate(String(c.id), result.templateId).subscribe();
          }
          this.toastr.success(this.translate.instant('users.couriers.approveSuccess'));
          this.router.navigate(['/users/couriers', c.id]);
          this.actionLoading.set(false);
        },
        error: () => {
          this.toastr.error(this.translate.instant('common.error'));
          this.actionLoading.set(false);
        },
      });
    });
  }

  rejectCourier(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.rejectCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.rejectSuccess'));
            this.router.navigate(['/users/couriers', c.id]);
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
}
