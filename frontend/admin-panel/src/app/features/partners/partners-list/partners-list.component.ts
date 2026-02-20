// Partners List - same design & behaviour as Admins List. Pagination & search are done on the backend.
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
import { PartnersService } from '../services/partners.service';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-partners-list',
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
  ],
  templateUrl: './partners-list.component.html',
  styleUrls: ['./partners-list.component.scss'],
})
export class PartnersListComponent implements OnInit, OnDestroy {
  private partnersService = inject(PartnersService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  partners = signal<any[]>([]);
  loading = signal(false);

  searchText = '';
  selectedStatus = 'all';
  itemsPerPage = 20;
  currentPage = 1;
  totalItems = 0;

  Math = Math;

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());
    this.loadPartners();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadPartners(): void {
    this.loading.set(true);
    const status = this.selectedStatus !== 'all' ? this.selectedStatus : undefined;

    this.partnersService
      .getPartners(this.currentPage - 1, this.itemsPerPage, status, this.searchText || undefined)
      .subscribe({
        next: (response: any) => {
          const list = response.content || [];
          this.partners.set(list);
          this.totalItems = response.totalElements ?? 0;
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadPartners();
  }

  onSearchChange(): void {
    this.searchInput$.next(this.searchText);
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  get paginatedPartners(): any[] {
    return this.partners();
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return '#10B981';
      case 'PENDING':
        return '#F59E0B';
      case 'REJECTED':
        return '#94A3B8';
      case 'SUSPENDED':
        return '#EF4444';
      case 'CLOSED':
        return '#64748B';
      default:
        return '#94A3B8';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':
        return this.translate.instant('common.pending');
      case 'ACTIVE':
        return this.translate.instant('common.active');
      case 'REJECTED':
        return this.translate.instant('partners.rejected');
      case 'SUSPENDED':
        return this.translate.instant('partners.suspended');
      case 'CLOSED':
        return this.translate.instant('partners.status.closed');
      default:
        return status;
    }
  }

  getTypeColor(type: string): string {
    const colors: Record<string, string> = {
      RESTAURANT: '#10B981',
      FAST_FOOD: '#F59E0B',
      CAFE: '#8B5CF6',
      BAKERY: '#EC4899',
      GROCERY: '#3B82F6',
      PHARMACY: '#06B6D4',
      FLORIST: '#84CC16',
      OTHER: '#64748B',
    };
    return colors[type] || '#64748B';
  }

  getTypeLabel(type: string): string {
    const key = `partners.types.${(type || 'other').toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : type;
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name
      .split(/\s+/)
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadPartners();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadPartners();
    }
  }

  goToFirstPage(): void {
    this.currentPage = 1;
    this.loadPartners();
  }

  goToLastPage(): void {
    this.currentPage = this.totalPages;
    this.loadPartners();
  }

  // ---- Status actions with confirmation (same as admins) ----

  activatePartner(partner: any): void {
    const name = partner.brandName || partner.businessName;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('partners.list.activateDialog.title'),
        message: this.translate.instant('partners.list.activateDialog.message', { name }),
        confirmLabel: this.translate.instant('partners.list.activateDialog.confirm'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'info',
        icon: 'check_circle',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.partnersService.activatePartner(partner.id).subscribe({
          next: () => {
            this.toastr.success(
              this.translate.instant('partners.list.activateSuccess', { name }),
              this.translate.instant('partners.list.statusChanged')
            );
            this.loadPartners();
          },
          error: (err) => {
            console.error('Erreur activation partenaire:', err);
            this.toastr.error(
              this.translate.instant('partners.list.activateError'),
              this.translate.instant('common.error')
            );
          },
        });
      }
    });
  }

  deactivatePartner(partner: any): void {
    const name = partner.brandName || partner.businessName;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('partners.list.deactivateDialog.title'),
        message: this.translate.instant('partners.list.deactivateDialog.message', { name }),
        confirmLabel: this.translate.instant('partners.list.deactivateDialog.confirm'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'warning',
        icon: 'block',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.partnersService.deactivatePartner(partner.id, '').subscribe({
          next: () => {
            this.toastr.success(
              this.translate.instant('partners.list.deactivateSuccess', { name }),
              this.translate.instant('partners.list.statusChanged')
            );
            this.loadPartners();
          },
          error: (err) => {
            console.error('Erreur désactivation partenaire:', err);
            this.toastr.error(
              this.translate.instant('partners.list.deactivateError'),
              this.translate.instant('common.error')
            );
          },
        });
      }
    });
  }

  suspendPartner(partner: any): void {
    const name = partner.brandName || partner.businessName;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('partners.list.suspendDialog.title'),
        message: this.translate.instant('partners.list.suspendDialog.message', { name }),
        confirmLabel: this.translate.instant('partners.list.suspendDialog.confirm'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'danger',
        icon: 'gpp_bad',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.partnersService.suspendPartner(partner.id, '').subscribe({
          next: () => {
            this.toastr.success(
              this.translate.instant('partners.list.suspendSuccess', { name }),
              this.translate.instant('partners.list.statusChanged')
            );
            this.loadPartners();
          },
          error: (err) => {
            console.error('Erreur suspension partenaire:', err);
            this.toastr.error(
              this.translate.instant('partners.list.suspendError'),
              this.translate.instant('common.error')
            );
          },
        });
      }
    });
  }
}
