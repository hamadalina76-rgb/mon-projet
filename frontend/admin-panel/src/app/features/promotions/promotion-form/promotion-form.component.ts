// src/app/features/promotions/promotion-form/promotion-form.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-promotion-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'promotions.create' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="promotionForm" (ngSubmit)="onSubmit()">
          <mat-form-field>
            <mat-label>Promotion Name</mat-label>
            <input matInput formControlName="name">
          </mat-form-field>
          <mat-form-field>
            <mat-label>Discount (%)</mat-label>
            <input matInput type="number" formControlName="discount">
          </mat-form-field>
          <button mat-raised-button color="primary" type="submit">
            Create Promotion
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      max-width: 600px;
    }
  `]
})
export class PromotionFormComponent implements OnInit {
  promotionForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.promotionForm = this.fb.group({
      name: [''],
      discount: [0],
    });
  }

  ngOnInit(): void {}

  onSubmit(): void {
    console.log('Creating promotion:', this.promotionForm.value);
  }
}
