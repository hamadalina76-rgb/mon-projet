// src/app/features/dashboard/components/platform-stats/platform-stats.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { StatsCardComponent } from '@shared/components/stats-card/stats-card.component';

@Component({
  selector: 'app-platform-stats',
  standalone: true,
  imports: [CommonModule, MatCardModule, StatsCardComponent],
  templateUrl: './platform-stats.component.html',
  styleUrls: ['./platform-stats.component.scss'],
})
export class PlatformStatsComponent implements OnInit {
  ngOnInit(): void {
    // TODO: Load platform stats
  }
}
