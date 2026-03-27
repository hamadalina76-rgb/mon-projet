import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateModule } from '@ngx-translate/core';
import { ZonesService } from '../../../zones/services/zones.service';
import { Zone } from '../../../zones/models/zone.model';

export interface ChangeTypeDialogData {
  courierType: string | null;
  assignedZoneIds: number[];
}

export interface ChangeTypeResult {
  courierType: 'INTERNAL' | 'EXTERNAL';
  zoneIds: number[];
}

@Component({
  selector: 'app-change-type-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatRippleModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    TranslateModule,
  ],
  templateUrl: './change-type-dialog.component.html',
  styleUrl: './change-type-dialog.component.scss',
})
export class ChangeTypeDialogComponent implements OnInit {
  private dialogRef = inject(MatDialogRef<ChangeTypeDialogComponent>);
  private data: ChangeTypeDialogData = inject(MAT_DIALOG_DATA);
  private zonesSvc = inject(ZonesService);

  selected = signal<'INTERNAL' | 'EXTERNAL' | null>(this.data?.courierType as any ?? null);

  zones        = signal<Zone[]>([]);
  zonesLoading = signal(false);
  selectedZoneIds = signal<Set<number>>(new Set(this.data?.assignedZoneIds ?? []));

  ngOnInit(): void {
    this.zonesLoading.set(true);
    this.zonesSvc.getActiveZones().subscribe({
      next: (z: Zone[]) => { this.zones.set(z); this.zonesLoading.set(false); },
      error: ()          => { this.zonesLoading.set(false); },
    });
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

  onConfirm(): void {
    const type = this.selected();
    if (!type) return;
    const result: ChangeTypeResult = {
      courierType: type,
      zoneIds: Array.from(this.selectedZoneIds()),
    };
    this.dialogRef.close(result);
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}

