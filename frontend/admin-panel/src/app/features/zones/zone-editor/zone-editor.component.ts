// src/app/features/zones/zone-editor/zone-editor.component.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-zone-editor',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        ReactiveFormsModule,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatSlideToggleModule,
        MatProgressSpinnerModule,
        TranslateModule,
    ],
    template: `
    <div class="zone-editor-container">
      <div class="page-header">
        <button mat-icon-button routerLink="/zones">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <h1>{{ isEditMode() ? ('zones.editZone' | translate) : ('zones.addZone' | translate) }}</h1>
      </div>

      @if (loading()) {
        <div class="loading-container">
          <mat-spinner></mat-spinner>
        </div>
      } @else {
        <mat-card>
          <mat-card-content>
            <form [formGroup]="zoneForm" (ngSubmit)="onSubmit()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'zones.name' | translate }}</mat-label>
                <input matInput formControlName="name" />
                @if (zoneForm.get('name')?.hasError('required')) {
                  <mat-error>{{ 'validation.required' | translate }}</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'zones.city' | translate }}</mat-label>
                <input matInput formControlName="city" />
                @if (zoneForm.get('city')?.hasError('required')) {
                  <mat-error>{{ 'validation.required' | translate }}</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'zones.description' | translate }}</mat-label>
                <textarea matInput formControlName="description" rows="4"></textarea>
              </mat-form-field>

              <div class="fee-row">
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'zones.deliveryFee' | translate }}</mat-label>
                  <input matInput type="number" formControlName="deliveryFee" min="0" step="0.01" />
                  <span matSuffix>TND</span>
                  @if (zoneForm.get('deliveryFee')?.hasError('min')) {
                    <mat-error>{{ 'zones.editor.validation.feeMin' | translate }}</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ 'zones.serviceFee' | translate }}</mat-label>
                  <input matInput type="number" formControlName="serviceFee" min="0" step="0.01" />
                  <span matSuffix>TND</span>
                  @if (zoneForm.get('serviceFee')?.hasError('min')) {
                    <mat-error>{{ 'zones.editor.validation.serviceFeeMin' | translate }}</mat-error>
                  }
                </mat-form-field>
              </div>

              <mat-slide-toggle formControlName="isActive" class="toggle-field">
                {{ 'zones.activeStatus' | translate }}
              </mat-slide-toggle>

              <div class="form-actions">
                <button mat-button type="button" routerLink="/zones">
                  {{ 'common.cancel' | translate }}
                </button>
                <button mat-raised-button color="primary" type="submit" [disabled]="zoneForm.invalid || saving()">
                  @if (saving()) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    {{ 'common.save' | translate }}
                  }
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
    styles: [`
    .zone-editor-container {
      padding: 1.5rem;
      max-width: 800px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1.5rem;

      h1 {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 600;
      }
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 3rem;
    }

    .full-width {
      width: 100%;
      margin-bottom: 1rem;
    }

    .toggle-field {
      display: block;
      margin-bottom: 1.5rem;
    }

    .fee-row {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;

      mat-form-field {
        flex: 1;
      }
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      margin-top: 1rem;
    }
  `],
})
export class ZoneEditorComponent implements OnInit {
    private fb = inject(FormBuilder);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private toastr = inject(ToastrService);

    loading = signal(false);
    saving = signal(false);
    isEditMode = signal(false);

    zoneForm: FormGroup = this.fb.group({
        name: ['', Validators.required],
        city: ['', Validators.required],
        description: [''],
        deliveryFee: [0, [Validators.min(0)]],
        serviceFee: [0, [Validators.min(0)]],
        isActive: [true],
    });

    ngOnInit(): void {
        const zoneId = this.route.snapshot.paramMap.get('id');
        if (zoneId) {
            this.isEditMode.set(true);
            this.loadZone(zoneId);
        }
    }

    private loadZone(id: string): void {
        this.loading.set(true);
        // Simulated API call - replace with actual service
        setTimeout(() => {
            this.zoneForm.patchValue({
                name: 'Zone Centre',
                city: 'Tunis',
                description: 'Zone centrale de Tunis',
                isActive: true,
            });
            this.loading.set(false);
        }, 500);
    }

    onSubmit(): void {
        if (this.zoneForm.valid) {
            this.saving.set(true);
            // Simulated API call
            setTimeout(() => {
                this.saving.set(false);
                this.toastr.success(
                    this.isEditMode() ? 'Zone mise à jour avec succès' : 'Zone créée avec succès'
                );
                this.router.navigate(['/zones']);
            }, 1000);
        }
    }
}
