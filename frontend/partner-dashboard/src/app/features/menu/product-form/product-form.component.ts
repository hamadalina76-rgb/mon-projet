// src/app/features/menu/product-form/product-form.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { ProductService } from '../services/product.service';
import { MenuService } from '../services/menu.service';

@Component({
  selector: 'app-product-form',
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
    MatChipsModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss'],
})
export class ProductFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private menuService = inject(MenuService);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  categories = signal<any[]>([]);
  loading = signal(false);
  saving = signal(false);
  isEdit = signal(false);
  productId = signal<string | null>(null);
  imagePreview = signal<string | null>(null);

  // Computed
  pageTitle = computed(() => this.isEdit() ? 'Modifier le produit' : 'Nouveau produit');

  productForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    categoryId: ['', Validators.required],
    prepTime: [15],
    isAvailable: [true],
    options: this.fb.array([]),
  });

  get optionsArray(): FormArray {
    return this.productForm.get('options') as FormArray;
  }

  ngOnInit(): void {
    this.loadCategories();
    const id = this.route.snapshot.paramMap.get('id');
    this.productId.set(id);
    if (id) {
      this.isEdit.set(true);
      this.loadProduct();
    }
  }

  loadCategories(): void {
    this.menuService.getCategories().subscribe({
      next: (data) => this.categories.set(data),
      error: (err) => console.error('Error loading categories:', err)
    });
  }

  loadProduct(): void {
    const id = this.productId();
    if (!id) return;
    
    this.loading.set(true);
    this.productService.getProduct(id).subscribe({
      next: (product) => {
        this.productForm.patchValue(product);
        this.imagePreview.set(product.image);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading product:', err);
        this.loading.set(false);
      }
    });
  }

  addOption(): void {
    const optionGroup = this.fb.group({
      name: ['', Validators.required],
      choices: this.fb.array([]),
    });
    this.optionsArray.push(optionGroup);
  }

  removeOption(index: number): void {
    this.optionsArray.removeAt(index);
  }

  onImageSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit(): void {
    if (this.productForm.invalid) return;
    // TODO: Implement save logic
  }
}
