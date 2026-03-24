import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { ToastrService } from 'ngx-toastr';

import { FullCalendarModule, FullCalendarComponent } from '@fullcalendar/angular';
import { CalendarOptions, EventInput } from '@fullcalendar/core';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import frLocale from '@fullcalendar/core/locales/fr';

import { CourierScheduleService } from '../services/courier-schedule.service';
import {
  CourierScheduleResponse,
  CourierScheduleSaveRequest,
  ScheduleTemplateResponse,
  DayOfWeek,
  DAYS_OF_WEEK,
  ShiftDTO,
  CourierScheduleAuditLog,
  AuditLogPageResponse,
} from '../models/courier-schedule.model';

// ── FullCalendar: DayOfWeek → daysOfWeek index (0 = Sunday) ─────────────────
const FC_DAY_INDEX: Record<DayOfWeek, number> = {
  SUNDAY: 0, MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3,
  THURSDAY: 4, FRIDAY: 5, SATURDAY: 6,
};

// ── One distinct colour per day ──────────────────────────────────────────────
const DAY_COLORS: Record<DayOfWeek, string> = {
  MONDAY: '#E31E24', TUESDAY: '#7C3AED', WEDNESDAY: '#D97706',
  THURSDAY: '#059669', FRIDAY: '#DB2777',
  SATURDAY: '#EA580C', SUNDAY: '#64748B',
};

@Component({
  selector: 'app-courier-schedule',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatTooltipModule,
    MatCheckboxModule, MatDividerModule, MatTabsModule,
    TranslateModule, FullCalendarModule, ListPageComponent,
  ],
  templateUrl: './courier-schedule.component.html',
  styleUrls: ['./courier-schedule.component.scss'],
})
export class CourierScheduleComponent implements OnInit {

  // ── DI ────────────────────────────────────────────────────────────────
  private route     = inject(ActivatedRoute);
  private svc       = inject(CourierScheduleService);
  private fb        = inject(FormBuilder);
  private toastr    = inject(ToastrService);
  private translate = inject(TranslateService);

  // ── State ─────────────────────────────────────────────────────────────
  courierId          = signal('');
  schedule           = signal<CourierScheduleResponse | null>(null);
  templates          = signal<ScheduleTemplateResponse[]>([]);
  loading            = signal(true);
  saving             = signal(false);
  editMode           = signal(false);
  copyDaySource      = signal<DayOfWeek | null>(null);
  copyTargets        = signal<Set<DayOfWeek>>(new Set());
  selectedTemplateId = signal<number | null>(null);
  weekLabel          = signal<string>('');

  // ── Audit log (traceability) ──────────────────────────────────────────
  auditEntries       = signal<CourierScheduleAuditLog[]>([]);
  auditTotalItems    = signal(0);
  auditLoading       = signal(false);
  auditPage          = signal(1);
  auditPageSize      = signal(10);
  auditFilterAction  = signal('');
  auditFilterDate    = signal('');

  readonly auditActionConfig: Record<string, { icon: string; cssClass: string; labelKey: string }> = {
    CREATED:          { icon: 'add_circle',    cssClass: 'cs-audit-action--created',  labelKey: 'schedule.audit.CREATED' },
    UPDATED:          { icon: 'edit',          cssClass: 'cs-audit-action--updated',  labelKey: 'schedule.audit.UPDATED' },
    TEMPLATE_APPLIED: { icon: 'auto_fix_high', cssClass: 'cs-audit-action--template', labelKey: 'schedule.audit.TEMPLATE_APPLIED' },
    DAY_COPIED:       { icon: 'content_copy',  cssClass: 'cs-audit-action--copied',   labelKey: 'schedule.audit.DAY_COPIED' },
    DELETED:          { icon: 'delete',        cssClass: 'cs-audit-action--deleted',  labelKey: 'schedule.audit.DELETED' },
  };

  @ViewChild('calendarRef') private calendarRef!: FullCalendarComponent;

  readonly days = DAYS_OF_WEEK;
  form!: FormGroup;

  // ── FullCalendar — base options (static, events updated separately) ───
  readonly calendarOptions: CalendarOptions = {
    plugins: [timeGridPlugin, interactionPlugin],
    initialView: 'timeGridWeek',
    locale: frLocale,
    headerToolbar: false,
    allDaySlot: false,
    slotMinTime: '05:00:00',
    slotMaxTime: '23:00:00',
    slotDuration: '00:30:00',
    slotLabelInterval: '01:00',
    height: 'auto',
    firstDay: 1,
    expandRows: true,
    nowIndicator: true,
    dayHeaderContent: (args: any) => {
      const lang = this.translate?.currentLang === 'fr' ? 'fr-FR' : 'en-US';
      const weekday = args.date.toLocaleDateString(lang, { weekday: 'short' })
        .replace(/\.$/, '').toUpperCase();
      const dayNum = args.date.getDate();
      return {
        html: `<div class="cs-col-hdr">
          <span class="cs-col-hdr__day">${weekday}</span>
          <span class="cs-col-hdr__num${args.isToday ? ' is-today' : ''}">${dayNum}</span>
        </div>`,
      };
    },
    datesSet: (info: any) => {
      this.weekLabel.set(this.formatWeekLabel(info.start, info.end));
    },
    eventDisplay: 'block',
    eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
    eventBorderColor: 'transparent',
    events: [],
  };

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.courierId.set(this.route.snapshot.paramMap.get('id') ?? '');
    this.svc.getActiveTemplates().subscribe({ next: (t) => this.templates.set(t) });
    this.loadSchedule();
    this.loadAuditLogs();
  }

  loadAuditLogs(): void {
    this.auditLoading.set(true);
    const action = this.auditFilterAction() || undefined;
    const date   = this.auditFilterDate()   || undefined;
    this.svc.getAuditLogs(this.courierId(), this.auditPage() - 1, this.auditPageSize(), action, date, date).subscribe({
      next: (r: AuditLogPageResponse) => {
        this.auditEntries.set(r.content ?? []);
        this.auditTotalItems.set(r.totalElements ?? 0);
        this.auditLoading.set(false);
      },
      error: () => this.auditLoading.set(false),
    });
  }

  onAuditFilterChange(): void {
    this.auditPage.set(1);
    this.loadAuditLogs();
  }

  onAuditPageChange(event: { page: number; pageSize: number }): void {
    this.auditPage.set(event.page);
    this.auditPageSize.set(event.pageSize);
    this.loadAuditLogs();
  }

  resetAuditFilters(): void {
    this.auditFilterAction.set('');
    this.auditFilterDate.set('');
    this.auditPage.set(1);
    this.loadAuditLogs();
  }

  loadSchedule(): void {
    this.loading.set(true);
    this.svc.getCourierSchedule(this.courierId()).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.updateCalendarEvents(s);
        this.buildForm(s);
        this.loading.set(false);
      },
      error: () => {
        this.schedule.set(null);
        this.buildForm(null);
        this.loading.set(false);
      },
    });
  }

  // ── FullCalendar ──────────────────────────────────────────────────────
  updateCalendarEvents(s: CourierScheduleResponse | null): void {
    const events: EventInput[] = [];
    if (s) {
      (Object.entries(s.days) as [DayOfWeek, ShiftDTO[]][]).forEach(([day, shifts]) => {
        shifts.forEach((shift, idx) => {
          const color = DAY_COLORS[day];
          const dow   = [FC_DAY_INDEX[day]];

          if (shift.breakStart && shift.breakEnd) {
            // Before break
            events.push({
              id: `${day}-${idx}-a`,
              title: `${shift.startTime} – ${shift.breakStart}`,
              startTime: `${shift.startTime}:00`,
              endTime:   `${shift.breakStart}:00`,
              daysOfWeek: dow,
              backgroundColor: color,
              borderColor: color,
            });
            // Break slot (lighter, dashed)
            events.push({
              id: `${day}-${idx}-break`,
              title: `☕ Pause`,
              startTime: `${shift.breakStart}:00`,
              endTime:   `${shift.breakEnd}:00`,
              daysOfWeek: dow,
              backgroundColor: '#F1F5F9',
              borderColor: '#CBD5E1',
              textColor: '#64748B',
              classNames: ['cs-break-event'],
            });
            // After break
            events.push({
              id: `${day}-${idx}-b`,
              title: `${shift.breakEnd} – ${shift.endTime}`,
              startTime: `${shift.breakEnd}:00`,
              endTime:   `${shift.endTime}:00`,
              daysOfWeek: dow,
              backgroundColor: color,
              borderColor: color,
            });
          } else {
            events.push({
              id: `${day}-${idx}`,
              title: `${shift.startTime} – ${shift.endTime}`,
              startTime: `${shift.startTime}:00`,
              endTime:   `${shift.endTime}:00`,
              daysOfWeek: dow,
              backgroundColor: color,
              borderColor: color,
            });
          }
        });
      });
    }
    (this.calendarOptions as any).events = events;
  }

  // ── Reactive Form ─────────────────────────────────────────────────────
  buildForm(s: CourierScheduleResponse | null): void {
    this.form = this.fb.group({ isPermanent: [s?.isPermanent ?? true] });
    this.days.forEach(day => {
      const shifts = s?.days?.[day] ?? [];
      this.form.addControl(
        day,
        this.fb.array(shifts.map(sh => this.mkShift(sh.startTime, sh.endTime, sh.breakStart ?? '', sh.breakEnd ?? '')))
      );
    });
  }

  mkShift(start = '', end = '', breakStart = '', breakEnd = ''): FormGroup {
    return this.fb.group({
      startTime:  [start, Validators.required],
      endTime:    [end,   Validators.required],
      breakStart: [breakStart],
      breakEnd:   [breakEnd],
    });
  }

  shiftsFor(day: DayOfWeek): FormArray { return this.form.get(day) as FormArray; }
  addShift(day: DayOfWeek): void       { this.shiftsFor(day).push(this.mkShift()); }
  removeShift(day: DayOfWeek, i: number): void { this.shiftsFor(day).removeAt(i); }

  // ── Apply Template ────────────────────────────────────────────────────
  applyTemplate(templateId: number): void {
    if (!templateId) return;
    this.saving.set(true);
    this.svc.applyTemplate(this.courierId(), templateId).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.updateCalendarEvents(s);
        this.buildForm(s);
        this.editMode.set(false);
        this.saving.set(false);
        this.loadAuditLogs();
        this.toastr.success(this.translate.instant('schedule.applySuccess'));
      },
      error: (err) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
        this.saving.set(false);
      },
    });
  }

  // ── Copy Day ──────────────────────────────────────────────────────────
  toggleCopyTarget(day: DayOfWeek): void {
    const s = new Set(this.copyTargets());
    s.has(day) ? s.delete(day) : s.add(day);
    this.copyTargets.set(s);
  }

  isCopyTarget(d: DayOfWeek): boolean { return this.copyTargets().has(d); }

  confirmCopyDay(): void {
    const source  = this.copyDaySource();
    const targets = [...this.copyTargets()];
    if (!source || !targets.length) return;
    this.saving.set(true);
    this.svc.copyDay(this.courierId(), { sourceDay: source, targetDays: targets }).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.updateCalendarEvents(s);
        this.buildForm(s);
        this.cancelCopy();
        this.saving.set(false);
        this.loadAuditLogs();
        this.toastr.success(this.translate.instant('schedule.copySuccess'));
      },
      error: (err) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
        this.saving.set(false);
      },
    });
  }

  cancelCopy(): void {
    this.copyDaySource.set(null);
    this.copyTargets.set(new Set());
  }

  // ── Save Manual Edits ─────────────────────────────────────────────────
  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);

    const daysPayload: Record<string, any[]> = {};
    this.days.forEach(day => {
      const shifts = this.shiftsFor(day).value;
      if (shifts.length) daysPayload[day] = shifts;
    });

    const req: CourierScheduleSaveRequest = {
      isPermanent: this.form.value.isPermanent,
      days: daysPayload as any,
    };

    this.svc.saveCourierSchedule(this.courierId(), req).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.updateCalendarEvents(s);
        this.buildForm(s);
        this.editMode.set(false);
        this.saving.set(false);
        this.loadAuditLogs();
        this.toastr.success(this.translate.instant('schedule.saveSuccess'));
      },
      error: (err) => {
        this.toastr.error(err?.error?.message ?? this.translate.instant('common.error'));
        this.saving.set(false);
      },
    });
  }

  cancelEdit(): void {
    this.editMode.set(false);
    this.buildForm(this.schedule());
  }

  // ── Week Navigation ───────────────────────────────────────────
  prevWeek(): void { this.calendarRef?.getApi().prev(); }
  nextWeek(): void { this.calendarRef?.getApi().next(); }
  goToday(): void  { this.calendarRef?.getApi().today(); }

  private formatWeekLabel(start: Date, end: Date): string {
    const lang   = this.translate.currentLang || 'fr';
    const locale = lang === 'fr' ? 'fr-FR' : 'en-US';
    const lastDay = new Date(end);
    lastDay.setDate(lastDay.getDate() - 1);
    const startStr = start.toLocaleDateString(locale, { day: 'numeric', month: 'long' });
    const endStr   = lastDay.toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' });
    return `${startStr} – ${endStr}`;
  }

  // ── Utils ─────────────────────────────────────────────────────────────
  dayLabel(day: DayOfWeek): string { return this.translate.instant(`schedule.day.${day}`); }
  dayShort(day: DayOfWeek): string { return this.translate.instant(`schedule.short.${day}`); }
  dayColor(day: DayOfWeek): string { return DAY_COLORS[day]; }

  totalShifts(): number {
    const s = this.schedule();
    if (!s) return 0;
    return Object.values(s.days).reduce((n, arr) => n + arr.length, 0);
  }

  activeDayCount(): number {
    const s = this.schedule();
    if (!s) return 0;
    return Object.values(s.days).filter(arr => arr.length > 0).length;
  }
}

