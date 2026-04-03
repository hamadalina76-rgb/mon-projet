import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { PromotionsService } from '../services/promotions.service';
import { PromotionAnalytics, Promotion, PromotionDto } from '../../../core/models/promotion.model';

@Component({
  selector: 'app-promotion-analytics',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatBadgeModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './promotion-analytics.component.html',
  styleUrls: ['./promotion-analytics.component.scss'],
})
export class PromotionAnalyticsComponent implements OnInit {
  loading = signal(true);
  promotion = signal<Promotion | null>(null);
  analytics = signal<PromotionAnalytics | null>(null);
  error = signal<string | null>(null);

  displayedColumns = ['userId', 'orderId', 'discountAmount', 'status', 'createdAt'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private promotionsService: PromotionsService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadData(+id);
    } else {
      this.loadGlobalAnalytics();
    }
  }

  private loadData(id: number): void {
    this.loading.set(true);
    this.promotionsService.getById(id).subscribe({
      next: (detail) => {
        this.promotion.set(detail.promotion);
        this.promotionsService.getAnalytics(id).subscribe({
          next: (a) => { this.analytics.set(a); this.loading.set(false); },
          error: () => { this.error.set('Failed to load analytics'); this.loading.set(false); },
        });
      },
      error: () => { this.error.set('Promotion not found'); this.loading.set(false); },
    });
  }

  private loadGlobalAnalytics(): void {
    this.loading.set(false);
  }

  get usageRate(): number {
    const p = this.promotion();
    if (!p?.usageLimitTotal || p.usageLimitTotal === 0) return 0;
    return Math.round((p.usageCount / p.usageLimitTotal) * 100);
  }

  get usageBarColor(): string {
    const r = this.usageRate;
    if (r >= 90) return '#f44336';
    if (r >= 70) return '#ff9800';
    return '#4caf50';
  }

  goBack(): void {
    this.router.navigate(['/promotions']);
  }
}
