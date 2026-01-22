// src/app/shared/components/confirmation-dialog/confirmation-dialog.component.ts
import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

export interface ConfirmationDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  type?: 'info' | 'warning' | 'danger';
  icon?: string;
}

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="confirmation-dialog" [attr.data-type]="data.type || 'info'">
      <div class="dialog-icon">
        <mat-icon>{{ data.icon || getDefaultIcon() }}</mat-icon>
      </div>
      <h2 mat-dialog-title>{{ data.title | translate }}</h2>
      <mat-dialog-content>
        <p>{{ data.message | translate }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button (click)="onCancel()">
          {{ (data.cancelLabel || 'common.cancel') | translate }}
        </button>
        <button
          mat-flat-button
          [color]="getButtonColor()"
          (click)="onConfirm()"
        >
          {{ (data.confirmLabel || 'common.confirm') | translate }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .confirmation-dialog {
        text-align: center;
        padding: 1rem;
      }

      .dialog-icon {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 1rem;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }
      }

      [data-type='info'] .dialog-icon {
        background: rgba(79, 70, 229, 0.1);
        color: #4f46e5;
      }

      [data-type='warning'] .dialog-icon {
        background: rgba(245, 158, 11, 0.1);
        color: #f59e0b;
      }

      [data-type='danger'] .dialog-icon {
        background: rgba(239, 68, 68, 0.1);
        color: #ef4444;
      }

      h2 {
        margin: 0 0 0.5rem;
      }

      mat-dialog-content p {
        color: var(--text-secondary);
        margin: 0;
      }

      mat-dialog-actions {
        margin-top: 1.5rem;
        justify-content: center;
      }
    `,
  ],
})
export class ConfirmationDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmationDialogData
  ) {}

  getDefaultIcon(): string {
    switch (this.data.type) {
      case 'warning':
        return 'warning';
      case 'danger':
        return 'error';
      default:
        return 'help';
    }
  }

  getButtonColor(): 'primary' | 'accent' | 'warn' {
    return this.data.type === 'danger' ? 'warn' : 'primary';
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
