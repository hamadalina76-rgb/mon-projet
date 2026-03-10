// src/app/features/menu/components/product-card/product-card.component.ts - Angular 19
import { Component, input, output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
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
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
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
  /** 'grid' = carte classique, 'list' = ligne compacte */
  layout = input<'grid' | 'list'>('grid');

  // Angular 19 Outputs
  edit = output<number>();
  toggleAvailability = output<number>();
  duplicate = output<number>();
  delete = output<number>();
  /** Édition inline : nouveau nom (productId, name). */
  nameChange = output<{ productId: number; name: string }>();

  // Computed
  /** Prix affiché : si promo avec réduction, prix actuel (déjà réduit côté backend). */
  priceFormatted = computed(() => {
    const p = this.product();
    const price = p?.price != null ? p.price : 0;
    return `${Number(price).toFixed(2)} DT`;
  });

  /** Prix original (barré) quand une réduction est active. */
  originalPriceFormatted = computed(() => {
    const p = this.product();
    const orig = p?.originalPrice ?? p?.price;
    if (orig == null) return null;
    return `${Number(orig).toFixed(2)} DT`;
  });

  /** True si on affiche le prix barré + prix promo. */
  hasDiscountPrice = computed(() => {
    const p = this.product();
    return (p?.discountPercentage != null && Number(p.discountPercentage) > 0) && (p?.originalPrice != null || p?.price != null);
  });

  isAvailable = computed(() => this.product()?.isAvailable ?? true);

  private readonly placeholderSvg = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200"%3E%3Crect fill="%23f0f0f0" width="300" height="200"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-size="14"%3EImage%3C/text%3E%3C/svg%3E';

  productImageSrc = computed(() => {
    const p = this.product();
    const url = p?.imageUrl || (p as any)?.image;
    return url || this.placeholderSvg;
  });

  /** True when promotionLabel is set and (no end date or end date >= today). */
  promotionBadgeVisible = computed(() => {
    const p = this.product();
    const label = p?.promotionLabel?.trim();
    if (!label) return false;
    const end = p?.promotionEndDate;
    if (!end) return true;
    try {
      return new Date(end) >= new Date(new Date().toISOString().slice(0, 10));
    } catch {
      return true;
    }
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

  onDuplicate(): void {
    this.duplicate.emit(this.product().id);
  }

  onDelete(): void {
    this.delete.emit(this.product().id);
  }

  // Édition inline du nom
  editingName = signal(false);
  inlineNameValue = '';

  startEditName(): void {
    this.inlineNameValue = this.product().name ?? '';
    this.editingName.set(true);
  }

  saveInlineName(): void {
    const trimmed = this.inlineNameValue?.trim();
    this.editingName.set(false);
    if (trimmed && trimmed !== this.product().name) {
      this.nameChange.emit({ productId: this.product().id, name: trimmed });
    }
  }

  cancelInlineName(): void {
    this.editingName.set(false);
  }
}

