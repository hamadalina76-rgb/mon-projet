// src/app/features/promotions/promotions-list/promotions-list.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { PromotionsService } from '../services/promotions.service';

@Component({
  selector: 'app-promotions-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './promotions-list.component.html',
  styleUrls: ['./promotions-list.component.scss'],
})
export class PromotionsListComponent implements OnInit {
  private promotionsService = inject(PromotionsService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  // Angular 19 Signals
  promotions = signal<any[]>([]);
  loading = signal(false);
  filter = signal<'all' | 'active' | 'scheduled' | 'expired'>('all');

  // Computed values
  filteredPromotions = computed(() => {
    const all = this.promotions();
    const currentFilter = this.filter();
    
    if (currentFilter === 'all') return all;
    return all.filter(p => this.getStatusClass(p) === currentFilter);
  });

  activeCount = computed(() => 
    this.promotions().filter(p => this.getStatusClass(p) === 'active').length
  );

  ngOnInit(): void {
    this.loadPromotions();
  }

  loadPromotions(): void {
    this.loading.set(true);
    this.promotionsService.getPromotions().subscribe({
      next: (data) => {
        this.promotions.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading promotions:', err);
        this.loading.set(false);
      }
    });
  }

  togglePromotion(id: string): void {
    this.promotionsService.togglePromotion(id).subscribe({
      next: (updated) => {
        this.promotions.update(promos => 
          promos.map(p => p.id === id ? updated : p)
        );
        this.snackBar.open('Promotion mise à jour', 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error toggling promotion:', err);
        this.snackBar.open('Erreur lors de la mise à jour', 'OK', { duration: 3000 });
      }
    });
  }

  deletePromotion(id: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette promotion ?')) {
      this.promotionsService.deletePromotion(id).subscribe({
        next: () => {
          this.promotions.update(promos => promos.filter(p => p.id !== id));
          this.snackBar.open('Promotion supprimée', 'OK', { duration: 3000 });
        },
        error: (err) => {
          console.error('Error deleting promotion:', err);
          this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 3000 });
        }
      });
    }
  }

  getStatusClass(promo: any): string {
    const now = new Date();
    const start = new Date(promo.startDate);
    const end = new Date(promo.endDate);
    
    if (!promo.isActive) return 'inactive';
    if (now < start) return 'scheduled';
    if (now > end) return 'expired';
    return 'active';
  }
}
