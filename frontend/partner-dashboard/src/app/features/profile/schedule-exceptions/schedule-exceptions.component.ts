// src/app/features/profile/schedule-exceptions/schedule-exceptions.component.ts
import { Component, OnInit, inject, signal, LOCALE_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';
import { PartnerService } from '@core/services/partner.service';
import { ScheduleException } from '@core/models/partner.model';

@Component({
  selector: 'app-schedule-exceptions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './schedule-exceptions.component.html',
  styleUrls: ['./schedule-exceptions.component.scss'],
})
export class ScheduleExceptionsComponent implements OnInit {
  private profileService = inject(ProfileService);
  private partnerService = inject(PartnerService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private localeId = inject(LOCALE_ID);

  loading = signal(false);
  saving = signal(false);
  partnerId = signal<number | null>(null);
  exceptions = signal<ScheduleException[]>([]);

  newDate = '';
  newLabel = '';

  ngOnInit(): void {
    this.loadExceptions();
  }

  loadExceptions(): void {
    this.loading.set(true);
    this.profileService.getCurrentPartner().subscribe({
      next: (partner) => {
        this.partnerId.set(partner?.id ?? null);
        const raw = partner?.scheduleExceptionsDisplay;
        if (raw) {
          try {
            const list = JSON.parse(raw) as ScheduleException[];
            if (Array.isArray(list)) {
              this.exceptions.set(
                list
                  .filter(e => e?.date)
                  .sort((a, b) => a.date.localeCompare(b.date))
              );
            }
          } catch {
            this.exceptions.set([]);
          }
        } else {
          this.exceptions.set([]);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  addException(): void {
    const date = this.newDate?.trim();
    if (!date) {
      this.snackBar.open(
        this.translate.instant('profilePages.exceptionDateRequired'),
        'OK',
        { duration: 3000 }
      );
      return;
    }
    const existing = this.exceptions().some(e => e.date === date);
    if (existing) {
      this.snackBar.open(
        this.translate.instant('profilePages.exceptionDateDuplicate'),
        'OK',
        { duration: 3000 }
      );
      return;
    }
    const today = new Date().toISOString().slice(0, 10);
    if (date < today) {
      this.snackBar.open(
        this.translate.instant('profilePages.exceptionDatePast'),
        'OK',
        { duration: 3000 }
      );
      return;
    }
    const newException: ScheduleException = {
      date,
      label: this.newLabel?.trim() || undefined,
      type: 'CLOSED',
    };
    this.exceptions.update(list =>
      [...list, newException].sort((a, b) => a.date.localeCompare(b.date))
    );
    this.newDate = '';
    this.newLabel = '';
  }

  removeException(index: number): void {
    this.exceptions.update(list => list.filter((_, i) => i !== index));
  }

  save(): void {
    const id = this.partnerId();
    if (id == null) {
      this.snackBar.open(this.translate.instant('profilePages.partnerNotFound'), 'OK', {
        duration: 3000,
      });
      return;
    }
    this.saving.set(true);
    this.partnerService.updateScheduleExceptions(id, this.exceptions()).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.translate.instant('profilePages.exceptionSaved'),
          'OK',
          { duration: 3000 }
        );
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open(this.translate.instant('profilePages.saveError'), 'OK', {
          duration: 3000,
        });
      },
    });
  }

  formatDateDisplay(dateStr: string): string {
    if (!dateStr || dateStr.length !== 10) return dateStr;
    const d = new Date(dateStr + 'T12:00:00');
    return d.toLocaleDateString(this.localeId, {
      weekday: 'short',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }
}
