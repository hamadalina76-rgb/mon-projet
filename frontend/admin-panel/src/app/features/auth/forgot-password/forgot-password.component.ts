// src/app/features/auth/forgot-password/forgot-password.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);

  // Étape 1 : Demander l'email
  forgotForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  // Étape 2 : Entrer le code OTP et le nouveau mot de passe
  resetForm: FormGroup = this.fb.group({
    otpCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  });

  loading = false;
  sent = false; // true quand l'email avec OTP est envoyé
  step: 'email' | 'reset' = 'email'; // Contrôle quelle étape afficher
  userEmail = ''; // Stocke l'email pour l'étape 2
  isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

  toggleTheme(): void {
    this.isDark = !this.isDark;
  }

  // Étape 1 : Envoyer l'OTP par email
  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.userEmail = this.forgotForm.value.email;

    this.authService
      .forgotPassword({ email: this.userEmail })
      .subscribe({
        next: () => {
          this.loading = false;
          this.sent = true;
          this.step = 'reset'; // Passer à l'étape 2
          this.toastr.success(
            this.translate.instant('auth.otpSentSuccess'),
            this.translate.instant('common.confirm'),
          );
        },
        error: (err) => {
          this.loading = false;
          this.toastr.error(
            err.error?.message || this.translate.instant('auth.forgotPasswordError'),
            this.translate.instant('common.error')
          );
        },
      });
  }

  // Étape 2 : Réinitialiser le mot de passe avec OTP
  onResetPassword(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword, otpCode } = this.resetForm.value;

    // Vérifier que les mots de passe correspondent
    if (newPassword !== confirmPassword) {
      this.toastr.error(
        this.translate.instant('auth.passwordMismatch'),
        this.translate.instant('common.error')
      );
      return;
    }

    this.loading = true;

    this.authService
      .resetPassword({
        email: this.userEmail,
        otpCode: otpCode,
        newPassword: newPassword,
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.toastr.success(
            this.translate.instant('auth.passwordResetSuccess'),
            this.translate.instant('common.confirm'),
          );
          // Rediriger vers la page de connexion après 2 secondes
          setTimeout(() => {
            window.location.href = '/auth/login';
          }, 2000);
        },
        error: (err) => {
          this.loading = false;
          this.toastr.error(
            err.error?.message || this.translate.instant('auth.resetPasswordError'),
            this.translate.instant('common.error')
          );
        },
      });
  }

  // Retour à l'étape 1 pour renvoyer un code
  backToEmail(): void {
    this.step = 'email';
    this.sent = false;
    this.resetForm.reset();
  }
}
