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
    <div class="reason-dialog">
      <div class="dialog-head">
        <div class="title-wrap">
          <mat-icon class="head-icon">{{ data.icon || 'error_outline' }}</mat-icon>
          <h2 mat-dialog-title>{{ data.title }}</h2>
        </div>
        <button
          type="button"
          mat-icon-button
          class="close-btn"
          aria-label="Fermer"
          (click)="onCancel()"
        >
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content>
        <p *ngIf="data.message" class="message">{{ data.message }}</p>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ data.placeholder || 'Motif' }}</mat-label>
          <textarea
            matInput
            rows="4"
            [(ngModel)]="reason"
            cdkFocusInitial>
          </textarea>
        </mat-form-field>
        <div class="hint" [class.hint-error]="!isValid() && reason.trim().length > 0">
          Minimum {{ data.minLength ?? 3 }} caractères
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-stroked-button (click)="onCancel()">
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
    }

    .reason-dialog {
      width: min(520px, 92vw);
      max-width: 520px;
      min-width: 0;
      box-sizing: border-box;
    }

    .dialog-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: .75rem;
      margin-bottom: .15rem;
    }

    .title-wrap {
      display: flex;
      align-items: center;
      gap: .55rem;
      min-width: 0;
    }
    .head-icon {
      color: #E31E24;
      font-size: 1.3rem;
      width: 1.3rem;
      height: 1.3rem;
    }
    .close-btn {
      color: #64748B;
      flex-shrink: 0;
    }

    h2[mat-dialog-title] {
      margin: 0;
      font-size: 1.95rem;
      font-weight: 700;
      color: #1E293B;
      line-height: 1.25;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .reason-dialog mat-dialog-content {
      margin: 0;
      padding: .35rem 0 .45rem 0;
      max-height: min(46vh, 330px);
      overflow: auto;
    }

    .message {
      margin: .25rem 0 .9rem;
      color: #64748B;
      font-size: .95rem;
      line-height: 1.45;
    }
    .hint {
      margin-top: .25rem;
      color: #94A3B8;
      font-size: .8rem;
    }
    .hint-error {
      color: #dc2626;
    }

    .full-width {
      width: 100%;
    }

    textarea[matInput] {
      resize: vertical;
      min-height: 120px;
      max-height: 240px;
      line-height: 1.4;
    }

    .reason-dialog mat-dialog-actions {
      padding-top: .35rem;
      gap: .5rem;
      margin: 0;
      flex-wrap: wrap;
      border-top: 1px solid #f1f5f9;
    }

    @media (max-width: 640px) {
      .reason-dialog {
        width: 92vw;
      }
      h2[mat-dialog-title] {
        font-size: 1.15rem;
        white-space: normal;
      }
      .message {
        font-size: .9rem;
      }
      textarea[matInput] {
        min-height: 100px;
      }
      .reason-dialog mat-dialog-actions {
        justify-content: flex-end;
      }
    }

    @media (max-width: 420px) {
      .reason-dialog {
        width: 94vw;
      }
      .title-wrap {
        gap: .4rem;
      }
      .head-icon {
        font-size: 1.1rem;
        width: 1.1rem;
        height: 1.1rem;
      }
      .close-btn {
        width: 32px;
        height: 32px;
        padding: 4px;
      }
      .reason-dialog mat-dialog-actions > button {
        min-width: 110px;
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

