// src/app/features/dashboard/dashboard.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-page">
      <h2>Admin Dashboard</h2>
      <p>Contenu du dashboard admin à implémenter...</p>
    </div>
  `,
  styles: [`
    .dashboard-page {
      padding: 1.5rem;
    }
  `],
})
export class DashboardComponent {}
