// Couriers List - same design & behaviour as Partners List. Pagination & search on backend.
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { CouriersService } from '../services/couriers.service';
import { ZonesService } from '../../../zones/services/zones.service';
import { Zone } from '../../../zones/models/zone.model';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { RejectDialogComponent } from '../../../partners/partner-approval/reject-dialog.component';
import { ChangeTypeDialogComponent, ChangeTypeResult } from '../courier-detail/change-type-dialog.component';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';

@Component({
  selector: 'app-couriers-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    TranslateModule,
    ListPageComponent,
  ],
  templateUrl: './couriers-list.component.html',
  styleUrls: ['./couriers-list.component.scss'],
})
export class CouriersListComponent implements OnInit, OnDestroy {
  private couriersService = inject(CouriersService);
  private zonesService = inject(ZonesService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  couriers = signal<any[]>([]);
  loading = signal(false);

  searchText = '';
  selectedStatus = 'all';
  selectedCourierType = 'all';
  selectedZoneId: number | 'all' = 'all';
  itemsPerPage = 20;
  currentPage = 1;
  totalItems = 0;
  zones = signal<Zone[]>([]);

  Math = Math;

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());
    this.loadZones();
    this.loadCouriers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCouriers(): void {
    this.loading.set(true);
    const status = this.selectedStatus !== 'all' ? this.selectedStatus : undefined;
    const courierType = this.selectedCourierType !== 'all' ? this.selectedCourierType : undefined;
    const zoneId = this.selectedZoneId !== 'all' ? Number(this.selectedZoneId) : undefined;
    this.couriersService
      .getCouriers(this.currentPage - 1, this.itemsPerPage, status, this.searchText || undefined, courierType, zoneId)
      .subscribe({
        next: (response: any) => {
          const list = response.content ?? [];
          this.couriers.set(list);
          this.totalItems = response.totalElements ?? 0;
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private loadZones(): void {
    this.zonesService.getActiveZones().subscribe({
      next: (zones) => this.zones.set(zones || []),
      error: () => this.zones.set([]),
    });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadCouriers();
  }

  onSearchChange(): void {
    this.searchInput$.next(this.searchText);
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  onCourierTypeChange(): void {
    this.applyFilters();
  }

  onZoneChange(): void {
    this.applyFilters();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  get paginatedCouriers(): any[] {
    return this.couriers();
  }

  getStatusColor(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'ACTIVE' || s === 'AVAILABLE' || s === 'BUSY' || s === 'OFFLINE') return '#10B981';
    if (s === 'PENDING_APPROVAL') return '#F59E0B';
    if (s === 'REJECTED') return '#94A3B8';
    if (s === 'SUSPENDED') return '#EF4444';
    if (s === 'DEACTIVATED') return '#64748B';
    return '#94A3B8';
  }

  getStatusLabel(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'PENDING_APPROVAL') return this.translate.instant('users.couriers.status.pending');
    if (s === 'ACTIVE') return this.translate.instant('users.couriers.status.approved');
    if (s === 'REJECTED') return this.translate.instant('users.couriers.status.rejected');
    if (s === 'SUSPENDED') return this.translate.instant('users.couriers.status.blocked');
    if (s === 'DEACTIVATED') return this.translate.instant('users.couriers.status.deactivated');
    if (s === 'AVAILABLE' || s === 'BUSY' || s === 'OFFLINE') return this.translate.instant('users.couriers.status.active');
    return status || '-';
  }

  getInitials(courier: any): string {
    const first = courier?.firstName || '';
    const last = courier?.lastName || '';
    if (!first && !last) return courier?.email?.[0]?.toUpperCase() || '?';
    return ((first[0] || '') + (last[0] || '')).toUpperCase().substring(0, 2);
  }

  getFullName(courier: any): string {
    if (!courier) return '-';
    const first = courier.firstName || '';
    const last = courier.lastName || '';
    return [first, last].filter(Boolean).join(' ') || courier.email || '-';
  }

  /** True si le livreur peut être désactivé (Block) – statuts actifs. */
  canDeactivate(courier: any): boolean {
    const s = (courier?.status || '').toUpperCase();
    return ['ACTIVE', 'APPROVED', 'AVAILABLE', 'OFFLINE', 'BUSY'].includes(s);
  }

  /** True si le livreur peut être suspendu. */
  canSuspend(courier: any): boolean {
    const s = (courier?.status || '').toUpperCase();
    return ['ACTIVE', 'APPROVED', 'AVAILABLE', 'OFFLINE', 'BUSY'].includes(s);
  }

  /** True si le livreur est suspendu. */
  isSuspended(courier: any): boolean {
    return (courier?.status || '').toUpperCase() === 'SUSPENDED';
  }

  /** True si le livreur est désactivé. */
  isDeactivated(courier: any): boolean {
    return (courier?.status || '').toUpperCase() === 'DEACTIVATED';
  }

  /** True si le livreur est rejeté. */
  isRejected(courier: any): boolean {
    return (courier?.status || '').toUpperCase() === 'REJECTED';
  }

  /** True si le livreur peut être réactivé (suspendu, désactivé ou rejeté). */
  canActivate(courier: any): boolean {
    return this.isSuspended(courier) || this.isDeactivated(courier) || this.isRejected(courier);
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadCouriers();
  }

  activateCourier(courier: any): void {
    const name = this.getFullName(courier);
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('users.couriers.activateDialog.title'),
        message: this.translate.instant('users.couriers.activateDialog.message', { name }),
        confirmLabel: this.translate.instant('common.confirm'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'info',
        icon: 'check_circle',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.couriersService.activateCourier(String(courier.id)).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.activateSuccess'));
            this.loadCouriers();
          },
          error: () => this.toastr.error(this.translate.instant('common.error')),
        });
      }
    });
  }

  deactivateCourier(courier: any): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.couriersService.deactivateCourier(String(courier.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.deactivateSuccess'));
            this.loadCouriers();
          },
          error: () => this.toastr.error(this.translate.instant('common.error')),
        });
      }
    });
  }

  suspendCourier(courier: any): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.couriersService.suspendCourier(String(courier.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.suspendSuccess'));
            this.loadCouriers();
          },
          error: () => this.toastr.error(this.translate.instant('common.error')),
        });
      }
    });
  }

  changeCourierType(courier: any): void {
    const dialogRef = this.dialog.open(ChangeTypeDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      data: { courierType: courier.courierType ?? null, assignedZoneIds: courier.assignedZoneIds ?? [] },
    });
    dialogRef.afterClosed().subscribe((result: ChangeTypeResult | null) => {
      if (!result) return;
      const id = String(courier.id);
      this.couriersService.updateCourier(id, { courierType: result.courierType }).subscribe({
        next: () => {
          this.couriersService.assignZones(id, result.zoneIds).subscribe();
          this.toastr.success(this.translate.instant('users.couriers.changeTypeSuccess'));
          this.loadCouriers();
        },
        error: () => this.toastr.error(this.translate.instant('common.error')),
      });
    });
  }
}
