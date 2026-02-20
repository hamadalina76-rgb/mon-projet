// src/app/features/auth/forgot-password/forgot-password.component.ts
import { Component, inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
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
    MatSelectModule,
    TranslateModule,
  ],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
})
export class ForgotPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);

  forgotForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  resetForm: FormGroup = this.fb.group({
    otpCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  });

  loading = false;
  step: 'email' | 'reset' = 'email';
  userEmail = '';
  currentLang = 'fr';
  currentYear = new Date().getFullYear();
  availableLanguages = [
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'en', name: 'English', flag: '🇬🇧' },
    { code: 'ar', name: 'العربية', flag: '🇹🇳' },
  ];

  constructor() {
    const savedLang = localStorage.getItem('partnerLang') || 'fr';
    this.currentLang = savedLang;
    this.updateDirection(savedLang);
  }

  ngOnInit(): void {
    this.translate.use(this.currentLang).subscribe(() => this.cdr.markForCheck());
  }

  changeLanguage(lang: string): void {
    this.currentLang = lang;
    this.translate.use(lang);
    localStorage.setItem('partnerLang', lang);
    this.updateDirection(lang);
  }

  updateDirection(lang: string): void {
    const html = document.documentElement;
    if (lang === 'ar') {
      html.setAttribute('dir', 'rtl');
      html.setAttribute('lang', 'ar');
    } else {
      html.setAttribute('dir', 'ltr');
      html.setAttribute('lang', lang);
    }
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.userEmail = this.forgotForm.value.email;
    this.authService.requestPasswordReset(this.userEmail).subscribe({
      next: () => {
        this.loading = false;
        this.step = 'reset';
        this.toastr.success(
          this.translate.instant('auth.login.fpCodeSentTo') + ' ' + this.userEmail,
          this.translate.instant('COMMON.CONFIRM')
        );
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(
          err.error?.message || this.translate.instant('auth.login.fpSendError'),
          this.translate.instant('COMMON.ERROR')
        );
      },
    });
  }

  onResetPassword(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }
    const { newPassword, confirmPassword, otpCode } = this.resetForm.value;
    if (newPassword !== confirmPassword) {
      this.toastr.error(
        this.translate.instant('auth.login.fpPasswordMismatch'),
        this.translate.instant('COMMON.ERROR')
      );
      return;
    }
    this.loading = true;
    this.authService
      .resetPassword({ email: this.userEmail, otpCode, newPassword })
      .subscribe({
        next: () => {
          this.loading = false;
          this.toastr.success(
            this.translate.instant('auth.login.fpResetSuccess'),
            this.translate.instant('COMMON.CONFIRM')
          );
          setTimeout(() => this.router.navigate(['/auth/login']), 2000);
        },
        error: (err) => {
          this.loading = false;
          this.toastr.error(
            err.error?.message || this.translate.instant('auth.login.fpResetError'),
            this.translate.instant('COMMON.ERROR')
          );
        },
      });
  }

  backToEmail(): void {
    this.step = 'email';
    this.resetForm.reset();
  }
}
