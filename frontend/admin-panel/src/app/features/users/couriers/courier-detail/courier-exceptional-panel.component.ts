import {
  Component,
  OnInit,
  OnDestroy,
  Input,
  inject,
  signal,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { Subject, takeUntil } from 'rxjs';

import { CourierScheduleService } from '../services/courier-schedule.service';
import {
  CourierExceptionalSchedule,
  ExceptionalScheduleCreateRequest,
  ExceptionType,
  EXCEPTION_TYPES,
} from '../models/courier-schedule.model';

// ── Helpers ───────────────────────────────────────────────────────────────────

const EXCEPTION_COLORS: Record<ExceptionType, string> = {
  JOUR_FERIE:        '#F59E0B',
  EVENEMENT_SPECIAL: '#3B82F6',
  CONGE:             '#10B981',
  FERMETURE:         '#EF4444',
  FORMATION:         '#8B5CF6',
  PANNE:             '#DC2626',
  ABSENT:            '#9CA3AF',
  RETARD:            '#F97316',
  NE_TRAVAILLE_PAS:  '#6B7280',
};

function toIsoDate(d: Date | string | null): string {
  if (!d) return '';
  if (typeof d === 'string') return d;
  return d.toISOString().split('T')[0];
}

// ─────────────────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-courier-exceptional-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatSelectModule,
    MatCheckboxModule, MatDatepickerModule, MatNativeDateModule,
    MatProgressSpinnerModule, TranslateModule,
  ],
  templateUrl: './courier-exceptional-panel.component.html',
  styleUrls:  ['./courier-exceptional-panel.component.scss'],
})
export class CourierExceptionalPanelComponent implements OnInit, OnDestroy {

  @Input() courierId!: number;
  @Input() courierName = '';

  // ── DI ────────────────────────────────────────────────────────────────
  private svc       = inject(CourierScheduleService);
  private fb        = inject(FormBuilder);
  private toastr    = inject(ToastrService);
  private translate = inject(TranslateService);
  private cdr       = inject(ChangeDetectorRef);

  // ── State ─────────────────────────────────────────────────────────────
  items          = signal<CourierExceptionalSchedule[]>([]);
  loading        = signal(true);
  saving         = signal(false);
  showForm       = signal(false);
  editId         = signal<number | null>(null);
  overlapWarning = signal<string[]>([]);

  readonly exceptionTypes = EXCEPTION_TYPES;
  readonly exColors       = EXCEPTION_COLORS;

  form!: FormGroup;
  private destroy$ = new Subject<void>();

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.buildForm();
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Data loading ──────────────────────────────────────────────────────
  load(): void {
    this.loading.set(true);
    const from = toIsoDate(new Date(Date.now() - 3 * 365 * 24 * 60 * 60 * 1000));
    const to   = toIsoDate(new Date(Date.now() + 3 * 365 * 24 * 60 * 60 * 1000));
    this.svc.getExceptionalSchedulesByCourier(String(this.courierId), from, to).subscribe({
      next: (res) => {
        this.items.set(res);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  // ── Form ──────────────────────────────────────────────────────────────
  buildForm(): void {
    this.form = this.fb.group({
      exceptionType: ['', Validators.required],
      label:         ['', [Validators.required, Validators.maxLength(200)]],
      startDate:     [null, Validators.required],
      endDate:       [null, Validators.required],
      reason:        [''],
      isRestPeriod:  [true],
    });
  }

  openNew(): void {
    this.editId.set(null);
    this.form.reset({ isRestPeriod: true });
    this.overlapWarning.set([]);
    this.showForm.set(true);
  }

  openEdit(item: CourierExceptionalSchedule): void {
    this.editId.set(item.id);
    this.form.patchValue({
      exceptionType: item.exceptionType,
      label:         item.label,
      startDate:     item.startDate ? new Date(item.startDate) : null,
      endDate:       item.endDate   ? new Date(item.endDate)   : null,
      reason:        item.reason ?? '',
      isRestPeriod:  item.isRestPeriod ?? true,
    });
    this.overlapWarning.set([]);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editId.set(null);
    this.overlapWarning.set([]);
  }

  checkOverlapIfReady(): void {
    const f = this.form.value;
    if (!f.startDate || !f.endDate) return;
    const start = toIsoDate(f.startDate);
    const end   = toIsoDate(f.endDate);
    if (!start || !end) return;

    this.svc.checkExceptionalOverlap(this.courierId, start, end, this.editId() ?? undefined)
      .subscribe({
        next: (res) => {
          if (res.hasOverlap) {
            const labels = res.overlapping.map(o =>
              `${o.label} (${o.startDate} → ${o.endDate})`);
            this.overlapWarning.set(labels);
          } else {
            this.overlapWarning.set([]);
          }
          this.cdr.markForCheck();
        },
      });
  }

  save(): void {
    if (this.form.invalid) return;

    const f = this.form.value;
    const req: ExceptionalScheduleCreateRequest = {
      courierId:     this.courierId,
      exceptionType: f.exceptionType,
      label:         f.label.trim(),
      startDate:     toIsoDate(f.startDate),
      endDate:       toIsoDate(f.endDate),
      reason:        f.reason?.trim() || undefined,
      isRestPeriod:  f.isRestPeriod ?? true,
    };

    this.saving.set(true);
    const obs = this.editId()
      ? this.svc.updateExceptionalSchedule(this.editId()!, req)
      : this.svc.createExceptionalSchedule(req);

    obs.subscribe({
      next: () => {
        this.toastr.success(this.translate.instant(
          this.editId() ? 'exceptional.updateSuccess' : 'exceptional.createSuccess'));
        this.saving.set(false);
        this.closeForm();
        this.load();
      },
      error: (err) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
        this.saving.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  delete(item: CourierExceptionalSchedule): void {
    if (!confirm(this.translate.instant('exceptional.deleteConfirm', { label: item.label }))) return;
    this.svc.deleteExceptionalSchedule(item.id).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('exceptional.deleteSuccess'));
        this.load();
      },
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }

  // ── Utils ─────────────────────────────────────────────────────────────
  exColor(type: string): string {
    return EXCEPTION_COLORS[type as ExceptionType] ?? '#6B7280';
  }

  truncateReason(reason: string | undefined): string {
    if (!reason) return '—';
    return reason.length > 50 ? reason.substring(0, 50) + '…' : reason;
  }
}
