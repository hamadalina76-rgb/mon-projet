import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { RuleType } from '@core/models/promotion.model';

@Component({
  selector: 'app-rule-builder',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    MatButtonModule, MatTooltipModule, TranslateModule,
  ],
  templateUrl: './rule-builder.component.html',
  styleUrls: ['./rule-builder.component.scss'],
})
export class RuleBuilderComponent {
  @Input() rulesArray!: FormArray;

  private fb = inject(FormBuilder);

  readonly ruleTypes: { value: RuleType; icon: string }[] = [
    { value: 'SPECIFIC_DAY',  icon: 'calendar_today' },
    { value: 'TIME_RANGE',    icon: 'schedule' },
    { value: 'MIN_ITEMS',     icon: 'inventory_2' },
  ];

  readonly weekDays = [
    'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
  ];

  addRule(): void {
    this.rulesArray.push(this.fb.group({
      ruleType:    ['', Validators.required],
      operator:    [''],
      targetValue: [''],
      days:        [[] as string[]],
      timeFrom:    [''],
      timeTo:      [''],
      minItems:    [null],
    }));
  }

  removeRule(index: number): void {
    this.rulesArray.removeAt(index);
  }

  onRuleTypeChange(index: number): void {
    const group = this.rulesArray.at(index) as FormGroup;
    group.patchValue({ days: [], timeFrom: '', timeTo: '', minItems: null, targetValue: '', operator: '' });
  }

  getRuleType(index: number): string {
    return (this.rulesArray.at(index) as FormGroup).get('ruleType')?.value || '';
  }
}
