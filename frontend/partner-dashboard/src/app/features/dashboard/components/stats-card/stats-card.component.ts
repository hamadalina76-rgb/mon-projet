// src/app/features/dashboard/components/stats-card/stats-card.component.ts - Angular 19
import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-stats-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  templateUrl: './stats-card.component.html',
  styleUrls: ['./stats-card.component.scss'],
})
export class StatsCardComponent {
  // Angular 19 Signal Inputs
  icon = input('');
  title = input('');
  value = input<any>();
  trend = input<number | undefined>();
  color = input('primary');

  // Computed
  hasTrend = computed(() => this.trend() !== undefined);
  isPositive = computed(() => (this.trend() ?? 0) >= 0);
  trendIcon = computed(() => this.isPositive() ? 'trending_up' : 'trending_down');
}
