// src/app/features/orders/dialogs/reject-order-dialog.component.ts
import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

export interface RejectOrderDialogData {
  orderNumber: string;
  orderId: string;
}

export interface RejectOrderDialogResult {
  rejected: true;
  reason: string;
}

const QUICK_REASON_KEYS = [
  'ORDERS.REJECT_DIALOG.REASONS.OUT_OF_STOCK',
  'ORDERS.REJECT_DIALOG.REASONS.CLOSED',
  'ORDERS.REJECT_DIALOG.REASONS.OUT_OF_ZONE',
  'ORDERS.REJECT_DIALOG.REASONS.INCORRECT',
  'ORDERS.REJECT_DIALOG.REASONS.TOO_SHORT',
] as const;

@Component({
  selector: 'app-reject-order-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, TranslateModule],
  template: `
    <div class="dialog-wrap">
      <div class="dialog-header reject">
        <div class="header-icon">
          <span class="material-icons">cancel</span>
        </div>
        <div class="header-text">
          <h2>{{ 'ORDERS.REJECT_DIALOG.TITLE' | translate }}</h2>
          <p>#{{ data.orderNumber }}</p>
        </div>
        <button class="close-icon-btn" (click)="cancel()">
          <span class="material-icons">close</span>
        </button>
      </div>

      <div class="dialog-body">
        <p class="body-hint">{{ 'ORDERS.REJECT_DIALOG.HINT' | translate }}</p>
        <div class="quick-reasons">
          @for (k of quickReasonKeys; track k) {
            <button
              class="reason-pill"
              [class.selected]="selectedKey() === k"
              (click)="selectQuick(k)">
              {{ k | translate }}
            </button>
          }
        </div>
        <textarea
          class="reason-textarea"
          rows="3"
          [placeholder]="'ORDERS.REJECT_DIALOG.CUSTOM_PLACEHOLDER' | translate"
          [value]="customReason()"
          (input)="onCustomReason($event)"
          maxlength="250">
        </textarea>
        <div class="char-count">{{ customReason().length }}/250</div>
        @if (showError()) {
          <p class="error-msg">{{ 'ORDERS.REJECT_DIALOG.ERROR_REQUIRED' | translate }}</p>
        }
      </div>

      <div class="dialog-footer">
        <button class="btn-cancel" (click)="cancel()">{{ 'ORDERS.REJECT_DIALOG.CANCEL' | translate }}</button>
        <button class="btn-reject" (click)="reject()">
          <span class="material-icons">block</span>
          {{ 'ORDERS.REJECT_DIALOG.CONFIRM' | translate }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .dialog-wrap {
      width: 100%;
      max-width: 100%;
      min-width: 0;
      box-sizing: border-box;
      overflow-x: clip;
      font-family: 'Roboto', sans-serif;
    }

    .dialog-header {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 20px 20px 16px;
      padding-right: 48px;
      border-bottom: 1px solid #E2E8F0;
      position: relative;

      &.reject .header-icon {
        width: 42px; height: 42px; border-radius: 50%;
        background: rgba(239,68,68,0.1);
        display: flex; align-items: center; justify-content: center;
        flex-shrink: 0;
        .material-icons { color: #EF4444; font-size: 1.3rem; }
      }
    }

    .header-text {
      flex: 1;
      min-width: 0;
    }

    .header-text h2 {
      margin: 0; font-size: 1rem; font-weight: 700; color: #1A202C;
      line-height: 1.3;
      word-break: break-word;
    }
    .header-text p {
      margin: 4px 0 0; font-size: 0.8rem; color: #718096;
      word-break: break-all;
    }

    .close-icon-btn {
      position: absolute; right: 8px; top: 12px;
      border: none; background: none; cursor: pointer;
      width: 40px; height: 40px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      color: #718096; transition: background 0.15s;
      flex-shrink: 0;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;
      .material-icons { font-size: 1.2rem; }
      &:hover { background: #EDF2F7; color: #1A202C; }
    }

    .dialog-body {
      padding: 20px;
    }

    .body-hint {
      margin: 0 0 14px; font-size: 0.875rem; color: #4A5568;
      line-height: 1.45;
    }

    .quick-reasons {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
      gap: 8px;
      margin-bottom: 16px;
      width: 100%;
      min-width: 0;
    }

    .reason-pill {
      padding: 8px 10px;
      border-radius: 10px;
      border: 1.5px solid #E2E8F0;
      background: #fff;
      font-size: 0.78rem; font-weight: 500;
      line-height: 1.3;
      color: #4A5568; cursor: pointer; font-family: inherit;
      transition: all 0.15s;
      text-align: center;
      min-height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;

      &:hover { border-color: #EF4444; color: #DC2626; }

      &.selected {
        border-color: #EF4444;
        background: rgba(239,68,68,0.08);
        color: #DC2626;
      }
    }

    .reason-textarea {
      display: block;
      width: 100%;
      max-width: 100%;
      min-width: 0;
      box-sizing: border-box;
      padding: 10px 12px;
      border: 1.5px solid #E2E8F0;
      border-radius: 8px;
      font-size: 0.875rem; font-family: inherit; color: #1A202C;
      resize: vertical;
      outline: none;
      min-height: 88px;
      &:focus { border-color: #EF4444; }
    }

    .char-count {
      text-align: right; font-size: 0.7rem; color: #A0AEC0; margin-top: 4px;
    }

    .error-msg {
      color: #EF4444; font-size: 0.8rem; margin-top: 6px;
    }

    .dialog-footer {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      justify-content: flex-end;
      padding: 14px 20px;
      border-top: 1px solid #E2E8F0;
    }

    .btn-cancel {
      padding: 0 20px; min-height: 44px; border-radius: 8px;
      border: 1.5px solid #E2E8F0; background: #fff;
      font-size: 0.875rem; font-weight: 500; color: #4A5568;
      cursor: pointer; font-family: inherit; transition: all 0.15s;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;
      &:hover { background: #EDF2F7; }
    }

    .btn-reject {
      display: inline-flex; align-items: center; justify-content: center; gap: 6px;
      padding: 0 14px; min-height: 44px; border-radius: 8px;
      border: none; background: #EF4444; color: #fff;
      font-size: 0.8rem; font-weight: 600; cursor: pointer;
      font-family: inherit; transition: all 0.15s;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;
      .material-icons { font-size: 1rem; flex-shrink: 0; }
      &:hover { background: #DC2626; }
    }

    @media (max-width: 420px) {
      .dialog-header {
        padding: 16px 44px 14px 14px;
      }

      .dialog-body {
        padding: 16px 14px;
      }

      .quick-reasons {
        grid-template-columns: 1fr;
      }

      .reason-pill {
        min-height: auto;
        padding: 10px 12px;
        justify-content: flex-start;
        text-align: left;
      }

      .dialog-footer {
        flex-direction: column;
        align-items: stretch;
        padding: 12px 14px;
      }

      .btn-cancel,
      .btn-reject {
        width: 100%;
        justify-content: center;
      }

      .btn-reject {
        order: 2;
      }

      .btn-cancel {
        order: 1;
      }
    }
  `]
})
export class RejectOrderDialogComponent {
  dialogRef = inject(MatDialogRef<RejectOrderDialogComponent>);
  data: RejectOrderDialogData = inject(MAT_DIALOG_DATA);
  private translate = inject(TranslateService);

  quickReasonKeys = [...QUICK_REASON_KEYS];
  selectedKey = signal<string | null>(null);
  customReason = signal('');
  showError = signal(false);

  selectQuick(k: string): void {
    this.selectedKey.set(k);
    this.customReason.set(this.translate.instant(k));
  }

  onCustomReason(event: Event): void {
    const val = (event.target as HTMLTextAreaElement).value;
    this.customReason.set(val);
    this.selectedKey.set(null);
  }

  reject(): void {
    const trimmed = this.customReason().trim();
    const fromKey = this.selectedKey() ? this.translate.instant(this.selectedKey()!) : '';
    const r = trimmed || fromKey;
    if (!r) {
      this.showError.set(true);
      return;
    }
    this.dialogRef.close({ rejected: true, reason: r });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
