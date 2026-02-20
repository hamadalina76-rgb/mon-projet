import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type GenericDialogType = 'success' | 'info' | 'warning' | 'danger' | 'confirm';

export interface GenericDialogData {
  type: GenericDialogType;
  title: string;
  message: string;
  detail?: string;
  icon?: string;
  confirmText?: string;
  cancelText?: string;
  showCancel?: boolean;
}

@Component({
  selector: 'app-generic-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="generic-dialog" [attr.data-type]="data.type">
      <div class="dialog-icon-wrapper">
        <div class="dialog-icon">
          <mat-icon>{{ getIcon() }}</mat-icon>
        </div>
      </div>

      <h2 [innerHTML]="data.title"></h2>

      <div class="dialog-message" [innerHTML]="data.message"></div>

      @if (data.detail) {
        <div class="dialog-detail" [innerHTML]="data.detail"></div>
      }

      <div class="dialog-actions">
        @if (data.showCancel) {
          <button class="btn btn-secondary" (click)="onCancel()">
            {{ data.cancelText || 'Annuler' }}
          </button>
        }
        <button class="btn" [class]="'btn btn-' + data.type" (click)="onConfirm()">
          {{ data.confirmText || 'OK' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    @use 'sass:color';
    @use 'assets/styles/variables' as *;

    .generic-dialog {
      padding: 2rem;
      text-align: center;
      max-width: 600px;
      width: 100%;
    }

    .dialog-icon-wrapper {
      display: flex;
      justify-content: center;
      margin-bottom: 1.5rem;
    }

    .dialog-icon {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
      }
    }

    [data-type='success'] .dialog-icon {
      background: rgba($success-color, 0.1);
      mat-icon { color: $success-color; }
    }

    [data-type='info'] .dialog-icon {
      background: rgba($info-color, 0.1);
      mat-icon { color: $info-color; }
    }

    [data-type='warning'] .dialog-icon {
      background: rgba($warning-color, 0.1);
      mat-icon { color: $warning-color; }
    }

    [data-type='danger'] .dialog-icon {
      background: rgba($error-color, 0.1);
      mat-icon { color: $error-color; }
    }

    [data-type='confirm'] .dialog-icon {
      background: rgba($primary-color, 0.1);
      mat-icon { color: $primary-color; }
    }

    h2 {
      font-size: 1.4rem;
      font-weight: 600;
      color: $text-primary;
      margin: 0 0 0.75rem;
    }

    .dialog-message {
      color: $text-secondary;
      font-size: 0.95rem;
      line-height: 1.6;
      margin: 0 0 1.25rem;
      
      ::ng-deep {
        .success-message {
          text-align: center;
          padding: 1rem 0;
          
          .success-icon {
            font-size: 72px;
            margin-bottom: 1rem;
            animation: scaleIn 0.5s ease-out;
          }
          
          h3 {
            font-size: 1.5rem;
            font-weight: 600;
            color: $success-color;
            margin: 1rem 0;
          }
          
          p {
            font-size: 1rem;
            color: $text-secondary;
            line-height: 1.6;
            margin: 0.5rem 0;
          }
        }
        
        @keyframes scaleIn {
          0% {
            transform: scale(0);
            opacity: 0;
          }
          50% {
            transform: scale(1.1);
          }
          100% {
            transform: scale(1);
            opacity: 1;
          }
        }
      }
    }

    .dialog-detail {
      text-align: left;
      margin-bottom: 1.5rem;
      
      ::ng-deep {
        .detail-section {
          margin-bottom: 1rem;
          padding: 1rem;
          background: rgba($info-color, 0.05);
          border-radius: $border-radius-lg;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          h4 {
            font-size: 0.95rem;
            font-weight: 600;
            margin: 0 0 0.5rem;
            color: $text-primary;
          }
          
          p, ul {
            font-size: 0.85rem;
            color: $text-secondary;
            line-height: 1.6;
            margin: 0.5rem 0;
          }
          
          ul {
            padding-left: 1.5rem;
          }
          
          li {
            margin: 0.5rem 0;
          }
          
          a {
            color: $primary-color;
            text-decoration: none;
            font-weight: 500;
            
            &:hover {
              text-decoration: underline;
            }
          }
        }
      }
    }

    .dialog-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border-radius: $border-radius-md;
      font-weight: 500;
      font-size: 0.95rem;
      cursor: pointer;
      border: none;
      min-width: 120px;
      transition: background 0.2s;
    }

    .btn-secondary {
      background: $gray-100;
      color: $text-primary;
      &:hover { background: $gray-200; }
    }

    .btn-success {
      background: $success-color;
      color: $white;
      &:hover { background: color.adjust($success-color, $lightness: -8%); }
    }

    .btn-info {
      background: $info-color;
      color: $white;
      &:hover { background: color.adjust($info-color, $lightness: -8%); }
    }

    .btn-warning {
      background: $warning-color;
      color: $text-primary;
      &:hover { background: color.adjust($warning-color, $lightness: -8%); }
    }

    .btn-danger {
      background: $error-color;
      color: $white;
      &:hover { background: color.adjust($error-color, $lightness: -8%); }
    }

    .btn-confirm {
      background: $primary-color;
      color: $white;
      &:hover { background: color.adjust($primary-color, $lightness: -8%); }
    }
  `],
})
export class GenericDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<GenericDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: GenericDialogData
  ) {}

  getIcon(): string {
    if (this.data.icon) return this.data.icon;
    switch (this.data.type) {
      case 'success': return 'check_circle';
      case 'info': return 'info';
      case 'warning': return 'warning';
      case 'danger': return 'error';
      case 'confirm': return 'help_outline';
      default: return 'info';
    }
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
