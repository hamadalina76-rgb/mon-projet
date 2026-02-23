// src/app/features/settings/profile/profile.component.ts
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '@core/services/auth.service';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogData,
} from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { AdminService } from '@core/services/admin.service';
import { Admin } from '@core/models/admin.model';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private adminService = inject(AdminService);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);

  profileForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    phoneNumber: [''],
    email: [{ value: '', disabled: true }],
  });

  loading = signal(false);
  loadingData = signal(true);
  admin = signal<Admin | null>(null);

  initials = computed(() => {
    const a = this.admin();
    const fn = this.profileForm.get('firstName')?.value;
    const ln = this.profileForm.get('lastName')?.value;
    if (fn || ln) return ((fn?.[0] || '') + (ln?.[0] || '')).toUpperCase() || 'A';
    if (a?.fullName) {
      const parts = a.fullName.trim().split(/\s+/);
      return (parts[0]?.[0] || '') + (parts[1]?.[0] || parts[0]?.[1] || '') || 'A';
    }
    return 'A';
  });

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (!user) {
      this.loadingData.set(false);
      return;
    }

    this.profileForm.patchValue({
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      email: user.email ?? '',
    });

    this.loadingData.set(true);
    forkJoin({
      profile: this.authService.getProfile().pipe(catchError(() => of(null))),
      admin: this.adminService.getAdminByUserId(Number(user.id)).pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ profile, admin }) => {
        if (profile) {
          this.profileForm.patchValue({
            firstName: profile.firstName ?? '',
            lastName: profile.lastName ?? '',
            phoneNumber: profile.phoneNumber ?? '',
          });
        }
        if (admin) {
          this.admin.set(admin);
          if (!profile && (admin.fullName || admin.email)) {
            this.profileForm.patchValue({ email: admin.email ?? '' });
            const parts = (admin.fullName || '').trim().split(/\s+/);
            if (parts.length >= 2 && !this.profileForm.get('firstName')?.value) {
              this.profileForm.patchValue({
                firstName: parts[0] ?? '',
                lastName: parts.slice(1).join(' ') ?? '',
              });
            } else if (parts.length === 1 && !this.profileForm.get('firstName')?.value) {
              this.profileForm.patchValue({ firstName: parts[0] ?? '' });
            }
          }
        }
        this.loadingData.set(false);
      },
      error: () => this.loadingData.set(false),
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const user = this.authService.currentUser();
    const adm = this.admin();
    if (!user || !adm) {
      this.toastr.error(
        this.translate.instant('settings.profileLoadError'),
        this.translate.instant('common.error')
      );
      return;
    }

    const { firstName, lastName, phoneNumber } = this.profileForm.getRawValue();
    const fullName = [firstName, lastName].filter(Boolean).join(' ').trim() || adm.fullName;

    this.loading.set(true);

    this.authService
      .updateProfile(firstName, lastName, phoneNumber)
      .pipe(
        switchMap(() =>
          this.adminService.updateAdmin(adm.id, {
            fullName,
            email: adm.email,
            role: adm.role,
            status: adm.status,
            avatar: adm.avatar,
            permissions: adm.permissions,
          })
        ),
        catchError((err) => {
          this.loading.set(false);
          throw err;
        })
      )
      .subscribe({
        next: (updatedAdmin) => {
          this.loading.set(false);
          this.admin.set(updatedAdmin);
          this.authService.setUser({
            ...user,
            firstName,
            lastName,
            phone: phoneNumber || undefined,
            avatar: updatedAdmin.avatar ?? user.avatar,
          });
          this.dialog.open(ConfirmationDialogComponent, {
            data: {
              type: 'success',
              title: this.translate.instant('common.confirm'),
              message: this.translate.instant('settings.profileSaveSuccess'),
              confirmLabel: 'common.confirm',
            } as ConfirmationDialogData,
            width: '400px',
          });
        },
        error: (err) => {
          this.loading.set(false);
          this.toastr.error(
            err.error?.message || this.translate.instant('settings.profileSaveError'),
            this.translate.instant('common.error')
          );
        },
      });
  }

  getRoleLabel(role: { label?: string; name?: string } | undefined): string {
    if (!role) return '—';
    return role.label || role.name || '—';
  }

  getLastLoginDisplay(): string {
    const lastLogin = this.admin()?.lastLogin;
    if (!lastLogin) return this.translate.instant('settings.never');
    if (typeof lastLogin === 'string' && lastLogin.startsWith('Il y a')) return lastLogin;
    return new Date(lastLogin as string).toLocaleString('fr-FR');
  }
}
