import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
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
import { Category, UpdateCategoryRequest } from '@core/models/category.model';
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
  totalElements = signal(0);

  searchQuery = signal('');
  selectedStatus = signal<'all' | 'active' | 'inactive'>('all');
  sortBy = signal<'name' | 'order' | 'products' | 'partners'>('order');
  sortOrder = signal<'asc' | 'desc'>('asc');

  statusOptions: { value: 'all' | 'active' | 'inactive'; label: string }[] = [
    { value: 'all',      label: 'categories.statusAll' },
    { value: 'active',   label: 'categories.statusActive' },
    { value: 'inactive', label: 'categories.statusInactive' },
  ];

  sortOptions = [
    { value: 'order',    label: 'categories.sortOrder' },
    { value: 'name',     label: 'categories.sortName' },
    { value: 'products', label: 'categories.sortProducts' },
    { value: 'partners', label: 'categories.sortPartners' },
  ];

  pageSize    = signal(4);
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

  /** Seules les catégories racines (sans parent) affichées dans la grille */
  rootCategories = computed(() =>
    this.filteredCategories().filter(c => c.parentId == null)
  );

  /** La pagination est gérée côté serveur — rootCategories contient déjà la bonne page. */
  paginatedCategories = computed(() => this.rootCategories());

  /** Indique si au moins un filtre actif (hors tri par défaut) */
  hasActiveFilters = computed(() =>
    this.searchQuery() !== '' || this.selectedStatus() !== 'all'
  );

  /** Langue courante (fr/en/ar) — pilote RTL dans le template */
  currentLang = signal(this.translate.currentLang || 'fr');
  isRtl = computed(() => this.currentLang() === 'ar');

  /** Retourne les sous-catégories directes d'une catégorie (triées par ordre) */
  getSubcategories(parentId: number): Category[] {
    return this.allCategories()
      .filter(c => c.parentId === parentId)
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  }

  ngOnInit(): void {
    this.setupSearch();
    this.loadCategories();
    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe(e => this.currentLang.set(e.lang));
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
    this.categoriesService.getCategoriesPaged(
      this.searchQuery() || undefined,
      this.selectedStatus(),
      this.currentPage(),
      this.pageSize()
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ content, totalElements }) => {
          this.allCategories.set(content);
          this.totalElements.set(totalElements);
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

  onStatusChange(value: 'all' | 'active' | 'inactive'): void {
    this.selectedStatus.set(value);
    this.currentPage.set(0);
    this.loadCategories();
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.selectedStatus.set('all');
    this.sortBy.set('order');
    this.sortOrder.set('asc');
    this.currentPage.set(0);
    this.pageSize.set(4);
    this.loadCategories();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadCategories();
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