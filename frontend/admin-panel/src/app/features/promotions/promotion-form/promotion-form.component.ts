import { Component, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { MatStepperModule, MatStepper } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { STEPPER_GLOBAL_OPTIONS } from '@angular/cdk/stepper';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';

import { PromotionsService } from '../services/promotions.service';
import { PromotionType } from '@core/models/promotion.model';

import { StepGeneralComponent } from './step-general/step-general.component';
import { StepConditionsComponent } from './step-conditions/step-conditions.component';
import { StepTargetingComponent } from './step-targeting/step-targeting.component';
import { StepSummaryComponent } from './step-summary/step-summary.component';

@Component({
  selector: 'app-promotion-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatStepperModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatTooltipModule, TranslateModule,
    StepGeneralComponent, StepConditionsComponent,
    StepTargetingComponent, StepSummaryComponent,
  ],
  providers: [
    { provide: STEPPER_GLOBAL_OPTIONS, useValue: { showError: true } },
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
  private toastr    = inject(ToastrService);
  private destroy$  = new Subject<void>();

  @ViewChild('stepper') stepper!: MatStepper;

  form!: FormGroup;
  stepGeneralGroup!: FormGroup;
  stepConditionsGroup!: FormGroup;
  stepTargetingGroup!: FormGroup;

  isEdit      = false;
  isDuplicate = false;
  editId?: number;
  loading  = false;
  saving   = false;

  // Data passed to StepSummary for display
  partners: { id: number; name: string }[] = [];
  categories: { id: number; name: string }[] = [];
  zones: { id: number; name: string }[] = [];

  ngOnInit(): void {
    this.buildForm();

    const id = this.route.snapshot.paramMap.get('id');
    const duplicateId = this.route.snapshot.queryParamMap.get('duplicate');

    if (id && id !== 'new') {
      this.isEdit = true;
      this.editId = +id;
      this.loadPromotion(this.editId);
    } else if (duplicateId) {
      this.isDuplicate = true;
      this.loadPromotion(+duplicateId, true);
    }

    // Disable code in edit mode
    if (this.isEdit) this.form.get('code')?.disable();

    // Toggle value validators based on type
    this.form.get('type')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((type: PromotionType) => {
        const v = this.form.get('value')!;
        if (type === 'FREE_DELIVERY') {
          v.clearValidators();
        } else {
          v.setValidators([Validators.required, Validators.min(0.01)]);
        }
        v.updateValueAndValidity();
      });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  private buildForm(): void {
    this.form = this.fb.group({
      // Step 1 — General
      code: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Z0-9_-]+$/)]],
      name:             ['', Validators.required],
      description:      [''],
      type:             ['PERCENTAGE', Validators.required],
      value:            [null, [Validators.required, Validators.min(0.01)]],
      maximumDiscount:  [null, Validators.min(0)],

      // Step 2 — Conditions
      minimumOrder:      [null, Validators.min(0)],
      usageLimitTotal:   [null, Validators.min(0)],
      usageLimitPerUser: [null, Validators.min(1)],
      startDate:         [null],
      endDate:           [null],
      firstOrderOnly:    [false],
      status:            ['ACTIVE'],

      // Step 3 — Targeting
      applicablePartnerIds:  [[] as number[]],
      applicableCategoryIds: [[] as number[]],
      applicableZoneIds:     [[] as number[]],
      rules: this.fb.array([]),
    });

    // Sub-groups for stepper step controls
    this.stepGeneralGroup = this.fb.group({
      code:            this.form.get('code')!,
      name:            this.form.get('name')!,
      description:     this.form.get('description')!,
      type:            this.form.get('type')!,
      value:           this.form.get('value')!,
      maximumDiscount: this.form.get('maximumDiscount')!,
    });

    this.stepConditionsGroup = this.fb.group({
      minimumOrder:      this.form.get('minimumOrder')!,
      usageLimitTotal:   this.form.get('usageLimitTotal')!,
      usageLimitPerUser: this.form.get('usageLimitPerUser')!,
      startDate:         this.form.get('startDate')!,
      endDate:           this.form.get('endDate')!,
      firstOrderOnly:    this.form.get('firstOrderOnly')!,
    });

    this.stepTargetingGroup = this.fb.group({
      applicablePartnerIds:  this.form.get('applicablePartnerIds')!,
      applicableCategoryIds: this.form.get('applicableCategoryIds')!,
      applicableZoneIds:     this.form.get('applicableZoneIds')!,
    });
  }

  // ── Load promotion (edit or duplicate) ────────────────────────
  private loadPromotion(id: number, isDuplicate = false): void {
    this.loading = true;
    this.service.getPromotionForEdit(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (p) => {
        this.form.patchValue({
          code:              isDuplicate ? '' : p.code,
          name:              p.name,
          description:       p.description ?? '',
          type:              p.type,
          value:             p.value,
          maximumDiscount:   p.maximumDiscount,
          minimumOrder:      p.minimumOrder,
          usageLimitTotal:   p.usageLimitTotal,
          usageLimitPerUser: p.usageLimitPerUser,
          startDate:         p.startDate ? new Date(p.startDate) : null,
          endDate:           p.endDate   ? new Date(p.endDate)   : null,
          firstOrderOnly:    p.firstOrderOnly,
          status:            isDuplicate ? 'INACTIVE' : p.status,
          applicablePartnerIds:  p.applicablePartnerIds ?? [],
          applicableCategoryIds: p.applicableCategoryIds ?? [],
          applicableZoneIds:     p.applicableZoneIds ?? [],
        });
        // Load existing rules
        if (p.rules && p.rules.length > 0) {
          const rulesArr = this.form.get('rules') as FormArray;
          rulesArr.clear();
          for (const r of p.rules) {
            let days: string[] = [];
            let timeFrom = '';
            let timeTo = '';
            let minItems: number | null = null;
            switch (r.ruleType) {
              case 'SPECIFIC_DAY':
                days = (r.targetValue || '').split(',').filter((s: string) => s);
                break;
              case 'TIME_RANGE':
                const parts = (r.targetValue || '').split('-');
                timeFrom = parts[0] || '';
                timeTo = parts[1] || '';
                break;
              case 'MIN_ITEMS':
                minItems = r.targetValue ? +r.targetValue : null;
                break;
            }
            rulesArr.push(this.fb.group({
              ruleType:    [r.ruleType, Validators.required],
              operator:    [r.operator || ''],
              targetValue: [r.targetValue || ''],
              days:        [days],
              timeFrom:    [timeFrom],
              timeTo:      [timeTo],
              minItems:    [minItems],
            }));
          }
        }
        if (!isDuplicate) this.form.get('code')?.disable();
        this.loading = false;
      },
      error: () => { this.loading = false; this.back(); },
    });
  }

  // ── Submit ────────────────────────────────────────────────────
  submitAsActive(): void { this.submit('ACTIVE'); }
  submitAsDraft(): void  { this.submit('INACTIVE'); }

  private submit(status: string): void {
    // Mark all as touched to show errors
    this.form.markAllAsTouched();

    // Check step 1
    if (this.stepGeneralGroup.invalid) {
      const errors = this.getStepErrors(this.stepGeneralGroup);
      this.toastr.warning(
        errors.join('<br>'),
        this.translate.instant('promotions.form.step1Title'),
        { enableHtml: true }
      );
      this.stepper.selectedIndex = 0;
      return;
    }

    // Check step 2
    if (this.stepConditionsGroup.invalid) {
      const errors = this.getStepErrors(this.stepConditionsGroup);
      this.toastr.warning(
        errors.join('<br>'),
        this.translate.instant('promotions.form.step2Title'),
        { enableHtml: true }
      );
      this.stepper.selectedIndex = 1;
      return;
    }

    // Check code taken
    if (this.form.get('code')?.hasError('codeTaken')) {
      this.toastr.warning(this.translate.instant('promotions.form.codeTaken'));
      this.stepper.selectedIndex = 0;
      return;
    }

    this.saving = true;
    const raw = this.form.getRawValue();

    const toLocalIso = (d: Date | null) => {
      if (!d) return undefined;
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    };

    const payload: any = {
      ...raw,
      status,
      startDate: toLocalIso(raw.startDate),
      endDate:   toLocalIso(raw.endDate),
      applicableZoneIds: raw.type === 'FREE_DELIVERY' ? (raw.applicableZoneIds ?? []) : [],
      rules: (raw.rules || []).filter((r: any) => r.ruleType).map((r: any) => {
        let operator = r.operator || '';
        let targetValue = r.targetValue || '';
        switch (r.ruleType) {
          case 'SPECIFIC_DAY':
            operator = 'IN';
            targetValue = (r.days || []).join(',');
            break;
          case 'TIME_RANGE':
            operator = 'BETWEEN';
            targetValue = (r.timeFrom || '') + '-' + (r.timeTo || '');
            break;
          case 'MIN_ITEMS':
            operator = 'GTE';
            targetValue = String(r.minItems || '');
            break;
        }
        return { ruleType: r.ruleType, operator, targetValue };
      }),
    };

    if (raw.type === 'FREE_DELIVERY') {
      payload.value = 0;
    }

    // Remove UI-only fields from rules
    delete payload.days;
    delete payload.timeFrom;
    delete payload.timeTo;
    delete payload.minItems;

    const obs = this.isEdit
      ? this.service.update(this.editId!, payload)
      : this.service.create({ ...payload, code: raw.code.toUpperCase() });

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.saving = false;
        this.toastr.success(this.translate.instant(
          this.isEdit ? 'promotions.form.updateSuccess' : 'promotions.form.createSuccess'
        ));
        this.back();
      },
      error: () => {
        this.saving = false;
        this.toastr.error(this.translate.instant('common.error'));
      },
    });
  }

  get rulesArray(): FormArray {
    return this.form.get('rules') as FormArray;
  }

  get promotionType(): string {
    return this.form.get('type')?.value || '';
  }

  get title(): string {
    if (this.isDuplicate) return this.translate.instant('promotions.form.duplicateTitle');
    if (this.isEdit)      return this.translate.instant('promotions.form.editTitle');
    return this.translate.instant('promotions.form.createTitle');
  }

  get subtitle(): string {
    if (this.isDuplicate) return this.translate.instant('promotions.form.duplicateSubtitle');
    if (this.isEdit)      return this.translate.instant('promotions.form.editSubtitle');
    return this.translate.instant('promotions.form.createSubtitle');
  }

  back(): void { this.router.navigate(['/promotions']); }

  /** Show toast with field-level errors when user clicks Next on an invalid step */
  validateStep(stepGroup: FormGroup, stepTitle: string): void {
    stepGroup.markAllAsTouched();
    if (stepGroup.invalid) {
      const errors = this.getStepErrors(stepGroup);
      this.toastr.warning(
        errors.join('<br>'),
        this.translate.instant(stepTitle),
        { enableHtml: true }
      );
    }
  }

  private getStepErrors(group: FormGroup): string[] {
    const errors: string[] = [];
    const fieldLabels: Record<string, string> = {
      code: this.translate.instant('promotions.code'),
      name: this.translate.instant('promotions.name'),
      type: this.translate.instant('promotions.form.reductionType'),
      value: this.translate.instant('promotions.value'),
      maximumDiscount: this.translate.instant('promotions.form.maxDiscount'),
      minimumOrder: this.translate.instant('promotions.form.minOrder'),
      usageLimitTotal: this.translate.instant('promotions.form.totalQuota'),
      usageLimitPerUser: this.translate.instant('promotions.form.userQuota'),
      startDate: this.translate.instant('promotions.form.startDate'),
      endDate: this.translate.instant('promotions.form.endDate'),
    };

    for (const key of Object.keys(group.controls)) {
      const ctrl = group.get(key);
      if (ctrl?.invalid) {
        const label = fieldLabels[key] || key;
        if (ctrl.errors?.['required']) {
          errors.push(`${label} : ${this.translate.instant('promotions.form.fieldRequired')}`);
        } else if (ctrl.errors?.['minlength']) {
          errors.push(`${label} : ${this.translate.instant('promotions.form.codeMinLength')}`);
        } else if (ctrl.errors?.['pattern']) {
          errors.push(`${label} : ${this.translate.instant('promotions.form.codePattern')}`);
        } else if (ctrl.errors?.['min']) {
          errors.push(`${label} : ${this.translate.instant('promotions.form.valueRequired')}`);
        } else if (ctrl.errors?.['codeTaken']) {
          errors.push(`${label} : ${this.translate.instant('promotions.form.codeTaken')}`);
        } else {
          errors.push(`${label} : ${this.translate.instant('promotions.form.codeInvalid')}`);
        }
      }
    }
    return errors.length > 0 ? errors : [this.translate.instant('promotions.form.fixErrors')];
  }

  /** Called by step-targeting to share loaded data with step-summary */
  onPartnersLoaded(p: { id: number; name: string }[]): void { this.partners = p; }
  onCategoriesLoaded(c: { id: number; name: string }[]): void { this.categories = c; }
  onZonesLoaded(z: { id: number; name: string }[]): void { this.zones = z; }
}
