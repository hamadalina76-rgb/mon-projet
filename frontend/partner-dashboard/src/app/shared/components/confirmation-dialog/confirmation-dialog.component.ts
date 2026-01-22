// src/app/shared/components/confirmation-dialog/confirmation-dialog.component.ts - Angular 19
import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (isOpen()) {
      <div class="dialog-overlay" (click)="onCancel()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <span class="material-icons" [class]="type()">{{ icon() }}</span>
            <h3>{{ title() }}</h3>
          </div>
          <div class="dialog-body">
            <p>{{ message() }}</p>
          </div>
          <div class="dialog-footer">
            <button class="btn btn-secondary" (click)="onCancel()">
              {{ cancelText() }}
            </button>
            <button class="btn" [class]="'btn-' + type()" (click)="onConfirm()">
              {{ confirmText() }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styleUrls: ['./confirmation-dialog.component.scss'],
})
export class ConfirmationDialogComponent {
  // Angular 19 Signal Inputs
  isOpen = input(false);
  title = input('Confirmation');
  message = input('Êtes-vous sûr de vouloir continuer ?');
  type = input<'danger' | 'warning' | 'info'>('danger');
  confirmText = input('Confirmer');
  cancelText = input('Annuler');
  
  // Angular 19 Outputs
  confirm = output<void>();
  cancel = output<void>();

  // Computed icon based on type
  icon = computed(() => {
    switch (this.type()) {
      case 'danger': return 'warning';
      case 'warning': return 'error_outline';
      case 'info': return 'info';
      default: return 'help_outline';
    }
  });

  onConfirm(): void {
    this.confirm.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
