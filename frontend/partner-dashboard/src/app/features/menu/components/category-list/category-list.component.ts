import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { MenuService } from '../../services/menu.service';
import { MenuCategory, ReorderItem } from '../../models/menu.models';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [
    CommonModule,
    DragDropModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatSlideToggleModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './category-list.component.html',
  styleUrls: ['./category-list.component.scss'],
})
export class CategoryListComponent {
  private menuService = inject(MenuService);

  // Inputs
  categories = input<MenuCategory[]>([]);
  selectedId = input<number | null>(null);

  // Outputs
  select = output<MenuCategory>();
  categoriesChange = output<MenuCategory[]>();
  visibilityToggled = output<MenuCategory>();
  deleteCategory = output<MenuCategory>();

  /** Catégorie ciblée par le menu contextuel (une seule instance de menu). */
  categoryForMenu = signal<MenuCategory | null>(null);

  canDelete(cat: MenuCategory): boolean {
    // Disable deletion when the category contains products.
    // (User request: disable delete when products are affected.)
    return (cat.productCount ?? 0) === 0;
  }

  openCategoryMenu(cat: MenuCategory, event: Event): void {
    event.stopPropagation();
    this.categoryForMenu.set(cat);
  }

  onDrop(event: CdkDragDrop<MenuCategory[]>): void {
    const cats = [...this.categories()];
    moveItemInArray(cats, event.previousIndex, event.currentIndex);
    // Persist new order
    const items: ReorderItem[] = cats.map((c, i) => ({ id: c.id, position: i + 1 }));
    this.menuService.reorderCategories(items).subscribe({
      next: (updated) => this.categoriesChange.emit(updated),
    });
    // Optimistic update
    this.categoriesChange.emit(cats);
  }

  onToggleVisibility(cat: MenuCategory, event: Event): void {
    event.stopPropagation();
    this.menuService.toggleCategoryVisibility(cat.id).subscribe({
      next: (updated) => this.visibilityToggled.emit(updated),
    });
  }

  onEdit(cat: MenuCategory, event: Event): void {
    event.stopPropagation();
    this.select.emit(cat);
  }

  onDelete(cat: MenuCategory, event: Event): void {
    event.stopPropagation();
    if (!this.canDelete(cat)) return;
    this.deleteCategory.emit(cat);
  }
}
