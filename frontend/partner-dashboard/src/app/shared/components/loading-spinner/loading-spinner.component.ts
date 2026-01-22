// src/app/shared/components/loading-spinner/loading-spinner.component.ts - Angular 19
import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner-container" [class.overlay]="overlay()">
      <div class="spinner" [style.width.px]="size()" [style.height.px]="size()"></div>
      @if (message()) {
        <span class="spinner-message">{{ message() }}</span>
      }
    </div>
  `,
  styles: [`
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      
      &.overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(255, 255, 255, 0.9);
        z-index: 9999;
      }
    }
    
    .spinner {
      border: 3px solid #f3f3f3;
      border-top: 3px solid #FF6B35;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    
    .spinner-message {
      margin-top: 1rem;
      color: #4A5568;
      font-size: 0.875rem;
    }
    
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `],
})
export class LoadingSpinnerComponent {
  // Angular 19 Signal Inputs
  size = input(40);
  overlay = input(false);
  message = input('');
}
