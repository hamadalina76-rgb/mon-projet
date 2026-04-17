// src/app/features/profile/business-info/business-info.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { environment } from '@environments/environment';
import { ProfileService } from '../services/profile.service';
import { PartnerService } from '@core/services/partner.service';
import { AuthService } from '@core/services/auth.service';
import { MapLocationSelectorComponent, LocationData } from '@shared/components/map-location-selector/map-location-selector.component';

const NOMINATIM_REVERSE_URL = 'https://nominatim.openstreetmap.org/reverse';

@Component({
  selector: 'app-business-info',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule,
    TranslateModule,
    MapLocationSelectorComponent,
  ],
  templateUrl: './business-info.component.html',
  styleUrls: ['./business-info.component.scss'],
})
export class BusinessInfoComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private partnerService = inject(PartnerService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  loading = signal(false);
  saving = signal(false);
  logoPreview = signal<string | null>(null);
  coverPreview = signal<string | null>(null);
  partnerId = signal<number | null>(null);
  /** Position initiale depuis le backend pour afficher la carte au bon endroit */
  initialMapLat = signal<number | null>(null);
  initialMapLng = signal<number | null>(null);

  private selectedLogoFile: File | null = null;
  private selectedCoverFile: File | null = null;

  private meaningfulTextValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const raw = control.value;
      if (raw == null) return null;

      const value = String(raw).trim();
      if (!value) return null;

      // Require at least one unicode letter to avoid numeric-only or meaningless symbols.
      const hasLetter = /\p{L}/u.test(value);
      return hasLetter ? null : { meaningfulText: true };
    };
  }

  private minTrimmedLengthValidator(min: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const raw = control.value;
      if (raw == null) return null;

      const value = String(raw).trim();
      if (!value) return null;

      return value.length >= min
        ? null
        : { minTrimmedLength: { requiredLength: min, actualLength: value.length } };
    };
  }

  /**
   * Champs bancaires optionnels : vide = OK. Si saisi, appliquer min + lettre.
   */
  private optionalTextMinMeaningful(min: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const raw = control.value;
      if (raw == null || String(raw).trim() === '') return null;
      const minErr = this.minTrimmedLengthValidator(min)(control);
      if (minErr) return minErr;
      return this.meaningfulTextValidator()(control);
    };
  }

  private ibanValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const raw = control.value;
      if (raw == null) return null;

      const value = String(raw).toUpperCase().replace(/\s+/g, '');
      if (!value) return null;

      // IBAN optionnel : ne pas bloquer tout le profil sur une saisie partielle / test
      if (value.length < 15) return null;

      // Generic IBAN structure: 2 letters + 2 digits + BBAN
      if (!/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(value)) {
        return { ibanInvalid: true };
      }

      const rearranged = `${value.substring(4)}${value.substring(0, 4)}`;
      let numeric = '';

      for (const ch of rearranged) {
        const code = ch.charCodeAt(0);
        if (code >= 65 && code <= 90) {
          numeric += String(code - 55);
        } else {
          numeric += ch;
        }
      }

      let mod = 0;
      for (const ch of numeric) {
        mod = (mod * 10 + Number(ch)) % 97;
      }

      return mod === 1 ? null : { ibanInvalid: true };
    };
  }

  businessForm: FormGroup = this.fb.group({
    businessName: ['', [Validators.required, this.meaningfulTextValidator()]],
    brandName: ['', [this.meaningfulTextValidator()]],
    shortDescription: ['', [Validators.maxLength(100), this.meaningfulTextValidator()]],
    description: ['', Validators.maxLength(500)],
    type: ['', Validators.required],
    address: [''],
    city: [''],
    postalCode: [''],
    state: [''],
    country: ['Tunisie'],
    latitude: [null as number | null],
    longitude: [null as number | null],
    phoneNumber: [''],
    email: ['', [Validators.email]],
    preparationTime: [30],
    minimumOrder: [0],
    acceptOnlinePayment: [true],
    acceptCashPayment: [true],
    tags: [''],
    legalStatus: [''],
    tva: [''],
    legalRepFirstName: [''],
    legalRepLastName: [''],
    position: [''],
    accountHolderName: ['', [this.optionalTextMinMeaningful(2)]],
    iban: ['', [this.ibanValidator()]],
    bankName: ['', [this.optionalTextMinMeaningful(2)]],
    currency: ['TND'],
  });

  partnerTypes = [
    { value: 'RESTAURANT', key: 'restaurant' },
    { value: 'FAST_FOOD', key: 'fastFood' },
    { value: 'CAFE', key: 'cafe' },
    { value: 'BAKERY', key: 'bakery' },
    { value: 'GROCERY', key: 'grocery' },
    { value: 'PHARMACY', key: 'pharmacy' },
    { value: 'FLORIST', key: 'florist' },
    { value: 'OTHER', key: 'other' },
  ];

  ngOnInit(): void {
    this.loadBusinessInfo();
  }

  loadBusinessInfo(): void {
    this.loading.set(true);
    this.profileService.getCurrentPartner().subscribe({
      next: (partner) => {
        this.partnerId.set(partner.id);
        const lat = partner.latitude != null ? Number(partner.latitude) : null;
        const lng = partner.longitude != null ? Number(partner.longitude) : null;
        this.initialMapLat.set(lat);
        this.initialMapLng.set(lng);
        this.businessForm.patchValue({
          businessName: partner.businessName || '',
          brandName: partner.brandName || '',
          shortDescription: partner.shortDescription || '',
          description: partner.description || '',
          type: partner.type || '',
          address: partner.address || '',
          city: partner.city || '',
          postalCode: partner.postalCode || '',
          state: partner.state || '',
          country: partner.country || 'Tunisie',
          latitude: lat,
          longitude: lng,
          phoneNumber: partner.phoneNumber || '',
          email: partner.email || '',
          preparationTime: partner.preparationTime ?? 30,
          minimumOrder: partner.minimumOrder ?? 0,
          acceptOnlinePayment: partner.acceptOnlinePayment ?? true,
          acceptCashPayment: partner.acceptCashPayment ?? true,
          tags: Array.isArray(partner.tags) ? partner.tags.join(', ') : (partner.tags || ''),
          legalStatus: (partner.legalStatus || '').toLowerCase() || '',
          tva: partner.tva || '',
          legalRepFirstName: partner.legalRepFirstName || '',
          legalRepLastName: partner.legalRepLastName || '',
          position: partner.position || '',
          accountHolderName: partner.accountHolderName || '',
          iban: partner.iban || '',
          bankName: partner.bankName || '',
          currency: partner.currency || 'TND',
        });

        if (partner.logo) this.logoPreview.set(partner.logo);
        if (partner.coverImage) this.coverPreview.set(partner.coverImage);

        this.loading.set(false);
        if (
          lat != null &&
          lng != null &&
          !(partner.address || '').trim() &&
          !(partner.city || '').trim() &&
          environment.mapboxToken
        ) {
          this.resolveAddressFromCoords(lat, lng);
        }
      },
      error: (err) => {
        console.error('Error loading business info:', err);
        this.loading.set(false);
        this.snackBar.open(this.translate.instant('profilePages.loadError'), this.translate.instant('profilePages.close'), { duration: 3000 });
      }
    });
  }

  onLogoSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.snackBar.open(this.translate.instant('auth.register.documents.formats'), this.translate.instant('profilePages.close'), { duration: 3000 });
      return;
    }
    const max = environment.uploadMaxSize ?? 5 * 1024 * 1024;
    if (file.size > max) {
      this.snackBar.open(`L'image ne doit pas dépasser ${Math.round(max / 1024 / 1024)} Mo`, 'Fermer', { duration: 3000 });
      return;
    }
    this.selectedLogoFile = file;
    const reader = new FileReader();
    reader.onload = () => this.logoPreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  onCoverSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.snackBar.open(this.translate.instant('auth.register.documents.formats'), this.translate.instant('profilePages.close'), { duration: 3000 });
      return;
    }
    const max = environment.uploadMaxSize ?? 5 * 1024 * 1024;
    if (file.size > max) {
      this.snackBar.open(`L'image ne doit pas dépasser ${Math.round(max / 1024 / 1024)} Mo`, 'Fermer', { duration: 3000 });
      return;
    }
    this.selectedCoverFile = file;
    const reader = new FileReader();
    reader.onload = () => this.coverPreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  onLocationSelected(data: LocationData): void {
    this.businessForm.patchValue({
      address: data.address || '',
      city: data.city || '',
      postalCode: data.postalCode || '',
      state: data.state || '',
      country: data.country || this.businessForm.get('country')?.value || 'Tunisie',
      latitude: data.latitude,
      longitude: data.longitude,
    });
  }

  /** Adresse complète affichée (rue, code postal, ville, pays) – jamais les coordonnées. */
  get fullAddressLine(): string {
    const v = this.businessForm.value;
    const parts = [v.address, [v.postalCode, v.city].filter(Boolean).join(' '), v.state, v.country].filter(Boolean);
    return parts.join(', ') || '';
  }

  /** Récupère l'adresse lisible depuis les coordonnées (Nominatim/OSM) et remplit le formulaire. */
  private resolveAddressFromCoords(lat: number, lng: number): void {
    fetch(
      `${NOMINATIM_REVERSE_URL}?format=jsonv2&lat=${lat}&lon=${lng}&accept-language=fr&addressdetails=1&zoom=18`
    )
      .then((res) => res.json())
      .then((data: any) => {
        if (!data || data.error) return;
        const addr = data.address || {};
        const houseNumber = addr.house_number || '';
        const road = addr.road || addr.pedestrian || addr.footway || '';
        const streetAddress = [houseNumber, road].filter(Boolean).join(' ')
          || addr.amenity || addr.building || '';
        const neighborhood = addr.neighbourhood || addr.suburb || addr.quarter || '';
        const city = addr.city || addr.town || addr.village || addr.municipality || '';
        const state = addr.state || addr.governorate || '';
        const postalCode = addr.postcode || '';
        const country = addr.country || this.businessForm.get('country')?.value || 'Tunisie';

        const addrParts = [streetAddress, neighborhood].filter(Boolean);
        this.businessForm.patchValue({
          address: addrParts.join(', ') || undefined,
          city: city || undefined,
          postalCode: postalCode || undefined,
          state: state || undefined,
          country,
        });
      })
      .catch(() => {});
  }

  saveBusinessInfo(): void {
    if (this.saving() || this.loading()) return;

    if (this.businessForm.invalid) {
      this.businessForm.markAllAsTouched();
      this.snackBar.open(
        this.translate.instant('profilePages.formInvalidHint'),
        this.translate.instant('profilePages.close'),
        { duration: 6000 }
      );
      return;
    }
    const formVal = this.businessForm.value;
    const hasAddress = !!(formVal.address && formVal.city);
    const hasCoords = formVal.latitude != null && formVal.longitude != null;
    if (!hasAddress && !hasCoords) {
      this.snackBar.open(this.translate.instant('auth.register.step2.selectLocation'), this.translate.instant('profilePages.close'), { duration: 4000 });
      return;
    }

    const id = this.partnerId();
    if (!id) {
      this.snackBar.open(this.translate.instant('profilePages.partnerNotFound'), this.translate.instant('profilePages.close'), { duration: 3000 });
      return;
    }

    this.saving.set(true);

    const payload = {
      businessName: formVal.businessName,
      brandName: formVal.brandName || undefined,
      shortDescription: formVal.shortDescription || undefined,
      fullDescription: formVal.description || undefined,
      partnerType: formVal.type,
      phoneNumber: formVal.phoneNumber || undefined,
      email: formVal.email || undefined,
      address: formVal.address || undefined,
      city: formVal.city || undefined,
      postalCode: formVal.postalCode || undefined,
      state: formVal.state || undefined,
      country: formVal.country,
      latitude: formVal.latitude != null ? Number(formVal.latitude) : undefined,
      longitude: formVal.longitude != null ? Number(formVal.longitude) : undefined,
      preparationTime: formVal.preparationTime,
      minimumOrder: formVal.minimumOrder,
      acceptOnlinePayment: formVal.acceptOnlinePayment,
      acceptCashPayment: formVal.acceptCashPayment,
      tags: formVal.tags || undefined,
      legalStatus: formVal.legalStatus || undefined,
      tva: formVal.tva || undefined,
      legalRepFirstName: formVal.legalRepFirstName || undefined,
      legalRepLastName: formVal.legalRepLastName || undefined,
      position: formVal.position || undefined,
      accountHolderName: formVal.accountHolderName || undefined,
      iban: formVal.iban || undefined,
      bankName: formVal.bankName || undefined,
      currency: formVal.currency || undefined,
    };

    this.partnerService.completeProfile(id, payload).subscribe({
      next: () => {
        this.saving.set(false);
        // Upload images if new files selected
        if (this.selectedLogoFile || this.selectedCoverFile) {
          this.uploadImages(id);
        } else {
          this.snackBar.open(this.translate.instant('profilePages.profileUpdated'), 'OK', { duration: 3000 });
        }
      },
      error: (err) => {
        console.error('Error saving business info:', err);
        this.saving.set(false);
        const msg =
          err?.error?.message ||
          err?.error?.error ||
          (typeof err?.error === 'string' ? err.error : null) ||
          err?.message;
        this.snackBar.open(
          msg ? `${this.translate.instant('profilePages.saveError')}: ${msg}` : this.translate.instant('profilePages.saveError'),
          this.translate.instant('profilePages.close'),
          { duration: 5000 }
        );
      },
    });
  }

  private uploadImages(id: number): void {
    const formData = new FormData();
    if (this.selectedLogoFile) formData.append('logo', this.selectedLogoFile);
    if (this.selectedCoverFile) formData.append('cover', this.selectedCoverFile);

    this.partnerService.uploadImages(id, formData).subscribe({
      next: () => {
        this.selectedLogoFile = null;
        this.selectedCoverFile = null;
        this.snackBar.open(this.translate.instant('profilePages.profileUpdated'), 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.warn('Image upload failed:', err);
        const msg = err?.error?.message || err?.error?.error || err?.message;
        this.snackBar.open(msg ? `Upload échoué : ${msg}` : 'Données sauvegardées, erreur upload images', 'Fermer', { duration: 4000 });
      },
    });
  }
}
