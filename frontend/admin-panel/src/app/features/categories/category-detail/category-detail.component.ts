import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { CategoriesService } from '../services/categories.service';
import { Category, CategoryBusinessType } from '@core/models/category.model';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTabsModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './category-detail.component.html',
  styleUrls: ['./category-detail.component.scss'],
})
export class CategoryDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private categoriesService = inject(CategoriesService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();

  category = signal<Category | null>(null);
  loading = signal(false);
  categoryId = signal<number | null>(null);
  selectedTabIndex = signal(0);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.categoryId.set(Number(id));
      this.loadCategory(Number(id));
    } else {
      this.router.navigate(['/categories']);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCategory(id: number): void {
    this.loading.set(true);
    this.categoriesService.getCategoryById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (category) => {
          this.category.set(category);
          this.loading.set(false);
        },
        error: () => {
          this.toastr.error('Impossible de charger la catégorie');
          this.loading.set(false);
          this.router.navigate(['/categories']);
        }
      });
  }

  onTabChange(index: number): void { this.selectedTabIndex.set(index); }

  goBack(): void { this.router.navigate(['/categories']); }

  editCategory(): void {
    const id = this.categoryId();
    if (id) this.router.navigate(['/categories', id, 'edit']);
  }

  toggleStatus(): void {
    const id = this.categoryId();
    if (!id) return;

    this.categoriesService.toggleCategoryStatus(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.category.set(updated);
          this.toastr.success('Statut mis à jour');
        },
        error: () => this.toastr.error('Erreur lors de la mise à jour')
      });
  }

  deleteCategory(): void {
    const cat = this.category();
    const id = this.categoryId();
    if (!cat || !id) return;

    const dialogData: ConfirmationDialogData = {
      title: 'Supprimer la catégorie',
      message: `Êtes-vous sûr de vouloir supprimer "${this.getDisplayName(cat)}" ?`,
      confirmLabel: 'Supprimer',
      cancelLabel: 'Annuler',
      type: 'danger'
    };

    this.dialog.open(ConfirmationDialogComponent, { width: '420px', data: dialogData })
      .afterClosed().subscribe(result => {
        if (result) {
          this.categoriesService.deleteCategory(id)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: () => {
                this.toastr.success('Catégorie supprimée');
                this.router.navigate(['/categories']);
              },
              error: () => this.toastr.error('Erreur lors de la suppression')
            });
        }
      });
  }

  getDisplayName(cat: Category): string {
    const lang = this.translate.currentLang || 'fr';
    return cat.nameI18n[lang]
      || cat.nameI18n['fr']
      || cat.nameI18n['en']
      || Object.values(cat.nameI18n)[0]
      || '';
  }

  getNameEntries(): { key: string; value: string }[] {
    const category = this.category();
    if (!category?.nameI18n) return [];
    
    return Object.entries(category.nameI18n)
      .filter(([key, value]) => value) // Filtrer les valeurs nulles/undefined
      .map(([key, value]) => ({ key, value: value! }));
  }

  getBusinessTypeLabel(type: CategoryBusinessType): string {
    const labels: Record<CategoryBusinessType, string> = {
      RESTAURANT: 'Restaurant',
      GROCERY: 'Épicerie',
      PHARMACY: 'Pharmacie',
      OTHER: 'Autre',
    };
    return labels[type] ?? type;
  }

  getBusinessTypeIcon(type: CategoryBusinessType): string {
    const icons: Record<CategoryBusinessType, string> = {
      RESTAURANT: 'restaurant',
      GROCERY: 'local_grocery_store',
      PHARMACY: 'local_pharmacy',
      OTHER: 'category',
    };
    return icons[type] ?? 'category';
  }
}