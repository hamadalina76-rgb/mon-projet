import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogData,
} from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { ZonesService } from '../services/zones.service';
import { Zone, ZoneType } from '../models/zone.model';

@Component({
  selector: 'app-zones-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatIconModule,
    MatTooltipModule,
    TranslateModule,
    ListPageComponent,
  ],
  templateUrl: './zones-list.component.html',
  styleUrls: ['./zones-list.component.scss'],
})
export class ZonesListComponent implements OnInit, OnDestroy {
  private zonesService = inject(ZonesService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  loading = signal(false);
  zones = signal<Zone[]>([]);
  exporting = signal(false);

  searchText = '';
  selectedStatus = 'all';
  itemsPerPage = 20;
  currentPage = 1;
  totalItems = 0;

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());
    this.loadZones();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadZones(): void {
    this.loading.set(true);
    const isActive =
      this.selectedStatus === 'all'
        ? undefined
        : this.selectedStatus === 'true';

    this.zonesService
      .getZones(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchText?.trim() || undefined,
        isActive
      )
      .subscribe({
        next: (response) => {
          this.zones.set(response.content || []);
          this.totalItems = response.totalElements ?? 0;
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.zones.set([]);
        },
      });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadZones();
  }

  onSearchChange(): void {
    this.searchInput$.next(this.searchText);
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadZones();
  }

  getZoneTypeLabel(type: ZoneType): string {
    const keys: Record<ZoneType, string> = {
      [ZoneType.DELIVERY]: 'zones.types.standard',
      [ZoneType.PREMIUM]: 'zones.types.premium',
      [ZoneType.RESTRICTED]: 'zones.types.restricted',
      [ZoneType.EXPRESS]: 'zones.types.express',
    };
    const key = keys[type] || 'zones.types.standard';
    const translated = this.translate.instant(key);
    return translated !== key ? translated : type;
  }

  getInternalAssignedCouriers(zone: Zone): number {
    if (typeof zone.internalAssignedCouriersCount === 'number') {
      return zone.internalAssignedCouriersCount;
    }
    return zone.internalCourierAssignments?.length ?? 0;
  }

  getExternalAssignedCouriers(zone: Zone): number {
    if (typeof zone.externalAssignedCouriersCount === 'number') {
      return zone.externalAssignedCouriersCount;
    }
    const zoneWithExternal = zone as Zone & { externalCourierAssignments?: unknown[] };
    return zoneWithExternal.externalCourierAssignments?.length ?? 0;
  }

  onDeleteZone(event: Event, zone: Zone): void {
    event.stopPropagation();
    event.preventDefault();
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('zones.editor.deleteZone'),
        message: this.translate.instant('zones.editor.deleteConfirm', { name: zone.name }),
        confirmLabel: this.translate.instant('common.delete'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'danger',
        icon: 'delete',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.zonesService.deleteZone(zone.id).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('zones.editor.deleteSuccess'));
            this.loadZones();
          },
          error: () => this.toastr.error(this.translate.instant('zones.editor.deleteError')),
        });
      }
    });
  }

  onExportZones(): void {
    this.exporting.set(true);
    this.zonesService.exportZones().subscribe({
      next: (geojson) => {
        if (!geojson || geojson.trim() === '') {
          this.toastr.warning('Aucune zone à exporter');
        } else {
          const blob = new Blob([geojson], { type: 'application/geo+json' });
          const a = document.createElement('a');
          a.href = URL.createObjectURL(blob);
          a.download = `zones-${new Date().toISOString().slice(0, 10)}.geojson`;
          a.click();
          URL.revokeObjectURL(a.href);
          try {
            const parsed = JSON.parse(geojson);
            const count = parsed?.features?.length ?? 0;
            this.toastr.success(`${count} zone(s) exportée(s)`);
          } catch {
            this.toastr.success('Export réussi');
          }
        }
        this.exporting.set(false);
      },
      error: () => {
        this.exporting.set(false);
        this.toastr.error('Erreur lors de l\'export');
      },
    });
  }

  onImportZones(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    input.value = '';
    const reader = new FileReader();
    reader.onload = () => {
      const geojson = reader.result as string;
      if (!geojson?.trim()) {
        this.toastr.error('Fichier vide');
        return;
      }
      try {
        JSON.parse(geojson);
      } catch {
        this.toastr.error('Fichier JSON invalide');
        return;
      }
      this.loading.set(true);
      this.zonesService.importZones(geojson).subscribe({
        next: (result) => {
          this.loading.set(false);
          this.loadZones();
          this.toastr.success(
            this.translate.instant('zones.editor.importSuccess', { count: result.created })
          );
          if (result.failed > 0) {
            this.toastr.warning(`${result.failed} zone(s) en échec`);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.toastr.error(err.error?.message || 'Erreur lors de l\'import');
        },
      });
    };
    reader.readAsText(file, 'UTF-8');
  }
}
