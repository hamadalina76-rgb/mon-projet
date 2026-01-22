// src/app/features/zones/pricing-config/pricing-config.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslateModule } from '@ngx-translate/core';
import { ZonesService } from '../services/zones.service';

@Component({
  selector: 'app-pricing-config',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    TranslateModule,
  ],
  templateUrl: './pricing-config.component.html',
  styleUrls: ['./pricing-config.component.scss'],
})
export class PricingConfigComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private zonesService = inject(ZonesService);

  pricingForm: FormGroup = this.fb.group({
    basePrice: [0],
    pricePerKm: [0],
    minimumCharge: [0],
    surgeMultiplier: [1],
    surgeEnabled: [false],
  });

  zoneId: string | null = null;
  loading = false;

  ngOnInit(): void {
    this.zoneId = this.route.snapshot.paramMap.get('id');
    if (this.zoneId) {
      this.loadPricing();
    }
  }

  loadPricing(): void {
    // TODO: Implement
  }

  savePricing(): void {
    // TODO: Implement
  }
}
