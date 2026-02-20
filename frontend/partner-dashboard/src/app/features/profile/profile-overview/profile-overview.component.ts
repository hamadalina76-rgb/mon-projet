// Profile Overview - Affichage complet du profil partenaire (données backend)
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';
import { PartnerProfileDto } from '@core/models/partner.model';
import { environment } from '@environments/environment';

interface OpeningHoursDay {
  day: string;
  isClosed: boolean;
  slots: { open: string; close: string }[];
}

@Component({
  selector: 'app-profile-overview',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './profile-overview.component.html',
  styleUrls: ['./profile-overview.component.scss'],
})
export class ProfileOverviewComponent implements OnInit {
  private profileService = inject(ProfileService);
  private translate = inject(TranslateService);

  loading = signal(true);
  error = signal<string | null>(null);
  partner = signal<PartnerProfileDto | null>(null);
  searchQuery = '';

  /** Horaires parsés depuis openingHoursDisplay (JSON) */
  openingHoursParsed = computed(() => {
    const p = this.partner();
    const raw = p?.openingHoursDisplay;
    if (!raw) return [];
    try {
      const arr = JSON.parse(raw) as OpeningHoursDay[];
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  });

  /** URLs des photos (photosJson est un JSON array de strings) */
  photoUrls = computed(() => {
    const p = this.partner();
    const raw = p?.photosJson;
    if (!raw) return [];
    try {
      const arr = JSON.parse(raw) as string[];
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  });

  /** Tags en liste */
  tagsList = computed(() => {
    const p = this.partner();
    if (p?.tags?.length) return p.tags;
    const raw = (p as any)?.tags;
    if (typeof raw === 'string') return raw ? raw.split(',').map((t: string) => t.trim()) : [];
    return [];
  });

  /** URL de la carte statique (Mapbox) pour afficher l’adresse */
  staticMapUrl = computed(() => {
    const p = this.partner();
    const lat = p?.latitude;
    const lng = p?.longitude;
    const token = environment.mapboxToken;
    if (lat == null || lng == null || !token) return null;
    const base = 'https://api.mapbox.com/styles/v1/mapbox/streets-v12/static';
    const pin = `pin-l+ef4444(${lng},${lat})`;
    const center = `${lng},${lat},14,0,0`;
    return `${base}/${pin}/${center}/400x200@2x?access_token=${encodeURIComponent(token)}`;
  });

  ngOnInit(): void {
    this.profileService.getCurrentPartner().subscribe({
      next: (data) => {
        this.partner.set(data);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        this.error.set(err?.error?.error || err?.message || this.translate.instant('profilePages.loadError'));
        this.loading.set(false);
      },
    });
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return '—';
    const keyMap: Record<string, string> = {
      PENDING: 'profilePages.statusPending',
      ACTIVE: 'profilePages.statusActive',
      INACTIVE: 'profilePages.statusInactive',
      SUSPENDED: 'profilePages.statusSuspended',
      REJECTED: 'profilePages.statusRejected',
    };
    const key = keyMap[status];
    return key ? this.translate.instant(key) : status;
  }

  getDayLabel(dayKeyOrLabel: string): string {
    const keyMap: Record<string, string> = {
      monday: 'auth.register.days.monday',
      tuesday: 'auth.register.days.tuesday',
      wednesday: 'auth.register.days.wednesday',
      thursday: 'auth.register.days.thursday',
      friday: 'auth.register.days.friday',
      saturday: 'auth.register.days.saturday',
      sunday: 'auth.register.days.sunday',
      Lundi: 'auth.register.days.monday',
      Mardi: 'auth.register.days.tuesday',
      Mercredi: 'auth.register.days.wednesday',
      Jeudi: 'auth.register.days.thursday',
      Vendredi: 'auth.register.days.friday',
      Samedi: 'auth.register.days.saturday',
      Dimanche: 'auth.register.days.sunday',
      Monday: 'auth.register.days.monday',
      Tuesday: 'auth.register.days.tuesday',
      Wednesday: 'auth.register.days.wednesday',
      Thursday: 'auth.register.days.thursday',
      Friday: 'auth.register.days.friday',
      Saturday: 'auth.register.days.saturday',
      Sunday: 'auth.register.days.sunday',
    };
    const key = keyMap[dayKeyOrLabel];
    return key ? this.translate.instant(key) : dayKeyOrLabel;
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return '';
    return 'status-' + status.toLowerCase();
  }

  hasValue(value: any): boolean {
    return value !== undefined && value !== null && value !== '';
  }
}
