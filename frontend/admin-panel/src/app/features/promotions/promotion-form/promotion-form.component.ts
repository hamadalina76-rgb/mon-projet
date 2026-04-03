import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators, AbstractControl,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRadioModule } from '@angular/material/radio';
import { TranslateModule } from '@ngx-translate/core';

import { PromotionsService } from '../services/promotions.service';
import { PromotionType, PromotionStatus } from '@core/models/promotion.model';
import { TranslateService } from '@ngx-translate/core';
import { PartnersService } from '../../partners/services/partners.service';
import { Zone } from '@core/models/zone.model';

@Component({
  selector: 'app-promotion-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatInputModule,
    MatFormFieldModule, MatSelectModule, MatCheckboxModule,
    MatDatepickerModule, MatNativeDateModule, MatProgressSpinnerModule,
    MatDividerModule, MatChipsModule, MatTooltipModule, MatRadioModule, TranslateModule,
  ],
  templateUrl: './promotion-form.component.html',
  styleUrls: ['./promotion-form.component.scss'],
})
export class PromotionFormComponent implements OnInit, OnDestroy {
  private fb        = inject(FormBuilder);
  private router    = inject(Router);
  private route     = inject(ActivatedRoute);
  private service   = inject(PromotionsService);
  private translate = inject(TranslateService);
  private partnersService = inject(PartnersService);
  private destroy$  = new Subject<void>();

  form!: FormGroup;
  isEdit   = false;
  editId?: number;
  loading  = false;
  saving   = false;
  today    = new Date();

  partners: { id: number; name: string; logo?: string }[] = [];
  filteredPartners: { id: number; name: string; logo?: string }[] = [];
  partnerSearch = '';
  zones: Zone[] = [];
  filteredZones: Zone[] = [];
  zoneSearch = '';
  allPartners = true; // radio: all or specific

  readonly types: { value: PromotionType; icon: string }[] = [
    { value: 'PERCENTAGE',    icon: 'percent' },
    { value: 'FIXED_AMOUNT',  icon: 'attach_money' },
    { value: 'FREE_DELIVERY', icon: 'local_shipping' },
  ];

  readonly statuses: { value: PromotionStatus }[] = [
    { value: 'ACTIVE' },
    { value: 'INACTIVE' },
    { value: 'SCHEDULED' },
  ];

  ngOnInit(): void {
    this.buildForm();
    this.loadPartners();
    this.loadZones();

    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEdit = true;
      this.editId = +id;
      this.loadPromotion(this.editId);
    }

    // Disable "Code" field in edit mode
    if (this.isEdit) this.form.get('code')?.disable();

    // Toggle max discount required for PERCENTAGE
    this.form.get('type')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updateValueLabel());
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  private buildForm(): void {
    this.form = this.fb.group({
      code: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[A-Z0-9_-]+$/),
      ]],
      name:        ['', Validators.required],
      description: [''],
      type:        ['PERCENTAGE', Validators.required],
      value:       [null, [Validators.required, Validators.min(0.01)]],
      maximumDiscount:  [null, Validators.min(0)],
      minimumOrder:     [null, Validators.min(0)],
      usageLimitTotal:  [null, Validators.min(1)],
      usageLimitPerUser:[null, Validators.min(1)],
      startDate:   [null],
      endDate:     [null],
      firstOrderOnly: [false],
      status:      ['ACTIVE', Validators.required],
      applicablePartnerIds: [[] as number[]],
      applicableZoneIds: [[] as number[]],
    });
  }

  private loadPromotion(id: number): void {
    this.loading = true;
    this.service.getPromotionForEdit(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (p) => {
        this.form.patchValue({
          code:            p.code,
          name:            p.name,
          description:     p.description ?? '',
          type:            p.type,
          value:           p.value,
          maximumDiscount: p.maximumDiscount,
          minimumOrder:    p.minimumOrder,
          usageLimitTotal: p.usageLimitTotal,
          usageLimitPerUser: p.usageLimitPerUser,
          startDate:       p.startDate ? new Date(p.startDate) : null,
          endDate:         p.endDate   ? new Date(p.endDate)   : null,
          firstOrderOnly:  p.firstOrderOnly,
          status:          p.status,
          applicablePartnerIds: p.applicablePartnerIds ?? [],
          applicableZoneIds: p.applicableZoneIds ?? [],
        });
        this.allPartners = !p.applicablePartnerIds || p.applicablePartnerIds.length === 0;
        this.form.get('code')?.disable();
        this.loading = false;
      },
      error: () => { this.loading = false; this.back(); },
    });
  }

  private loadPartners(): void {
    this.partnersService.getPartners(0, 200).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        const list = res?.data?.content ?? res?.content ?? (Array.isArray(res) ? res : []);
        this.partners = list.map((p: any) => ({
          id: p.id,
          name: p.businessName || p.brandName || p.name || `Partner #${p.id}`,
          logo: p.logo || null,
        }));
        this.filteredPartners = [...this.partners];
      },
    });
  }

  filterPartners(event: Event): void {
    const q = (event.target as HTMLInputElement).value.toLowerCase().trim();
    this.partnerSearch = q;
    this.filteredPartners = q
      ? this.partners.filter(p => p.name.toLowerCase().includes(q))
      : [...this.partners];
  }

  getPartnerInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || '?';
  }

  filterZones(): void {
    const q = this.zoneSearch.toLowerCase().trim();
    this.filteredZones = q
      ? this.zones.filter(z => z.name.toLowerCase().includes(q))
      : [...this.zones];
  }

  private loadZones(): void {
    this.partnersService.getAllZones().pipe(takeUntil(this.destroy$)).subscribe({
      next: (zones) => { this.zones = zones; this.filteredZones = zones; },
    });
  }

  onPartnerModeChange(allPartners: boolean): void {
    this.allPartners = allPartners;
    if (allPartners) {
      this.form.get('applicablePartnerIds')?.setValue([]);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const raw = this.form.getRawValue();

    const toIso = (d: Date | null) => d ? d.toISOString() : undefined;
    const payload: any = {
      ...raw,
      startDate: toIso(raw.startDate),
      endDate: toIso(raw.endDate),
      applicablePartnerIds: this.allPartners ? [] : (raw.applicablePartnerIds ?? []),
      applicableZoneIds: raw.type === 'FREE_DELIVERY' ? (raw.applicableZoneIds ?? []) : [],
    };

    // Free delivery: value not needed (discount = zone's delivery fee)
    if (raw.type === 'FREE_DELIVERY') {
      payload.value = 0;
    }

    const obs = this.isEdit
      ? this.service.update(this.editId!, payload)
      : this.service.create({ ...payload, code: raw.code.toUpperCase() });

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.saving = false; this.back(); },
      error: () => { this.saving = false; },
    });
  }

  back(): void { this.router.navigate(['/promotions']); }

  get valueLabel(): string {
    const t = this.form.get('type')?.value as PromotionType;
    if (t === 'PERCENTAGE')    return this.translate.instant('promotions.form.valuePercent');
    if (t === 'FREE_DELIVERY') return this.translate.instant('promotions.form.valueIgnored');
    return this.translate.instant('promotions.form.valueFixed');
  }

  private updateValueLabel(): void {
    // Free delivery — value not required
    const t = this.form.get('type')?.value as PromotionType;
    const v = this.form.get('value')!;
    if (t === 'FREE_DELIVERY') {
      v.clearValidators();
    } else {
      v.setValidators([Validators.required, Validators.min(0.01)]);
    }
    v.updateValueAndValidity();
  }

  err(field: string): boolean {
    const c = this.form.get(field);
    return !!(c?.invalid && c?.touched);
  }
}
