// src/app/features/profile/business-info/business-info.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';

@Component({
  selector: 'app-business-info',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './business-info.component.html',
  styleUrls: ['./business-info.component.scss'],
})
export class BusinessInfoComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  loading = signal(false);
  saving = signal(false);
  logoPreview = signal<string | null>(null);
  coverPreview = signal<string | null>(null);

  businessForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    category: ['', Validators.required],
    cuisine: [[]],
    address: ['', Validators.required],
    city: ['', Validators.required],
    phone: ['', Validators.required],
    email: ['', Validators.email],
  });

  categories = [
    { value: 'restaurant', label: 'Restaurant' },
    { value: 'fast-food', label: 'Fast Food' },
    { value: 'cafe', label: 'Café' },
    { value: 'bakery', label: 'Boulangerie' },
    { value: 'grocery', label: 'Épicerie' },
  ];

  cuisines = ['Marocaine', 'Française', 'Italienne', 'Japonaise', 'Indienne', 'Mexicaine', 'Américaine'];

  ngOnInit(): void {
    this.loadBusinessInfo();
  }

  loadBusinessInfo(): void {
    this.loading.set(true);
    this.profileService.getProfile().subscribe({
      next: (data: any) => {
        this.businessForm.patchValue(data);
        this.logoPreview.set(data.logo);
        this.coverPreview.set(data.cover);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Error loading business info:', err);
        this.loading.set(false);
      }
    });
  }

  onLogoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        this.logoPreview.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  onCoverSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        this.coverPreview.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  saveBusinessInfo(): void {
    if (this.businessForm.invalid) return;
    // TODO: Implement
  }
}
