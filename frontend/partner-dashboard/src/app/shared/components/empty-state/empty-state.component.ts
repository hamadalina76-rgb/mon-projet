// src/app/shared/components/empty-state/empty-state.component.ts - Angular 19
import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="empty-state">
      <span class="material-icons icon">{{ icon() }}</span>
      <h3>{{ title() }}</h3>
      <p>{{ message() }}</p>
      @if (hasAction()) {
        <button class="btn btn-primary" (click)="action.emit()">
          {{ actionLabel() }}
        </button>
      }
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem;
      text-align: center;
    }
    
    .icon {
      font-size: 64px;
      color: #CBD5E0;
      margin-bottom: 1rem;
    }
    
    h3 {
      font-size: 1.25rem;
      font-weight: 600;
      color: #2D3748;
      margin: 0 0 0.5rem;
    }
    
    p {
      color: #718096;
      margin: 0 0 1.5rem;
      max-width: 300px;
    }
    
    .btn {
      padding: 0.5rem 1.5rem;
      background: #FF6B35;
      color: white;
      border: none;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      
      &:hover {
        background: #E55A2B;
      }
    }
  `],
})
export class EmptyStateComponent {
  // Angular 19 Signal Inputs
  icon = input('inbox');
  title = input('Aucune donnée');
  message = input('Il n\'y a rien à afficher pour le moment.');
  actionLabel = input('');
  
  // Angular 19 Signal Output
  action = output<void>();

  // Computed
  hasAction = computed(() => !!this.actionLabel());
}
