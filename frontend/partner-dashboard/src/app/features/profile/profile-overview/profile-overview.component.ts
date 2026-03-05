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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProfileService } from '../services/profile.service';
import { PartnerService } from '@core/services/partner.service';
import { AuthService } from '@core/services/auth.service';
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
    MatSlideToggleModule,
    MatSnackBarModule,
    TranslateModule,
  ],
  templateUrl: './profile-overview.component.html',
  styleUrls: ['./profile-overview.component.scss'],
})
export class ProfileOverviewComponent implements OnInit {
  private profileService = inject(ProfileService);
  private partnerService = inject(PartnerService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  loading = signal(true);
  error = signal<string | null>(null);
  partner = signal<PartnerProfileDto | null>(null);
  searchQuery = '';
  /** Chargement du toggle Ouvert/Fermé */
  statusToggleLoading = signal(false);

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
    const base = 'https://api.mapbox.com/styles/v1/mapbox/streets-v11/static';
    const pin = `pin-l+ef4444(${lng},${lat})`;
    const center = `${lng},${lat},14,0,0`;
    return `${base}/${pin}/${center}/400x200@2x?access_token=${encodeURIComponent(token)}`;
  });

  /** Adresse résolue depuis les coordonnées GPS (Mapbox) quand le partenaire n'a pas d'adresse texte */
  resolvedAddress = signal<{ address?: string; city?: string; country?: string } | null>(null);
  addressResolving = signal(false);

  /** Adresse à afficher (lisible) : partenaire ou résolue depuis les coords – jamais les degrés GPS */
  displayAddressText = computed(() => {
    const p = this.partner();
    const resolved = this.resolvedAddress();
    if (this.hasValue(p?.address) || this.hasValue(p?.city)) {
      const parts = [p?.address, [p?.postalCode, p?.city].filter(Boolean).join(' '), [p?.state, p?.country].filter(Boolean).join(', ')].filter(Boolean);
      return parts.join(' — ') || null;
    }
    if (resolved) {
      const parts = [resolved.address, resolved.city, resolved.country].filter(Boolean);
      return parts.join(', ') || null;
    }
    return null;
  });

  ngOnInit(): void {
    this.profileService.getCurrentPartner().subscribe({
      next: (data) => {
        this.partner.set(data);
        this.loading.set(false);
        this.error.set(null);
        this.resolvedAddress.set(null);
        if (
          data?.latitude != null &&
          data?.longitude != null &&
          !this.hasValue(data.address) &&
          !this.hasValue(data.city) &&
          environment.mapboxToken
        ) {
          this.resolveAddressFromCoords(data.latitude, data.longitude);
        }
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

  /** Classe du badge affiché : ACTIF (vert) si ouvert, INACTIF (gris) si fermé (compte ACTIVE). */
  getDisplayStatusClass(): string {
    const p = this.partner();
    if (!p) return '';
    if (p.status === 'ACTIVE' && !this.isCurrentlyOpen()) return 'status-inactive';
    return this.getStatusClass(p.status);
  }

  /** Libellé du badge affiché : Actif si ouvert, Inactif si fermé (compte ACTIVE). */
  getDisplayStatusLabel(): string {
    const p = this.partner();
    if (!p) return '—';
    if (p.status === 'ACTIVE' && !this.isCurrentlyOpen()) return this.translate.instant('profilePages.statusInactive');
    return this.getStatusLabel(p.status);
  }

  /** Établissement actuellement ouvert (accepte les commandes) */
  isCurrentlyOpen(): boolean {
    const p = this.partner();
    return p?.acceptsOrders === true || p?.isCurrentlyOpen === true;
  }

  /** Bascule Ouvert / Fermé – PATCH /partners/{id}/status */
  toggleOpenStatus(): void {
    const p = this.partner();
    const partnerId = this.authService.getPartnerId() ?? p?.id;
    if (!partnerId || !p) return;
    const newState = !this.isCurrentlyOpen();
    this.statusToggleLoading.set(true);
    this.partnerService.updateStatus(partnerId, newState).subscribe({
      next: (updated) => {
        this.partner.set(updated);
        this.statusToggleLoading.set(false);
        const key = newState ? 'profilePages.statusNowOpen' : 'profilePages.statusNowClosed';
        this.snackBar.open(this.translate.instant(key), this.translate.instant('profilePages.close'), { duration: 3000 });
      },
      error: (err) => {
        this.statusToggleLoading.set(false);
        const msg = err?.error?.error || err?.message || this.translate.instant('profilePages.saveError');
        this.snackBar.open(msg, this.translate.instant('profilePages.close'), { duration: 4000 });
      },
    });
  }

  hasValue(value: any): boolean {
    return value !== undefined && value !== null && value !== '';
  }

  /** Récupère l'adresse lisible depuis les coordonnées (Mapbox reverse geocoding) */
  private resolveAddressFromCoords(lat: number, lng: number): void {
    this.addressResolving.set(true);
    this.resolvedAddress.set(null);
    const token = environment.mapboxToken;
    if (!token) {
      this.addressResolving.set(false);
      return;
    }
    fetch(
      `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${token}&language=fr`
    )
      .then((res) => res.json())
      .then((data: { features?: any[] }) => {
        this.addressResolving.set(false);
        if (!data?.features?.length) return;
        const features = data.features;
        const order = ['address', 'poi', 'place', 'locality', 'neighborhood', 'district', 'region', 'country'];
        let place = features[0];
        for (const type of order) {
          const found = features.find((f: any) => f.place_type?.includes(type));
          if (found) {
            place = found;
            break;
          }
        }
        let streetAddress = '';
        let city = '';
        let country = '';
        if (place.place_type?.includes('address')) {
          streetAddress = place.address ? `${place.address} ${place.text}` : place.text || '';
        } else if (place.place_type?.includes('poi')) {
          streetAddress = place.text || '';
        }
        place.context?.forEach((ctx: any) => {
          const id = String(ctx.id || '');
          if (id.startsWith('place')) city = ctx.text;
          else if (id.startsWith('country')) country = ctx.text;
        });
        if (!city && place.place_type?.includes('place')) city = place.text;
        this.resolvedAddress.set({
          address: streetAddress,
          city: city || undefined,
          country: country || undefined,
        });
      })
      .catch(() => this.addressResolving.set(false));
  }
}
