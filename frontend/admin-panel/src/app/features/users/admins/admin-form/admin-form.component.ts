// src/app/features/users/admins/admin-form/admin-form.component.ts
import { Component, OnInit, OnDestroy, inject, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminService } from '@core/services/admin.service';
import { AdminRole, Permission } from '@core/models/admin.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-admin-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    TranslateModule,
  ],
  templateUrl: './admin-form.component.html',
  styleUrls: ['./admin-form.component.scss'],
})
export class AdminFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private adminService = inject(AdminService);
  private translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  adminForm!: FormGroup;

  isEditMode = false;
  loading = signal(false);
  roles = signal<AdminRole[]>([]);
  permissions = signal<Permission[]>([]);
  selectedPermissions = signal<Set<string>>(new Set());
  currentLang = signal<string>('fr');

  ngOnInit(): void {
    // S'abonner aux changements de langue
    this.currentLang.set(this.translate.currentLang || 'fr');
    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((event) => {
        this.currentLang.set(event.lang);
        this.cdr.markForCheck(); // Forcer la détection de changement
      });

    const id = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!id;

    // Build form — password only required in create mode
    this.adminForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', this.isEditMode ? [] : [Validators.required, Validators.minLength(8)]],
      role: ['', Validators.required],
    });

    this.loadRoles();
    this.loadPermissions();

    if (id) {
      this.loadAdmin(id);
    }
  }

  loadRoles(): void {
    this.adminService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles)
    });
  }

  loadPermissions(): void {
    this.adminService.getAvailablePermissions().subscribe({
      next: (permissions) => this.permissions.set(permissions)
    });
  }

  loadAdmin(id: string): void {
    this.loading.set(true);
    this.adminService.getAdminById(id).subscribe({
      next: (admin) => {
        if (admin) {
          this.adminForm.patchValue({
            fullName: admin.fullName,
            email: admin.email,
            role: admin.role.id
          });

          // Pre-check the permissions that were previously saved (customPermissions)
          if (admin.permissions && admin.permissions.length > 0) {
            this.selectedPermissions.set(new Set(admin.permissions));
          } else if (admin.role?.permissions?.length) {
            // Fallback: pre-select permissions from the role definition
            const enabledModules = admin.role.permissions
              .filter(p => p.enabled)
              .map(p => p.module);
            this.selectedPermissions.set(new Set(enabledModules));
          }
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onRoleChange(): void {
    const roleId = this.adminForm.get('role')?.value;
    if (!roleId) return;

    const role = this.roles().find(r => r.id === roleId);
    if (role?.permissions?.length) {
      // Auto-select enabled permissions from the chosen role
      const enabledModules = role.permissions
        .filter(p => p.enabled)
        .map(p => p.module);
      this.selectedPermissions.set(new Set(enabledModules));
    }
  }

  togglePermission(moduleId: string): void {
    const current = new Set(this.selectedPermissions());
    if (current.has(moduleId)) {
      current.delete(moduleId);
    } else {
      current.add(moduleId);
    }
    this.selectedPermissions.set(current);
  }

  isPermissionSelected(moduleId: string): boolean {
    return this.selectedPermissions().has(moduleId);
  }

  getRoleName(roleId: string): string {
    const role = this.roles().find(r => r.id === roleId);
    return role ? role.label : '';
  }

  getRoleColor(roleId: string): string {
    const role = this.roles().find(r => r.id === roleId);
    return role ? role.color : '#94A3B8';
  }

  onSubmit(): void {
    if (this.adminForm.invalid) {
      Object.keys(this.adminForm.controls).forEach(key => {
        this.adminForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading.set(true);
    const formData = this.adminForm.value;
    const role = this.roles().find(r => r.id === formData.role);

    const adminData = {
      ...formData,
      role,
      permissions: Array.from(this.selectedPermissions())
    };

    const request$ = this.isEditMode
      ? this.adminService.updateAdmin(this.route.snapshot.paramMap.get('id')!, adminData)
      : this.adminService.createAdminWithAuth(adminData);

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/users/admins']);
      },
      error: (error) => {
        console.error('Erreur lors de la sauvegarde de l\'admin:', error);
        this.loading.set(false);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/users/admins']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Retourne la description de la permission dans la langue courante
   */
  getPermissionDescription(permission: Permission): string {
    const lang = this.currentLang();
    
    switch(lang) {
      case 'en':
        return permission.descriptionEn || permission.description;
      case 'ar':
        return permission.descriptionAr || permission.description;
      default:
        return permission.description;
    }
  }

  /**
   * Retourne le nom du module de permission dans la langue courante
   */
  getPermissionLabel(permission: Permission): string {
    const key = `permissions.${permission.module}`;
    const translated = this.translate.instant(key);
    // Si la traduction n'existe pas, retourner le label par défaut
    return translated !== key ? translated : permission.moduleLabel;
  }
}
