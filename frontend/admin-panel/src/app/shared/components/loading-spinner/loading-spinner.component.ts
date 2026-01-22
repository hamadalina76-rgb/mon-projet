// src/app/shared/components/loading-spinner/loading-spinner.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="loading-container" [class.overlay]="overlay" [class.fullscreen]="fullscreen">
      <mat-spinner [diameter]="diameter" [color]="color"></mat-spinner>
      @if (message) {
        <p class="loading-message">{{ message }}</p>
      }
    </div>
  `,
  styles: [
    `
      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        padding: 2rem;

        &.overlay {
          position: absolute;
          inset: 0;
          background: rgba(255, 255, 255, 0.8);
          z-index: 1000;
        }

        &.fullscreen {
          position: fixed;
          inset: 0;
          background: rgba(255, 255, 255, 0.9);
          z-index: 9999;
        }
      }

      .loading-message {
        color: var(--text-secondary);
        margin: 0;
      }
    `,
  ],
})
export class LoadingSpinnerComponent {
  @Input() diameter = 40;
  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';
  @Input() message?: string;
  @Input() overlay = false;
  @Input() fullscreen = false;
}
