// src/app/features/auth/login/login.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '@core/services/auth.service';
import { ActivityLogService } from '@core/services/activity-log.service';

@Component({
  selector: 'app-login',
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
    MatCheckboxModule,
    MatSelectModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-10px)' }))
      ])
    ])
  ]
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translateService = inject(TranslateService);
  private toastr = inject(ToastrService);
  private activityLog = inject(ActivityLogService);

  loginForm!: FormGroup;
  loading = false;
  hidePassword = true;
  isDark = false;
  errorMessage = '';
  returnUrl = '/dashboard';

  languages = [
    { value: 'en', label: 'English (EN)' },
    { value: 'fr', label: 'Français (FR)' },
    { value: 'ar', label: 'العربية (AR)' },
  ];
  selectedLanguage = localStorage.getItem('language') || this.translateService.currentLang || this.translateService.defaultLang || 'fr';
  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false],
    });

    this.returnUrl =
      this.route.snapshot.queryParams['returnUrl'] || '';

    // Sync selected language with TranslateService
    this.selectedLanguage = this.translateService.currentLang || 'fr';

    // If already authenticated, redirect
    if (this.authService.isAuthenticated()) {
      this.router.navigate([this.returnUrl || this.getFirstPermittedRoute()]);
    }

    // Detect system dark mode
    this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
  }

  onLanguageChange(lang: string): void {
    this.selectedLanguage = lang;
    this.translateService.use(lang);
    localStorage.setItem('language', lang);
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const credentials = {
      email: this.loginForm.value.email,
      password: this.loginForm.value.password,
      rememberMe: this.loginForm.value.rememberMe,
    };

    this.authService.login(credentials).subscribe({
      next: () => {
        // Navigate to first permitted route
        const firstRoute = this.getFirstPermittedRoute();
        console.log('Login successful, navigating to:', firstRoute);
        this.router.navigate([firstRoute]);
      },
      error: (err) => {
        this.loading = false;
        console.error('Login error:', err);
        const code = err.error?.code || '';
        const serverMsg = err.error?.message || err.message || '';
        
        // Map backend error code (priorité) puis message
        let errorKey = 'auth.invalidCredentials';
        
        if (code === 'ACCOUNT_SUSPENDED' || serverMsg.includes('SUSPENDED') || serverMsg.includes('suspendu')) {
          errorKey = 'auth.accountSuspended';
        } else if (code === 'ACCOUNT_INACTIVE' || serverMsg.includes('INACTIVE') || serverMsg.includes('désactivé') || serverMsg.includes('deactivated')) {
          errorKey = 'auth.accountInactive';
        } else if (code === 'ACCOUNT_DELETED' || serverMsg.includes('DELETED') || serverMsg.includes('supprimé')) {
          errorKey = 'auth.accountDeleted';
        } else if (code === 'ACCOUNT_PENDING' || serverMsg.includes('PENDING') || serverMsg.includes('vérification')) {
          errorKey = 'auth.accountPending';
        } else if (code === 'ACCESS_DENIED' || serverMsg.includes('Access denied') || serverMsg.includes('accès refusé')) {
          errorKey = 'auth.accessDenied';
        }
        
        this.errorMessage = this.translateService.instant(errorKey);
        
        this.toastr.error(
          this.errorMessage,
          this.translateService.instant('auth.login'),
        );
      },
    });
  }

  /**
   * Returns the first route the user has permission to access.
   * Matches the sidebar nav items order so the user lands on
   * the most relevant page after login.
   */
  private getFirstPermittedRoute(): string {
    const permissionRouteMap: { permission: string; route: string }[] = [
      { permission: 'dashboard:view', route: '/dashboard' },
      { permission: 'orders:view', route: '/orders' },
      { permission: 'partners:view', route: '/partners' },
      { permission: 'delivery:view', route: '/delivery' },
      { permission: 'users:view', route: '/users/couriers' },
      { permission: 'admins:view', route: '/users/admins' },
      { permission: 'payments:view', route: '/payments' },
      { permission: 'promotions:view', route: '/promotions' },
      { permission: 'reviews:view', route: '/reviews' },
      { permission: 'support:view', route: '/support' },
      { permission: 'zones:view', route: '/zones' },
      { permission: 'analytics:view', route: '/analytics' },
      { permission: 'notifications:view', route: '/notifications' },
      { permission: 'settings:view', route: '/settings' },
      { permission: 'monitoring:view', route: '/monitoring' },
    ];

    for (const entry of permissionRouteMap) {
      if (this.authService.hasPermission(entry.permission)) {
        return entry.route;
      }
    }

    // Fallback — should never happen for a valid admin
    return '/dashboard';
  }

  get emailError(): string {
    const ctrl = this.loginForm.get('email');
    if (ctrl?.hasError('required')) return 'Email is required';
    if (ctrl?.hasError('email')) return 'Invalid email format';
    return '';
  }

  get passwordError(): string {
    const ctrl = this.loginForm.get('password');
    if (ctrl?.hasError('required')) return 'Password is required';
    if (ctrl?.hasError('minlength')) return 'Minimum 6 characters';
    return '';
  }

  getErrorIcon(): string {
    const msg = this.errorMessage.toLowerCase();
    if (msg.includes('suspendu') || msg.includes('suspended') || msg.includes('تعليق')) {
      return 'gpp_bad';
    }
    if (msg.includes('désactivé') || msg.includes('deactivated') || msg.includes('تعطيل')) {
      return 'block';
    }
    if (msg.includes('supprimé') || msg.includes('deleted') || msg.includes('حذف')) {
      return 'delete_forever';
    }
    if (msg.includes('vérification') || msg.includes('verification') || msg.includes('انتظار')) {
      return 'schedule';
    }
    return 'error_outline';
  }

  getErrorTitle(): string {
    const msg = this.errorMessage.toLowerCase();
    if (msg.includes('suspendu') || msg.includes('suspended') || msg.includes('تعليق')) {
      return 'auth.accountSuspendedTitle';
    }
    if (msg.includes('désactivé') || msg.includes('deactivated') || msg.includes('تعطيل')) {
      return 'auth.accountInactiveTitle';
    }
    if (msg.includes('supprimé') || msg.includes('deleted') || msg.includes('حذف')) {
      return 'auth.accountDeletedTitle';
    }
    if (msg.includes('vérification') || msg.includes('verification') || msg.includes('انتظار')) {
      return 'auth.accountPendingTitle';
    }
    return 'auth.authenticationError';
  }
}
