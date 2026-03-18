import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CategoriesService } from '@features/categories/services/categories.service';
import { Category } from '@core/models/category.model';

export interface PartnerEditDialogData {
  partner: any;
}

@Component({
  selector: 'app-partner-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDividerModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './partner-edit-dialog.component.html',
  styleUrls: ['./partner-edit-dialog.component.scss'],
})
export class PartnerEditDialogComponent implements OnInit {
  public dialogRef = inject(MatDialogRef<PartnerEditDialogComponent>);
  private categoriesService = inject(CategoriesService);
  private translate = inject(TranslateService);
  data: PartnerEditDialogData = inject(MAT_DIALOG_DATA);

  partnerTypes = [
    { value: 'RESTAURANT', label: 'Restaurant' },
    { value: 'FAST_FOOD', label: 'Fast Food' },
    { value: 'CAFE', label: 'Café' },
    { value: 'BAKERY', label: 'Boulangerie' },
    { value: 'GROCERY', label: 'Épicerie' },
    { value: 'PHARMACY', label: 'Pharmacie' },
    { value: 'FLORIST', label: 'Fleuriste' },
    { value: 'OTHER', label: 'Autre' },
  ];

  form = {
    commissionType: 'PERCENTAGE' as 'PERCENTAGE' | 'MARKUP',
    commissionRate: 15,
  };

  // Signal dédié pour categoryId (réactif dans les computed)
  selectedCategoryId = signal<number | null>(null);
  selectedSubcategoryIds = signal<number[]>([]);
  allCategories = signal<Category[]>([]);
  categoriesLoading = signal(false);

  rootCategories = computed(() =>
    this.allCategories().filter(cat => !cat.parentId)
  );

  subcategories = computed(() => {
    const catId = this.selectedCategoryId();
    if (!catId) return [];
    return this.allCategories().filter(
      cat => cat.parentId != null && Number(cat.parentId) === Number(catId)
    );
  });

  ngOnInit(): void {
    const p = this.data.partner;

    this.form = {
      commissionType: (p.commissionType as 'PERCENTAGE' | 'MARKUP') || 'PERCENTAGE',
      commissionRate: p.commissionRate ?? 15,
    };

    // Charger les catégories D'ABORD, puis définir la sélection initiale
    // (le mat-select a besoin des options avant de pouvoir pré-sélectionner)
    this.loadCategories(p.categoryIds);
  }

  private loadCategories(rawCategoryIds?: any): void {
    this.categoriesLoading.set(true);
    this.categoriesService.getCategories().subscribe({
      next: (cats) => {
        this.allCategories.set(cats.filter(c => c.isActive));
        this.categoriesLoading.set(false);

        // Parse et appliquer la sélection APRES le chargement
        if (rawCategoryIds) {
          const parts = String(rawCategoryIds)
            .split(',')
            .map((s: string) => Number(s.trim()))
            .filter((n: number) => !isNaN(n) && n > 0);
          if (parts.length > 0) {
            this.selectedCategoryId.set(parts[0]);
            this.selectedSubcategoryIds.set(parts.slice(1));
          }
        }
      },
      error: () => this.categoriesLoading.set(false),
    });
  }

  getCategoryName(cat: Category): string {
    const lang = this.translate.currentLang || 'fr';
    return cat.nameI18n?.[lang] || cat.nameI18n?.['fr'] || cat.nameI18n?.['en'] || Object.values(cat.nameI18n ?? {})[0] || 'Sans nom';
  }

  /** Retourne true si l'icône est une URL (image), false si c'est un emoji */
  isIconUrl(icon: string | null | undefined): boolean {
    return !!(icon && (icon.startsWith('http') || icon.startsWith('/')));
  }

  // compareWith pour mat-select : compare par valeur numérique
  compareCategoryId = (a: number | null, b: number | null): boolean => {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return Number(a) === Number(b);
  };

  onCategoryChange(newCatId: number | null): void {
    this.selectedCategoryId.set(newCatId);
    this.selectedSubcategoryIds.set([]);
  }

  toggleSubcategory(id: number): void {
    const current = this.selectedSubcategoryIds();
    if (current.includes(id)) {
      this.selectedSubcategoryIds.set(current.filter(x => x !== id));
    } else {
      this.selectedSubcategoryIds.set([...current, id]);
    }
  }

  isSubSelected(id: number): boolean {
    return this.selectedSubcategoryIds().includes(id);
  }

  get isRtl(): boolean {
    return this.translate.currentLang === 'ar';
  }

  isValid(): boolean {
    return !!(this.form.commissionType && this.form.commissionRate > 0);
  }

  onSave(): void {
    if (!this.isValid()) return;
    this.dialogRef.close({
      ...this.form,
      categoryId: this.selectedCategoryId(),
      subcategoryIds: this.selectedSubcategoryIds(),
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
