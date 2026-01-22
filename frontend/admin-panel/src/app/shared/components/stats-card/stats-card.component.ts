// src/app/shared/components/stats-card/stats-card.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-stats-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <mat-card class="stats-card" [attr.data-color]="color">
      <div class="stats-content">
        <div class="stats-info">
          <span class="stats-label">{{ label }}</span>
          <span class="stats-value">{{ value }}</span>
          @if (trend) {
            <div class="stats-trend" [class.positive]="trend > 0" [class.negative]="trend < 0">
              <mat-icon>{{ trend > 0 ? 'trending_up' : 'trending_down' }}</mat-icon>
              <span>{{ trend > 0 ? '+' : '' }}{{ trend }}%</span>
            </div>
          }
        </div>
        <div class="stats-icon">
          <mat-icon>{{ icon }}</mat-icon>
        </div>
      </div>
    </mat-card>
  `,
  styles: [
    `
      .stats-card {
        border-radius: 12px;
        overflow: hidden;
      }

      .stats-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem;
      }

      .stats-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }

      .stats-label {
        font-size: 0.875rem;
        color: var(--text-secondary);
      }

      .stats-value {
        font-size: 1.75rem;
        font-weight: 700;
        color: var(--text-primary);
      }

      .stats-trend {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-size: 0.75rem;
        font-weight: 500;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }

        &.positive {
          color: #10b981;
        }

        &.negative {
          color: #ef4444;
        }
      }

      .stats-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(79, 70, 229, 0.1);

        mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
          color: var(--primary);
        }
      }

      [data-color='success'] .stats-icon {
        background: rgba(16, 185, 129, 0.1);
        mat-icon {
          color: #10b981;
        }
      }

      [data-color='warning'] .stats-icon {
        background: rgba(245, 158, 11, 0.1);
        mat-icon {
          color: #f59e0b;
        }
      }

      [data-color='danger'] .stats-icon {
        background: rgba(239, 68, 68, 0.1);
        mat-icon {
          color: #ef4444;
        }
      }

      [data-color='info'] .stats-icon {
        background: rgba(59, 130, 246, 0.1);
        mat-icon {
          color: #3b82f6;
        }
      }
    `,
  ],
})
export class StatsCardComponent {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() icon = 'analytics';
  @Input() trend?: number;
  @Input() color: 'primary' | 'success' | 'warning' | 'danger' | 'info' = 'primary';
}
