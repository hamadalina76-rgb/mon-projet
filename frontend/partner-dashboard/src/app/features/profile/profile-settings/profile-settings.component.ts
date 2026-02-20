// src/app/features/profile/profile-settings/profile-settings.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { ApiService } from '@core/services/api.service';

@Component({
  selector: 'app-profile-settings',
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
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './profile-settings.component.html',
  styleUrls: ['./profile-settings.component.scss'],
})
export class ProfileSettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private api = inject(ApiService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  saving = signal(false);
  changingPassword = signal(false);
  hideCurrentPassword = signal(true);
  hideNewPassword = signal(true);

  profileForm: FormGroup = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: [{ value: '', disabled: true }],
    phoneNumber: [''],
  });

  passwordForm: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.profileForm.patchValue({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        email: user.email || '',
        phoneNumber: user.phoneNumber || user.phone || '',
      });
    }
    // Charger le profil depuis l'API pour avoir le phoneNumber à jour (cas session existante sans phoneNumber)
    this.api.get<{ id: number; email: string; firstName: string; lastName: string; phoneNumber?: string }>('v1/auth/profile').subscribe({
      next: (profile) => {
        this.profileForm.patchValue({
          firstName: profile.firstName || '',
          lastName: profile.lastName || '',
          email: profile.email || '',
          phoneNumber: profile.phoneNumber || '',
        });
        // Mettre à jour le user stocké avec les données complètes
        this.authService.updateStoredUser({
          firstName: profile.firstName,
          lastName: profile.lastName,
          phoneNumber: profile.phoneNumber,
          phone: profile.phoneNumber,
        });
      },
      error: () => {},
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;

    this.saving.set(true);
    const { firstName, lastName, phoneNumber } = this.profileForm.getRawValue();

    this.api.put('v1/auth/profile', { firstName, lastName, phoneNumber }).subscribe({
      next: (updated: any) => {
        // Update local cache with all returned fields including phoneNumber
        const phone = updated?.phoneNumber ?? phoneNumber ?? '';
        this.authService.updateStoredUser({
          firstName: updated?.firstName ?? firstName,
          lastName: updated?.lastName ?? lastName,
          phoneNumber: phone,
          phone: phone,
        });
        this.saving.set(false);
        this.snackBar.open(this.translate.instant('profilePages.profileUpdated'), 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error saving profile:', err);
        this.saving.set(false);
        const msg = err?.error?.message || err?.message || this.translate.instant('profilePages.loadError');
        this.snackBar.open(msg, this.translate.instant('profilePages.close'), { duration: 4000 });
      },
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;

    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.value;

    if (newPassword !== confirmPassword) {
      this.snackBar.open(this.translate.instant('profilePages.passwordMismatch'), this.translate.instant('profilePages.close'), { duration: 3000 });
      return;
    }

    this.changingPassword.set(true);
    this.api.post('v1/auth/change-password', { currentPassword, newPassword }).subscribe({
      next: () => {
        this.changingPassword.set(false);
        this.passwordForm.reset();
        this.snackBar.open(this.translate.instant('profilePages.passwordChanged'), 'OK', { duration: 3000 });
      },
      error: (err: any) => {
        console.error('Error changing password:', err);
        this.changingPassword.set(false);
        this.snackBar.open(
          err?.error?.message || this.translate.instant('profilePages.loadError'),
          'Fermer',
          { duration: 3000 }
        );
      },
    });
  }
}
