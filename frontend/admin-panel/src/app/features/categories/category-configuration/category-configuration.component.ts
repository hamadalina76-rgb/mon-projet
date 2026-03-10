import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { CategoriesService } from '../services/categories.service';
import { Category, CategoryBusinessType, CreateCategoryRequest, UpdateCategoryRequest } from '@core/models/category.model';

@Component({
  selector: 'app-category-configuration',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    TranslateModule
  ],
  templateUrl: './category-configuration.component.html',
  styleUrls: ['./category-configuration.component.scss']
})
export class CategoryConfigurationComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private categoriesService = inject(CategoriesService);
  private destroy$ = new Subject<void>();

  categoryForm!: FormGroup;
  selectedTabIndex = signal(0);
  isEditMode = signal(false);
  categoryId = signal<number | null>(null);
  saving = signal(false);
  uploading = signal(false);
  iconPreviewUrl = signal<string>('');

  // Hierarchy / autocomplete
  parentCandidates = signal<Category[]>([]);
  selectedParent = signal<Category | null>(null);
  parentSearchCtrl = new FormControl('');
  filteredParents = signal<Category[]>([]);

  /** Breadcrumb path: [root name, …, selected parent name] */
  breadcrumbPath = computed<string[]>(() => {
    const parent = this.selectedParent();
    if (!parent) return [];
    const path: string[] = [this.getCatName(parent)];
    // walk ancestors from parentCandidates list
    let pid = parent.parentId;
    const checked = new Set<number>();
    while (pid != null) {
      if (checked.has(pid)) break;
      checked.add(pid);
      const ancestor = this.parentCandidates().find(c => c.id === pid);
      if (!ancestor) break;
      path.unshift(this.getCatName(ancestor));
      pid = ancestor.parentId;
    }
    return path;
  });

  isSubcategoryMode = computed(() => this.selectedParent() !== null || !!this.categoryForm?.get('parentId')?.value);

  private iconObjectUrl: string | null = null;

  businessTypes: { value: CategoryBusinessType; label: string; icon: string }[] = [
    { value: CategoryBusinessType.RESTAURANT, label: 'Restaurant', icon: 'restaurant' },
    { value: CategoryBusinessType.GROCERY, label: 'Épicerie / Supermarché', icon: 'local_grocery_store' },
    { value: CategoryBusinessType.PHARMACY, label: 'Pharmacie', icon: 'local_pharmacy' },
    { value: CategoryBusinessType.OTHER, label: 'Autre', icon: 'category' },
  ];

  ngOnInit(): void {
    this.initializeForm();
    this.checkEditMode();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.iconObjectUrl) URL.revokeObjectURL(this.iconObjectUrl);
  }

  private initializeForm(): void {
    this.categoryForm = this.fb.group({
      nameFr: ['', [Validators.required, Validators.minLength(2)]],
      nameEn: ['', [Validators.required, Validators.minLength(2)]],
      nameAr: [''],
      description: [''],
      parentId: [null],
      categoryBusinessType: ['', Validators.required],
      categoryType: [''],
      displayOrder: [1, [Validators.required, Validators.min(1)]],
      backgroundColor: ['#EC131E'],
      textColor: ['#FFFFFF'],
      icon: [''],
      isActive: [true],
      isFeatured: [false],
    });

    // Filter autocomplete on each keystroke
    this.parentSearchCtrl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(term => this.filterParents(term ?? ''));
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    // Support both route param (:parentId/create-sub) and query param (?parentId=)
    const parentIdParam =
      this.route.snapshot.paramMap.get('parentId') ??
      this.route.snapshot.queryParamMap.get('parentId');

    if (id) {
      this.isEditMode.set(true);
      this.categoryId.set(Number(id));
      this.loadParentCandidates(Number(id));
      this.loadCategory(Number(id));
    } else {
      this.loadParentCandidates();
      // Pre-fill parentId from query param (e.g. ?parentId=3)
      if (parentIdParam) {
        const pid = Number(parentIdParam);
        this.categoryForm.patchValue({ parentId: pid });
        // selectedParent will be resolved once candidates load
        this.categoriesService.getParentCandidates()
          .pipe(takeUntil(this.destroy$))
          .subscribe(candidates => {
            const found = candidates.find(c => c.id === pid) ?? null;
            if (found) {
              this.selectedParent.set(found);
              this.parentSearchCtrl.setValue(this.getCatName(found), { emitEvent: false });
            }
          });
      }
    }
  }

  private loadParentCandidates(excludeId?: number): void {
    this.categoriesService.getParentCandidates(excludeId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: candidates => {
          this.parentCandidates.set(candidates);
          this.filteredParents.set(candidates);
          // If we already have a parentId (edit mode), resolve the selected parent
          const pid = this.categoryForm.get('parentId')?.value as number | null;
          if (pid) {
            const found = candidates.find(c => c.id === pid) ?? null;
            if (found) {
              this.selectedParent.set(found);
              this.parentSearchCtrl.setValue(this.getCatName(found), { emitEvent: false });
            }
          }
        },
        error: err => console.error('Erreur chargement parent candidates:', err)
      });
  }

  private filterParents(term: string): void {
    const lower = term.toLowerCase();
    this.filteredParents.set(
      this.parentCandidates().filter(c =>
        this.getCatName(c).toLowerCase().includes(lower)
      )
    );
  }

  onParentSelected(cat: Category | null): void {
    this.selectedParent.set(cat);
    this.categoryForm.patchValue({ parentId: cat?.id ?? null });
  }

  clearParent(): void {
    this.selectedParent.set(null);
    this.categoryForm.patchValue({ parentId: null });
    this.parentSearchCtrl.setValue('');
    this.filteredParents.set(this.parentCandidates());
  }

  getCatName(cat: Category): string {
    return cat.nameI18n['fr'] || cat.nameI18n['en'] || cat.nameI18n['ar'] || '';
  }

  displayParentFn = (cat: Category | null): string => cat ? this.getCatName(cat) : '';

  private loadCategory(id: number): void {
    this.categoriesService.getCategoryById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cat: Category) => {
          this.categoryForm.patchValue({
            nameFr: cat.nameI18n['fr'] || '',
            nameEn: cat.nameI18n['en'] || '',
            nameAr: cat.nameI18n['ar'] || '',
            description: cat.description || '',
            parentId: cat.parentId || null,
            categoryBusinessType: cat.categoryBusinessType,
            categoryType: cat.categoryType || '',
            displayOrder: cat.displayOrder,
            backgroundColor: cat.backgroundColor || '#EC131E',
            textColor: cat.textColor || '#FFFFFF',
            isActive: cat.isActive,
            isFeatured: cat.isFeatured,
            icon: cat.icon || '',
          });
          this.iconPreviewUrl.set(cat.icon || '');
          // Resolve parent after candidates are potentially already loaded
          if (cat.parentId) {
            const found = this.parentCandidates().find(c => c.id === cat.parentId) ?? null;
            if (found) {
              this.selectedParent.set(found);
              this.parentSearchCtrl.setValue(this.getCatName(found), { emitEvent: false });
            }
          }
        },
        error: () => {
          this.toastr.error('Impossible de charger la catégorie');
          this.router.navigate(['/categories']);
        }
      });
  }

  onTabChange(index: number): void {
    this.selectedTabIndex.set(index);
  }

  onCancel(): void {
    this.router.navigate(['/categories']);
  }

  onSave(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      this.toastr.warning('Veuillez corriger les erreurs dans le formulaire');
      return;
    }

    const v = this.categoryForm.value;
    const nameI18n: { [locale: string]: string } = {};
    if (v.nameFr) nameI18n['fr'] = v.nameFr;
    if (v.nameEn) nameI18n['en'] = v.nameEn;
    if (v.nameAr) nameI18n['ar'] = v.nameAr;

    this.saving.set(true);

    if (this.isEditMode()) {
      const payload: UpdateCategoryRequest = {
        nameI18n,
        description: v.description || undefined,
        parentId: v.parentId || undefined,
        displayOrder: v.displayOrder,
        isFeatured: v.isFeatured,
        isActive: v.isActive,
        categoryBusinessType: v.categoryBusinessType,
        categoryType: v.categoryType || undefined,
        backgroundColor: v.backgroundColor || undefined,
        textColor: v.textColor || undefined,
        icon: v.icon || undefined,
      };
      this.categoriesService.updateCategory(this.categoryId()!, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.toastr.success('Catégorie mise à jour');
            this.router.navigate(['/categories']);
          },
          error: (err) => {
            this.handleSaveError(err, 'mise à jour');
          }
        });
    } else {
      const payload: CreateCategoryRequest = {
        nameI18n,
        description: v.description || undefined,
        parentId: v.parentId || undefined,
        displayOrder: v.displayOrder,
        isFeatured: v.isFeatured,
        categoryBusinessType: v.categoryBusinessType,
        categoryType: v.categoryType || undefined,
        backgroundColor: v.backgroundColor || undefined,
        textColor: v.textColor || undefined,
        icon: v.icon || undefined,
      };
      this.categoriesService.createCategory(payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.toastr.success('Catégorie créée avec succès');
            this.router.navigate(['/categories']);
          },
          error: (err) => {
            this.handleSaveError(err, 'création');
          }
        });
    }
  }

  private handleSaveError(err: any, action: string): void {
    if (err.status === 409) {
      this.toastr.error('Ce nom de catégorie existe déjà pour cette locale');
    } else if (err.status === 400) {
      this.toastr.error(err.error?.message || 'Données invalides');
    } else {
      this.toastr.error(`Erreur lors de la ${action}`);
    }
    this.saving.set(false);
  }

  getError(field: string): string {
    const ctrl = this.categoryForm.get(field);
    if (ctrl?.hasError('required')) return 'Ce champ est requis';
    if (ctrl?.hasError('minlength')) return `Minimum ${ctrl.getError('minlength').requiredLength} caractères`;
    if (ctrl?.hasError('min')) return `Valeur minimum: ${ctrl.getError('min').min}`;
    return '';
  }

  onIconError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];

    if (this.iconObjectUrl) URL.revokeObjectURL(this.iconObjectUrl);
    this.iconObjectUrl = URL.createObjectURL(file);
    this.iconPreviewUrl.set(this.iconObjectUrl);

    this.uploading.set(true);
    this.categoriesService.uploadIcon(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (url) => {
          this.categoryForm.patchValue({ icon: url });
          this.iconPreviewUrl.set(url);
          if (this.iconObjectUrl) { URL.revokeObjectURL(this.iconObjectUrl); this.iconObjectUrl = null; }
          this.uploading.set(false);
        },
        error: () => {
          this.toastr.error("Erreur lors de l'upload de l'icône");
          this.iconPreviewUrl.set('');
          if (this.iconObjectUrl) { URL.revokeObjectURL(this.iconObjectUrl); this.iconObjectUrl = null; }
          this.uploading.set(false);
        }
      });
    input.value = '';
  }

  removeIcon(event: Event): void {
    event.stopPropagation();
    this.categoryForm.patchValue({ icon: '' });
    this.iconPreviewUrl.set('');
    if (this.iconObjectUrl) { URL.revokeObjectURL(this.iconObjectUrl); this.iconObjectUrl = null; }
  }
}