// src/app/features/promotions/promotion-form/promotion-form.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { PromotionsService } from '../services/promotions.service';

@Component({
  selector: 'app-promotion-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './promotion-form.component.html',
  styleUrls: ['./promotion-form.component.scss'],
})
export class PromotionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private promotionsService = inject(PromotionsService);

  promoForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    type: ['percentage', Validators.required],
    value: [10, [Validators.required, Validators.min(1)]],
    code: [''],
    startDate: [null, Validators.required],
    endDate: [null, Validators.required],
    minOrder: [0],
    maxUses: [null],
    isActive: [true],
    applicableProducts: [[]],
  });

  // Angular 19 Signals
  isEdit = signal(false);
  promoId = signal<string | null>(null);
  loading = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.promoId.set(id);
      this.isEdit.set(true);
      this.loadPromotion();
    }
  }

  loadPromotion(): void {
    const id = this.promoId();
    if (!id) return;
    this.loading.set(true);
    this.promotionsService.getPromotion(id).subscribe({
      next: (promo) => {
        this.promoForm.patchValue(promo);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  generateCode(): void {
    const code = 'PROMO' + Math.random().toString(36).substring(2, 8).toUpperCase();
    this.promoForm.patchValue({ code });
  }

  onSubmit(): void {
    if (this.promoForm.invalid) return;
    this.loading.set(true);
    
    const data = this.promoForm.value;
    const operation = this.isEdit()
      ? this.promotionsService.updatePromotion(this.promoId()!, data)
      : this.promotionsService.createPromotion(data);

    operation.subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/promotions']);
      },
      error: () => this.loading.set(false),
    });
  }
}
