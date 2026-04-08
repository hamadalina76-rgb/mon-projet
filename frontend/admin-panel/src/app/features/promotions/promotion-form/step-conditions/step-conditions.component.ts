import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-step-conditions',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatCheckboxModule,
    MatDatepickerModule, MatNativeDateModule, TranslateModule,
  ],
  templateUrl: './step-conditions.component.html',
  styleUrls: ['./step-conditions.component.scss'],
})
export class StepConditionsComponent {
  @Input() formGroup!: FormGroup;

  today = new Date();

  get startDateValue(): Date | null {
    return this.formGroup.get('startDate')?.value;
  }

  get endDateInvalid(): boolean {
    const start = this.formGroup.get('startDate')?.value;
    const end = this.formGroup.get('endDate')?.value;
    return start && end && end < start;
  }

  err(field: string): boolean {
    const c = this.formGroup.get(field);
    return !!(c?.invalid && c?.touched);
  }
}
