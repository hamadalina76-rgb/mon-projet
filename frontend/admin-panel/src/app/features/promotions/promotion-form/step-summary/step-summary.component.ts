import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, FormArray } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SimulatorComponent } from '../simulator/simulator.component';
import { PromotionType } from '@core/models/promotion.model';

@Component({
  selector: 'app-step-summary',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule, MatButtonModule, MatProgressSpinnerModule,
    MatDividerModule, TranslateModule,
    SimulatorComponent,
  ],
  templateUrl: './step-summary.component.html',
  styleUrls: ['./step-summary.component.scss'],
})
export class StepSummaryComponent {
  @Input() form!: FormGroup;
  @Input() isEdit = false;
  @Input() saving = false;
  @Input() partners: { id: number; name: string }[] = [];
  @Input() categories: { id: number; name: string }[] = [];
  @Input() zones: { id: number; name: string }[] = [];

  @Output() submitDraft = new EventEmitter<void>();
  @Output() submitActive = new EventEmitter<void>();

  constructor(private translate: TranslateService) {}

  get raw(): any { return this.form.getRawValue(); }

  get typeLabel(): string {
    return this.translate.instant('promotions.types.' + this.raw.type);
  }

  get formattedValue(): string {
    if (this.raw.type === 'PERCENTAGE') return this.raw.value + '%';
    if (this.raw.type === 'FIXED_AMOUNT') return this.raw.value + ' TND';
    return '-';
  }

  get selectedPartnerNames(): string[] {
    const ids: number[] = this.raw.applicablePartnerIds || [];
    if (!ids.length) return [];
    return ids.map((id: number) => this.partners.find(p => p.id === id)?.name || `#${id}`);
  }

  get selectedCategoryNames(): string[] {
    const ids: number[] = this.raw.applicableCategoryIds || [];
    if (!ids.length) return [];
    return ids.map((id: number) => this.categories.find(c => c.id === id)?.name || `#${id}`);
  }

  get selectedZoneNames(): string[] {
    const ids: number[] = this.raw.applicableZoneIds || [];
    if (!ids.length) return [];
    return ids.map((id: number) => this.zones.find(z => z.id === id)?.name || `#${id}`);
  }

  get rulesArray(): FormArray {
    return this.form.get('rules') as FormArray;
  }

  getRuleLabel(rule: any): string {
    const type = this.translate.instant('promotions.form.ruleTypes.' + rule.ruleType);
    if (rule.ruleType === 'SPECIFIC_DAY') {
      const days = (rule.days || []).map((d: string) => this.translate.instant('promotions.form.weekDays.' + d));
      return `${type}: ${days.join(', ')}`;
    }
    if (rule.ruleType === 'TIME_RANGE') {
      return `${type}: ${rule.timeFrom} → ${rule.timeTo}`;
    }
    if (rule.ruleType === 'MIN_ITEMS') {
      return `${type}: ≥ ${rule.minItems}`;
    }
    return type;
  }
}
