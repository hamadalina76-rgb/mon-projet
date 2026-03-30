import { Component, OnInit, OnDestroy, AfterViewInit, inject, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { ToastrService } from 'ngx-toastr';
import { CouriersService } from '../services/couriers.service';
import { RejectDialogComponent } from '../../../partners/partner-approval/reject-dialog.component';
import { RequestMoreInfoDialogComponent } from '../../../partners/partner-approval/request-more-info-dialog.component';
import { ApproveTypeDialogComponent, ApproveTypeResult } from '../courier-approval/approve-type-dialog.component';
import { CourierScheduleService } from '../services/courier-schedule.service';
import { ZonesService } from '../../../zones/services/zones.service';
import { Zone } from '../../../zones/models/zone.model';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { environment } from '@environments/environment';

declare const mapboxgl: any;

@Component({
  selector: 'app-courier-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatSelectModule,
    MatInputModule,
    MatFormFieldModule,
    TranslateModule,
    ListPageComponent,
  ],
  templateUrl: './courier-detail.component.html',
  styleUrls: ['./courier-detail.component.scss'],
})
export class CourierDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private couriersService = inject(CouriersService);
  private scheduleSvc = inject(CourierScheduleService);
  private zonesSvc = inject(ZonesService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);

  @ViewChild('zonesMapContainer', { static: false }) zonesMapContainer!: ElementRef;
  private zonesMap: any = null;

  courier = signal<any>(null);
  loading = signal(true);
  actionLoading = signal(false);
  uploadsBaseUrl = environment.uploadsBaseUrl || '';

  // ─── Zone signals ──────────────────────────────────────────────
  allZones           = signal<Zone[]>([]);
  assignedZoneIds    = signal<number[]>([]);
  zonesLoading       = signal(false);
  zonesEditMode      = signal(false);
  editingZoneIds     = signal<number[]>([]);

  assignedZones = computed<Zone[]>(() => {
    const ids = new Set(this.assignedZoneIds());
    return this.allZones().filter(z => ids.has(z.id));
  });

  // ─── Audit log (journal d'audit) ──────────────────────────────────
  auditEntries    = signal<any[]>([]);
  auditTotalItems = signal(0);
  auditLoading    = signal(false);
  auditPage       = signal(1);
  auditPageSize   = signal(10);
  auditFilterAction = signal('');
  auditFilterDate   = signal('');

  readonly auditActionConfig: Partial<Record<string, { icon: string; cssClass: string; labelKey: string }>> = {
    APPROVE:            { icon: 'check_circle',  cssClass: 'cl-audit-action--approve',    labelKey: 'users.couriers.audit.APPROVE' },
    REJECT:             { icon: 'cancel',        cssClass: 'cl-audit-action--reject',     labelKey: 'users.couriers.audit.REJECT' },
    SUSPEND:            { icon: 'gpp_bad',       cssClass: 'cl-audit-action--suspend',    labelKey: 'users.couriers.audit.SUSPEND' },
    DEACTIVATE:         { icon: 'block',         cssClass: 'cl-audit-action--deactivate', labelKey: 'users.couriers.audit.DEACTIVATE' },
    ACTIVATE:           { icon: 'check_circle',  cssClass: 'cl-audit-action--activate',   labelKey: 'users.couriers.audit.ACTIVATE' },
    REQUEST_MORE_INFO:  { icon: 'info',          cssClass: 'cl-audit-action--info',       labelKey: 'users.couriers.audit.REQUEST_MORE_INFO' },
    ASSIGN_ZONES:       { icon: 'map',           cssClass: 'cl-audit-action--zones',      labelKey: 'users.couriers.audit.ASSIGN_ZONES' },
    CHANGE_TYPE:        { icon: 'swap_horiz',    cssClass: 'cl-audit-action--type',       labelKey: 'users.couriers.audit.CHANGE_TYPE' },
    SCHEDULE_UPDATED:           { icon: 'edit_calendar',  cssClass: 'cl-audit-action--schedule',  labelKey: 'users.couriers.audit.SCHEDULE_UPDATED' },
    SCHEDULE_TEMPLATE_APPLIED:  { icon: 'content_copy',   cssClass: 'cl-audit-action--schedule',  labelKey: 'users.couriers.audit.SCHEDULE_TEMPLATE_APPLIED' },
    SCHEDULE_DAY_COPIED:        { icon: 'copy_all',       cssClass: 'cl-audit-action--schedule',  labelKey: 'users.couriers.audit.SCHEDULE_DAY_COPIED' },
    SCHEDULE_DELETED:           { icon: 'delete_sweep',   cssClass: 'cl-audit-action--schedule',  labelKey: 'users.couriers.audit.SCHEDULE_DELETED' },
    VEHICLE_UPDATED:            { icon: 'directions_car', cssClass: 'cl-audit-action--vehicle',   labelKey: 'users.couriers.audit.VEHICLE_UPDATED' },
    DELIVERY_ZONE_UPDATED:      { icon: 'explore',        cssClass: 'cl-audit-action--zone-pref', labelKey: 'users.couriers.audit.DELIVERY_ZONE_UPDATED' },
    BANK_INFO_UPDATED:          { icon: 'account_balance',cssClass: 'cl-audit-action--bank',      labelKey: 'users.couriers.audit.BANK_INFO_UPDATED' },
    DOCUMENTS_UPDATED:          { icon: 'description',    cssClass: 'cl-audit-action--docs',      labelKey: 'users.couriers.audit.DOCUMENTS_UPDATED' },
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadCourier(id);
  }

  loadCourier(id: string): void {
    this.loading.set(true);
    this.couriersService.getCourier(id).subscribe({
      next: (data) => {
        this.courier.set(data);
        this.assignedZoneIds.set(data.assignedZoneIds ?? []);
        this.loading.set(false);
        this.loadZonesData();
        this.loadAuditLogs();
      },
      error: () => this.loading.set(false),
    });
  }

  loadZonesData(): void {
    this.zonesLoading.set(true);
    this.zonesSvc.getActiveZones().subscribe({
      next: (zones: Zone[]) => { this.allZones.set(zones); this.zonesLoading.set(false); setTimeout(() => this.initZonesMap(), 300); },
      error: ()              => { this.zonesLoading.set(false); },
    });
  }

  isZoneAssigned(zoneId: number): boolean {
    return this.editingZoneIds().includes(zoneId);
  }

  toggleZoneAssignment(zoneId: number): void {
    this.editingZoneIds.update(ids =>
      ids.includes(zoneId) ? ids.filter(i => i !== zoneId) : [...ids, zoneId]
    );
    setTimeout(() => this.initZonesMap(), 100);
  }

  enterZonesEditMode(): void {
    this.editingZoneIds.set([...this.assignedZoneIds()]);
    this.zonesEditMode.set(true);
  }

  cancelZonesEdit(): void {
    this.zonesEditMode.set(false);
    this.editingZoneIds.set([]);
    setTimeout(() => this.initZonesMap(), 150);
  }

  saveZones(): void {
    const c = this.courier();
    if (!c) return;
    this.zonesLoading.set(true);
    this.couriersService.assignZones(String(c.id), this.editingZoneIds()).subscribe({
      next: (updated) => {
        this.assignedZoneIds.set(updated.assignedZoneIds ?? this.editingZoneIds());
        this.zonesEditMode.set(false);
        this.editingZoneIds.set([]);
        this.zonesLoading.set(false);
        this.toastr.success(this.translate.instant('users.couriers.zonesUpdated'));
        setTimeout(() => this.initZonesMap(), 300);
      },
      error: () => {
        this.zonesLoading.set(false);
        this.toastr.error(this.translate.instant('common.error'));
      },
    });
  }

  ngOnDestroy(): void {
    if (this.zonesMap) { this.zonesMap.remove(); this.zonesMap = null; }
  }

  private initZonesMap(): void {
    const assignedIds = this.zonesEditMode() ? this.editingZoneIds() : this.assignedZoneIds();
    if (assignedIds.length === 0 || !this.zonesMapContainer?.nativeElement) return;

    const token = (environment as any).mapboxToken;
    if (typeof mapboxgl === 'undefined' || !token) return;

    try {
      if (this.zonesMap) { this.zonesMap.remove(); this.zonesMap = null; }
      mapboxgl.accessToken = token;
      const zones = this.allZones().filter(z => assignedIds.includes(z.id));

      this.zonesMap = new mapboxgl.Map({
        container: this.zonesMapContainer.nativeElement,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [10.1815, 36.8065],
        zoom: 11,
      });

      this.zonesMap.on('load', () => {
        zones.forEach((zone) => {
          if (!zone.boundaryJson) return;
          try {
            const geojson = JSON.parse(zone.boundaryJson);
            const sourceId = `zone-${zone.id}`;
            this.zonesMap.addSource(sourceId, { type: 'geojson', data: { type: 'Feature', geometry: geojson, properties: {} } });
            this.zonesMap.addLayer({ id: `${sourceId}-fill`, type: 'fill', source: sourceId, paint: { 'fill-color': '#3b82f6', 'fill-opacity': 0.2 } });
            this.zonesMap.addLayer({ id: `${sourceId}-line`, type: 'line', source: sourceId, paint: { 'line-color': '#2563eb', 'line-width': 2 } });
          } catch {}
        });
        this.zonesMap.addControl(new mapboxgl.NavigationControl(), 'top-right');
        setTimeout(() => this.zonesMap?.resize(), 100);
      });
    } catch (e) { console.warn('Zones map init error:', e); }
  }

  getFullName(c: any): string {
    if (!c) return '-';
    return [c.firstName, c.lastName].filter(Boolean).join(' ') || c.email || '-';
  }

  getCourierTypeLabel(type: string): string {
    if (type === 'INTERNAL') return this.translate.instant('users.couriers.internal');
    if (type === 'EXTERNAL') return this.translate.instant('users.couriers.external');
    return '—';
  }

  getStatusLabel(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'PENDING_APPROVAL') return this.translate.instant('users.couriers.status.pending');
    if (s === 'ACTIVE') return this.translate.instant('users.couriers.status.approved');
    if (s === 'REJECTED') return this.translate.instant('users.couriers.status.rejected');
    if (s === 'SUSPENDED') return this.translate.instant('users.couriers.status.blocked');
    if (s === 'DEACTIVATED') return this.translate.instant('users.couriers.status.deactivated');
    return status || '-';
  }

  /** True si le livreur peut être désactivé (Block). */
  canDeactivate(c: any): boolean {
    const s = (c?.status || '').toUpperCase();
    return ['ACTIVE', 'APPROVED', 'AVAILABLE', 'OFFLINE', 'BUSY'].includes(s);
  }

  /** True si le livreur peut être suspendu. */
  canSuspend(c: any): boolean {
    const s = (c?.status || '').toUpperCase();
    return ['ACTIVE', 'APPROVED', 'AVAILABLE', 'OFFLINE', 'BUSY'].includes(s);
  }

  /** True si le livreur est suspendu. */
  isSuspended(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'SUSPENDED';
  }

  /** True si le livreur est désactivé. */
  isDeactivated(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'DEACTIVATED';
  }

  /** True si le livreur est rejeté. */
  isRejected(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'REJECTED';
  }

  /** True si le livreur peut être réactivé (suspendu, désactivé ou rejeté). */
  canActivate(c: any): boolean {
    return this.isSuspended(c) || this.isDeactivated(c) || this.isRejected(c);
  }

  /** True si le livreur est en attente d'approbation. */
  isPendingApproval(c: any): boolean {
    return (c?.status || '').toUpperCase() === 'PENDING_APPROVAL';
  }

  docUrl(url: string | null): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return this.uploadsBaseUrl + url;
  }

  approve(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(ApproveTypeDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      panelClass: 'approve-type-panel',
    });
    dialogRef.afterClosed().subscribe((result: ApproveTypeResult | null) => {
      if (!result) return;
      this.actionLoading.set(true);
      this.couriersService.approveCourier(String(c.id), result.courierType).subscribe({
        next: () => {
          if (result.courierType === 'INTERNAL' && result.templateId) {
            this.scheduleSvc.applyTemplate(String(c.id), result.templateId).subscribe();
          }
          this.toastr.success(this.translate.instant('users.couriers.approveSuccess'));
          this.loadCourier(String(c.id));
          this.actionLoading.set(false);
        },
        error: () => {
          this.toastr.error(this.translate.instant('common.error'));
          this.actionLoading.set(false);
        },
      });
    });
  }

  reject(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.rejectCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.rejectSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  requestMoreInfo(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RequestMoreInfoDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((message) => {
      if (message) {
        this.actionLoading.set(true);
        this.couriersService.requestMoreInfo(String(c.id), message).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.requestMoreInfoSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  deactivate(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.deactivateCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.deactivateSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  suspend(): void {
    const c = this.courier();
    if (!c) return;
    const dialogRef = this.dialog.open(RejectDialogComponent, { width: '420px' });
    dialogRef.afterClosed().subscribe((reason) => {
      if (reason !== false && reason !== undefined) {
        this.actionLoading.set(true);
        this.couriersService.suspendCourier(String(c.id), reason || '').subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.couriers.suspendSuccess'));
            this.loadCourier(String(c.id));
            this.actionLoading.set(false);
          },
          error: () => {
            this.toastr.error(this.translate.instant('common.error'));
            this.actionLoading.set(false);
          },
        });
      }
    });
  }

  activate(): void {
    const c = this.courier();
    if (!c) return;
    this.actionLoading.set(true);
    this.couriersService.activateCourier(String(c.id)).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('users.couriers.activateSuccess'));
        this.loadCourier(String(c.id));
        this.actionLoading.set(false);
      },
      error: () => {
        this.toastr.error(this.translate.instant('common.error'));
        this.actionLoading.set(false);
      },
    });
  }

  goToApproval(): void {
    const c = this.courier();
    if (c) this.router.navigate(['/users/couriers', c.id, 'approval']);
  }

  goBack(): void {
    this.router.navigate(['/users/couriers']);
  }

  getCourierIdDisplay(): string {
    const c = this.courier();
    if (!c?.id) return '—';
    return 'SL-' + String(c.id).padStart(5, '0');
  }

  // ─── Audit log helpers ────────────────────────────────────────────
  loadAuditLogs(): void {
    const c = this.courier();
    if (!c) return;
    this.auditLoading.set(true);
    this.couriersService.getChangeLogs(String(c.id), this.auditPage() - 1, this.auditPageSize()).subscribe({
      next: (r: any) => {
        let entries = r.content ?? r ?? [];
        const action = this.auditFilterAction();
        if (action) entries = entries.filter((e: any) => e.action === action);
        const dateStr = this.auditFilterDate();
        if (dateStr) entries = entries.filter((e: any) => e.changedAt?.startsWith(dateStr));
        this.auditEntries.set(entries);
        this.auditTotalItems.set(r.totalElements ?? entries.length);
        this.auditLoading.set(false);
      },
      error: () => { this.auditLoading.set(false); },
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

  getZoneNames(zoneIdsStr: string | null): string {
    if (!zoneIdsStr) return '—';
    const ids = zoneIdsStr.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n));
    if (ids.length === 0) return '—';
    const zones = this.allZones();
    return ids.map(id => {
      const z = zones.find(zone => zone.id === id);
      return z ? z.name : `#${id}`;
    }).join(', ');
  }

  getVehicleTypeLabel(type: string | { name?: string } | null): string {
    if (type == null) return '—';
    const name = typeof type === 'object' && type?.name ? String(type.name) : String(type);
    const t = name.toUpperCase();
    if (t === 'BICYCLE') return 'Vélo';
    if (t === 'MOTORCYCLE') return 'Moto';
    if (t === 'CAR') return 'Voiture';
    return name;
  }
}
