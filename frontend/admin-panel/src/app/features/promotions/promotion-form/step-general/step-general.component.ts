import { Component, Input, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { PromotionsService } from '../../services/promotions.service';

@Component({
  selector: 'app-step-general',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule,
    MatTooltipModule, MatRadioModule, MatProgressSpinnerModule, TranslateModule,
  ],
  templateUrl: './step-general.component.html',
  styleUrls: ['./step-general.component.scss'],
})
export class StepGeneralComponent implements OnInit, OnDestroy {
  @Input() formGroup!: FormGroup;
  @Input() isEdit = false;

  private service = inject(PromotionsService);
  private destroy$ = new Subject<void>();

  checkingCode = false;
  codeAvailable: boolean | null = null;

  readonly types = [
    { value: 'PERCENTAGE',    icon: 'percent',        label: 'promotions.types.PERCENTAGE' },
    { value: 'FIXED_AMOUNT',  icon: 'attach_money',   label: 'promotions.types.FIXED_AMOUNT' },
    { value: 'FREE_DELIVERY', icon: 'local_shipping',  label: 'promotions.types.FREE_DELIVERY' },
  ];

  ngOnInit(): void {
    // Code uniqueness check with debounce
    this.formGroup.get('code')?.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe((code: string) => {
      this.codeAvailable = null;
      if (!code || code.length < 3 || this.isEdit) {
        this.checkingCode = false;
        return;
      }
      this.checkingCode = true;
      this.service.checkCodeAvailability(code).pipe(takeUntil(this.destroy$)).subscribe({
        next: (available) => {
          this.checkingCode = false;
          this.codeAvailable = available;
          const ctrl = this.formGroup.get('code')!;
          if (!available) {
            ctrl.setErrors({ ...ctrl.errors, codeTaken: true });
          } else if (ctrl.errors?.['codeTaken']) {
            const { codeTaken, ...rest } = ctrl.errors;
            ctrl.setErrors(Object.keys(rest).length ? rest : null);
          }
        },
        error: () => { this.checkingCode = false; },
      });
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  generateCode(): void {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    const array = new Uint8Array(8);
    crypto.getRandomValues(array);
    for (let i = 0; i < 8; i++) {
      code += chars[array[i] % chars.length];
    }
    this.formGroup.get('code')?.setValue(code);
    this.formGroup.get('code')?.markAsTouched();
  }

  get typeValue(): string {
    return this.formGroup.get('type')?.value || '';
  }

  err(field: string): boolean {
    const c = this.formGroup.get(field);
    return !!(c?.invalid && c?.touched);
  }
}
