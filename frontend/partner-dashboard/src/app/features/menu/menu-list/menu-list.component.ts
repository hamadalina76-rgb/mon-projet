import { Component, OnInit, inject, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ConfirmationDialogComponent } from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { MenuService } from '../services/menu.service';
import { ProductService } from '../services/product.service';
import { OptionService } from '../services/option.service';
import { CategoryListComponent } from '../components/category-list/category-list.component';
import { ProductCardComponent } from '../components/product-card/product-card.component';
import {
  MenuCategory, Product, OptionGroup, OptionType,
  CreateCategoryRequest, UpdateCategoryRequest,
  CreateOptionGroupRequest, CreateOptionRequest,
} from '../models/menu.models';

@Component({
  selector: 'app-menu-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    DragDropModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatTooltipModule,
    MatChipsModule,
    TranslateModule,
    CategoryListComponent,
    ProductCardComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    ConfirmationDialogComponent,
  ],
  templateUrl: './menu-list.component.html',
  styleUrls: ['./menu-list.component.scss'],
})
export class MenuListComponent implements OnInit {
  private menuService = inject(MenuService);
  private productService = inject(ProductService);
  private optionService = inject(OptionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  // ─── Global state ─────────────────────────────────────────────────────────
  activeTab = signal(0);
  loading = signal(false);
  saving = signal(false);

  // ─── Tab 1: Categories ────────────────────────────────────────────────────
  categories = signal<MenuCategory[]>([]);
  selectedCategory = signal<MenuCategory | null>(null);
  /** 'create' | 'edit' | null */
  panelMode = signal<'create' | 'edit' | null>(null);

  /** Catégories avec productCount renvoyé par l’API GET /categories. */
  categoriesWithCounts = computed(() => this.categories());

  @ViewChild('categoryImageInput') categoryImageInputRef?: ElementRef<HTMLInputElement>;

  /** Catégorie en attente de confirmation de suppression (TC-35). */
  deleteCategoryToConfirm = signal<MenuCategory | null>(null);
  /** Message du dialog de confirmation (évite l’expression pipe dans le template). */
  deleteCategoryConfirmMessage = computed(() => {
    const cat = this.deleteCategoryToConfirm();
    return cat ? this.translate.instant('MENU.CONFIRM_DELETE_CATEGORY', { name: cat.name }) : '';
  });

  /** Fichier image sélectionné en mode création (uploadé après création de la catégorie). */
  pendingCategoryImageFile = signal<File | null>(null);
  /** URL d'aperçu (object URL) en mode création. */
  categoryImagePreviewUrl = signal<string | null>(null);
  uploadingCategoryImage = signal(false);

  categoryForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    imageUrl: [''],
    isVisible: [true],
  });

  // ─── Tab 2: Products (filtres et pagination côté backend) ───────────────────
  products = signal<Product[]>([]);
  filterCategoryId = signal<number | null>(null);
  searchQuery = signal('');
  statusFilter = signal<string>('all');
  currentPage = signal(0);
  pageSize = signal(20);
  totalElements = signal(0);
  totalPages = signal(0);
  productsLoading = signal(false);
  /** Pour l’onglet Options : liste chargée sans pagination stricte (selector). */
  productsForOptionsTab = signal<Product[]>([]);

  pageRangeFrom = computed(() =>
    this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1
  );
  pageRangeTo = computed(() =>
    Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements())
  );

  /** Toggle vue grille / liste */
  viewMode = signal<'grid' | 'list'>('grid');

  /** Drag-drop : réordonnancement possible uniquement sur la page courante (une seule page). */
  onProductsDrop(event: CdkDragDrop<Product[]>): void {
    const list = [...this.products()];
    moveItemInArray(list, event.previousIndex, event.currentIndex);
    this.products.set(list);
    const items = list.map((p, i) => ({ id: p.id, position: i + 1 }));
    this.productService.reorderProducts(items).subscribe({
      next: () => {},
      error: () => {
        this.loadProductsPage();
        this.snackBar.open(
          this.translate.instant('MENU.SNACK.REORDER_ERROR'),
          this.translate.instant('MENU.CLOSE'),
          { duration: 3000 }
        );
      },
    });
  }

  // ─── Tab 3: Options ───────────────────────────────────────────────────────
  selectedProductForOptions = signal<Product | null>(null);
  optionGroups = signal<OptionGroup[]>([]);
  optionsLoading = signal(false);
  readonly OptionType = OptionType;

  addGroupForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    type: [OptionType.SINGLE, Validators.required],
    isRequired: [false],
    minSelection: [0],
    maxSelection: [1],
  });

  addOptionForms = signal<{ [groupId: number]: FormGroup }>({});

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab) this.activeTab.set(+tab);
    this.loadCategories();
    this.loadProductsPage();
  }

  onTabChange(index: number): void {
    this.activeTab.set(index);
    if (index === 2) this.loadProductsForOptionsTab();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: index },
      replaceUrl: true,
    });
  }

  navigateToNewProduct(): void {
    this.router.navigate(['products/new'], { relativeTo: this.route });
  }

  // ─── Categories ──────────────────────────────────────────────────────────

  loadCategories(): void {
    this.loading.set(true);
    this.menuService.getCategories().subscribe({
      next: (data) => {
        // attach productCount from products if already loaded
        this.categories.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onCategorySelect(cat: MenuCategory): void {
    this.selectedCategory.set(cat);
    this.panelMode.set('edit');
    this.clearCategoryImageSelection();
    this.categoryForm.patchValue({
      name: cat.name,
      description: cat.description ?? '',
      imageUrl: cat.imageUrl ?? '',
      isVisible: cat.isVisible,
    });
  }

  onVisibilityToggled(updated: MenuCategory): void {
    this.categories.update(cats =>
      cats.map(c => (c.id === updated.id ? { ...c, isVisible: updated.isVisible } : c))
    );
  }

  onDeleteCategory(cat: MenuCategory): void {
    this.deleteCategoryToConfirm.set(cat);
  }

  confirmDeleteCategory(): void {
    const cat = this.deleteCategoryToConfirm();
    if (!cat) return;
    this.menuService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.deleteCategoryToConfirm.set(null);
        this.snackBar.open(
          this.translate.instant('MENU.SNACK.CATEGORY_DELETED'),
          this.translate.instant('MENU.CLOSE'),
          { duration: 3000 }
        );
        if (this.selectedCategory()?.id === cat.id) {
          this.closePanel();
        }
        this.loadCategories();
        this.loadProductsPage();
      },
      error: (err) => {
        const msg = err?.error?.message || this.translate.instant('MENU.SNACK.DELETE_ERROR');
        this.snackBar.open(msg, this.translate.instant('MENU.CLOSE'), { duration: 4000 });
      },
    });
  }

  openCreatePanel(): void {
    this.panelMode.set('create');
    this.selectedCategory.set(null);
    this.clearCategoryImageSelection();
    this.categoryForm.reset({ name: '', description: '', imageUrl: '', isVisible: true });
  }

  closePanel(): void {
    this.clearCategoryImageSelection();
    this.panelMode.set(null);
    this.selectedCategory.set(null);
    this.categoryForm.reset();
  }

  triggerCategoryImageInput(): void {
    this.categoryImageInputRef?.nativeElement?.click();
  }

  onCategoryImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    input.value = '';
    if (!file || !file.type.startsWith('image/')) return;

    const mode = this.panelMode();
    const sel = this.selectedCategory();

    if (mode === 'edit' && sel) {
      this.uploadingCategoryImage.set(true);
      this.menuService.uploadCategoryImage(sel.id, file).subscribe({
        next: (res) => {
          this.uploadingCategoryImage.set(false);
          this.categoryForm.patchValue({ imageUrl: res.url });
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.IMAGE_UPDATED'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 2000 }
          );
        },
        error: () => {
          this.uploadingCategoryImage.set(false);
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.IMAGE_UPLOAD_ERROR'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 4000 }
          );
        },
      });
    } else {
      this.pendingCategoryImageFile.set(file);
      const prev = this.categoryImagePreviewUrl();
      if (prev) URL.revokeObjectURL(prev);
      this.categoryImagePreviewUrl.set(URL.createObjectURL(file));
    }
  }

  private clearCategoryImageSelection(): void {
    const prev = this.categoryImagePreviewUrl();
    if (prev) URL.revokeObjectURL(prev);
    this.categoryImagePreviewUrl.set(null);
    this.pendingCategoryImageFile.set(null);
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) return;
    this.saving.set(true);

    const raw = this.categoryForm.value;
    const mode = this.panelMode();
    const sel = this.selectedCategory();
    const pendingFile = this.pendingCategoryImageFile();

    const data: CreateCategoryRequest = {
      name: raw.name,
      description: raw.description || undefined,
      imageUrl: raw.imageUrl || undefined,
      isVisible: raw.isVisible ?? true,
    };

    if (mode === 'edit' && sel) {
      this.menuService.updateCategory(sel.id, data).subscribe({
        next: () => this.onCategorySaved(),
        error: (err) => this.onCategorySaveError(err),
      });
      return;
    }

    this.menuService.createCategory(data).subscribe({
      next: (saved) => {
        if (pendingFile) {
          this.menuService.uploadCategoryImage(saved.id, pendingFile).subscribe({
            next: () => this.onCategorySaved(),
            error: (err) => this.onCategorySaveError(err),
          });
        } else {
          this.onCategorySaved();
        }
      },
      error: (err) => this.onCategorySaveError(err),
    });
  }

  private onCategorySaved(): void {
    this.saving.set(false);
    this.snackBar.open(
      this.translate.instant('MENU.SNACK.CATEGORY_SAVED'),
      this.translate.instant('MENU.CLOSE'),
      { duration: 3000 }
    );
    this.loadCategories();
    this.closePanel();
  }

  private onCategorySaveError(err: any): void {
    this.saving.set(false);
    const msg = err?.error?.message || this.translate.instant('MENU.CATEGORY_FORM.SAVE_ERROR');
    this.snackBar.open(msg, this.translate.instant('MENU.CLOSE'), { duration: 4000 });
  }

  // ─── Products ────────────────────────────────────────────────────────────

  loadProductsPage(): void {
    this.productsLoading.set(true);
    this.productService.getProductsPage({
      search: this.searchQuery() || undefined,
      categoryId: this.filterCategoryId(),
      status: this.statusFilter(),
      page: this.currentPage(),
      size: this.pageSize(),
    }).subscribe({
      next: (res) => {
        this.products.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.productsLoading.set(false);
      },
      error: () => this.productsLoading.set(false),
    });
  }

  filterByCategory(catId: number | null): void {
    this.filterCategoryId.set(catId);
    this.currentPage.set(0);
    this.loadProductsPage();
  }

  private searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;

  onSearchChange(value: string): void {
    this.searchQuery.set(value ?? '');
    this.currentPage.set(0);
    if (this.searchDebounceTimer != null) clearTimeout(this.searchDebounceTimer);
    this.searchDebounceTimer = setTimeout(() => {
      this.searchDebounceTimer = null;
      this.loadProductsPage();
    }, 400);
  }

  onStatusFilterChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadProductsPage();
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadProductsPage();
  }

  /** Charge une liste de produits pour le sélecteur de l’onglet Options. */
  loadProductsForOptionsTab(): void {
    this.productService.getProductsPage({
      search: '',
      categoryId: null,
      status: 'all',
      page: 0,
      size: 500,
    }).subscribe({
      next: (res) => this.productsForOptionsTab.set(res.content),
    });
  }

  editProduct(productId: number): void {
    this.router.navigate(['products', productId, 'edit'], { relativeTo: this.route });
  }

  toggleProductAvailability(productId: number): void {
    const product = this.products().find(p => p.id === productId);
    const newAvailable = product ? !product.isAvailable : true;
    this.productService.toggleAvailability(productId, newAvailable).subscribe({
      next: (updated) => {
        this.products.update(prods =>
          prods.map(p => (p.id === updated.id ? { ...p, isAvailable: updated.isAvailable } : p))
        );
      },
    });
  }

  duplicateProduct(productId: number): void {
    this.productService.duplicateProduct(productId).subscribe({
      next: () => {
        this.loadProductsPage();
        this.snackBar.open(
          this.translate.instant('MENU.SNACK.PRODUCT_DUPLICATED'),
          this.translate.instant('MENU.CLOSE'),
          { duration: 3000 }
        );
      },
      error: (err) => {
        const msg = err?.error?.message || this.translate.instant('MENU.SNACK.DUPLICATE_ERROR');
        this.snackBar.open(msg, this.translate.instant('MENU.CLOSE'), { duration: 4000 });
      },
    });
  }

  deleteProduct(productId: number): void {
    this.productService.deleteProduct(productId).subscribe({
      next: () => {
        this.loadProductsPage();
        this.snackBar.open('Produit supprimé', 'Fermer', { duration: 3000 });
      },
    });
  }

  onProductNameChange(event: { productId: number; name: string }): void {
    this.productService.updateProduct(event.productId, { name: event.name }).subscribe({
      next: (updated) => {
        this.products.update(prods =>
          prods.map(p => (p.id === updated.id ? { ...p, name: updated.name } : p))
        );
      },
    });
  }

  // ─── Options (Tab 3) ─────────────────────────────────────────────────────

  selectProductForOptions(product: Product): void {
    this.selectedProductForOptions.set(product);
    this.loadOptionGroups(product.id);
  }

  loadOptionGroups(productId: number): void {
    this.optionsLoading.set(true);
    this.optionService.getGroups(productId).subscribe({
      next: (groups) => {
        this.optionGroups.set(groups);
        // Build a blank add-option form per group
        const forms: { [groupId: number]: FormGroup } = {};
        groups.forEach(g => {
          forms[g.id] = this.fb.group({
            name: ['', Validators.required],
            priceModifier: [0],
            isDefault: [false],
          });
        });
        this.addOptionForms.set(forms);
        this.optionsLoading.set(false);
      },
      error: () => this.optionsLoading.set(false),
    });
  }

  addGroup(): void {
    const product = this.selectedProductForOptions();
    if (!product || this.addGroupForm.invalid) return;
    const data: CreateOptionGroupRequest = this.addGroupForm.value;
    this.optionService.createGroup(product.id, data).subscribe({
      next: () => {
        this.addGroupForm.reset({ name: '', type: OptionType.SINGLE, isRequired: false, minSelection: 0, maxSelection: 1 });
        this.loadOptionGroups(product.id);
      },
    });
  }

  deleteGroup(groupId: number): void {
    const product = this.selectedProductForOptions();
    if (!product) return;
    this.optionService.deleteGroup(product.id, groupId).subscribe({
      next: () => this.loadOptionGroups(product.id),
    });
  }

  addOptionToGroup(groupId: number): void {
    const product = this.selectedProductForOptions();
    if (!product) return;
    const form = this.addOptionForms()[groupId];
    if (!form || form.invalid) return;
    const data: CreateOptionRequest = form.value;
    this.optionService.createOption(product.id, groupId, data).subscribe({
      next: () => {
        form.reset({ name: '', priceModifier: 0, isDefault: false });
        this.loadOptionGroups(product.id);
      },
    });
  }

  deleteOption(groupId: number, optionId: number): void {
    const product = this.selectedProductForOptions();
    if (!product) return;
    this.optionService.deleteOption(product.id, groupId, optionId).subscribe({
      next: () => this.loadOptionGroups(product.id),
    });
  }

  getGroupForm(groupId: number): FormGroup {
    return this.addOptionForms()[groupId];
  }
}
