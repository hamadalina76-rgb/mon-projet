// src/app/features/orders/dialogs/accept-order-dialog.component.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';

export interface AcceptOrderDialogData {
  orderNumber: string;
  orderId: string;
  /**
   * Minutes suggérées à partir des fiches produits (max par ligne). Si absent, défaut 15 min.
   */
  suggestedFromProductsMinutes?: number;
  /** Afficher la ligne d’aide « d’après vos fiches produits ». */
  showProductPrepHint?: boolean;
  /** Créneau livraison client (commande planifiée). */
  isScheduled?: boolean;
  scheduledDeliveryTime?: string;
}

export interface AcceptOrderDialogResult {
  accepted: true;
  prepTime: number;
}

@Component({
  selector: 'app-accept-order-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, TranslateModule],
  template: `
    <div class="dialog-wrap">
      <div class="dialog-header accept">
        <div class="header-icon">
          <span class="material-icons">check_circle</span>
        </div>
        <div class="header-text">
          <h2>{{ 'ORDERS.ACCEPT_DIALOG.TITLE' | translate }}</h2>
          <p>#{{ data.orderNumber }}</p>
        </div>
        <button class="close-icon-btn" (click)="cancel()">
          <span class="material-icons">close</span>
        </button>
      </div>

      <div class="dialog-body">
        @if (data.isScheduled && data.scheduledDeliveryTime) {
          <div class="scheduled-dialog-note">
            <span class="material-icons">event</span>
            <div class="scheduled-dialog-note-inner">
              <span class="scheduled-dialog-label">{{ 'ORDERS.ACCEPT_DIALOG.SCHEDULED_LABEL' | translate }}</span>
              <span class="scheduled-dialog-slot">{{ data.scheduledDeliveryTime | date:'EEEE d MMM yyyy, HH:mm' }}</span>
              <p class="scheduled-dialog-hint">{{ 'ORDERS.ACCEPT_DIALOG.SCHEDULED_HINT' | translate }}</p>
            </div>
          </div>
        }
        <p class="body-hint">{{ 'ORDERS.ACCEPT_DIALOG.HINT' | translate }}</p>
        @if (data.showProductPrepHint && data.suggestedFromProductsMinutes != null && data.suggestedFromProductsMinutes > 0) {
          <p class="product-prep-hint">
            {{ 'ORDERS.ACCEPT_DIALOG.PRODUCT_HINT_PREFIX' | translate }}
            <strong>{{ data.suggestedFromProductsMinutes }} {{ 'ORDERS.ACCEPT_DIALOG.MIN_UNIT' | translate }}</strong>
          </p>
        }
        <div class="time-grid">
          @for (opt of prepOptions; track opt) {
            <button class="time-pill" [class.selected]="prepTime() === opt" (click)="prepTime.set(opt)">
              {{ opt }}<span class="unit">{{ 'ORDERS.ACCEPT_DIALOG.MIN_UNIT' | translate }}</span>
            </button>
          }
        </div>
        <div class="custom-time-row">
          <label class="custom-label">{{ 'ORDERS.ACCEPT_DIALOG.CUSTOM_LABEL' | translate }}</label>
          <input
            class="custom-input"
            type="number"
            min="1" max="180"
            [placeholder]="'ORDERS.ACCEPT_DIALOG.CUSTOM_PLACEHOLDER' | translate"
            [value]="customValue()"
            (input)="onCustom($event)" />
        </div>
      </div>

      <div class="dialog-footer">
        <button class="btn-cancel" (click)="cancel()">{{ 'ORDERS.ACCEPT_DIALOG.CANCEL' | translate }}</button>
        <button class="btn-accept" [class.disabled]="!prepTime()" [disabled]="!prepTime()" (click)="accept()">
          <span class="material-icons">check</span>
          {{ 'ORDERS.ACCEPT_DIALOG.CONFIRM' | translate: { minutes: prepTime() } }}
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

      &.accept .header-icon {
        width: 42px; height: 42px; border-radius: 50%;
        background: rgba(16,185,129,0.12);
        display: flex; align-items: center; justify-content: center;
        flex-shrink: 0;
        .material-icons { color: #10B981; font-size: 1.3rem; }
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

    .scheduled-dialog-note {
      display: flex; gap: 10px; align-items: flex-start;
      padding: 12px 14px; margin-bottom: 14px;
      border-radius: 10px; border: 1px solid rgba(59, 130, 246, 0.35);
      background: rgba(59, 130, 246, 0.08);
      .material-icons { font-size: 1.25rem; color: #2563EB; flex-shrink: 0; }
    }
    .scheduled-dialog-note-inner { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
    .scheduled-dialog-label {
      font-size: 0.65rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; color: #1D4ED8;
    }
    .scheduled-dialog-slot { font-size: 0.95rem; font-weight: 700; color: #1A202C; }
    .scheduled-dialog-hint {
      margin: 4px 0 0; font-size: 0.8rem; color: #4A5568; line-height: 1.4;
    }

    .body-hint {
      margin: 0 0 16px; font-size: 0.875rem; color: #4A5568;
      line-height: 1.45;
    }

    .product-prep-hint {
      margin: -8px 0 14px; font-size: 0.8rem; color: #047857;
      line-height: 1.4;
    }

    .time-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(76px, 1fr));
      gap: 8px;
      margin-bottom: 16px;
      width: 100%;
      min-width: 0;
    }

    .time-pill {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      min-height: 48px;
      padding: 8px 6px;
      border-radius: 10px;
      border: 1.5px solid #E2E8F0;
      background: #fff;
      cursor: pointer;
      font-family: inherit;
      font-size: 0.95rem;
      font-weight: 700;
      color: #1A202C;
      transition: all 0.15s;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;
      .unit { font-size: 0.65rem; font-weight: 500; color: #718096; }

      &:hover { border-color: #10B981; background: rgba(16,185,129,0.06); }

      &.selected {
        border-color: #10B981;
        background: rgba(16,185,129,0.1);
        color: #059669;
        box-shadow: 0 0 0 3px rgba(16,185,129,0.12);
        .unit { color: #059669; }
      }
    }

    .custom-time-row {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
    }

    .custom-label {
      font-size: 0.8rem; color: #4A5568;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .custom-input {
      flex: 1;
      min-width: 0;
      padding: 10px 12px;
      border: 1.5px solid #E2E8F0; border-radius: 8px;
      font-size: 0.9rem; color: #1A202C; font-family: inherit;
      outline: none;
      max-width: 100%;
      &:focus { border-color: #10B981; }
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
      cursor: pointer; font-family: inherit;
      transition: all 0.15s;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;
      &:hover { background: #EDF2F7; }
    }

    .btn-accept {
      display: inline-flex; align-items: center; justify-content: center; gap: 6px;
      padding: 0 16px; min-height: 44px; border-radius: 8px;
      border: none; background: #10B981; color: #fff;
      font-size: 0.875rem; font-weight: 600; cursor: pointer;
      font-family: inherit; transition: all 0.15s;
      touch-action: manipulation;
      -webkit-tap-highlight-color: transparent;
      .material-icons { font-size: 1rem; flex-shrink: 0; }
      &:hover:not(:disabled) { background: #059669; }
      &.disabled, &:disabled { opacity: 0.5; cursor: not-allowed; }
    }

    @media (max-width: 420px) {
      .dialog-header {
        padding: 16px 44px 14px 14px;
      }

      .dialog-body {
        padding: 16px 14px;
      }

      .time-grid {
        grid-template-columns: repeat(3, 1fr);
        gap: 8px;
      }

      .custom-time-row {
        flex-direction: column;
        align-items: stretch;
        gap: 8px;
      }

      .custom-label {
        white-space: normal;
      }

      .dialog-footer {
        flex-direction: column;
        align-items: stretch;
        padding: 12px 14px;
      }

      .btn-cancel,
      .btn-accept {
        width: 100%;
        justify-content: center;
      }

      .btn-accept {
        order: 2;
      }

      .btn-cancel {
        order: 1;
      }
    }
  `]
})
export class AcceptOrderDialogComponent implements OnInit {
  dialogRef = inject(MatDialogRef<AcceptOrderDialogComponent>);
  data: AcceptOrderDialogData = inject(MAT_DIALOG_DATA);

  prepOptions = [10, 15, 20, 30, 45];
  prepTime = signal(15);
  customValue = signal<string>('');

  ngOnInit(): void {
    const s = this.data.suggestedFromProductsMinutes;
    if (s != null && s > 0 && Number.isFinite(s)) {
      const rounded = Math.round(s);
      this.prepTime.set(rounded);
      this.customValue.set(String(rounded));
    }
  }

  onCustom(event: Event): void {
    const val = parseInt((event.target as HTMLInputElement).value, 10);
    this.customValue.set((event.target as HTMLInputElement).value);
    if (!isNaN(val) && val > 0) this.prepTime.set(val);
  }

  accept(): void {
    if (!this.prepTime()) return;
    this.dialogRef.close({ accepted: true, prepTime: this.prepTime() });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
