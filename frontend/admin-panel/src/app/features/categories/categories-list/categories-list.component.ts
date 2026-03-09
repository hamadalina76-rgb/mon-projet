import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { CategoriesService } from '../services/categories.service';
import { Category, CategoryBusinessType, UpdateCategoryRequest } from '@core/models/category.model';
import { CdkDrag, CdkDropList, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-categories-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatChipsModule,
    MatCardModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatPaginatorModule,
    TranslateModule,
    CdkDrag,
    CdkDropList,
  ],
  templateUrl: './categories-list.component.html',
  styleUrls: ['./categories-list.component.scss'],
})
export class CategoriesListComponent implements OnInit, OnDestroy {
  private categoriesService = inject(CategoriesService);

  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  allCategories = signal<Category[]>([]);
  loading = signal(false);

  searchQuery = signal('');
  selectedBusinessType = signal<CategoryBusinessType | ''>('');
  selectedStatus = signal<'all' | 'active' | 'inactive'>('all');
  sortBy = signal<'name' | 'order' | 'products' | 'partners'>('order');
  sortOrder = signal<'asc' | 'desc'>('asc');

  businessTypes: { value: CategoryBusinessType | ''; label: string; icon: string }[] = [
    { value: '', label: 'Tous les types', icon: 'apps' },
    { value: CategoryBusinessType.RESTAURANT, label: 'Restaurant', icon: 'restaurant' },
    { value: CategoryBusinessType.GROCERY, label: 'Epicerie', icon: 'local_grocery_store' },
    { value: CategoryBusinessType.PHARMACY, label: 'Pharmacie', icon: 'local_pharmacy' },
    { value: CategoryBusinessType.OTHER, label: 'Autre', icon: 'category' },
  ];

  statusOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'active', label: 'Actives' },
    { value: 'inactive', label: 'Inactives' },
  ];

  sortOptions = [
    { value: 'order', label: 'Ordre affichage' },
    { value: 'name', label: 'Nom' },
    { value: 'products', label: 'Produits' },
    { value: 'partners', label: 'Partenaires' },
  ];

  readonly PAGE_SIZE = 50;
  currentPage = signal(0);

  // Le tri reste client-side car déjà chargé ; search/type/status viennent du backend
  filteredCategories = computed(() => {
    const list = [...this.allCategories()];

    list.sort((a, b) => {
      let cmp = 0;
      switch (this.sortBy()) {
        case 'name':
          cmp = this.getDisplayName(a).localeCompare(this.getDisplayName(b));
          break;
        case 'order':
          cmp = (a.displayOrder ?? 0) - (b.displayOrder ?? 0);
          break;
        case 'products':
          cmp = (a.productCount ?? 0) - (b.productCount ?? 0);
          break;
        case 'partners':
          cmp = (a.partnerCount ?? 0) - (b.partnerCount ?? 0);
          break;
      }
      return this.sortOrder() === 'asc' ? cmp : -cmp;
    });

    return list;
  });

  paginatedCategories = computed(() => {
    const list = this.filteredCategories();
    if (list.length <= this.PAGE_SIZE) return list;
    const start = this.currentPage() * this.PAGE_SIZE;
    return list.slice(start, start + this.PAGE_SIZE);
  });

  ngOnInit(): void {
    this.setupSearch();
    this.loadCategories();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearch(): void {
    // Debounce sur la saisie texte → appel backend
    this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(query => {
        this.searchQuery.set(query);
        this.currentPage.set(0);
        this.loadCategories();
      });
  }

  loadCategories(): void {
    this.loading.set(true);
    this.categoriesService.getCategories(
      this.searchQuery() || undefined,
      this.selectedBusinessType() || undefined,
      this.selectedStatus()
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (categories) => {
          this.allCategories.set(categories);
          this.loading.set(false);
        },
        error: (err) => {
          let errorMessage = 'Erreur de connexion au serveur';
          if (err.status === 0)   errorMessage = 'Impossible de contacter le serveur.';
          if (err.status === 401) errorMessage = 'Non autorisé. Vérifiez votre authentification.';
          if (err.status === 500) errorMessage = 'Erreur serveur interne.';
          this.toastr.error(errorMessage);
          this.loading.set(false);
        }
      });
  }

  onSearch(query: string): void {
    this.searchInput$.next(query);
  }

  onTypeChange(value: CategoryBusinessType | ''): void {
    this.selectedBusinessType.set(value);
    this.currentPage.set(0);
    this.loadCategories();
  }

  onStatusChange(value: 'all' | 'active' | 'inactive'): void {
    this.selectedStatus.set(value);
    this.currentPage.set(0);
    this.loadCategories();
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.selectedBusinessType.set('');
    this.selectedStatus.set('all');
    this.sortBy.set('order');
    this.sortOrder.set('asc');
    this.currentPage.set(0);
    this.loadCategories();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
  }

  toggleCategoryStatus(category: Category, event?: Event): void {
    if (event) event.stopPropagation();
    if (!category.id) {
      this.toastr.error('ID de catégorie invalide');
      return;
    }
    this.categoriesService.toggleCategoryStatus(category.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.allCategories.update(list =>
            list.map(c => c.id === updated.id ? updated : c)
          );
          this.toastr.success('Statut mis a jour');
        },
        error: () => this.toastr.error('Erreur lors de la mise a jour du statut')
      });
  }

  deleteCategory(category: Category, event?: Event): void {
    if (event) event.stopPropagation();

    // Bloquer si produits liés
    if ((category.productCount ?? 0) > 0) {
      this.toastr.error(
        `Impossible — ${category.productCount} produit${category.productCount! > 1 ? 's' : ''} lié${category.productCount! > 1 ? 's' : ''} à cette catégorie`
      );
      return;
    }

    const dialogData: ConfirmationDialogData = {
      title: 'Supprimer la categorie',
      message: 'Etes-vous sur de vouloir supprimer "' + this.getDisplayName(category) + '" ?',
      confirmLabel: 'Supprimer',
      cancelLabel: 'Annuler',
      type: 'danger'
    };

    const ref = this.dialog.open(ConfirmationDialogComponent, { width: '420px', data: dialogData });
    ref.afterClosed().subscribe(result => {
      if (result) {
        if (!category || !category.id) {
          this.toastr.error('ID de catégorie invalide');
          return;
        }
        this.categoriesService.deleteCategory(category.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.allCategories.update(list => list.filter(c => c.id !== category.id));
              this.toastr.success('Categorie supprimee');
            },
            error: () => this.toastr.error('Erreur lors de la suppression')
          });
      }
    });
  }

  getDisplayName(category: Category): string {
    const lang = this.translate.currentLang || 'fr';
    return category.nameI18n[lang]
      || category.nameI18n['fr']
      || category.nameI18n['en']
      || Object.values(category.nameI18n)[0]
      || '';
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

  getBusinessTypeLabel(type: CategoryBusinessType): string {
    const t = this.businessTypes.find(b => b.value === type);
    return t ? t.label : type;
  }

  onDrop(event: CdkDragDrop<Category[]>): void {
    if (event.previousIndex === event.currentIndex) return;

    const filtered = [...this.filteredCategories()];
    const prev = event.previousIndex;
    const curr = event.currentIndex;

    const movedItem = filtered[prev];
    const targetItem = filtered[curr];

    moveItemInArray(filtered, prev, curr);

    // Recalculate displayOrder only for items between prev and curr positions
    const minIdx = Math.min(prev, curr);
    const maxIdx = Math.max(prev, curr);
    const baseOrder = Math.min(movedItem.displayOrder, targetItem.displayOrder);

    const changed: Category[] = [];
    const all = [...this.allCategories()];

    filtered.slice(minIdx, maxIdx + 1).forEach((item, i) => {
      const newOrder = baseOrder + i;
      if (item.displayOrder !== newOrder) {
        const updated = { ...item, displayOrder: newOrder };
        changed.push(updated);
        const idx = all.findIndex(c => c.id === item.id);
        if (idx !== -1) all[idx] = updated;
      }
    });

    this.allCategories.set(all);

    // Persist each changed item via existing updateCategory endpoint
    changed.forEach(cat => {
      const payload: UpdateCategoryRequest = {
        nameI18n: cat.nameI18n,
        displayOrder: cat.displayOrder,
        isFeatured: cat.isFeatured,
        isActive: cat.isActive,
        categoryBusinessType: cat.categoryBusinessType,
        categoryType: cat.categoryType || undefined,
        backgroundColor: cat.backgroundColor || undefined,
        textColor: cat.textColor || undefined,
        icon: cat.icon || undefined,
      };
      this.categoriesService.updateCategory(cat.id!, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          error: () => this.toastr.error('Erreur lors de la sauvegarde de l\'ordre')
        });
    });
  }
}