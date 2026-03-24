import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule, MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';
import { CourierScheduleService } from '../services/courier-schedule.service';
import { ScheduleTemplateResponse, DAYS_OF_WEEK, DayOfWeek } from '../models/courier-schedule.model';

@Component({
  selector: 'app-schedule-templates',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatSlideToggleModule,
    MatDialogModule, MatTooltipModule, MatChipsModule, MatSelectModule, TranslateModule
  ],
  templateUrl: './schedule-templates.component.html',
  styleUrls: ['./schedule-templates.component.scss'],
})
export class ScheduleTemplatesComponent implements OnInit, OnDestroy {
  private scheduleService = inject(CourierScheduleService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  templates = signal<ScheduleTemplateResponse[]>([]);
  loading = signal(true);
  days = DAYS_OF_WEEK;

  searchText = '';
  selectedStatus = 'all';
  currentPage = 1;
  itemsPerPage = 6;
  totalItems = 0;

  Math = Math;

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading.set(true);
    const isActive = this.selectedStatus === 'active' ? true
                   : this.selectedStatus === 'inactive' ? false
                   : undefined;
    this.scheduleService
      .getTemplates(this.currentPage - 1, this.itemsPerPage, this.searchText || undefined, isActive)
      .subscribe({
        next: (response: any) => {
          this.templates.set(response.content ?? []);
          this.totalItems = response.totalElements ?? 0;
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.load();
  }

  onSearchChange(): void {
    this.searchInput$.next(this.searchText);
  }

  onStatusFilterChange(): void {
    this.applyFilters();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(template?: ScheduleTemplateResponse): void {
    const ref = this.dialog.open(ScheduleTemplateFormComponent, {
      width: '860px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      data: template ?? null,
      panelClass: 'schedule-form-panel',
    });
    ref.afterClosed().subscribe((saved) => { if (saved) this.load(); });
  }

  toggle(t: ScheduleTemplateResponse): void {
    this.scheduleService.toggleTemplate(t.id).subscribe({
      next: () => { this.load(); this.toastr.success(this.translate.instant('schedule.toggleSuccess')); },
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }

  delete(t: ScheduleTemplateResponse): void {
    if (!confirm(this.translate.instant('schedule.deleteConfirm', { name: t.name }))) return;
    this.scheduleService.deleteTemplate(t.id).subscribe({
      next: () => { this.load(); this.toastr.success(this.translate.instant('schedule.deleteSuccess')); },
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }

  shiftCount(t: ScheduleTemplateResponse): number {
    return Object.values(t.days).reduce((sum, s) => sum + s.length, 0);
  }

  activeDays(t: ScheduleTemplateResponse): number {
    return Object.values(t.days).filter(s => s.length > 0).length;
  }
}

// ── Inline Form Dialog Component ──────────────────────────────────────────────

@Component({
  selector: 'app-schedule-template-form',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatInputModule,
    MatFormFieldModule, MatDialogModule, MatTooltipModule, TranslateModule
  ],
  templateUrl: './schedule-template-form.component.html',
  styleUrls: ['./schedule-templates.component.scss'],
})
export class ScheduleTemplateFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private scheduleService = inject(CourierScheduleService);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  dialogRef = inject(MatDialogRef<ScheduleTemplateFormComponent>);
  data: ScheduleTemplateResponse | null = inject(MAT_DIALOG_DATA);

  days = DAYS_OF_WEEK;
  saving = signal(false);
  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      name: [this.data?.name ?? '', [Validators.required, Validators.maxLength(100)]],
      description: [this.data?.description ?? ''],
    });

    // Build per-day shift arrays
    const dayControls: Record<string, FormArray> = {};
    this.days.forEach(day => {
      const existingShifts = this.data?.days?.[day] ?? [];
      dayControls[day] = this.fb.array(
        existingShifts.length > 0
          ? existingShifts.map(s => this.buildShiftGroup(s.startTime, s.endTime, s.breakStart ?? '', s.breakEnd ?? ''))
          : []
      );
    });
    Object.entries(dayControls).forEach(([day, arr]) => this.form.addControl(day, arr));
  }

  buildShiftGroup(start = '', end = '', breakStart = '', breakEnd = ''): FormGroup {
    return this.fb.group({
      startTime:  [start, Validators.required],
      endTime:    [end,   Validators.required],
      breakStart: [breakStart],
      breakEnd:   [breakEnd],
    });
  }

  shiftsFor(day: DayOfWeek): FormArray {
    return this.form.get(day) as FormArray;
  }

  addShift(day: DayOfWeek): void {
    this.shiftsFor(day).push(this.buildShiftGroup());
  }

  removeShift(day: DayOfWeek, idx: number): void {
    this.shiftsFor(day).removeAt(idx);
  }

  copyDayTo(sourceDay: DayOfWeek, targetDay: DayOfWeek): void {
    const sourceShifts = this.shiftsFor(sourceDay).value as Array<{ startTime: string; endTime: string; breakStart: string; breakEnd: string }>;
    const target = this.shiftsFor(targetDay);
    target.clear();
    sourceShifts.forEach(s => target.push(this.buildShiftGroup(s.startTime, s.endTime, s.breakStart, s.breakEnd)));
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);

    const daysPayload: Record<string, any[]> = {};
    this.days.forEach(day => {
      const shifts = this.shiftsFor(day).value;
      if (shifts.length > 0) daysPayload[day] = shifts;
    });

    const payload = { name: this.form.value.name, description: this.form.value.description, days: daysPayload };

    const req$ = this.data
      ? this.scheduleService.updateTemplate(this.data.id, payload as any)
      : this.scheduleService.createTemplate(payload as any);

    req$.subscribe({
      next: () => {
        this.toastr.success(this.translate.instant(this.data ? 'schedule.updateSuccess' : 'schedule.createSuccess'));
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
        this.saving.set(false);
      },
    });
  }
}
