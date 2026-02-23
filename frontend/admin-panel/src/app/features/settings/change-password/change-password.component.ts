// src/app/features/settings/change-password/change-password.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
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

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss'],
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);

  changePasswordForm: FormGroup = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  });

  loading = false;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.changePasswordForm.value;

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
      .changePassword(currentPassword, newPassword)
      .subscribe({
        next: () => {
          this.loading = false;
          this.changePasswordForm.reset();
          this.dialog.open(ConfirmationDialogComponent, {
            data: {
              type: 'success',
              title: this.translate.instant('common.confirm'),
              message: this.translate.instant('settings.passwordChangedSuccess'),
              confirmLabel: 'common.confirm',
            } as ConfirmationDialogData,
            width: '400px',
          });
        },
        error: (err) => {
          this.loading = false;
          this.toastr.error(
            err.error?.message || this.translate.instant('settings.passwordChangeError'),
            this.translate.instant('common.error')
          );
        },
      });
  }
}
