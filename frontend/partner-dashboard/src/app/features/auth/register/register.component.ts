// src/app/features/auth/register/register.component.ts - Phase 1: Simple Signup
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { RegisterRequest } from '@core/models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
    TranslateModule,
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  // Signals
  hidePassword = signal(true);
  hideConfirmPassword = signal(true);
  loading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  currentLang = signal('fr');
  currentYear = new Date().getFullYear();

  availableLanguages = [
    { code: 'fr', name: 'Français', flag: '🇫🇷' },
    { code: 'en', name: 'English', flag: '🇬🇧' },
    { code: 'ar', name: 'العربية', flag: '🇹🇳' },
  ];

  // Simple registration form - only fields needed by auth-service
  registerForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    password: ['', [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
    confirmPassword: ['', Validators.required],
    acceptTerms: [false, Validators.requiredTrue],
  }, { validators: this.passwordMatchValidator });

  constructor() {
    const savedLang = localStorage.getItem('partnerLang') || 'fr';
    this.currentLang.set(savedLang);
    this.translate.use(savedLang);
    this.updateDirection(savedLang);
  }

  // Validators
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');
    if (!password || !confirmPassword) return null;
    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;
    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumber = /[0-9]/.test(value);
    if (!hasUpperCase || !hasLowerCase || !hasNumber) {
      return { weakPassword: true };
    }
    return null;
  }

  // Password visibility
  togglePasswordVisibility(): void {
    this.hidePassword.update(v => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword.update(v => !v);
  }

  // Language
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

  // Submit
  onSubmit(): void {
    if (this.registerForm.invalid) {
      Object.keys(this.registerForm.controls).forEach(key => {
        this.registerForm.controls[key].markAsTouched();
      });
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const formValue = this.registerForm.value;

    const registerData: RegisterRequest = {
      firstName: formValue.firstName.trim(),
      lastName: formValue.lastName.trim(),
      email: formValue.email.trim().toLowerCase(),
      phoneNumber: formValue.phoneNumber.trim(),
      password: formValue.password,
      role: 'PARTNER',
    };

    this.authService.register(registerData).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.router.navigate(['/auth/registration-success'], {
          queryParams: { email: registerData.email }
        });
      },
      error: (err) => {
        this.loading.set(false);
        const message = err.error?.message || err.error?.error || 'auth.register.error';
        this.errorMessage.set(message);
      },
    });
  }
}
