// src/app/features/menu/menu-list/menu-list.component.ts
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { TranslateModule } from '@ngx-translate/core';
import { MenuService } from '../services/menu.service';
import { ProductService } from '../services/product.service';
import { ProductCardComponent } from '../components/product-card/product-card.component';

@Component({
  selector: 'app-menu-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatMenuModule,
    MatSlideToggleModule,
    DragDropModule,
    TranslateModule,
    ProductCardComponent,
  ],
  templateUrl: './menu-list.component.html',
  styleUrls: ['./menu-list.component.scss'],
})
export class MenuListComponent implements OnInit {
  private menuService = inject(MenuService);
  private productService = inject(ProductService);
  private router = inject(Router);

  // Angular 19 Signals
  categories = signal<any[]>([]);
  products = signal<any[]>([]);
  selectedCategoryId = signal<string | null>(null);
  loading = signal(false);
  showUnavailable = signal(false);

  // Computed signals
  filteredProducts = computed(() => {
    const prods = this.products();
    return this.showUnavailable() ? prods : prods.filter(p => p.isAvailable);
  });

  productsCount = computed(() => this.filteredProducts().length);

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.menuService.getCategories().subscribe({
      next: (data) => {
        this.categories.set(data);
        if (data.length > 0) {
          this.loadProducts(data[0].id);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadProducts(categoryId: string): void {
    this.selectedCategoryId.set(categoryId);
    this.productService.getProducts(categoryId).subscribe({
      next: (data) => this.products.set(data),
    });
  }

  onCategoryDrop(event: CdkDragDrop<any[]>): void {
    const cats = [...this.categories()];
    moveItemInArray(cats, event.previousIndex, event.currentIndex);
    this.categories.set(cats);
    this.menuService.reorderCategories(cats.map(c => c.id)).subscribe();
  }

  editProduct(productId: string): void {
    this.router.navigate(['/menu/product', productId, 'edit']);
  }

  deleteProduct(productId: string): void {
    this.productService.deleteProduct(productId).subscribe(() => {
      this.products.update(prods => prods.filter(p => p.id !== productId));
    });
  }

  toggleProductAvailability(productId: string): void {
    // TODO: Implement
  }
}
