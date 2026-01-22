// src/app/features/menu/category-manager/category-manager.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { TranslateModule } from '@ngx-translate/core';
import { MenuService } from '../services/menu.service';

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
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    DragDropModule,
    TranslateModule,
  ],
  templateUrl: './category-manager.component.html',
  styleUrls: ['./category-manager.component.scss'],
})
export class CategoryManagerComponent implements OnInit {
  private fb = inject(FormBuilder);
  private menuService = inject(MenuService);
  private dialog = inject(MatDialog);

  // Angular 19 Signals
  categories = signal<any[]>([]);
  showForm = signal(false);
  editingId = signal<string | null>(null);
  loading = signal(false);

  categoryForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.menuService.getCategories().subscribe({
      next: (data) => {
        this.categories.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onDrop(event: CdkDragDrop<any[]>): void {
    const cats = [...this.categories()];
    moveItemInArray(cats, event.previousIndex, event.currentIndex);
    this.categories.set(cats);
    this.saveOrder();
  }

  saveOrder(): void {
    const order = this.categories().map(c => c.id);
    this.menuService.reorderCategories(order).subscribe();
  }

  showAddForm(): void {
    this.showForm.set(true);
    this.editingId.set(null);
    this.categoryForm.reset();
  }

  editCategory(category: any): void {
    this.showForm.set(true);
    this.editingId.set(category.id);
    this.categoryForm.patchValue(category);
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) return;
    this.loading.set(true);
    
    const data = this.categoryForm.value;
    const operation = this.editingId()
      ? this.menuService.updateCategory(this.editingId()!, data)
      : this.menuService.createCategory(data);

    operation.subscribe({
      next: () => {
        this.loading.set(false);
        this.cancel();
        this.loadCategories();
      },
      error: () => this.loading.set(false),
    });
  }

  deleteCategory(id: string): void {
    this.menuService.deleteCategory(id).subscribe({
      next: () => this.loadCategories(),
    });
  }

  cancel(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.categoryForm.reset();
  }
}
