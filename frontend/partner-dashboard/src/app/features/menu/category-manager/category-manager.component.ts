import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MenuService } from '../services/menu.service';
import { MenuCategory } from '../models/menu.models';

@Component({
  selector: 'app-category-manager',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './category-manager.component.html',
  styleUrls: ['./category-manager.component.scss'],
})
export class CategoryManagerComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private menuService = inject(MenuService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  // Signals
  loading = signal(false);
  saving = signal(false);
  editId = signal<number | null>(null);
  categories = signal<MenuCategory[]>([]);
  imagePreview = signal<string | null>(null);

  isEdit = computed(() => this.editId() !== null);
  pageTitleKey = computed(() => this.isEdit() ? 'MENU.CATEGORY_FORM.EDIT_TITLE' : 'MENU.CATEGORY_FORM.CREATE_TITLE');

  categoryForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    imageUrl: [''],
    position: [null],
    isVisible: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId.set(+id);
      this.loadCategory(+id);
    }
    this.loadAllCategories();
  }

  private loadAllCategories(): void {
    this.menuService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
    });
  }

  private loadCategory(id: number): void {
    this.loading.set(true);
    this.menuService.getCategory(id).subscribe({
      next: (cat) => {
        this.categoryForm.patchValue({
          name: cat.name,
          description: cat.description ?? '',
          imageUrl: cat.imageUrl ?? '',
          position: cat.position,
          isVisible: cat.isVisible,
        });
        this.imagePreview.set(cat.imageUrl ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onImageUrlChange(): void {
    const val = this.categoryForm.get('imageUrl')?.value;
    this.imagePreview.set(val || null);
  }

  onSubmit(): void {
    if (this.categoryForm.invalid) return;
    this.saving.set(true);

    const raw = this.categoryForm.value;
    const data = {
      name: raw.name,
      description: raw.description || undefined,
      imageUrl: raw.imageUrl || undefined,
      position: raw.position ?? undefined,
      isVisible: raw.isVisible ?? true,
    };

    const op = this.isEdit()
      ? this.menuService.updateCategory(this.editId()!, data)
      : this.menuService.createCategory(data);

    op.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.translate.instant(this.isEdit() ? 'MENU.CATEGORY_FORM.UPDATED' : 'MENU.CATEGORY_FORM.CREATED'),
          this.translate.instant('MENU.CLOSE'),
          { duration: 3000 }
        );
        this.router.navigate(['/menu'], { queryParams: { tab: 0 } });
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err?.error?.message || this.translate.instant('MENU.CATEGORY_FORM.SAVE_ERROR');
        this.snackBar.open(msg, this.translate.instant('MENU.CLOSE'), { duration: 4000 });
      },
    });
  }

  onCancel(): void {
    this.router.navigate(['/menu'], { queryParams: { tab: 0 } });
  }
}
