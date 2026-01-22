// src/app/features/menu/components/category-list/category-list.component.ts - Angular 19
import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [CommonModule, DragDropModule, MatIconModule, MatButtonModule],
  templateUrl: './category-list.component.html',
  styleUrls: ['./category-list.component.scss'],
})
export class CategoryListComponent {
  // Angular 19 Signal Inputs
  categories = input<any[]>([]);
  selectedId = input<string | null>(null);

  // Angular 19 Signal Outputs
  select = output<string>();
  reorder = output<CdkDragDrop<any[]>>();
  edit = output<any>();
  delete = output<string>();
}
