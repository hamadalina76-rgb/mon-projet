// src/app/features/menu/components/product-card/product-card.component.ts - Angular 19
import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { Product } from '../../models/menu.models';

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
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss'],
})
export class ProductCardComponent {
  // Angular 19 Signal Inputs
  product = input.required<Product>();

  // Angular 19 Outputs
  edit = output<number>();
  toggleAvailability = output<number>();
  delete = output<number>();

  // Computed
  priceFormatted = computed(() => {
    const p = this.product();
    return p?.price != null ? `${p.price.toFixed(2)} MAD` : '0.00 MAD';
  });

  isAvailable = computed(() => this.product()?.isAvailable ?? true);

  private readonly placeholderSvg = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200"%3E%3Crect fill="%23f0f0f0" width="300" height="200"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-size="14"%3EImage%3C/text%3E%3C/svg%3E';

  productImageSrc = computed(() => {
    const p = this.product();
    const url = p?.imageUrl || (p as any)?.image;
    return url || this.placeholderSvg;
  });

  onImageError(event: Event): void {
    const el = event.target as HTMLImageElement;
    if (el && el.src !== this.placeholderSvg) {
      el.src = this.placeholderSvg;
    }
  }

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

