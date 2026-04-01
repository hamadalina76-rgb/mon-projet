import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { Subject, forkJoin, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { CourierScheduleService } from '../services/courier-schedule.service';
import { CouriersService } from '../services/couriers.service';
import {
  CourierExceptionalSchedule,
  ExceptionalScheduleCreateRequest,
  ManagerDecisionRequest,
  ExceptionType,
  EXCEPTION_TYPES,
} from '../models/courier-schedule.model';

// ── Types & constants ─────────────────────────────────────────────────────────

interface CourierOption { id: number; name: string; }

interface SignalementLogRow {
  id: number;
  courierName: string;
  declarationTypeLabel: string;
  reason: string;
  statusLabel: string;
  adminName: string;
  actionAt: string;
  comment: string;
}

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
  selector: 'app-exceptional-schedules',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatSelectModule,
    MatTooltipModule, MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './exceptional-schedules.component.html',
  styleUrls: ['./exceptional-schedules.component.scss'],
})
export class ExceptionalSchedulesComponent implements OnInit, OnDestroy {

  // ── DI ────────────────────────────────────────────────────────────────
  private svc       = inject(CourierScheduleService);
  private couriersS = inject(CouriersService);
  private fb        = inject(FormBuilder);
  private toastr    = inject(ToastrService);
  private translate = inject(TranslateService);
  private cdr       = inject(ChangeDetectorRef);

  // ── Table state ───────────────────────────────────────────────────────
  items        = signal<CourierExceptionalSchedule[]>([]);
  recentSignalements = signal<CourierExceptionalSchedule[]>([]);
  loading      = signal(true);
  logsLoading  = signal(true);
  currentPage  = 0;
  totalItems   = 0;
  itemsPerPage = 20;

  // ── Drawer / Form state ───────────────────────────────────────────────
  saving         = signal(false);
  showForm       = signal(false);
  editId         = signal<number | null>(null);
  approvalItem   = signal<CourierExceptionalSchedule | null>(null);
  overlapWarning = signal<string[]>([]);

  // ── Courier selection ─────────────────────────────────────────────────
  allCouriers       = signal<CourierOption[]>([]);
  loadingCouriers   = signal(false);
  courierFilterText = signal('');
  allSelected       = signal(false);

  filteredCouriersForSelect = computed<CourierOption[]>(() => {
    const q   = this.courierFilterText().toLowerCase().trim();
    const all = this.allCouriers();
    return (q ? all.filter(c => c.name.toLowerCase().includes(q)) : all).slice(0, 60);
  });

  selectedCourierName = computed<string>(() => {
    if (this.allSelected()) {
      return this.translate.instant('exceptional.allCouriersSelected');
    }
    const id = this.form?.get('courierId')?.value as number | null;
    if (!id) return '';
    return this.allCouriers().find(c => c.id === id)?.name ?? `Livreur #${id}`;
  });

  // ── Filters ───────────────────────────────────────────────────────────
  searchText      = '';
  filterType      = '';
  filterDateFrom  = '';
  filterDateTo    = '';
  filterCourierId = 0;
  filterValidationStatus = '';
  filterCourierType: '' | 'INTERNAL' | 'EXTERNAL' = '';
  filterUnavailabilityReason = '';

  readonly validationStatuses = [
    'PENDING_VALIDATION',
    'APPROVED_ACTIVE',
    'REJECTED',
    'RESOLVED_AVAILABLE',
  ] as const;

  readonly courierTypes = ['INTERNAL', 'EXTERNAL'] as const;

  readonly unavailabilityReasons = [
    'PANNE',
    'CONGE',
    'ABSENT',
    'RETARD',
    'NE_TRAVAILLE_PAS',
  ] as const;

  readonly exceptionTypes = EXCEPTION_TYPES;

  readonly signalementLogRows = computed<SignalementLogRow[]>(() =>
    this.recentSignalements().map((item) => ({
      id: item.id,
      courierName: item.courierName ?? `#${item.courierId}`,
      declarationTypeLabel: this.translate.instant(`exceptional.type_${item.exceptionType}`),
      reason: this.reasonLabel(item),
      statusLabel: this.validationLabel(item),
      adminName: item.validatorAdminName ?? item.adminName ?? '—',
      actionAt: item.validatedAt ?? item.createdAt ?? item.startDate,
      comment: item.validationComment ?? '—',
    }))
  );

  form!: FormGroup;

  private destroy$     = new Subject<void>();
  private searchInput$ = new Subject<string>();

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.buildForm();
    this.searchInput$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.load(); });
    this.load();
    this.loadRecentSignalements();
    this.loadAllCouriers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Data loading ──────────────────────────────────────────────────────
  load(): void {
    this.loading.set(true);
    this.svc.getAllExceptionalSchedules(
      this.currentPage, this.itemsPerPage,
      this.searchText      || undefined,
      this.filterType      || undefined,
      this.filterDateFrom  || undefined,
      this.filterDateTo    || undefined,
      this.filterCourierId || undefined,
      this.filterUnavailabilityReason || undefined,
      this.filterValidationStatus || undefined,
      this.filterCourierType || undefined,
    ).subscribe({
      next: (res: { content: CourierExceptionalSchedule[]; totalElements: number }) => {
        this.items.set(res.content ?? []);
        this.totalItems = res.totalElements ?? 0;
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => { this.loading.set(false); this.cdr.markForCheck(); },
    });
  }

  loadAllCouriers(): void {
    this.loadingCouriers.set(true);
    this.couriersS.getCouriers(0, 200).subscribe({
      next: (res: any) => {
        const raw: any[] = res?.content ?? (Array.isArray(res) ? res : []);
        this.allCouriers.set(raw.map((c: any): CourierOption => ({
          id:   c.id,
          name: `${c.firstName ?? ''} ${c.lastName ?? ''}`.trim() || c.email || `Livreur #${c.id}`,
        })));
        this.loadingCouriers.set(false);
        this.cdr.markForCheck();
      },
      error: () => { this.loadingCouriers.set(false); this.cdr.markForCheck(); },
    });
  }

  loadRecentSignalements(): void {
    this.logsLoading.set(true);
    // Charger les 10 derniers signalements (livreurs internes)
    this.svc.getAllExceptionalSchedules(
      0,
      10,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      'INTERNAL',
    ).subscribe({
      next: (res: { content: CourierExceptionalSchedule[]; totalElements: number }) => {
        this.recentSignalements.set(res.content ?? []);
        this.logsLoading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.recentSignalements.set([]);
        this.logsLoading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  // ── Form ──────────────────────────────────────────────────────────────
  buildForm(): void {
    this.form = this.fb.group({
      courierId:     [null],
      exceptionType: ['',   Validators.required],
      label:         ['',   [Validators.required, Validators.maxLength(200)]],
      startDate:     ['',   Validators.required],
      endDate:       ['',   Validators.required],
      startTime:     [''],
      endTime:       [''],
      reason:        [''],
      isRestPeriod:  [true],
      comment:       [''],
    });
  }

  openNew(): void {
    this.editId.set(null);
    this.approvalItem.set(null);
    this.allSelected.set(false);
    this.form.reset({ isRestPeriod: true });
    this.overlapWarning.set([]);
    this.courierFilterText.set('');
    this.showForm.set(true);
  }

  openEdit(item: CourierExceptionalSchedule): void {
    this.editId.set(item.id);
    this.approvalItem.set(null);
    this.allSelected.set(false);

    const startTime = item.startsAt ? item.startsAt.substring(11, 16) : '';
    const endTime   = item.endsAt   ? item.endsAt.substring(11, 16)   : '';

    this.form.patchValue({
      courierId:     item.courierId,
      exceptionType: item.exceptionType,
      label:         item.label,
      startDate:     item.startDate ?? '',
      endDate:       item.endDate   ?? '',
      startTime,
      endTime,
      reason:        item.reason ?? '',
      isRestPeriod:  item.isRestPeriod ?? true,
      comment:       '',
    });
    this.overlapWarning.set([]);
    this.courierFilterText.set('');
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editId.set(null);
    this.approvalItem.set(null);
    this.overlapWarning.set([]);
  }

  selectCourier(c: CourierOption): void {
    this.allSelected.set(false);
    this.form.patchValue({ courierId: c.id });
    this.form.get('courierId')?.markAsTouched();
    this.courierFilterText.set('');
    this.checkOverlapIfReady();
    this.cdr.markForCheck();
  }

  clearCourier(): void {
    this.allSelected.set(false);
    this.form.patchValue({ courierId: null });
    this.courierFilterText.set('');
    this.overlapWarning.set([]);
    this.cdr.markForCheck();
  }

  selectAllCouriers(): void {
    this.allSelected.set(true);
    this.form.patchValue({ courierId: null });
    this.courierFilterText.set('');
    this.overlapWarning.set([]);
    this.cdr.markForCheck();
  }

  checkOverlapIfReady(): void {
    const f = this.form.value;
    if (!f.courierId || !f.startDate || !f.endDate) return;
    const start = toIsoDate(f.startDate);
    const end   = toIsoDate(f.endDate);
    if (!start || !end) return;
    this.svc.checkExceptionalOverlap(f.courierId, start, end, this.editId() ?? undefined)
      .subscribe({
        next: (res) => {
          this.overlapWarning.set(
            res.hasOverlap
              ? res.overlapping.map(o => `${o.label} (${o.startDate} → ${o.endDate})`)
              : []
          );
          this.cdr.markForCheck();
        },
      });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.cdr.markForCheck(); return; }
    const f = this.form.value;

    // Single courier or all couriers?
    if (!this.allSelected()) {
      if (!f.courierId) { this.form.get('courierId')?.markAsTouched(); this.cdr.markForCheck(); return; }
      const startDate = toIsoDate(f.startDate);
      const endDate   = toIsoDate(f.endDate);
      const req: ExceptionalScheduleCreateRequest = {
        courierId:     f.courierId,
        exceptionType: f.exceptionType,
        label:         f.label.trim(),
        startDate,
        endDate,
        startsAt:      startDate && f.startTime ? `${startDate}T${f.startTime}:00` : undefined,
        endsAt:        (endDate || startDate) && f.endTime ? `${endDate || startDate}T${f.endTime}:00` : undefined,
        reason:        f.reason?.trim() || undefined,
        isRestPeriod:  f.isRestPeriod ?? true,
      };
      this.saving.set(true);
      const isEdit = this.editId();
      const obs    = isEdit
        ? this.svc.updateExceptionalSchedule(isEdit, req)
        : this.svc.createExceptionalSchedule(req);
      obs.subscribe({
        next: () => {
          this.toastr.success(this.translate.instant(
            isEdit ? 'exceptional.updateSuccess' : 'exceptional.createSuccess'));
          this.saving.set(false);
          this.closeForm();
          this.load();
          this.loadRecentSignalements();
        },
        error: (err: any) => {
          this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
          this.saving.set(false);
        },
      });
    } else {
      // Bulk creation for all internal couriers
      const couriers = this.allCouriers();
      if (!couriers.length) return;
      const bStartDate = toIsoDate(f.startDate);
      const bEndDate   = toIsoDate(f.endDate);
      const reqs = couriers.map(c => this.svc.createExceptionalSchedule({
        courierId:     c.id,
        exceptionType: f.exceptionType,
        label:         f.label.trim(),
        startDate:     bStartDate,
        endDate:       bEndDate,
        startsAt:      bStartDate && f.startTime ? `${bStartDate}T${f.startTime}:00` : undefined,
        endsAt:        (bEndDate || bStartDate) && f.endTime ? `${bEndDate || bStartDate}T${f.endTime}:00` : undefined,
        reason:        f.reason?.trim() || undefined,
        isRestPeriod:  f.isRestPeriod ?? true,
      }));
      this.saving.set(true);
      forkJoin(reqs).subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('exceptional.bulkCreateSuccess'));
          this.saving.set(false);
          this.closeForm();
          this.load();
          this.loadRecentSignalements();
        },
        error: (err: any) => {
          this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
          this.saving.set(false);
        },
      });
    }
  }

  // ── Filters ───────────────────────────────────────────────────────────
  onSearchChange():  void { this.searchInput$.next(this.searchText); }
  onTypeChange():    void { this.currentPage = 0; this.load(); }
  onDateChange():    void { this.currentPage = 0; this.load(); }
  onCourierChange(): void { this.currentPage = 0; this.load(); }
  onValidationStatusChange(): void { this.currentPage = 0; this.load(); }
  onCourierTypeChange(): void { this.currentPage = 0; this.load(); }
  onReasonChange(): void { this.currentPage = 0; this.load(); }

  resetFilters(): void {
    this.searchText = ''; this.filterType = '';
    this.filterDateFrom = ''; this.filterDateTo = '';
    this.filterCourierId = 0;
    this.filterValidationStatus = '';
    this.filterCourierType = '';
    this.filterUnavailabilityReason = '';
    this.currentPage = 0;
    this.load();
  }

  // ── Pagination ────────────────────────────────────────────────────────
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  onPageChange(p: number): void {
    if (p < 1 || p > this.totalPages) return;
    this.currentPage = p - 1;
    this.load();
  }

  // ── Utils ─────────────────────────────────────────────────────────────
  exColor(type: string): string {
    return EXCEPTION_COLORS[type as ExceptionType] ?? '#6B7280';
  }

  canValidate(item: CourierExceptionalSchedule): boolean {
    return item.courierType === 'INTERNAL' && item.validationStatus === 'PENDING_VALIDATION';
  }

  reasonLabel(item: CourierExceptionalSchedule): string {
    if (item.unavailabilityReason) {
      return this.translate.instant(`exceptional.reason_${item.unavailabilityReason}`);
    }
    return item.reason || '—';
  }

  validationLabel(item: CourierExceptionalSchedule): string {
    if (!item.validationStatus) return '—';
    return this.translate.instant(`exceptional.validation_${item.validationStatus}`);
  }

  courierTypeLabel(item: CourierExceptionalSchedule): string {
    if (!item.courierType) return '—';
    return this.translate.instant(`exceptional.courierType_${item.courierType}`);
  }

  approve(item: CourierExceptionalSchedule): void {
    this.approvalItem.set(item);
    this.editId.set(null);
    this.allSelected.set(false);

    // Auto-remplir le formulaire avec les données de la déclaration du livreur
    const startTime = item.startsAt ? item.startsAt.substring(11, 16) : '';
    const endTime   = item.endsAt   ? item.endsAt.substring(11, 16)   : '';

    this.form.reset({ isRestPeriod: true });
    this.form.patchValue({
      courierId:     item.courierId,
      exceptionType: item.exceptionType,
      label:         item.label ?? '',
      startDate:     item.startDate ?? '',
      endDate:       item.endDate ?? '',
      startTime,
      endTime,
      reason:        item.reason ?? '',
      isRestPeriod:  item.isRestPeriod ?? true,
      comment:       '',
    });

    this.overlapWarning.set([]);
    this.courierFilterText.set('');
    this.showForm.set(true);
  }

  submitApproval(): void {
    const item = this.approvalItem();
    if (!item) return;

    const f = this.form.value;
    const startDate = toIsoDate(f.startDate);
    const endDate   = toIsoDate(f.endDate);

    // Construire startsAt/endsAt à partir de date + heure
    let startsAt: string | undefined;
    let endsAt:   string | undefined;
    if (startDate && f.startTime) {
      startsAt = `${startDate}T${f.startTime}:00`;
    }
    if (endDate && f.endTime) {
      endsAt = `${endDate}T${f.endTime}:00`;
    } else if (startDate && f.endTime) {
      endsAt = `${startDate}T${f.endTime}:00`;
    }

    const req: ManagerDecisionRequest = {
      comment:       f.comment?.trim() || undefined,
      exceptionType: f.exceptionType,
      label:         f.label?.trim(),
      startDate,
      endDate,
      startsAt,
      endsAt,
      reason:        f.reason?.trim() || undefined,
      isRestPeriod:  f.isRestPeriod ?? true,
    };

    this.saving.set(true);
    this.svc.approveExceptionalSchedule(item.id, req).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('exceptional.approveSuccess'));
        this.saving.set(false);
        this.closeForm();
        this.load();
        this.loadRecentSignalements();
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
        this.saving.set(false);
      },
    });
  }

  reject(item: CourierExceptionalSchedule): void {
    const comment = window.prompt(
      this.translate.instant('exceptional.validationCommentPrompt'),
      ''
    ) ?? undefined;
    this.svc.rejectExceptionalSchedule(item.id, comment).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('exceptional.rejectSuccess'));
        this.load();
        this.loadRecentSignalements();
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
      },
    });
  }
}
