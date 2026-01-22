// src/app/features/profile/delivery-zones/delivery-zones.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';

@Component({
  selector: 'app-delivery-zones',
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
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './delivery-zones.component.html',
  styleUrls: ['./delivery-zones.component.scss'],
})
export class DeliveryZonesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  loading = signal(false);
  saving = signal(false);

  zonesForm: FormGroup = this.fb.group({
    zones: this.fb.array([]),
  });

  get zonesArray(): FormArray {
    return this.zonesForm.get('zones') as FormArray;
  }

  // Computed
  zonesCount = computed(() => this.zonesArray.length);

  ngOnInit(): void {
    this.loadZones();
  }

  loadZones(): void {
    this.loading.set(true);
    this.profileService.getDeliveryZones().subscribe({
      next: (data) => {
        if (data?.length) {
          data.forEach((zone: any) => {
            const zoneGroup = this.fb.group({
              name: [zone.name, Validators.required],
              radius: [zone.radius, [Validators.required, Validators.min(0.5)]],
              deliveryFee: [zone.deliveryFee, [Validators.required, Validators.min(0)]],
              minOrder: [zone.minOrder, [Validators.required, Validators.min(0)]],
              freeDeliveryThreshold: [zone.freeDeliveryThreshold || 0],
              isActive: [zone.isActive ?? true],
            });
            this.zonesArray.push(zoneGroup);
          });
        } else {
          this.addZone();
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading zones:', err);
        this.addZone();
        this.loading.set(false);
      }
    });
  }

  addZone(): void {
    const zoneGroup = this.fb.group({
      name: ['', Validators.required],
      radius: [3, [Validators.required, Validators.min(0.5)]],
      deliveryFee: [10, [Validators.required, Validators.min(0)]],
      minOrder: [50, [Validators.required, Validators.min(0)]],
      freeDeliveryThreshold: [0],
      isActive: [true],
    });
    this.zonesArray.push(zoneGroup);
  }

  removeZone(index: number): void {
    this.zonesArray.removeAt(index);
  }

  saveZones(): void {
    if (this.zonesForm.invalid) return;
    
    this.saving.set(true);
    this.profileService.updateDeliveryZones(this.zonesArray.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Zones de livraison mises à jour', 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error saving zones:', err);
        this.saving.set(false);
        this.snackBar.open('Erreur lors de la sauvegarde', 'OK', { duration: 3000 });
      }
    });
  }
}
