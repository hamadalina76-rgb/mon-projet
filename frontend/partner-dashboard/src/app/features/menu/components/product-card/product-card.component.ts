// src/app/features/menu/components/product-card/product-card.component.ts - Angular 19
import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSlideToggleModule,
  ],
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss'],
})
export class ProductCardComponent {
  // Angular 19 Signal Inputs
  product = input.required<any>();
  
  // Angular 19 Outputs
  edit = output<string>();
  toggleAvailability = output<string>();
  delete = output<string>();

  // Computed
  priceFormatted = computed(() => {
    const p = this.product();
    return p?.price ? `${p.price.toFixed(2)} MAD` : '0.00 MAD';
  });

  isAvailable = computed(() => this.product()?.isAvailable ?? true);

  onEdit(): void {
    this.edit.emit(this.product().id);
  }

  onToggleAvailability(): void {
    this.toggleAvailability.emit(this.product().id);
  }

  onDelete(): void {
    this.delete.emit(this.product().id);
  }
}
