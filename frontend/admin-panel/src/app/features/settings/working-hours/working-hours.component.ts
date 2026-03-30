import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { SettingsService, DaySchedule, WeeklyScheduleResponse, AuditLogEntry, AppStatusResponse, PagedAuditLogResponse } from '../services/settings.service';

export type DayKey = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

const DEFAULT_OPEN  = '08:00';
const DEFAULT_CLOSE = '22:00';

const DAYS: DayKey[] = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];

@Component({
  selector: 'app-working-hours',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatTabsModule,
    MatChipsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    TranslateModule,
  ],
  templateUrl: './working-hours.component.html',
  styleUrls: ['./working-hours.component.scss'],
})
export class WorkingHoursComponent implements OnInit {
  private svc       = inject(SettingsService);
  private toastr    = inject(ToastrService);
  private translate = inject(TranslateService);

  days = DAYS;
  schedule     = signal<DaySchedule[]>([]);
  loading      = signal(true);
  saving       = signal(false);
  auditLogs    = signal<AuditLogEntry[]>([]);
  totalLogs    = signal(0);             // totalElements retourné par le backend
  logsLoading  = signal(false);
  appStatus    = signal<AppStatusResponse | null>(null);

  // ── Audit filters (tout envoyé au backend) ───────────────────────────────
  filterAction = signal<string>('');
  filterAdmin  = signal<string>('');
  filterDate   = signal<string>('');    // YYYY-MM-DD
  pageIndex    = signal(0);
  pageSize     = signal(10);

  hasActiveFilters = computed(() =>
    !!this.filterAction() || !!this.filterAdmin() || !!this.filterDate()
  );

  openDaysCount = computed(() => this.schedule().filter(d => d.isOpen).length);

  totalPages = computed(() => Math.max(1, Math.ceil(this.totalLogs() / this.pageSize())));

  ngOnInit(): void {
    this.load();
    this.loadLogs();
    this.loadStatus();
  }

  load(): void {
    this.loading.set(true);
    this.svc.getWorkingHours().subscribe({
      next: (res) => {
        this.schedule.set(res.days ?? this.defaultSchedule());
        this.loading.set(false);
      },
      error: () => {
        this.schedule.set(this.defaultSchedule());
        this.loading.set(false);
      },
    });
  }

  loadLogs(): void {
    this.logsLoading.set(true);
    this.svc.getAuditLogs({
      action:    this.filterAction() || undefined,
      adminName: this.filterAdmin()  || undefined,
      date:      this.filterDate()   || undefined,
      page:      this.pageIndex(),
      size:      this.pageSize(),
    }).subscribe({
      next: (res: PagedAuditLogResponse) => {
        this.auditLogs.set(res.logs ?? []);
        this.totalLogs.set(res.totalElements ?? 0);
        this.logsLoading.set(false);
      },
      error: () => this.logsLoading.set(false),
    });
  }

  loadStatus(): void {
    this.svc.getAppStatus().subscribe({
      next: (s) => this.appStatus.set(s),
      error: () => {},
    });
  }

  dayFor(day: DayKey): DaySchedule {
    return this.schedule().find(d => d.dayOfWeek === (day as string)) ?? this.defaultDay(day);
  }

  toggleDay(day: DayKey): void {
    this.schedule.update(s => s.map(d => {
      if (d.dayOfWeek !== (day as string)) return d;
      const open = !d.isOpen;
      return {
        ...d,
        isOpen:    open,
        openTime:  open ? (d.openTime  ?? DEFAULT_OPEN)  : null,
        closeTime: open ? (d.closeTime ?? DEFAULT_CLOSE) : null,
      };
    }));
  }

  updateTime(day: DayKey, field: 'openTime' | 'closeTime', value: string): void {
    this.schedule.update(s => s.map(d =>
      d.dayOfWeek === (day as string) ? { ...d, [field]: value || null } : d
    ));
  }

  /** Apply same hours to all open days */
  applyToAll(day: DayKey): void {
    const src = this.dayFor(day);
    if (!src.isOpen) return;
    this.schedule.update(s => s.map(d =>
      d.isOpen ? { ...d, openTime: src.openTime, closeTime: src.closeTime } : d
    ));
    this.toastr.info(this.translate.instant('settings.workingHours.appliedToAll'));
  }

  save(): void {
    if (!this.isValid()) {
      this.toastr.warning(this.translate.instant('settings.workingHours.invalidTimes'));
      return;
    }
    this.saving.set(true);
    this.svc.updateWorkingHours({ days: this.schedule() }).subscribe({
      next: (res) => {
        this.schedule.set(res.days);
        this.saving.set(false);
        this.toastr.success(this.translate.instant('settings.workingHours.saveSuccess'));
        this.loadLogs();
        this.loadStatus(); // refresh live status after save
      },
      error: () => {
        this.saving.set(false);
        this.toastr.error(this.translate.instant('common.error'));
      },
    });
  }

  isValid(): boolean {
    return this.schedule().every(d => {
      if (!d.isOpen) return true;
      if (!d.openTime || !d.closeTime) return false;
      return d.openTime < d.closeTime;
    });
  }

  /** Duration string "Xh Ym" for a day */
  duration(day: DayKey): string {
    const d = this.dayFor(day);
    if (!d.isOpen || !d.openTime || !d.closeTime) return '';
    const [oh, om] = d.openTime.split(':').map(Number);
    const [ch, cm] = d.closeTime.split(':').map(Number);
    const mins = (ch * 60 + cm) - (oh * 60 + om);
    if (mins <= 0) return '';
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
  }

  /** Format ISO datetime to readable local string */
  formatDate(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  /** CSS class for action badge */
  actionClass(action: string): string {
    if (action === 'APP_ENABLED')               return 'badge--green';
    if (action === 'APP_DISABLED')              return 'badge--red';
    if (action === 'WORKING_HOURS_UPDATED')     return 'badge--blue';
    if (action === 'GENERAL_SETTINGS_UPDATED')  return 'badge--orange';
    return 'badge--grey';
  }

  /** Human-readable action label */
  actionLabel(action: string): string {
    const map: Record<string, string> = {
      APP_ENABLED:              this.translate.instant('settings.auditLog.actions.APP_ENABLED'),
      APP_DISABLED:             this.translate.instant('settings.auditLog.actions.APP_DISABLED'),
      WORKING_HOURS_UPDATED:    this.translate.instant('settings.auditLog.actions.WORKING_HOURS_UPDATED'),
      GENERAL_SETTINGS_UPDATED: this.translate.instant('settings.auditLog.actions.GENERAL_SETTINGS_UPDATED'),
    };
    return map[action] ?? action;
  }

  onFilterChange(field: 'action' | 'admin' | 'date', value: string): void {
    switch (field) {
      case 'action': this.filterAction.set(value); break;
      case 'admin':  this.filterAdmin.set(value);  break;
      case 'date':   this.filterDate.set(value);   break;
    }
    this.pageIndex.set(0);
    this.loadLogs();   // tout va au backend
  }

  clearFilters(): void {
    this.filterAction.set('');
    this.filterAdmin.set('');
    this.filterDate.set('');
    this.pageIndex.set(0);
    this.loadLogs();
  }

  goToPage(page: number): void {
    this.pageIndex.set(page);
    this.loadLogs();
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(Number(size));
    this.pageIndex.set(0);
    this.loadLogs();
  }

  private defaultDay(day: DayKey): DaySchedule {
    const weekDay = (['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'] as DayKey[]).includes(day);
    return { dayOfWeek: day as string, isOpen: weekDay, openTime: DEFAULT_OPEN, closeTime: DEFAULT_CLOSE };
  }

  private defaultSchedule(): DaySchedule[] {
    return DAYS.map(d => this.defaultDay(d));
  }
}
