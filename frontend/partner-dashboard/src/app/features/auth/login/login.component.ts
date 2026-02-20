// src/app/features/auth/login/login.component.ts - Login with OTP verification
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';

type LoginStep = 'credentials' | 'otp';

@Component({
  selector: 'app-login',
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
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    TranslateModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  // Forms
  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    rememberMe: [false],
  });

  otpForm: FormGroup = this.fb.group({
    otpCode: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
  });

  // Signals
  hidePassword = signal(true);
  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  currentLang = signal('fr');
  currentYear = new Date().getFullYear();
  
  // OTP flow
  currentStep = signal<LoginStep>('credentials');
  otpEmail = signal('');
  otpExpirationMinutes = signal(5);
  resendCountdown = signal(0);
  private resendTimer: any;

  availableLanguages = [
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'en', name: 'English', flag: '🇬🇧' },
    { code: 'ar', name: 'العربية', flag: '🇹🇳' },
  ];

  constructor() {
    const savedLang = localStorage.getItem('partnerLang') || 'fr';
    this.currentLang.set(savedLang);
    this.translate.use(savedLang);
    this.updateDirection(savedLang);
  }

  togglePasswordVisibility(): void {
    this.hidePassword.update(v => !v);
  }

  changeLanguage(lang: string): void {
    this.currentLang.set(lang);
    this.translate.use(lang);
    localStorage.setItem('partnerLang', lang);
    this.updateDirection(lang);
  }

  updateDirection(lang: string): void {
    const htmlElement = document.documentElement;
    if (lang === 'ar') {
      htmlElement.setAttribute('dir', 'rtl');
      htmlElement.setAttribute('lang', 'ar');
    } else {
      htmlElement.setAttribute('dir', 'ltr');
      htmlElement.setAttribute('lang', lang);
    }
  }

  // Step 1: Submit credentials → backend returns JWT (verified) or OTP info (first login)
  onSubmitCredentials(): void {
    if (this.loginForm.invalid) {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.controls[key].markAsTouched();
      });
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const credentials = {
      email: this.loginForm.value.email,
      password: this.loginForm.value.password,
    };

    this.authService.login(credentials).subscribe({
      next: (response: any) => {
        this.loading.set(false);

        console.log('========== LOGIN RESPONSE ==========');
        console.log('Full response:', response);
        console.log('User:', response.user);
        console.log('User partnerId:', response.user?.partnerId);
        console.log('isNewUser:', response.isNewUser);

        // Check if backend returned JWT tokens directly (email already verified)
        if (response.access_token) {
          // Tokens already saved by auth.service.ts tap()
          // Navigate to dashboard or complete-profile
          if (response.isNewUser || !response.user?.partnerId) {
            console.log('❌ Redirecting to complete-profile (partnerId missing)');
            this.router.navigate(['/auth/complete-profile']);
          } else {
            console.log('✅ Redirecting to dashboard (partnerId exists:', response.user.partnerId, ')');
            this.router.navigate(['/dashboard']);
          }
        } else {
          // Backend sent OTP (first login, email not verified)
          this.otpEmail.set(response.email || credentials.email);
          this.otpExpirationMinutes.set(response.expirationMinutes || 5);
          this.successMessage.set(response.message || '');
          this.currentStep.set('otp');
          this.startResendCountdown();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'auth.login.error');
      },
    });
  }

  // Step 2: Verify OTP → get JWT tokens
  onSubmitOtp(): void {
    if (this.otpForm.invalid) {
      this.otpForm.controls['otpCode'].markAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.verifyOtp({
      email: this.otpEmail(),
      otpCode: this.otpForm.value.otpCode,
      type: 'LOGIN',
    }).subscribe({
      next: (authResponse) => {
        this.loading.set(false);

        console.log('========== VERIFY OTP RESPONSE ==========');
        console.log('Full response:', authResponse);
        console.log('User:', authResponse.user);
        console.log('User partnerId:', authResponse.user?.partnerId);
        console.log('isNewUser:', authResponse.isNewUser);

        // Check if partner needs to complete profile
        if (authResponse.isNewUser || !authResponse.user?.partnerId) {
          console.log('❌ Redirecting to complete-profile (partnerId missing)');
          this.router.navigate(['/auth/complete-profile']);
        } else {
          console.log('✅ Redirecting to dashboard (partnerId exists:', authResponse.user.partnerId, ')');
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'auth.login.otpInvalid');
      },
    });
  }

  // Resend OTP
  resendOtp(): void {
    if (this.resendCountdown() > 0) return;

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.resendOtp(this.otpEmail()).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.successMessage.set(response.message || 'auth.login.otpResent');
        this.startResendCountdown();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'auth.login.resendError');
      },
    });
  }

  // Go back to credentials step
  backToCredentials(): void {
    this.currentStep.set('credentials');
    this.otpForm.reset();
    this.errorMessage.set('');
    this.successMessage.set('');
    this.clearResendTimer();
  }

  // Resend countdown timer
  private startResendCountdown(): void {
    this.clearResendTimer();
    this.resendCountdown.set(60);
    this.resendTimer = setInterval(() => {
      this.resendCountdown.update(v => {
        if (v <= 1) {
          this.clearResendTimer();
          return 0;
        }
        return v - 1;
      });
    }, 1000);
  }

  private clearResendTimer(): void {
    if (this.resendTimer) {
      clearInterval(this.resendTimer);
      this.resendTimer = null;
    }
  }

  forgotPassword(): void {
    this.router.navigate(['/auth/forgot-password']);
  }
}
