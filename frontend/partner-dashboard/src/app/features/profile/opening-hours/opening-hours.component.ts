// src/app/features/profile/opening-hours/opening-hours.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray } from '@angular/forms';
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
  selector: 'app-opening-hours',
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
  templateUrl: './opening-hours.component.html',
  styleUrls: ['./opening-hours.component.scss'],
})
export class OpeningHoursComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  loading = signal(false);
  saving = signal(false);

  hoursForm: FormGroup = this.fb.group({
    days: this.fb.array([]),
  });

  days = [
    { key: 'monday', label: 'Lundi' },
    { key: 'tuesday', label: 'Mardi' },
    { key: 'wednesday', label: 'Mercredi' },
    { key: 'thursday', label: 'Jeudi' },
    { key: 'friday', label: 'Vendredi' },
    { key: 'saturday', label: 'Samedi' },
    { key: 'sunday', label: 'Dimanche' },
  ];

  get daysArray(): FormArray {
    return this.hoursForm.get('days') as FormArray;
  }

  ngOnInit(): void {
    this.initForm();
    this.loadOpeningHours();
  }

  initForm(): void {
    this.days.forEach(day => {
      const dayGroup = this.fb.group({
        day: [day.key],
        isOpen: [true],
        openTime: ['09:00'],
        closeTime: ['22:00'],
        breakStart: [''],
        breakEnd: [''],
      });
      this.daysArray.push(dayGroup);
    });
  }

  loadOpeningHours(): void {
    this.loading.set(true);
    this.profileService.getOpeningHours().subscribe({
      next: (data) => {
        if (data?.length) {
          this.daysArray.clear();
          data.forEach((dayData: any) => {
            const dayGroup = this.fb.group(dayData);
            this.daysArray.push(dayGroup);
          });
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading opening hours:', err);
        this.loading.set(false);
      }
    });
  }

  saveOpeningHours(): void {
    this.saving.set(true);
    this.profileService.updateOpeningHours(this.daysArray.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Horaires mis à jour', 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error saving opening hours:', err);
        this.saving.set(false);
        this.snackBar.open('Erreur lors de la sauvegarde', 'OK', { duration: 3000 });
      }
    });
  }

  copyToAll(index: number): void {
    const source = this.daysArray.at(index).value;
    this.daysArray.controls.forEach((ctrl, i) => {
      if (i !== index) {
        ctrl.patchValue({
          isOpen: source.isOpen,
          openTime: source.openTime,
          closeTime: source.closeTime,
          breakStart: source.breakStart,
          breakEnd: source.breakEnd,
        });
      }
    });
  }
}
