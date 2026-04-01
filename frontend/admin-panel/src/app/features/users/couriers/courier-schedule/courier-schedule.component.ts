import { Component, OnInit, ViewChild, inject, signal, computed } from '@angular/core';
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
  CourierExceptionalSchedule,
} from '../models/courier-schedule.model';

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
    TranslateModule, FullCalendarModule,
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
  weekOverride       = signal<CourierScheduleResponse | null>(null);
  templates          = signal<ScheduleTemplateResponse[]>([]);
  loading            = signal(true);
  saving             = signal(false);
  editMode           = signal(false);
  copyDaySource      = signal<DayOfWeek | null>(null);
  copyTargets        = signal<Set<DayOfWeek>>(new Set());
  selectedTemplateId = signal<number | null>(null);
  weekLabel          = signal<string>('');

  templateOptions = computed<ScheduleTemplateResponse[]>(() => {
    const activeTemplates = this.templates();
    const currentSchedule = this.schedule();
    if (!currentSchedule?.templateId) return activeTemplates;

    const existsInActiveList = activeTemplates.some((t) => t.id === currentSchedule.templateId);
    if (existsInActiveList) return activeTemplates;

    return [
      {
        id: currentSchedule.templateId,
        name: currentSchedule.templateName ?? `Template #${currentSchedule.templateId}`,
        isActive: false,
        description: undefined,
        days: currentSchedule.days,
        createdAt: undefined,
      },
      ...activeTemplates,
    ];
  });

  /** Map day → active exception (if any) for the currently viewed week */
  dayExceptionMap    = signal<Partial<Record<DayOfWeek, CourierExceptionalSchedule>>>({});

  /** Monday of the week currently displayed in FullCalendar */
  currentViewStart   = signal<Date>(new Date());

  /** All exceptions loaded for the year — persisted for week-navigation rebuilds */
  exceptionsCache    = signal<CourierExceptionalSchedule[]>([]);

  @ViewChild('calendarRef') private calendarRef!: FullCalendarComponent;

  readonly days = DAYS_OF_WEEK;
  form!: FormGroup;

  private scheduleForCurrentView(): CourierScheduleResponse | null {
    return this.weekOverride() ?? this.schedule();
  }

  // ── FullCalendar — base options (static, events updated separately) ───
  readonly calendarOptions: CalendarOptions = {
    plugins: [timeGridPlugin, interactionPlugin],
    initialView: 'timeGridWeek',
    locale: frLocale,
    headerToolbar: false,
    allDaySlot: false,
    slotMinTime: '00:00:00',
    slotMaxTime: '24:00:00',
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
      // Track the Monday of the currently viewed week so exception overlays
      // are anchored to the EXACT dates being displayed, not always today.
      const viewStart = new Date(info.start);
      this.currentViewStart.set(viewStart);
      this.loadWeekOverrideForView(viewStart);
      this._rebuildExceptionMapForView(viewStart);
      this.refreshCalendarWithExceptions();
    },
    eventDisplay: 'block',
    editable: false,
    eventStartEditable: false,
    eventDurationEditable: false,
    eventDidMount: (info: any) => {
      if (!info?.event?.classNames?.includes('cs-exception-block')) return;
      const harness = info.el?.parentElement as HTMLElement | null;
      if (harness) {
        harness.style.left = '0px';
        harness.style.right = '0px';
        harness.style.zIndex = '6';
      }
    },
    eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
    eventBorderColor: 'transparent',
    events: [],
  };

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.courierId.set(this.route.snapshot.paramMap.get('id') ?? '');
    this.svc.getActiveTemplates().subscribe({ next: (t) => this.templates.set(t) });
    this.loadSchedule();
    this.loadExceptionsForCurrentWeek();
  }

  loadSchedule(): void {
    this.loading.set(true);
    this.svc.getCourierSchedule(this.courierId()).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.selectedTemplateId.set(s.templateId ?? null);
        this.loadWeekOverrideForView(this.currentViewStart());
        this.buildForm(s);
        this.refreshCalendarWithExceptions();
        this.loading.set(false);
      },
      error: () => {
        this.schedule.set(null);
        this.weekOverride.set(null);
        this.selectedTemplateId.set(null);
        this.buildForm(null);
        this.loading.set(false);
      },
    });
  }

  private toIsoDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private loadWeekOverrideForView(viewStart: Date): void {
    const target = this.toIsoDate(viewStart);
    this.svc.getAllSchedules(this.courierId()).subscribe({
      next: (all) => {
        const override = (all ?? []).find((s) => s.weekStartDate === target) ?? null;
        this.weekOverride.set(override);
        this.refreshCalendarWithExceptions();
      },
      error: () => {
        this.weekOverride.set(null);
      },
    });
  }

  /** Load exceptions covering a wide range, then map each day-of-week to active exceptions */
  loadExceptionsForCurrentWeek(): void {
    const now  = new Date();
    const from = new Date(now.getFullYear(), 0, 1).toISOString().slice(0, 10);
    const to   = new Date(now.getFullYear() + 1, 11, 31).toISOString().slice(0, 10);
    this.svc.getExceptionalSchedulesByCourier(this.courierId(), from, to).subscribe({
      next: (exceptions) => this.buildDayExceptionMap(exceptions),
    });
  }

  /** For each day of the currently viewed week, check if that exact date falls within an exception */
  buildDayExceptionMap(exceptions: CourierExceptionalSchedule[]): void {
    this.exceptionsCache.set(exceptions);
    this._rebuildExceptionMapForView(this.currentViewStart());
    this.refreshCalendarWithExceptions();
  }

  /**
   * Rebuilds the day→exception map for the week that starts on `viewStart`.
   * Only dates that are EXPLICITLY covered by an exception record are flagged —
   * there is no day-of-week extrapolation to other weeks.
   */
  private _rebuildExceptionMapForView(viewStart: Date): void {
    const exceptions = this.exceptionsCache();
    const map: Partial<Record<DayOfWeek, CourierExceptionalSchedule>> = {};
    this.days.forEach((day, idx) => {
      const d = new Date(viewStart);
      d.setDate(viewStart.getDate() + idx);
      
      // Timezone-safe ISO date (YYYY-MM-DD)
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const dayNum = String(d.getDate()).padStart(2, '0');
      const iso = `${y}-${m}-${dayNum}`;

      const match = exceptions.find(ex => iso >= ex.startDate && iso <= ex.endDate);
      if (match) map[day] = match;
    });
    this.dayExceptionMap.set(map);
  }

  /** Returns the exception covering a specific day, or undefined */
  exceptionForDay(day: DayOfWeek): CourierExceptionalSchedule | undefined {
    return this.dayExceptionMap()[day];
  }

  /** Returns true if the exception for this day is partial (has specific hours, not full day) */
  isPartialException(day: DayOfWeek): boolean {
    const ex = this.exceptionForDay(day);
    if (!ex) return false;
    return !!(ex.startsAt && ex.endsAt && ex.startsAt.length >= 16 && ex.endsAt.length >= 16);
  }

  /** True if at least one day this week has an active exception */
  hasExceptionsThisWeek(): boolean {
    return Object.keys(this.dayExceptionMap()).length > 0;
  }

  // ── FullCalendar ──────────────────────────────────────────────────────

  /**
   * Builds shift events for a single specific ISO date.
   * Returns date-anchored events (no daysOfWeek), so they only appear on
   * that exact date in the calendar view.
   */
  private _shiftEventsForDate(day: DayOfWeek, isoDate: string, shifts: ShiftDTO[]): EventInput[] {
    const events: EventInput[] = [];
    const color = DAY_COLORS[day];
    shifts.forEach((shift, idx) => {
      if (shift.breakStart && shift.breakEnd) {
        events.push({
          id: `${day}-${idx}-a`,
          title: `${shift.startTime} – ${shift.breakStart}`,
          start: `${isoDate}T${shift.startTime}:00`,
          end:   `${isoDate}T${shift.breakStart}:00`,
          backgroundColor: color,
          borderColor: color,
        });
        events.push({
          id: `${day}-${idx}-break`,
          title: `☕ Pause`,
          start: `${isoDate}T${shift.breakStart}:00`,
          end:   `${isoDate}T${shift.breakEnd}:00`,
          backgroundColor: '#F1F5F9',
          borderColor: '#CBD5E1',
          textColor: '#64748B',
          classNames: ['cs-break-event'],
        });
        events.push({
          id: `${day}-${idx}-b`,
          title: `${shift.breakEnd} – ${shift.endTime}`,
          start: `${isoDate}T${shift.breakEnd}:00`,
          end:   `${isoDate}T${shift.endTime}:00`,
          backgroundColor: color,
          borderColor: color,
        });
      } else {
        events.push({
          id: `${day}-${idx}`,
          title: `${shift.startTime} – ${shift.endTime}`,
          start: `${isoDate}T${shift.startTime}:00`,
          end:   `${isoDate}T${shift.endTime}:00`,
          backgroundColor: color,
          borderColor: color,
        });
      }
    });
    return events;
  }

  /**
   * Rebuilds ALL calendar events for the currently viewed week.
   * Each day is rendered using its exact ISO date (not daysOfWeek), so:
   * - exception overlays appear ONLY on the selected exception dates;
   * - shift blocks appear on every other day of this specific week;
   * - nothing bleeds into other weeks.
   */
  refreshCalendarWithExceptions(): void {
    const s      = this.scheduleForCurrentView();
    const exMap  = this.dayExceptionMap();
    const monday = this.currentViewStart();
    const events: EventInput[] = [];

    this.days.forEach((day, idx) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + idx);
      
      // Timezone-safe ISO date (YYYY-MM-DD)
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const dayNum = String(d.getDate()).padStart(2, '0');
      const isoDate = `${y}-${m}-${dayNum}`;

      const ex = exMap[day];

      if (ex) {
        // Determine if the exception has specific hours (partial day)
        const hasTime = ex.startsAt && ex.endsAt
          && ex.startsAt.length >= 16 && ex.endsAt.length >= 16;

        const exStartTime = hasTime ? ex.startsAt!.substring(11, 16) : '00:00';
        const exEndTime   = hasTime ? ex.endsAt!.substring(11, 16)   : '23:59';

        // Exception background overlay
        events.push({
          id: `exception-bg-${day}`,
          start: `${isoDate}T${exStartTime}:00`,
          end:   `${isoDate}T${exEndTime}:59`,
          display: 'background',
          backgroundColor: 'rgba(148,163,184,0.35)',
          classNames: ['cs-exception-bg'],
        });

        const typeLabel = this.translate.instant('exceptional.type_' + ex.exceptionType);
        events.push({
          id: `exception-block-${day}`,
          title: `${typeLabel}\n${ex.label}`,
          start: `${isoDate}T${exStartTime}:00`,
          end:   `${isoDate}T${exEndTime}:59`,
          backgroundColor: 'rgba(100,116,139,0.75)',
          borderColor: '#475569',
          textColor: '#ffffff',
          classNames: ['cs-exception-block'],
        });

        // For partial-day exceptions, also render normal shifts outside the blocked hours
        if (hasTime && s) {
          events.push(...this._shiftEventsForDate(day, isoDate, s.days[day] ?? []));
        }
      } else if (s) {
        // Normal date: render the courier's shifts for this exact day
        events.push(...this._shiftEventsForDate(day, isoDate, s.days[day] ?? []));
      }
    });

    (this.calendarOptions as any).events = events;
    const calApi = this.calendarRef?.getApi();
    if (calApi) {
      calApi.removeAllEvents();
      events.forEach(e => calApi.addEvent(e));
    }
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
    const now = new Date();
    const effectiveDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const y = effectiveDate.getFullYear();
    const m = String(effectiveDate.getMonth() + 1).padStart(2, '0');
    const d = String(effectiveDate.getDate()).padStart(2, '0');
    const effectiveFrom = `${y}-${m}-${d}`;

    this.saving.set(true);
    this.svc.applyTemplate(this.courierId(), templateId, effectiveFrom).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.selectedTemplateId.set(s.templateId ?? null);
        this.loadWeekOverrideForView(this.currentViewStart());
        this.buildForm(s);
        this.refreshCalendarWithExceptions();
        this.editMode.set(false);
        this.saving.set(false);
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
        this.selectedTemplateId.set(s.templateId ?? null);
        this.buildForm(s);
        this.refreshCalendarWithExceptions();
        this.cancelCopy();
        this.saving.set(false);
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

    // Timezone-safe ISO date for the current week start (Monday)
    const monday = this.currentViewStart();
    const y = monday.getFullYear();
    const m = String(monday.getMonth() + 1).padStart(2, '0');
    const d = String(monday.getDate()).padStart(2, '0');
    const isoMonday = `${y}-${m}-${d}`;

    const req: CourierScheduleSaveRequest = {
      isPermanent: this.form.value.isPermanent,
      weekStartDate: this.form.value.isPermanent ? undefined : isoMonday,
      days: daysPayload as any,
    };

    this.svc.saveCourierSchedule(this.courierId(), req).subscribe({
      next: (s) => {
        this.schedule.set(s);
        this.selectedTemplateId.set(s.templateId ?? null);
        this.buildForm(s);
        this.refreshCalendarWithExceptions();
        this.editMode.set(false);
        this.saving.set(false);
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
    const s = this.scheduleForCurrentView();
    if (!s) return 0;
    return Object.values(s.days).reduce((n, arr) => n + arr.length, 0);
  }

  activeDayCount(): number {
    const s = this.scheduleForCurrentView();
    if (!s) return 0;
    return Object.values(s.days).filter(arr => arr.length > 0).length;
  }
}

