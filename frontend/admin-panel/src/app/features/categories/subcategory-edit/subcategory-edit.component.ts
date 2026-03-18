import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { CategoriesService } from '../services/categories.service';
import { Category, UpdateCategoryRequest } from '@core/models/category.model';

@Component({
  selector: 'app-subcategory-edit',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTabsModule,
    MatDividerModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './subcategory-edit.component.html',
  styleUrls: ['./subcategory-edit.component.scss'],
})
export class SubcategoryEditComponent implements OnInit, OnDestroy {
  private fb              = inject(FormBuilder);
  private router          = inject(Router);
  private route           = inject(ActivatedRoute);
  private toastr          = inject(ToastrService);
  private translate       = inject(TranslateService);
  private categoriesService = inject(CategoriesService);
  private destroy$        = new Subject<void>();

  form!: FormGroup;
  subcategoryId = signal<number | null>(null);
  subcategory   = signal<Category | null>(null);
  parentCat     = signal<Category | null>(null);
  loading       = signal(true);
  saving        = signal(false);
  uploading     = signal(false);
  iconPreviewUrl = signal<string>('');
  selectedTabIndex = signal(0);

  private iconObjectUrl: string | null = null;

  currentLang = signal(this.translate.currentLang || 'fr');
  isRtl = computed(() => this.currentLang() === 'ar');

  ngOnInit(): void {
    this.initForm();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.subcategoryId.set(id);
    this.loadSubcategory(id);
    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe(e => this.currentLang.set(e.lang));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.iconObjectUrl) URL.revokeObjectURL(this.iconObjectUrl);
  }

  private initForm(): void {
    this.form = this.fb.group({
      nameFr:          ['', [Validators.required, Validators.minLength(2)]],
      nameEn:          ['', [Validators.required, Validators.minLength(2)]],
      nameAr:          [''],
      description:     [''],
      displayOrder:    [1, [Validators.required, Validators.min(1)]],
      backgroundColor: ['#EC131E'],
      textColor:       ['#FFFFFF'],
      icon:            [''],
      isActive:        [true],
      isFeatured:      [false],
    });
  }

  private loadSubcategory(id: number): void {
    this.loading.set(true);
    this.categoriesService.getCategoryById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: cat => {
          this.subcategory.set(cat);
          this.form.patchValue({
            nameFr:          cat.nameI18n['fr'] || '',
            nameEn:          cat.nameI18n['en'] || '',
            nameAr:          cat.nameI18n['ar'] || '',
            description:     cat.description   || '',
            displayOrder:    cat.displayOrder,
            backgroundColor: cat.backgroundColor || '#EC131E',
            textColor:       cat.textColor       || '#FFFFFF',
            icon:            cat.icon            || '',
            isActive:        cat.isActive,
            isFeatured:      cat.isFeatured,
          });
          this.iconPreviewUrl.set(cat.icon || '');
          if (cat.parentId) {
            this.categoriesService.getCategoryById(cat.parentId)
              .pipe(takeUntil(this.destroy$))
              .subscribe({ next: p => this.parentCat.set(p) });
          }
          this.loading.set(false);
        },
        error: () => {
          this.toastr.error(this.translate.instant('categories.loadError'));
          this.router.navigate(['/categories']);
        }
      });
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (ctrl?.hasError('required'))  return this.translate.instant('categories.fieldRequired');
    if (ctrl?.hasError('minlength')) return this.translate.instant('categories.minLength', { count: ctrl.getError('minlength').requiredLength });
    if (ctrl?.hasError('min'))       return this.translate.instant('categories.minValue', { min: ctrl.getError('min').min });
    return '';
  }

  getCatName(cat: Category | null): string {
    if (!cat) return '';
    return cat.nameI18n['fr'] || cat.nameI18n['en'] || cat.nameI18n['ar'] || '';
  }

  onTabChange(index: number): void {
    this.selectedTabIndex.set(index);
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning(this.translate.instant('categories.formInvalid'));
      return;
    }
    const v = this.form.value;
    const nameI18n: Record<string, string> = {};
    if (v.nameFr) nameI18n['fr'] = v.nameFr;
    if (v.nameEn) nameI18n['en'] = v.nameEn;
    if (v.nameAr) nameI18n['ar'] = v.nameAr;

    const payload: UpdateCategoryRequest = {
      nameI18n,
      description:     v.description     || undefined,
      displayOrder:    v.displayOrder,
      backgroundColor: v.backgroundColor || undefined,
      textColor:       v.textColor        || undefined,
      icon:            v.icon             || undefined,
      isActive:        v.isActive,
      isFeatured:      v.isFeatured,
      parentId:        this.subcategory()?.parentId ?? undefined,
    };

    this.saving.set(true);
    this.categoriesService.updateCategory(this.subcategoryId()!, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('categories.subcategoryUpdated'));
          const parentId = this.subcategory()?.parentId;
          if (parentId) {
            this.router.navigate(['/categories']);
          } else {
            this.router.navigate(['/categories']);
          }
        },
        error: (err) => {
          const typedErr = err as { status?: number; error?: { message?: string } };
          if (typedErr.status === 409) {
            this.toastr.error(this.translate.instant('categories.duplicateName'));
          } else {
            this.toastr.error(this.translate.instant('categories.saveError'));
          }
          this.saving.set(false);
        }
      });
  }

  onCancel(): void {
    this.router.navigate(['/categories']);
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
        next: url => {
          this.form.patchValue({ icon: url });
          this.iconPreviewUrl.set(url);
          if (this.iconObjectUrl) { URL.revokeObjectURL(this.iconObjectUrl); this.iconObjectUrl = null; }
          this.uploading.set(false);
        },
        error: () => {
          this.toastr.error(this.translate.instant('categories.uploadError'));
          this.iconPreviewUrl.set('');
          this.uploading.set(false);
        }
      });
    input.value = '';
  }

  removeIcon(event: Event): void {
    event.stopPropagation();
    this.form.patchValue({ icon: '' });
    this.iconPreviewUrl.set('');
    if (this.iconObjectUrl) { URL.revokeObjectURL(this.iconObjectUrl); this.iconObjectUrl = null; }
  }

  onIconError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }
}
