// src/app/features/users/admins/admins-list/admins-list.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { ToastrService } from 'ngx-toastr';
import { AdminService } from '@core/services/admin.service';
import { Admin, AdminRole, AdminStatus } from '@core/models/admin.model';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-admins-list',
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
    MatChipsModule,
    TranslateModule,
    ListPageComponent,
  ],
  templateUrl: './admins-list.component.html',
  styleUrls: ['./admins-list.component.scss'],
})
export class AdminsListComponent implements OnInit {
  private adminService = inject(AdminService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  
  admins = signal<Admin[]>([]);
  filteredAdmins = signal<Admin[]>([]);
  roles = signal<AdminRole[]>([]);
  loading = signal(false);
  
  searchText = '';
  selectedRole = 'all';
  selectedStatus = 'all';
  itemsPerPage = 20;
  currentPage = 1;
  totalItems = 0;

  // Track which admin's permissions are expanded
  expandedAdminId: string | null = null;

  AdminStatus = AdminStatus;

  ngOnInit(): void {
    this.loadRoles();
    this.loadAdmins();
  }

  loadRoles(): void {
    this.adminService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles),
      error: (err) => console.error('Erreur chargement des rôles:', err)
    });
  }

  loadAdmins(): void {
    this.loading.set(true);
    
    this.adminService.getAdmins({
      page: this.currentPage - 1, // Backend uses 0-based indexing
      size: this.itemsPerPage,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
      search: this.searchText || undefined,
      status: this.selectedStatus !== 'all' ? this.selectedStatus as AdminStatus : undefined,
      roleId: this.selectedRole !== 'all' ? parseInt(this.selectedRole) : undefined
    }).subscribe({
      next: ({ admins, total }) => {
        this.admins.set(admins);
        this.filteredAdmins.set(admins);
        this.totalItems = total;
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Erreur lors du chargement des admins:', error);
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    // On recharge depuis le backend avec les filtres
    this.currentPage = 1; // Retour à la première page lors du filtrage
    this.loadAdmins();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onRoleChange(): void {
    this.applyFilters();
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadAdmins();
  }

  get totalPages(): number {
    return Math.ceil(this.totalItems / this.itemsPerPage);
  }

  getStatusColor(status: AdminStatus): string {
    switch (status) {
      case AdminStatus.ACTIVE:
        return '#10B981';
      case AdminStatus.INACTIVE:
        return '#94A3B8';
      case AdminStatus.PENDING:
        return '#F59E0B';
      case AdminStatus.SUSPENDED:
        return '#EF4444';
      default:
        return '#94A3B8';
    }
  }

  getStatusLabel(status: AdminStatus): string {
    const labels: Record<AdminStatus, string> = {
      [AdminStatus.ACTIVE]: 'Actif',
      [AdminStatus.INACTIVE]: 'Inactif',
      [AdminStatus.PENDING]: 'En attente',
      [AdminStatus.SUSPENDED]: 'Suspendu'
    };
    return labels[status];
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  // ---- Status Actions ----

  activateAdmin(admin: Admin): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Activer le compte',
        message: `Voulez-vous réactiver le compte de ${admin.fullName} ? L'administrateur pourra de nouveau se connecter.`,
        confirmLabel: 'Activer',
        cancelLabel: 'Annuler',
        type: 'info',
        icon: 'check_circle'
      } as ConfirmationDialogData
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.changeStatus(admin, AdminStatus.ACTIVE);
    });
  }

  deactivateAdmin(admin: Admin): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Désactiver le compte',
        message: `Voulez-vous désactiver le compte de ${admin.fullName} ? L'administrateur ne pourra plus se connecter jusqu'à réactivation.`,
        confirmLabel: 'Désactiver',
        cancelLabel: 'Annuler',
        type: 'warning',
        icon: 'block'
      } as ConfirmationDialogData
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.changeStatus(admin, AdminStatus.INACTIVE);
    });
  }

  suspendAdmin(admin: Admin): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Suspendre le compte',
        message: `Voulez-vous suspendre le compte de ${admin.fullName} ? Cette action bloque immédiatement l'accès au système. Utile en cas de comportement suspect ou de violation des règles.`,
        confirmLabel: 'Suspendre',
        cancelLabel: 'Annuler',
        type: 'danger',
        icon: 'gpp_bad'
      } as ConfirmationDialogData
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) this.changeStatus(admin, AdminStatus.SUSPENDED);
    });
  }

  private changeStatus(admin: Admin, newStatus: AdminStatus): void {
    this.adminService.changeAdminStatus(admin.id, newStatus).subscribe({
      next: () => {
        const labels: Record<string, string> = {
          [AdminStatus.ACTIVE]: 'activé',
          [AdminStatus.INACTIVE]: 'désactivé',
          [AdminStatus.SUSPENDED]: 'suspendu',
          [AdminStatus.PENDING]: 'mis en attente'
        };
        this.toastr.success(
          `Le compte de ${admin.fullName} a été ${labels[newStatus]} avec succès.`,
          'Statut modifié'
        );
        this.loadAdmins();
      },
      error: (err) => {
        console.error('Erreur lors du changement de statut:', err);
        this.toastr.error(
          'Une erreur est survenue lors du changement de statut.',
          'Erreur'
        );
      }
    });
  }

  deleteAdmin(admin: Admin): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'Supprimer définitivement',
        message: `Êtes-vous sûr de vouloir supprimer définitivement le compte de ${admin.fullName} (${admin.email}) ? Cette action supprime le profil ET le compte de connexion. Elle est irréversible.`,
        confirmLabel: 'Supprimer définitivement',
        cancelLabel: 'Annuler',
        type: 'danger',
        icon: 'delete_forever'
      } as ConfirmationDialogData
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.adminService.deleteAdmin(admin.id).subscribe({
          next: () => {
            this.toastr.success(
              `Le compte de ${admin.fullName} a été supprimé définitivement.`,
              'Compte supprimé'
            );
            this.loadAdmins();
          },
          error: (err) => {
            console.error('Erreur lors de la suppression:', err);
            this.toastr.error(
              'Une erreur est survenue lors de la suppression du compte.',
              'Erreur'
            );
          }
        });
      }
    });
  }

  // ---- Permissions Toggle ----

  togglePermissions(adminId: string): void {
    this.expandedAdminId = this.expandedAdminId === adminId ? null : adminId;
  }

  get paginatedAdmins(): Admin[] {
    return this.filteredAdmins();
  }

  /**
   * Traduit le nom du module de permission
   */
  getPermissionLabel(module: string): string {
    const key = `permissions.${module}`;
    const translated = this.translate.instant(key);
    // Si la traduction n'existe pas, retourner le module tel quel
    return translated !== key ? translated : module;
  }
}
