import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

export interface ReasonDialogData {
  title: string;
  message?: string;
  placeholder?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  minLength?: number;
  icon?: string;
}

@Component({
  selector: 'app-reason-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  template: `
    <div class="reason-dialog request-like-dialog">
      <h2 mat-dialog-title>{{ data.title }}</h2>
      <mat-dialog-content>
        <p *ngIf="data.message" class="message">{{ data.message }}</p>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ data.placeholder || 'Motif' }}</mat-label>
          <textarea
            matInput
            rows="6"
            [(ngModel)]="reason"
            [placeholder]="data.placeholder || 'Motif'"
            cdkFocusInitial>
          </textarea>
        </mat-form-field>
        <div class="hint" [class.hint-error]="!isValid() && reason.trim().length > 0">
          Minimum {{ data.minLength ?? 3 }} caractères
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="onCancel()">
          {{ data.cancelLabel || 'Annuler' }}
        </button>
        <button mat-flat-button color="primary" [disabled]="!isValid()" (click)="onConfirm()">
          {{ data.confirmLabel || 'Confirmer' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      max-width: 100%;
      overflow: hidden;
    }

    .reason-dialog {
      width: 100%;
      max-width: 100%;
      min-width: 0;
      box-sizing: border-box;
      padding: 0;
      overflow: hidden;
    }

    h2[mat-dialog-title] {
      margin: 0 0 1rem;
      font-size: 1.375rem;
      font-weight: 700;
      color: #111827;
      letter-spacing: -0.01em;
      line-height: 1.3;
      padding: 1.25rem 1.25rem 0;
    }

    .reason-dialog mat-dialog-content {
      margin: 0;
      padding: 0 1.25rem 0.75rem;
      max-height: 70vh;
      overflow-y: auto;
      overflow-x: hidden;
      box-sizing: border-box;
    }

    .message {
      margin-bottom: 0.9rem;
      color: #475569;
      font-size: 0.9375rem;
      font-weight: 400;
      line-height: 1.45;
    }

    .full-width {
      width: 100%;
    }

    .reason-dialog mat-dialog-content mat-form-field {
      width: 100%;
      display: block;
    }

    .reason-dialog mat-dialog-content textarea[matInput] {
      font-size: 0.9375rem;
      line-height: 1.5;
      color: #111827;
      resize: vertical;
      min-height: 120px;
      max-height: 280px;
      box-sizing: border-box;
    }

    .hint {
      margin-top: 0.3rem;
      color: #94A3B8;
      font-size: 0.8rem;
    }
    .hint-error {
      color: #dc2626;
    }

    .reason-dialog mat-dialog-actions {
      padding: 0.75rem 1.25rem 1.25rem;
      margin: 0;
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      flex-wrap: wrap;
      position: sticky;
      bottom: 0;
      z-index: 2;
      background: #fff;
      border-top: 1px solid #f1f5f9;
    }

    .reason-dialog mat-dialog-actions button {
      font-weight: 600;
      font-size: 0.9375rem;
      padding: 0.625rem 1.25rem;
      border-radius: 10px;
      min-width: 100px;
    }

    .reason-dialog mat-dialog-actions button[mat-button] {
      color: #4B5563;
    }

    .reason-dialog mat-dialog-actions button[mat-button]:hover {
      background: #f3f4f6;
      color: #111827;
    }

    .reason-dialog mat-dialog-actions button[mat-flat-button] {
      background: #E31E24;
      color: #fff;
    }

    .reason-dialog mat-dialog-actions button[mat-flat-button]:hover:not(:disabled) {
      background: #c81b20;
    }

    .reason-dialog mat-dialog-actions button[mat-flat-button]:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    @media (max-width: 640px) {
      .reason-dialog {
        width: 100%;
      }
      h2[mat-dialog-title] {
        font-size: 1.25rem;
        padding: 1rem 1rem 0;
      }
      .reason-dialog mat-dialog-content {
        padding: 0 1rem 0.75rem;
      }
      .reason-dialog mat-dialog-content textarea[matInput] {
        font-size: 0.875rem;
        min-height: 100px;
      }
      .reason-dialog mat-dialog-actions {
        padding: 0.75rem 1rem 1rem;
        flex-direction: column-reverse;
        gap: 0.4rem;
      }
      .reason-dialog mat-dialog-actions button {
        width: 100%;
        min-width: auto;
      }
    }

    @media (max-width: 420px) {
      h2[mat-dialog-title] {
        font-size: 1.125rem;
        padding: 0.75rem 0.75rem 0;
      }
      .reason-dialog mat-dialog-content {
        padding: 0 0.75rem 0.75rem;
      }
      .reason-dialog mat-dialog-actions {
        padding: 0.75rem;
      }
    }
  `]
})
export class ReasonDialogComponent {
  reason = '';

  constructor(
    private dialogRef: MatDialogRef<ReasonDialogComponent, string | null>,
    @Inject(MAT_DIALOG_DATA) public data: ReasonDialogData
  ) {}

  isValid(): boolean {
    const minLength = this.data.minLength ?? 3;
    return this.reason.trim().length >= minLength;
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onConfirm(): void {
    this.dialogRef.close(this.reason.trim());
  }
}

