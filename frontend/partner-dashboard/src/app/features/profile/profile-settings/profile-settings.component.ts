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
import { TranslateModule } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';

@Component({
  selector: 'app-profile-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './profile-settings.component.html',
  styleUrls: ['./profile-settings.component.scss'],
})
export class ProfileSettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  loading = signal(false);
  saving = signal(false);
  changingPassword = signal(false);
  hideCurrentPassword = signal(true);
  hideNewPassword = signal(true);

  profileForm: FormGroup = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
  });

  passwordForm: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.profileForm.patchValue(profile);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading profile:', err);
        this.loading.set(false);
      }
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    
    this.saving.set(true);
    this.profileService.updateProfile(this.profileForm.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Profil mis à jour', 'Fermer', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error saving profile:', err);
        this.saving.set(false);
        this.snackBar.open('Erreur lors de la mise à jour', 'Fermer', { duration: 3000 });
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    
    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.value;
    
    if (newPassword !== confirmPassword) {
      this.snackBar.open('Les mots de passe ne correspondent pas', 'Fermer', { duration: 3000 });
      return;
    }
    
    this.changingPassword.set(true);
    this.profileService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.changingPassword.set(false);
        this.passwordForm.reset();
        this.snackBar.open('Mot de passe modifié', 'Fermer', { duration: 3000 });
      },
      error: (err: any) => {
        console.error('Error changing password:', err);
        this.changingPassword.set(false);
        this.snackBar.open('Erreur lors du changement de mot de passe', 'Fermer', { duration: 3000 });
      }
    });
  }
}
