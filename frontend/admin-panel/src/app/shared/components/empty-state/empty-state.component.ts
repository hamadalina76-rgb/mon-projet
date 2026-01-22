// src/app/shared/components/empty-state/empty-state.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="empty-state">
      <div class="empty-state-icon">
        <mat-icon>{{ icon }}</mat-icon>
      </div>
      <h3 class="empty-state-title">{{ title | translate }}</h3>
      @if (description) {
        <p class="empty-state-description">{{ description | translate }}</p>
      }
      @if (actionLabel) {
        <button mat-flat-button color="primary" (click)="action.emit()">
          @if (actionIcon) {
            <mat-icon>{{ actionIcon }}</mat-icon>
          }
          {{ actionLabel | translate }}
        </button>
      }
    </div>
  `,
  styles: [
    `
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 3rem;
        text-align: center;
      }

      .empty-state-icon {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: rgba(79, 70, 229, 0.1);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1.5rem;

        mat-icon {
          font-size: 40px;
          width: 40px;
          height: 40px;
          color: var(--primary);
        }
      }

      .empty-state-title {
        margin: 0 0 0.5rem;
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--text-primary);
      }

      .empty-state-description {
        margin: 0 0 1.5rem;
        color: var(--text-secondary);
        max-width: 400px;
      }
    `,
  ],
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'common.noData';
  @Input() description?: string;
  @Input() actionLabel?: string;
  @Input() actionIcon?: string;
  @Output() action = new EventEmitter<void>();
}
