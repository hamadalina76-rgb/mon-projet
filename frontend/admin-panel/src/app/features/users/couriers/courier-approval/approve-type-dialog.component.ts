import { Component, OnInit, ViewEncapsulation, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { CourierScheduleService } from '../services/courier-schedule.service';
import { ZonesService } from '../../../zones/services/zones.service';
import { Zone } from '../../../zones/models/zone.model';
import {
  ScheduleTemplateResponse,
  DayOfWeek,
  DAYS_OF_WEEK,
} from '../models/courier-schedule.model';

export interface ApproveTypeResult {
  courierType: 'INTERNAL' | 'EXTERNAL';
  templateId?: number;
  zoneIds: number[];
}

@Component({
  selector: 'app-approve-type-dialog',
  standalone: true,
  encapsulation: ViewEncapsulation.None,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatRippleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatExpansionModule,
    MatTabsModule,
    TranslateModule,
  ],
  templateUrl: './approve-type-dialog.component.html',
  styleUrls: ['./approve-type-dialog.component.scss'],
})
export class ApproveTypeDialogComponent implements OnInit {
  private dialogRef = inject(MatDialogRef<ApproveTypeDialogComponent>);
  private scheduleSvc = inject(CourierScheduleService);
  private zonesSvc = inject(ZonesService);

  selected       = signal<'INTERNAL' | 'EXTERNAL' | null>(null);
  templates      = signal<ScheduleTemplateResponse[]>([]);
  templatesLoading = signal(false);
  selectedTemplateId = signal<number | null>(null);
  previewTemplateId  = signal<number | null>(null);

  zones        = signal<Zone[]>([]);
  zonesLoading = signal(false);
  selectedZoneIds = signal<Set<number>>(new Set());

  readonly DAYS = DAYS_OF_WEEK;

  ngOnInit(): void {
    // Pre-load so they appear instantly when INTERNAL is clicked
    this.templatesLoading.set(true);
    this.scheduleSvc.getActiveTemplates().subscribe({
      next: (t) => { this.templates.set(t); this.templatesLoading.set(false); },
      error: ()  => { this.templatesLoading.set(false); },
    });

    // Load active zones from location-service
    this.zonesLoading.set(true);
    this.zonesSvc.getActiveZones().subscribe({
      next: (z: Zone[]) => { this.zones.set(z); this.zonesLoading.set(false); },
      error: ()         => { this.zonesLoading.set(false); },
    });
  }

  select(type: 'INTERNAL' | 'EXTERNAL'): void {
    this.selected.set(type);
    if (type === 'EXTERNAL') {
      this.selectedTemplateId.set(null);
      this.previewTemplateId.set(null);
    }
  }

  selectTemplate(id: number): void {
    this.selectedTemplateId.update(cur => cur === id ? null : id);
  }

  togglePreview(id: number, event: Event): void {
    event.stopPropagation();
    this.previewTemplateId.update(cur => cur === id ? null : id);
  }

  toggleZone(id: number): void {
    this.selectedZoneIds.update(cur => {
      const next = new Set(cur);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  isZoneSelected(id: number): boolean {
    return this.selectedZoneIds().has(id);
  }

  getActiveDays(tpl: ScheduleTemplateResponse): DayOfWeek[] {
    return this.DAYS.filter(d => tpl.days[d]?.length > 0);
  }

  hasBreak(tpl: ScheduleTemplateResponse, day: DayOfWeek): boolean {
    return (tpl.days[day] ?? []).some(s => s.breakStart);
  }

  onConfirm(): void {
    const type = this.selected();
    if (!type) return;
    if (this.selectedZoneIds().size === 0) return; // zones requises
    const result: ApproveTypeResult = {
      courierType: type,
      zoneIds: Array.from(this.selectedZoneIds()),
    };
    const tid = this.selectedTemplateId();
    if (type === 'INTERNAL' && tid) result.templateId = tid;
    this.dialogRef.close(result);
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
