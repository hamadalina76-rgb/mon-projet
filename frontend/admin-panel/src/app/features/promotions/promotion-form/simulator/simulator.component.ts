import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { PromotionsService } from '../../services/promotions.service';
import { PromotionType } from '@core/models/promotion.model';

@Component({
  selector: 'app-simulator',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatIconModule,
    MatButtonModule, MatProgressSpinnerModule, TranslateModule,
  ],
  templateUrl: './simulator.component.html',
  styleUrls: ['./simulator.component.scss'],
})
export class SimulatorComponent {
  @Input() type: PromotionType = 'PERCENTAGE';
  @Input() value: number | null = null;
  @Input() maximumDiscount: number | null = null;
  @Input() minimumOrder: number | null = null;

  private service = inject(PromotionsService);

  orderSubtotal = 40;
  deliveryFee = 5;
  itemCount = 3;

  loading = false;
  result: {
    discountAmount: number;
    newSubtotal: number;
    deliveryFee: number;
    total: number;
    message: string;
  } | null = null;

  simulate(): void {
    this.loading = true;
    this.result = null;

    this.service.simulate({
      type: this.type,
      value: this.value,
      maximumDiscount: this.maximumDiscount,
      minimumOrder: this.minimumOrder,
      orderSubtotal: this.orderSubtotal,
      deliveryFee: this.deliveryFee,
      itemCount: this.itemCount,
    }).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }
}
