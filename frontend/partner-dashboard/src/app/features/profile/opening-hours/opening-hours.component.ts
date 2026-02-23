// src/app/features/profile/opening-hours/opening-hours.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';
import { PartnerService } from '@core/services/partner.service';
import { AuthService } from '@core/services/auth.service';

/** Format backend (complete-profile, openingHoursDisplay) */
interface OpeningHoursBackend {
  day: string;
  isClosed: boolean;
  slots: { open: string; close: string }[];
}

@Component({
  selector: 'app-opening-hours',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './opening-hours.component.html',
  styleUrls: ['./opening-hours.component.scss'],
})
export class OpeningHoursComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private partnerService = inject(PartnerService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  loading = signal(false);
  saving = signal(false);

  hoursForm: FormGroup = this.fb.group({
    days: this.fb.array([]),
  });

  days = [
    { key: 'monday' },
    { key: 'tuesday' },
    { key: 'wednesday' },
    { key: 'thursday' },
    { key: 'friday' },
    { key: 'saturday' },
    { key: 'sunday' },
  ];

  get daysArray(): FormArray {
    return this.hoursForm.get('days') as FormArray;
  }

  getSlotsArray(dayIndex: number): FormArray {
    return this.daysArray.at(dayIndex).get('slots') as FormArray;
  }

  ngOnInit(): void {
    this.initForm();
    this.loadOpeningHours();
  }

  initForm(): void {
    this.daysArray.clear();
    this.days.forEach(day => {
      const dayGroup = this.fb.group({
        day: [day.key],
        isOpen: [true],
        slots: this.fb.array([
          this.fb.group({ openTime: ['09:00'], closeTime: ['22:00'] }),
        ]),
      });
      this.daysArray.push(dayGroup);
    });
  }

  loadOpeningHours(): void {
    this.loading.set(true);
    this.profileService.getCurrentPartner().subscribe({
      next: (partner) => {
        const raw = partner?.openingHoursDisplay;
        if (raw) {
          try {
            const backendHours = JSON.parse(raw) as OpeningHoursBackend[];
            if (Array.isArray(backendHours) && backendHours.length > 0) {
              this.days.forEach((day, i) => {
                const backend = backendHours[i] ?? this.backendDefault(day.key);
                const slotsArray = this.getSlotsArray(i);
                slotsArray.clear();
                const slots = backend.slots?.length
                  ? backend.slots
                  : [{ open: '09:00', close: '22:00' }];
                slots.forEach(slot => {
                  slotsArray.push(
                    this.fb.group({
                      openTime: [slot.open || '09:00'],
                      closeTime: [slot.close || '22:00'],
                    })
                  );
                });
                this.daysArray.at(i).patchValue({
                  day: day.key,
                  isOpen: !backend.isClosed,
                });
              });
              this.loading.set(false);
              return;
            }
          } catch {
            // JSON invalide, garder les valeurs par défaut de initForm
          }
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading partner:', err);
        this.loading.set(false);
      },
    });
  }

  private backendDefault(dayKey: string): OpeningHoursBackend {
    return {
      day: dayKey,
      isClosed: false,
      slots: [{ open: '09:00', close: '22:00' }],
    };
  }

  saveOpeningHours(): void {
    const partnerId = this.authService.getPartnerId();
    if (!partnerId) {
      this.snackBar.open(this.translate.instant('profilePages.partnerNotFound'), 'OK', { duration: 3000 });
      return;
    }

    const backendHours: OpeningHoursBackend[] = this.daysArray.controls.map((ctrl, i) => {
      const v = ctrl.value;
      return {
        day: this.days[i].key,
        isClosed: !v.isOpen,
        slots: v.isOpen
          ? (v.slots || []).map((s: { openTime: string; closeTime: string }) => ({
              open: s.openTime || '09:00',
              close: s.closeTime || '22:00',
            }))
          : [],
      };
    });

    this.saving.set(true);
    this.partnerService.completeProfile(partnerId, {
      openingHoursJson: JSON.stringify(backendHours),
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(this.translate.instant('profilePages.hoursUpdated'), 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error saving opening hours:', err);
        this.saving.set(false);
        this.snackBar.open(this.translate.instant('profilePages.saveError'), 'OK', { duration: 3000 });
      },
    });
  }

  addSlot(dayIndex: number): void {
    const slotsArray = this.getSlotsArray(dayIndex);
    slotsArray.push(this.fb.group({ openTime: ['09:00'], closeTime: ['22:00'] }));
  }

  removeSlot(dayIndex: number, slotIndex: number): void {
    const slotsArray = this.getSlotsArray(dayIndex);
    if (slotsArray.length > 1) {
      slotsArray.removeAt(slotIndex);
    }
  }

  copyToAll(index: number): void {
    const source = this.daysArray.at(index).value;
    const sourceSlots = source.slots || [{ openTime: '09:00', closeTime: '22:00' }];
    this.daysArray.controls.forEach((ctrl, i) => {
      if (i === index) return;
      const slotsArray = this.getSlotsArray(i);
      slotsArray.clear();
      sourceSlots.forEach((s: { openTime: string; closeTime: string }) => {
        slotsArray.push(
          this.fb.group({ openTime: [s.openTime], closeTime: [s.closeTime] })
        );
      });
      ctrl.patchValue({
        isOpen: source.isOpen,
      });
    });
  }
}
