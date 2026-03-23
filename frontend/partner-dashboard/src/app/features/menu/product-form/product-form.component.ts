// src/app/features/menu/product-form/product-form.component.ts - Angular 19
import { Component, OnInit, OnDestroy, inject, signal, computed, ViewChild, ElementRef } from '@angular/core';
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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProductService } from '../services/product.service';
import { MenuService } from '../services/menu.service';
import { MenuCategory, Product, CreateProductRequest, UpdateProductRequest } from '../models/menu.models';
import { WebSocketService, PartnerNotification } from '@core/services/websocket.service';
import { Subscription } from 'rxjs';

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
    MatProgressSpinnerModule,
    MatSnackBarModule,
    TranslateModule,
  ],
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss'],
})
export class ProductFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private menuService = inject(MenuService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private wsService = inject(WebSocketService);

  private wsSub?: Subscription;

  @ViewChild('productImageInput') productImageInputRef?: ElementRef<HTMLInputElement>;

  // Signals
  editId = signal<number | null>(null);
  categories = signal<MenuCategory[]>([]);
  loading = signal(false);
  saving = signal(false);
  imagePreview = signal<string | null>(null);
  pendingProductImageFile = signal<File | null>(null);
  productImagePreviewUrl = signal<string | null>(null);
  uploadingProductImage = signal(false);
  isDragging = signal(false);

  // Moderation state (admin approvals/rejections)
  moderationStatus = signal<'PENDING' | 'APPROVED' | 'REJECTED' | null>(null);
  moderationReason = signal<string | null>(null);

  // Computed
  isEdit = computed(() => this.editId() !== null);
  pageTitleKey = computed(() => this.isEdit() ? 'MENU.PRODUCT_FORM.EDIT_TITLE' : 'MENU.PRODUCT_FORM.CREATE_TITLE');

  productForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
    price: [null, [Validators.required, Validators.min(0)]],
    categoryId: [null, Validators.required],
    imageUrl: [''],
    preparationTimeMin: [null],
    isAvailable: [true],
    isPopular: [false],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId.set(+idParam);
      this.loadProduct(+idParam);
    }
    this.loadCategories();

    // Realtime updates (bandeau modération) sur la page d'édition courante.
    this.wsSub = this.wsService.onPartnerNotification.subscribe((notif: PartnerNotification) => {
      const action = notif?.data?.['action'];
      if (action !== 'PRODUCT_APPROVED' && action !== 'PRODUCT_REJECTED') return;

      const rawProductId = notif?.data?.['productId'];
      const productId = typeof rawProductId === 'number' ? rawProductId : Number(rawProductId);
      if (!productId || Number.isNaN(productId)) return;

      if (this.editId() == null || productId !== this.editId()) return;

      this.moderationStatus.set(action === 'PRODUCT_APPROVED' ? 'APPROVED' : 'REJECTED' as any);
      this.moderationReason.set(
        action === 'PRODUCT_REJECTED' && notif?.data?.['reason'] != null ? String(notif.data['reason']) : null
      );
    });
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  loadCategories(): void {
    this.menuService.getCategories().subscribe({
      next: (data) => this.categories.set(data),
      error: (err) => console.error('Error loading categories:', err),
    });
  }

  loadProduct(id: number): void {
    this.loading.set(true);
    const prev = this.productImagePreviewUrl();
    if (prev) URL.revokeObjectURL(prev);
    this.productImagePreviewUrl.set(null);
    this.pendingProductImageFile.set(null);
    this.productService.getProduct(id).subscribe({
      next: (product: Product) => {
        this.productForm.patchValue({
          name: product.name,
          description: product.description ?? '',
          price: product.price,
          categoryId: product.categoryId,
          imageUrl: product.imageUrl ?? '',
          preparationTimeMin: product.preparationTimeMin ?? null,
          isAvailable: product.isAvailable,
          isPopular: product.isPopular ?? false,
        });
        this.imagePreview.set(product.imageUrl ?? null);
        this.moderationStatus.set(product.moderationStatus ?? null);
        this.moderationReason.set(product.moderationReason ?? null);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading product:', err);
        this.snackBar.open(
          this.translate.instant('MENU.PRODUCT_FORM.LOAD_ERROR'),
          this.translate.instant('MENU.CLOSE'),
          { duration: 3000 }
        );
        this.loading.set(false);
      },
    });
  }

  onImageUrlChange(): void {
    const url = this.productForm.get('imageUrl')?.value;
    this.imagePreview.set(url?.trim() || null);
  }

  triggerProductImageInput(): void {
    this.productImageInputRef?.nativeElement?.click();
  }

  onProductImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    input.value = '';
    if (!file || !file.type.startsWith('image/')) return;
    this.processImageFile(file);
  }

  productImageDisplayUrl(): string | null {
    return this.productImagePreviewUrl() || this.productForm.get('imageUrl')?.value || this.imagePreview();
  }

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    const val = this.productForm.value;
    this.saving.set(true);

    if (this.isEdit()) {
      const payload: UpdateProductRequest = {
        name: val.name,
        description: val.description || undefined,
        price: val.price,
        categoryId: val.categoryId,
        imageUrl: val.imageUrl || undefined,
        preparationTimeMin: val.preparationTimeMin || undefined,
        isAvailable: val.isAvailable,
        isPopular: val.isPopular,
      };
      this.productService.updateProduct(this.editId()!, payload).subscribe({
        next: () => {
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.UPDATE_SUCCESS'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 3000 }
          );
          this.saving.set(false);
          this.router.navigate(['/menu'], { queryParams: { tab: 1 } });
        },
        error: (err) => {
          console.error('Error updating product:', err);
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.UPDATE_ERROR'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 3000 }
          );
          this.saving.set(false);
        },
      });
    } else {
      const payload: CreateProductRequest = {
        name: val.name,
        description: val.description || undefined,
        price: val.price,
        categoryId: val.categoryId,
        imageUrl: val.imageUrl || undefined,
        preparationTimeMin: val.preparationTimeMin || undefined,
        isAvailable: val.isAvailable,
        isPopular: val.isPopular,
      };
      this.productService.createProduct(payload).subscribe({
        next: (saved) => {
          const pendingFile = this.pendingProductImageFile();
          if (pendingFile) {
            this.productService.uploadProductImage(saved.id, pendingFile).subscribe({
              next: () => {
                this.snackBar.open(
                  this.translate.instant('MENU.PRODUCT_FORM.CREATE_SUCCESS'),
                  this.translate.instant('MENU.CLOSE'),
                  { duration: 3000 }
                );
                this.saving.set(false);
                this.router.navigate(['/menu'], { queryParams: { tab: 1 } });
              },
              error: () => {
                this.snackBar.open(
                  this.translate.instant('MENU.PRODUCT_FORM.CREATE_IMAGE_UPLOAD_ERROR'),
                  this.translate.instant('MENU.CLOSE'),
                  { duration: 3000 }
                );
                this.saving.set(false);
                this.router.navigate(['/menu'], { queryParams: { tab: 1 } });
              },
            });
          } else {
            this.snackBar.open(
              this.translate.instant('MENU.PRODUCT_FORM.CREATE_SUCCESS'),
              this.translate.instant('MENU.CLOSE'),
              { duration: 3000 }
            );
            this.saving.set(false);
            this.router.navigate(['/menu'], { queryParams: { tab: 1 } });
          }
        },
        error: (err) => {
          console.error('Error creating product:', err);
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.CREATE_ERROR'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 3000 }
          );
          this.saving.set(false);
        },
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/menu'], { queryParams: { tab: 1 } });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    const file = event.dataTransfer?.files.item(0);
    if (!file || !file.type.startsWith('image/')) return;
    this.processImageFile(file);
  }

  private processImageFile(file: File): void {
    if (this.isEdit() && this.editId()) {
      this.uploadingProductImage.set(true);
      this.productService.uploadProductImage(this.editId()!, file).subscribe({
        next: (res) => {
          this.uploadingProductImage.set(false);
          this.productForm.patchValue({ imageUrl: res.url });
          this.imagePreview.set(res.url);
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.IMAGE_UPDATED'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 2000 }
          );
        },
        error: () => {
          this.uploadingProductImage.set(false);
          this.snackBar.open(
            this.translate.instant('MENU.PRODUCT_FORM.IMAGE_UPLOAD_ERROR'),
            this.translate.instant('MENU.CLOSE'),
            { duration: 4000 }
          );
        },
      });
    } else {
      this.pendingProductImageFile.set(file);
      const prev = this.productImagePreviewUrl();
      if (prev) URL.revokeObjectURL(prev);
      this.productImagePreviewUrl.set(URL.createObjectURL(file));
      this.imagePreview.set(this.productImagePreviewUrl());
    }
  }
}
