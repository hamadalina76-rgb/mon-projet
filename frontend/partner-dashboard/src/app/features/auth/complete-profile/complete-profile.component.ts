// src/app/features/auth/complete-profile/complete-profile.component.ts
// Phase 2: Partner Profile Completion (after login, authenticated)
import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { PartnerService } from '@core/services/partner.service';
import { MapLocationSelectorComponent, LocationData } from '@shared/components/map-location-selector/map-location-selector.component';
import { GenericDialogComponent } from '@shared/components/generic-dialog/generic-dialog.component';
import { ProfileSuccessDialogComponent } from './profile-success-dialog/profile-success-dialog.component';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatStepperModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatCheckboxModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatDialogModule,
    TranslateModule,
    MapLocationSelectorComponent,
  ],
  templateUrl: './complete-profile.component.html',
  styleUrls: ['./complete-profile.component.scss'],
})
export class CompleteProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private partnerService = inject(PartnerService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);

  // Signals
  loading = signal(false);
  errorMessage = signal('');
  translationsReady = signal(false);
  uploadProgress = signal<{[key: string]: number}>({});
  currentYear = new Date().getFullYear();
  partnerId = signal<number | null>(null);

  // Document/Image previews
  uploadedDocuments = signal<{[key: string]: { file?: File | null; preview?: string }}>({});
  uploadedImages = signal<{logo?: {file: File, preview: string}, cover?: {file: File, preview: string}, photos: {file: File, preview: string}[]}>({photos: []});

  // User info from auth
  userName = signal('');

  // Options (loaded from translations)
  partnerTypes: { value: string; label: string }[] = [];
  legalStatuses: { value: string; label: string }[] = [];
  preparationTimes: {label: string, value: number}[] = [];
  days: string[] = [];
  availableTags: string[] = [];
  currencies = ['TND', 'EUR', 'USD'];
  countries: string[] = [];

  // Step 1: Business Information
  step1Form: FormGroup = this.fb.group({
    partnerType: ['', Validators.required],
    otherType: [''],
    businessName: ['', Validators.required],
    brandName: ['', Validators.required],
  });

  // Step 2: Address and Location (rempli uniquement par Mapbox)
  step2Form: FormGroup = this.fb.group({
    fullAddress: ['', Validators.required],
    postalCode: [''],
    city: [''],
    state: [''],
    country: ['', Validators.required],
    latitude: [null as number | null, Validators.required],
    longitude: [null as number | null, Validators.required],
  });

  // Step 3: Legal Information
  step3Form: FormGroup = this.fb.group({
    legalStatus: ['', Validators.required],
    tva: [''],
    legalRepFirstName: ['', Validators.required],
    legalRepLastName: ['', Validators.required],
    position: ['', Validators.required],
  });

  // Step 4: Bank Information
  step4Form: FormGroup = this.fb.group({
    accountHolderName: ['', Validators.required],
    iban: ['', [Validators.required, this.ibanValidator]],
    bankName: ['', Validators.required],
    currency: ['TND', Validators.required],
  });

  // Step 5: Configuration
  step5Form: FormGroup = this.fb.group({
    preparationTime: [30, Validators.required],
    acceptOnlinePayment: [true, Validators.required],
    acceptCashPayment: [false, Validators.required],
    minimumOrder: [10],
    noMinimum: [false],
  });

  // Step 6: Photos and Presentation
  step6Form: FormGroup = this.fb.group({
    shortDescription: ['', [Validators.required, Validators.maxLength(100)]],
    fullDescription: ['', [Validators.required, Validators.maxLength(500)]],
    tags: [[]],
  });

  // Step 7: Validation
  step7Form: FormGroup = this.fb.group({
    acceptTerms: [false, Validators.requiredTrue],
    acceptPrivacy: [false, Validators.requiredTrue],
    acceptCommission: [false, Validators.requiredTrue],
    certifyInformation: [false, Validators.requiredTrue],
    acceptMarketing: [false],
  });

  // Files
  documentsFiles: { [key: string]: File | null } = {
    kbis: null, idCard: null, insurance: null, rib: null,
  };

  // Opening hours
  openingHours: Array<{
    day: string; isClosed: boolean;
    slots: Array<{ open: string; close: string }>;
  }> = [];

  constructor() {
    const savedLang = localStorage.getItem('partnerLang') || 'fr';
    this.updateDirection(savedLang);

    // Load user name
    const user = this.authService.currentUser();
    if (user) {
      this.userName.set(`${user.firstName} ${user.lastName}`);
      // Pre-fill legal rep with user's name
      this.step3Form.patchValue({
        legalRepFirstName: user.firstName,
        legalRepLastName: user.lastName,
      });
    }

    // Load translations
    this.translate.use(savedLang).subscribe(() => {
      this.loadTranslations();
      this.translationsReady.set(true);
    });

    // Watch for partner type changes
    this.step1Form.get('partnerType')?.valueChanges.subscribe(value => {
      const otherTypeControl = this.step1Form.get('otherType');
      if (value === 'other') {
        otherTypeControl?.setValidators([Validators.required]);
      } else {
        otherTypeControl?.clearValidators();
      }
      otherTypeControl?.updateValueAndValidity();
    });

    // Watch for no minimum checkbox
    this.step5Form.get('noMinimum')?.valueChanges.subscribe(value => {
      const minimumControl = this.step5Form.get('minimumOrder');
      if (value) {
        minimumControl?.disable();
        minimumControl?.setValue(0);
      } else {
        minimumControl?.enable();
      }
    });
  }

  ngOnInit(): void {
    // Load partner profile
    const user = this.authService.currentUser();
    if (user?.id) {
      this.loading.set(true);
      this.partnerService.getPartnerByUserId(user.id).subscribe({
        next: (partner) => {
          this.loading.set(false);
          this.partnerId.set(partner.id);
          console.log('Partner loaded:', partner);
          // Load existing partner data into forms if partner exists
          this.loadPartnerDataIntoForms(partner);
        },
        error: (err) => {
          this.loading.set(false);
          console.error('Failed to load partner:', err);
          // If partner doesn't exist yet, that's OK - it's a new registration
          if (err.status !== 404) {
            this.errorMessage.set('Failed to load partner profile');
          }
        }
      });
    }
  }

  /**
   * Load existing partner data into forms for editing
   */
  private loadPartnerDataIntoForms(partner: any): void {
    // Step 1: Business Info
    if (partner.partnerType || partner.type) {
      this.step1Form.patchValue({
        partnerType: partner.partnerType || partner.type,
        businessName: partner.businessName || '',
        brandName: partner.brandName || '',
      });
    }

    // Step 2: Address
    if (partner.address || partner.latitude) {
      this.step2Form.patchValue({
        fullAddress: partner.address || partner.fullAddress || '',
        city: partner.city || '',
        postalCode: partner.postalCode || '',
        state: partner.state || '',
        country: partner.country || this.countries[0],
        latitude: partner.latitude || null,
        longitude: partner.longitude || null,
      });
    }

    // Step 3: Legal Info
    if (partner.legalStatus) {
      this.step3Form.patchValue({
        legalStatus: partner.legalStatus || '',
        tva: partner.tva || partner.vatNumber || '',
        legalRepFirstName: partner.legalRepFirstName || partner.legalRep?.firstName || '',
        legalRepLastName: partner.legalRepLastName || partner.legalRep?.lastName || '',
        position: partner.position || '',
      });
    }

    // Step 4: Bank Info
    if (partner.iban || partner.accountHolderName) {
      this.step4Form.patchValue({
        accountHolderName: partner.accountHolderName || '',
        iban: partner.iban || '',
        bankName: partner.bankName || partner.bank || '',
        currency: partner.currency || this.currencies[0],
      });
    }

    // Step 5: Operational Info
    if (partner.preparationTime || partner.minimumOrder !== undefined) {
      this.step5Form.patchValue({
        preparationTime: partner.preparationTime?.toString() || '',
        acceptOnlinePayment: partner.acceptOnlinePayment || false,
        acceptCashPayment: partner.acceptCashPayment || false,
        minimumOrder: partner.minimumOrder || 0,
        noMinimum: partner.noMinimum || (partner.minimumOrder === 0),
      });
    }

    // Load opening hours if available
    if (partner.openingHours || partner.openingHoursDisplay) {
      try {
        const hours = typeof partner.openingHours === 'string' 
          ? JSON.parse(partner.openingHours) 
          : partner.openingHours || partner.openingHoursDisplay;
        if (Array.isArray(hours) && hours.length > 0) {
          this.openingHours = hours.map((h: any) => ({
            day: h.day || '',
            isClosed: h.isClosed || false,
            slots: h.slots || [{ open: '09:00', close: '18:00' }],
          }));
        }
      } catch (e) {
        console.warn('Failed to parse opening hours:', e);
      }
    }

    // Step 6: Presentation
    if (partner.shortDescription || partner.fullDescription) {
      const tags = partner.tags 
        ? (typeof partner.tags === 'string' ? partner.tags.split(',') : partner.tags)
        : [];
      this.step6Form.patchValue({
        shortDescription: partner.shortDescription || '',
        fullDescription: partner.fullDescription || partner.description || '',
        tags: tags,
      });
    }

    // Load existing images if available
    if (partner.logo) {
      this.uploadedImages.update(img => ({ ...img, logo: { file: null as any, preview: partner.logo } }));
    }
    if (partner.coverImage) {
      this.uploadedImages.update(img => ({ ...img, cover: { file: null as any, preview: partner.coverImage } }));
    }
    if (partner.photosJson) {
      try {
        const photos = typeof partner.photosJson === 'string' ? JSON.parse(partner.photosJson) : partner.photosJson;
        if (Array.isArray(photos)) {
          const photoPreviews = photos.map((url: string) => ({ file: null as any, preview: url }));
          this.uploadedImages.update(img => ({ ...img, photos: photoPreviews }));
        }
      } catch (e) {
        console.warn('Failed to parse photos:', e);
      }
    }

    // Load existing documents if URLs are available
    const docs: { [key: string]: { file?: File | null; preview?: string } } = {};
    if (partner.kbisUrl) {
      docs['kbis'] = { preview: partner.kbisUrl };
    }
    if (partner.idCardUrl) {
      docs['idCard'] = { preview: partner.idCardUrl };
    }
    if (partner.insuranceUrl) {
      docs['insurance'] = { preview: partner.insuranceUrl };
    }
    if (partner.ribUrl) {
      docs['rib'] = { preview: partner.ribUrl };
    }
    if (Object.keys(docs).length > 0) {
      this.uploadedDocuments.set({ ...this.uploadedDocuments(), ...docs });
    }
  }

  // Validators
  ibanValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const iban = control.value.replace(/\s/g, '');
    if (iban.length < 15 || iban.length > 34) {
      return { invalidIban: true };
    }
    return null;
  }

  // File handling
  onFileSelect(event: Event, type: string): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];

    if (type === 'logo') {
      this.createImagePreview(file, 'logo');
    } else if (type === 'cover') {
      this.createImagePreview(file, 'cover');
    } else if (type === 'photos') {
      const currentImages = this.uploadedImages();
      if (currentImages.photos.length < 5) {
        this.createImagePreview(file, 'photos');
      }
    } else {
      this.documentsFiles[type] = file;
      const currentDocs = this.uploadedDocuments();
      this.uploadedDocuments.set({ ...currentDocs, [type]: { file } });
    }
    this.simulateUpload(type);
  }

  createImagePreview(file: File, type: string): void {
    const reader = new FileReader();
    reader.onload = (e: any) => {
      const currentImages = this.uploadedImages();
      if (type === 'logo') {
        this.uploadedImages.set({ ...currentImages, logo: { file, preview: e.target.result } });
      } else if (type === 'cover') {
        this.uploadedImages.set({ ...currentImages, cover: { file, preview: e.target.result } });
      } else if (type === 'photos') {
        const photos = [...currentImages.photos, { file, preview: e.target.result }];
        this.uploadedImages.set({ ...currentImages, photos });
      }
    };
    reader.readAsDataURL(file);
  }

  removeDocument(type: string): void {
    this.documentsFiles[type] = null;
    const currentDocs = this.uploadedDocuments();
    delete currentDocs[type];
    this.uploadedDocuments.set({ ...currentDocs });
  }

  removeImage(type: string, index?: number): void {
    const current = this.uploadedImages();
    if (type === 'logo') {
      delete current.logo;
      this.uploadedImages.set({ ...current });
    } else if (type === 'cover') {
      delete current.cover;
      this.uploadedImages.set({ ...current });
    } else if (type === 'photos' && index !== undefined) {
      current.photos.splice(index, 1);
      this.uploadedImages.set({ ...current });
    }
  }

  viewDocument(type: string): void {
    const doc = this.uploadedDocuments()[type];
    if (!doc) return;

    // If we have a direct URL/preview (existing document from backend)
    if (doc.preview && !doc.file) {
      window.open(doc.preview, '_blank');
      return;
    }

    // If we have a local file with preview
    if (doc.preview && doc.file) {
      const win = window.open('', '_blank');
      if (win) {
        const title = doc.file?.name || 'Document';
        win.document.write(`<html><head><title>${title}</title><style>body{margin:0;display:flex;justify-content:center;align-items:center;background:#000;}img{max-width:100%;max-height:100vh;}</style></head><body><img src="${doc.preview}" alt="${title}"/></body></html>`);
      }
      return;
    }

    // Fallback: open local file blob
    if (doc.file) {
      const url = URL.createObjectURL(doc.file);
      window.open(url, '_blank');
    }
  }

  simulateUpload(type: string): void {
    let progress = 0;
    const interval = setInterval(() => {
      progress += 10;
      this.uploadProgress.update(p => ({ ...p, [type]: progress }));
      if (progress >= 100) {
        clearInterval(interval);
        setTimeout(() => {
          this.uploadProgress.update(p => { const newP = { ...p }; delete newP[type]; return newP; });
        }, 500);
      }
    }, 100);
  }

  // Opening hours
  addTimeSlot(dayIndex: number): void {
    this.openingHours[dayIndex].slots.push({ open: '09:00', close: '18:00' });
  }

  removeTimeSlot(dayIndex: number, slotIndex: number): void {
    if (this.openingHours[dayIndex].slots.length > 1) {
      this.openingHours[dayIndex].slots.splice(slotIndex, 1);
    }
  }

  // Tags
  toggleTag(tag: string): void {
    const currentTags = this.step6Form.get('tags')?.value || [];
    const index = currentTags.indexOf(tag);
    if (index > -1) {
      currentTags.splice(index, 1);
    } else {
      currentTags.push(tag);
    }
    this.step6Form.patchValue({ tags: currentTags });
  }

  isTagSelected(tag: string): boolean {
    return (this.step6Form.get('tags')?.value || []).includes(tag);
  }

  // Load translations
  loadTranslations(): void {
    this.partnerTypes = [
      { value: 'RESTAURANT', label: this.translate.instant('auth.register.partnerTypes.restaurant') },
      { value: 'FAST_FOOD', label: this.translate.instant('auth.register.partnerTypes.fastFood') },
      { value: 'CAFE', label: this.translate.instant('auth.register.partnerTypes.cafe') },
      { value: 'BAKERY', label: this.translate.instant('auth.register.partnerTypes.bakery') },
      { value: 'GROCERY', label: this.translate.instant('auth.register.partnerTypes.grocery') },
      { value: 'PHARMACY', label: this.translate.instant('auth.register.partnerTypes.pharmacy') },
      { value: 'FLORIST', label: this.translate.instant('auth.register.partnerTypes.florist') },
      { value: 'OTHER', label: this.translate.instant('auth.register.partnerTypes.other') },
    ];

    this.legalStatuses = [
      { value: 'auto-entrepreneur', label: this.translate.instant('auth.register.legalStatuses.autoEntrepreneur') },
      { value: 'sarl', label: this.translate.instant('auth.register.legalStatuses.sarl') },
      { value: 'sas', label: this.translate.instant('auth.register.legalStatuses.sas') },
      { value: 'association', label: this.translate.instant('auth.register.legalStatuses.association') },
      { value: 'other', label: this.translate.instant('auth.register.legalStatuses.other') },
    ];

    this.days = [
      this.translate.instant('auth.register.days.monday'),
      this.translate.instant('auth.register.days.tuesday'),
      this.translate.instant('auth.register.days.wednesday'),
      this.translate.instant('auth.register.days.thursday'),
      this.translate.instant('auth.register.days.friday'),
      this.translate.instant('auth.register.days.saturday'),
      this.translate.instant('auth.register.days.sunday'),
    ];

    this.preparationTimes = [
      { label: this.translate.instant('auth.register.preparationTimes.time1'), value: 15 },
      { label: this.translate.instant('auth.register.preparationTimes.time2'), value: 20 },
      { label: this.translate.instant('auth.register.preparationTimes.time3'), value: 30 },
      { label: this.translate.instant('auth.register.preparationTimes.time4'), value: 45 },
      { label: this.translate.instant('auth.register.preparationTimes.time5'), value: 60 },
      { label: this.translate.instant('auth.register.preparationTimes.time6'), value: 90 },
    ];

    this.availableTags = [
      this.translate.instant('auth.register.tags.bio'),
      this.translate.instant('auth.register.tags.vegetarian'),
      this.translate.instant('auth.register.tags.vegan'),
      this.translate.instant('auth.register.tags.halal'),
      this.translate.instant('auth.register.tags.glutenFree'),
      this.translate.instant('auth.register.tags.homemade'),
      this.translate.instant('auth.register.tags.local'),
      this.translate.instant('auth.register.tags.fastDelivery'),
      this.translate.instant('auth.register.tags.new'),
    ];

    this.countries = [this.translate.instant('auth.register.countries.tunisia')];
    this.step2Form.patchValue({
      country: this.countries[0],
      latitude: 36.8065,
      longitude: 10.1815,
      fullAddress: 'Tunis, Tunisie',
    });

    this.openingHours = this.days.map(day => ({
      day, isClosed: false, slots: [{ open: '09:00', close: '18:00' }],
    }));
  }

  updateDirection(lang: string): void {
    const htmlElement = document.documentElement;
    if (lang === 'ar') {
      htmlElement.setAttribute('dir', 'rtl');
      htmlElement.setAttribute('lang', 'ar');
    } else {
      htmlElement.setAttribute('dir', 'ltr');
      htmlElement.setAttribute('lang', lang);
    }
  }

  // Mapping pays Mapbox -> option formulaire (fr)
  private readonly countryMap: Record<string, string> = {
    tunisia: 'Tunisie',
    tunisie: 'Tunisie',
    france: 'France',
    morocco: 'Maroc',
    maroc: 'Maroc',
    algeria: 'Algérie',
    algérie: 'Algérie',
    belgium: 'Belgique',
    belgique: 'Belgique',
  };

  // Handle location selection from map
  onLocationSelected(locationData: LocationData): void {
    const lat = locationData.latitude;
    const lng = locationData.longitude;
    const patchData: any = {
      latitude: lat,
      longitude: lng,
    };

    // Adresse lisible uniquement (jamais les coordonnées en texte)
    patchData.fullAddress = locationData.address || locationData.city || [locationData.neighborhood, locationData.state, locationData.country].filter(Boolean).join(', ') || '';
    patchData.city = locationData.city || locationData.state || '';
    patchData.postalCode = locationData.postalCode || '';
    patchData.state = locationData.state || '';

    // Pays : mapping Mapbox -> option formulaire
    if (locationData.country) {
      const key = locationData.country.toLowerCase().replace(/\s/g, '');
      patchData.country = this.countryMap[key] || this.countries.find(
        c => c.toLowerCase().includes(locationData.country!.toLowerCase()) || locationData.country!.toLowerCase().includes(c.toLowerCase())
      ) || this.countries[0];
    } else {
      patchData.country = this.countries[0];
    }

    this.step2Form.patchValue(patchData);
    this.step2Form.updateValueAndValidity();
  }

  // Save & skip (save progress)
  saveProgress(): void {
    // TODO: Save current progress to backend or localStorage
    const progress = {
      step1: this.step1Form.value,
      step2: this.step2Form.value,
      step3: this.step3Form.value,
      step4: this.step4Form.value,
      step5: this.step5Form.value,
      step6: this.step6Form.value,
    };
    localStorage.setItem('partnerProfileProgress', JSON.stringify(progress));
  }

  // Logout
  logout(): void {
    this.authService.logout();
  }

  // Form submission
  onSubmit(): void {
    if (
      this.step1Form.invalid ||
      this.step2Form.invalid ||
      this.step3Form.invalid ||
      this.step4Form.invalid ||
      this.step5Form.invalid ||
      this.step6Form.invalid ||
      this.step7Form.invalid
    ) {
      this.errorMessage.set('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (!this.partnerId()) {
      this.errorMessage.set('Partner ID not found. Please try logging in again.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    // Compile all profile data
    const profileData = {
      // Step 1: Business info
      partnerType: this.step1Form.value.partnerType,
      businessName: this.step1Form.value.businessName,
      brandName: this.step1Form.value.brandName,

      // Step 2: Address
      address: this.step2Form.value.fullAddress,
      city: this.step2Form.value.city,
      postalCode: this.step2Form.value.postalCode,
      state: this.step2Form.value.state || null,
      country: this.step2Form.value.country,
      latitude: this.step2Form.value.latitude,
      longitude: this.step2Form.value.longitude,

      // Step 3: Legal
      legalStatus: this.step3Form.value.legalStatus,
      tva: this.step3Form.value.tva,
      legalRepFirstName: this.step3Form.value.legalRepFirstName,
      legalRepLastName: this.step3Form.value.legalRepLastName,
      position: this.step3Form.value.position,

      // Step 4: Bank
      accountHolderName: this.step4Form.value.accountHolderName,
      iban: this.step4Form.value.iban,
      bankName: this.step4Form.value.bankName,
      currency: this.step4Form.value.currency,

      // Step 5: Config
      preparationTime: this.step5Form.value.preparationTime,
      acceptOnlinePayment: this.step5Form.value.acceptOnlinePayment,
      acceptCashPayment: this.step5Form.value.acceptCashPayment,
      minimumOrder: this.step5Form.value.minimumOrder,
      noMinimum: this.step5Form.value.noMinimum,
      openingHoursJson: JSON.stringify(this.openingHours),

      // Step 6: Presentation
      shortDescription: this.step6Form.value.shortDescription,
      fullDescription: this.step6Form.value.fullDescription,
      tags: Array.isArray(this.step6Form.value.tags) ? this.step6Form.value.tags.join(',') : '',

      // Step 7: Terms
      acceptTerms: this.step7Form.value.acceptTerms,
      acceptPrivacy: this.step7Form.value.acceptPrivacy,
    };

    console.log('Submitting profile data:', profileData);

    // Step 1: Submit profile JSON data
    this.partnerService.completeProfile(this.partnerId()!, profileData).subscribe({
      next: (response) => {
        console.log('Profile completed successfully:', response);
        // Step 2: Upload files (images + documents) after profile is saved
        this.uploadFiles();
      },
      error: (err) => {
        this.loading.set(false);
        console.error('Failed to complete profile:', err);
        this.errorMessage.set(
          err.error?.message || err.error?.error || 'Failed to complete profile. Please try again.'
        );
      }
    });
  }

  /**
   * Upload images and documents after profile data is saved
   */
  private uploadFiles(): void {
    const partnerId = this.partnerId()!;
    const images = this.uploadedImages();
    const hasImages = images.logo || images.cover || images.photos.length > 0;
    const hasDocuments = Object.values(this.documentsFiles).some(f => f !== null);

    // Collect upload promises
    const uploads: Array<() => void> = [];
    let pendingUploads = 0;

    const checkComplete = () => {
      pendingUploads--;
      if (pendingUploads <= 0) {
        this.loading.set(false);
        // Update user with partnerId so isProfileComplete() returns true
        this.authService.updateStoredUser({ 
          partnerId: this.partnerId()!,
          isProfileComplete: true 
        });
        this.showSuccessDialog();
      }
    };

    // Upload images
    if (hasImages) {
      pendingUploads++;
      const imageFormData = new FormData();
      
      if (images.logo) {
        imageFormData.append('logo', images.logo.file);
      }
      if (images.cover) {
        imageFormData.append('cover', images.cover.file);
      }
      if (images.photos.length > 0) {
        images.photos.forEach(photo => {
          imageFormData.append('photos', photo.file);
        });
      }
      
      this.partnerService.uploadImages(partnerId, imageFormData).subscribe({
        next: (res) => {
          console.log('Images uploaded successfully:', res);
          checkComplete();
        },
        error: (err) => {
          console.warn('Image upload failed (profile saved):', err);
          checkComplete();
        }
      });
    }

    // Upload documents
    if (hasDocuments) {
      pendingUploads++;
      const docFormData = new FormData();
      
      console.log('========== UPLOADING DOCUMENTS ==========');
      console.log('Documents files:', this.documentsFiles);
      
      let documentCount = 0;
      Object.entries(this.documentsFiles).forEach(([key, file]) => {
        if (file) {
          console.log(`Adding document ${key}:`, file.name, file.size, 'bytes');
          docFormData.append(key, file);
          documentCount++;
        }
      });
      
      console.log(`Total documents to upload: ${documentCount}`);
      
      this.partnerService.uploadDocuments(partnerId, docFormData).subscribe({
        next: (res) => {
          console.log('✅ Documents uploaded successfully:', res);
          checkComplete();
        },
        error: (err) => {
          console.error('❌ Document upload failed:', err);
          console.error('Error details:', err.error);
          checkComplete();
        }
      });
    }

    // If no files to upload, show success dialog directly
    if (!hasImages && !hasDocuments) {
      this.loading.set(false);
      // Update user with partnerId so isProfileComplete() returns true
      this.authService.updateStoredUser({ 
        partnerId: this.partnerId()!,
        isProfileComplete: true 
      });
      this.showSuccessDialog();
    }
  }

  private showSuccessDialog(): void {
    const dialogRef = this.dialog.open(ProfileSuccessDialogComponent, {
      disableClose: true,
      width: '540px',
      maxWidth: '95vw',
      panelClass: 'profile-success-panel',
    });

    dialogRef.afterClosed().subscribe(() => {
      this.router.navigate(['/dashboard']);
    });
  }

  getPreparationTimeLabel(value: number): string {
    const time = this.preparationTimes.find(t => t.value === value);
    return time ? time.label : value + ' minutes';
  }
}
