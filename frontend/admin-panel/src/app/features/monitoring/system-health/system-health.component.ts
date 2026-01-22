// src/app/features/monitoring/system-health/system-health.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { MonitoringService } from '../services/monitoring.service';

@Component({
  selector: 'app-system-health',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    TranslateModule,
  ],
  templateUrl: './system-health.component.html',
  styleUrls: ['./system-health.component.scss'],
})
export class SystemHealthComponent implements OnInit {
  private monitoringService = inject(MonitoringService);

  healthData: any = null;
  services: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadHealth();
  }

  loadHealth(): void {
    // TODO: Implement
  }
}
