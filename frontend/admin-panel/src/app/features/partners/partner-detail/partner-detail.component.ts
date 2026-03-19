// src/app/features/partners/partner-detail/partner-detail.component.ts
import { Component, OnInit, OnDestroy, inject, signal, computed, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PartnersService } from '../services/partners.service';
import { PartnerEditDialogComponent, PartnerEditDialogData } from '../partner-edit-dialog/partner-edit-dialog.component';
import { CommissionSetupDialogComponent, CommissionSetupData, CommissionSetupResult } from '../partner-approval/commission-setup-dialog.component';
import { CategoriesService } from '@features/categories/services/categories.service';
import { Category } from '@core/models/category.model';
import { Zone } from '@core/models/zone.model';
import { ToastrService } from 'ngx-toastr';
import { environment } from '@environments/environment';

declare const mapboxgl: any;

@Component({
  selector: 'app-partner-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    ListPageComponent,
    TranslateModule,
  ],
  templateUrl: './partner-detail.component.html',
  styleUrls: ['./partner-detail.component.scss'],
})
export class PartnerDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  private route             = inject(ActivatedRoute);
  private router            = inject(Router);
  private partnersService   = inject(PartnersService);
  private categoriesService = inject(CategoriesService);
  private toastr            = inject(ToastrService);
  private translate         = inject(TranslateService);
  private dialog            = inject(MatDialog);

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef;
  @ViewChild('zonesMapContainer', { static: false }) zonesMapContainer!: ElementRef;

  partner       = signal<any>(null);
  allCategories = signal<Category[]>([]);
  loading       = signal(false);
  error         = signal<string | null>(null);
  internalNotes = '';

  // ── Zones ────────────────────────────────────────────────────────────────
  allZones               = signal<Zone[]>([]);
  assignedZonesData      = signal<Zone[]>([]);  // objets complets retournés par l'API
  assignedZoneIds        = signal<number[]>([]);
  initialAssignedZoneIds = signal<number[]>([]);
  zonesLoading           = signal(false);
  zonesEditMode          = signal(false);
  private zonesMap: any  = null;

  // Mode vue : on affiche directement les données API, sans dépendre de allZones
  assignedZones = computed<Zone[]>(() => this.assignedZonesData());


  // ── Catégories computed ───────────────────────────────────────────────────

  partnerCategoryIds = computed<number[]>(() => {
    const p = this.partner();
    if (!p) return [];
    const ids = p.categoryIds;
    if (Array.isArray(ids)) return ids.map((n: any) => Number(n)).filter((n: number) => !isNaN(n));
    if (typeof ids === 'string' && ids.trim())
      return ids.split(',').map((s: string) => parseInt(s.trim(), 10)).filter((n: number) => !isNaN(n));
    return [];
  });

  partnerMainCategory = computed<Category | null>(() => {
    const ids = this.partnerCategoryIds();
    if (ids.length === 0) return null;
    return this.allCategories().find(c => c.id === ids[0]) ?? null;
  });

  partnerSubcategories = computed<Category[]>(() => {
    const ids = this.partnerCategoryIds();
    if (ids.length <= 1) return [];
    const subIds = ids.slice(1);
    return this.allCategories().filter(c => subIds.includes(c.id));
  });

  commissionTypeLabel = computed<string>(() => {
    const type = this.partner()?.commissionType;
    if (type === 'PERCENTAGE') return 'Pourcentage';
    if (type === 'MARKUP')     return 'Markup';
    return type ?? '';
  });

  // ── Status timeline ──────────────────────────────────────────────────────

  /** First APPROVE/APPROVE_WITH_COMMISSION log entry's changedAt */
  approvalDate = computed<string | null>(() => {
    const log = this.changeLogs().find(l =>
      l.action === 'APPROVE' || l.action === 'APPROVE_WITH_COMMISSION'
    );
    return log?.changedAt ?? null;
  });

  /** Last 5 status-changing log entries (excluding UPDATE_INFO, note changes) */
  statusTimeline = computed<any[]>(() => {
    const statusActions = new Set(['APPROVE', 'APPROVE_WITH_COMMISSION', 'REJECT', 'ACTIVATE', 'DEACTIVATE', 'SUSPEND']);
    return this.changeLogs()
      .filter(l => statusActions.has(l.action))
      .slice(0, 5);
  });

  private map: any    = null;
  private marker: any = null;

  // ── Historique des modifications ──────────────────────────────────────────

  changeLogs        = signal<any[]>([]);
  changeLogsLoading = signal(false);
  readonly LOGS_PAGE_SIZE = 10;
  logsPageSize      = signal(1000); // charge suffisamment depuis le serveur
  logsCurrentPage   = signal(0);
  logsTotalElements = signal(0);
  readonly logsDisplayedColumns = ['changedAt', 'action', 'admin', 'modifications'];

  // ── Filtres logs ──────────────────────────────────────────────────────────

  logsFilterAction   = signal<string>('');
  logsFilterAdminId  = signal<number | null>(null);
  logsFilterDateFrom = signal<string>('');
  logsFilterDateTo   = signal<string>('');  logsSearchText    = signal<string>(''); // Texte de recherche
  // Valeurs ngModel — pont two-way vers les signaux
  logsFilterActionValue: string        = '';
  logsFilterAdminIdValue: number | null = null;
  logsFilterDateFromValue: string      = '';
  logsFilterDateToValue: string        = '';
  logsFilterDateValue: string          = ''; // Date unique pour le filtre simplifiéé

  hasActiveFilters = computed(() =>
    !!(this.logsFilterAction()   ||
       this.logsFilterAdminId()  ||
       this.logsFilterDateFrom() ||
       this.logsFilterDateTo()   ||
       this.logsFilterDateValue  ||
       this.logsSearchText())
  );

  /** Filtre côté frontend : action + date + texte libre */
  filteredChangeLogs = computed(() => {
    const logs       = this.changeLogs();
    const action     = this.logsFilterAction().toLowerCase();
    const filterDate = this.logsFilterDateValue;
    const search     = this.logsSearchText().toLowerCase().trim();
    return logs.filter(log => {
      const matchAction = !action || (log.action ?? '').toLowerCase().includes(action);
      let matchDate = true;
      if (filterDate && log.changedAt) {
        const logDate = log.changedAt.substring(0, 10);
        matchDate = logDate === filterDate;
      }
      const matchSearch = !search ||
        (log.action ?? '').toLowerCase().includes(search) ||
        this.getAdminName(log).toLowerCase().includes(search) ||
        (log.adminEmail ?? '').toLowerCase().includes(search) ||
        (log.statusBefore ?? '').toLowerCase().includes(search) ||
        (log.statusAfter ?? '').toLowerCase().includes(search);
      return matchAction && matchDate && matchSearch;
    });
  });

  /** Tranche visible selon la page courante */
  paginatedChangeLogs = computed(() => {
    const start = this.logsCurrentPage() * this.LOGS_PAGE_SIZE;
    return this.filteredChangeLogs().slice(start, start + this.LOGS_PAGE_SIZE);
  });

  /** Admins uniques présents dans la page courante */
  uniqueAdmins = computed(() => {
    const seen = new Map<number, string>();
    for (const log of this.changeLogs()) {
      if (log.adminId && !seen.has(log.adminId))
        seen.set(log.adminId, this.getAdminName(log));
    }
    return Array.from(seen.entries()).map(([id, name]) => ({ id, name }));
  });

  // ── Méthodes logs ─────────────────────────────────────────────────────────

  parseChanges(json: string | null): { key: string; value: string }[] {
    if (!json) return [];
    try {
      const obj = JSON.parse(json);
      return Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }));
    } catch {
      return [{ key: 'details', value: json }];
    }
  }

  loadChangeLogs(id: string, page = 0, size = this.logsPageSize()): void {
    this.changeLogsLoading.set(true);

    const filters = {
      action:   this.logsFilterAction()   || undefined,
      adminId:  this.logsFilterAdminId()  ?? undefined,
      dateFrom: this.logsFilterDateFrom() || undefined,
      dateTo:   this.logsFilterDateTo()   || undefined,
    };

    const hasBackendFilter = Object.values(filters).some(v => v !== undefined);

    const call$ = hasBackendFilter
      ? this.partnersService.getPartnerChangeLogsFiltered(id, page, size, filters)
      : this.partnersService.getPartnerChangeLogs(id, page, size);

    call$.subscribe({
      next: (res) => {
        this.changeLogs.set(res.content ?? []);
        this.logsTotalElements.set(res.totalElements ?? 0);
        this.changeLogsLoading.set(false);
      },
      error: () => this.changeLogsLoading.set(false),
    });
  }

  applyLogsFilters(): void {
    // Date filter is purely client-side (logsFilterDateValue checked in filteredChangeLogs)
    this.logsCurrentPage.set(0);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadChangeLogs(id, 0, this.logsPageSize());
  }

  logsSearchTextChanged(text: string): void {
    this.logsSearchText.set(text);
    this.logsCurrentPage.set(0);
  }

  resetLogsFilters(): void {
    this.logsFilterAction.set('');
    this.logsFilterAdminId.set(null);
    this.logsFilterDateFrom.set('');
    this.logsFilterDateTo.set('');
    this.logsSearchText.set('');
    this.logsFilterActionValue   = '';
    this.logsFilterAdminIdValue  = null;
    this.logsFilterDateFromValue = '';
    this.logsFilterDateToValue   = '';
    this.logsFilterDateValue     = '';
    this.logsCurrentPage.set(0);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadChangeLogs(id, 0, this.logsPageSize());
  }

  onLogsPageChange(event: PageEvent): void {
    this.logsCurrentPage.set(event.pageIndex);
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPartner(id);
      this.loadChangeLogs(id, 0, this.logsPageSize());
      this.loadZonesData(id);
    }
    this.categoriesService.getCategories().subscribe({
      next: (cats) => this.allCategories.set(cats),
      error: () => {},
    });
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    if (this.map) { this.map.remove(); this.map = null; }
    if (this.zonesMap) { this.zonesMap.remove(); this.zonesMap = null; }
  }

  // ── Partenaire ────────────────────────────────────────────────────────────

  loadPartner(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.partnersService.getPartner(id).subscribe({
      next: (data) => {
        this.partner.set(data);
        if (data.internalNotes) this.internalNotes = data.internalNotes;
        this.loading.set(false);
        setTimeout(() => this.initializeMap(), 100);
      },
      error: (err) => {
        console.error('Failed to load partner:', err);
        this.error.set('Failed to load partner details');
        this.loading.set(false);
      },
    });
  }

  private initializeMap(): void {
    const partner = this.partner();
    if (!partner?.latitude || !partner?.longitude || !this.mapContainer) return;
    const token = environment.mapboxToken;
    if (typeof mapboxgl === 'undefined' || !token) return;
    try {
      if (this.map) { this.map.remove(); this.map = null; }
      mapboxgl.accessToken = token;
      this.map = new mapboxgl.Map({
        container: this.mapContainer.nativeElement,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [Number(partner.longitude), Number(partner.latitude)],
        zoom: 15,
      });
      this.marker = new mapboxgl.Marker({ color: '#FF6B35' })
        .setLngLat([Number(partner.longitude), Number(partner.latitude)])
        .setPopup(new mapboxgl.Popup().setHTML(
          `<div style="padding:8px;">
            <strong>${(partner.businessName || partner.brandName || '').replace(/</g,'&lt;')}</strong><br/>
            ${(partner.address || '').replace(/</g,'&lt;')}<br/>
            ${(partner.city || '').replace(/</g,'&lt;')}${partner.postalCode ? ', '+partner.postalCode : ''}
          </div>`
        ))
        .addTo(this.map);
      this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');
      setTimeout(() => this.map?.resize(), 100);
    } catch (e) { console.error('Error initializing map:', e); }
  }

  // ── Zones ─────────────────────────────────────────────────────────────────

  loadZonesData(partnerId: string): void {
    this.zonesLoading.set(true);
    this.partnersService.getAllZones().subscribe({
      next: (zones) => {
        this.allZones.set(zones);
        this.partnersService.getPartnerZones(partnerId).subscribe({
          next: (assigned) => {
            // Normaliser : l'API peut retourner un tableau ou { content: [] }
            const list: Zone[] = Array.isArray(assigned)
              ? assigned
              : (assigned as any)?.content ?? [];
            const ids = list.map((z: any) => Number(z.id));
            this.assignedZonesData.set(list);
            this.assignedZoneIds.set(ids);
            this.initialAssignedZoneIds.set([...ids]);
            this.zonesLoading.set(false);
            setTimeout(() => this.initZonesMap(), 300);
          },
          error: () => this.zonesLoading.set(false)
        });
      },
      error: () => this.zonesLoading.set(false)
    });
  }

  isZoneAssigned(zoneId: number): boolean {
    return this.assignedZoneIds().includes(Number(zoneId));
  }

  toggleZoneAssignment(zoneId: number): void {
    const current = this.assignedZoneIds();
    if (current.includes(zoneId)) {
      this.assignedZoneIds.set(current.filter(id => id !== zoneId));
    } else {
      this.assignedZoneIds.set([...current, zoneId]);
    }
    setTimeout(() => this.initZonesMap(), 100);
  }

  enterZonesEditMode(): void {
    this.zonesEditMode.set(true);
  }

  cancelZonesEdit(): void {
    // Restaurer l’état initial
    this.assignedZoneIds.set([...this.initialAssignedZoneIds()]);
    // Restaurer aussi les objets zones assignées depuis allZones
    const initial = this.initialAssignedZoneIds();
    const fromAll = this.allZones().filter(z => initial.includes(Number(z.id)));
    // Privilégier les données existantes si allZones ne couvre pas tout
    const existing = this.assignedZonesData().filter(z => initial.includes(Number(z.id)));
    this.assignedZonesData.set(existing.length === initial.length ? existing : fromAll);
    this.zonesEditMode.set(false);
    setTimeout(() => this.initZonesMap(), 150);
  }

  saveZones(): void {
    const partner = this.partner();
    if (!partner) return;

    // TC-53 : ne pas appeler l'API si aucune modification
    const current = this.assignedZoneIds();
    const initial = this.initialAssignedZoneIds();
    const hasChanges =
      current.length !== initial.length ||
      current.some(id => !initial.includes(id)) ||
      initial.some(id => !current.includes(id));
    if (!hasChanges) {
      this.toastr.info(this.translate.instant('partners.detail.zones.noChanges'));
      this.zonesEditMode.set(false);
      return;
    }

    this.zonesLoading.set(true);
    this.partnersService.assignZones(partner.id.toString(), current).subscribe({
      next: () => {
        this.zonesEditMode.set(false);
        this.toastr.success(this.translate.instant('partners.detail.zones.saveSuccess'));
        // Reload complet : getAllZones() + getPartnerZones() → tout est frais
        this.loadZonesData(partner.id.toString());
      },
      error: () => {
        this.zonesLoading.set(false);
        this.toastr.error(this.translate.instant('partners.detail.zones.saveError'));
      }
    });
  }

  private initZonesMap(): void {
    const assignedIds = this.assignedZoneIds();
    if (assignedIds.length === 0 || !this.zonesMapContainer?.nativeElement) return;

    const token = (environment as any).mapboxToken;
    if (typeof mapboxgl === 'undefined' || !token) return;

    try {
      if (this.zonesMap) { this.zonesMap.remove(); this.zonesMap = null; }
      mapboxgl.accessToken = token;
      const assignedZones = this.allZones().filter(z => assignedIds.includes(z.id));
      const center = this.partner()
        ? [Number(this.partner().longitude || 10.1815), Number(this.partner().latitude || 36.8065)]
        : [10.1815, 36.8065];

      this.zonesMap = new mapboxgl.Map({
        container: this.zonesMapContainer.nativeElement,
        style: 'mapbox://styles/mapbox/streets-v12',
        center,
        zoom: 11,
      });

      this.zonesMap.on('load', () => {
        assignedZones.forEach((zone, i) => {
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

  approvePartner(): void {
    const partner = this.partner();
    if (!partner) return;
    const dialogRef = this.dialog.open(CommissionSetupDialogComponent, {
      width: '95vw',
      maxWidth: '640px',
      panelClass: 'commission-dialog-panel',
      disableClose: true,
      data: {
        partnerId: partner.id.toString(),
        partnerName: partner.businessName || partner.brandName || 'Partenaire'
      } as CommissionSetupData,
    });
    dialogRef.afterClosed().subscribe((result: CommissionSetupResult | undefined) => {
      if (!result) return;
      this.partnersService.approvePartnerWithCommission(partner.id.toString(), {
        commissionType: result.commissionType,
        commissionRate: result.commissionRate,
        categoryId: result.categoryId,
        subcategoryIds: result.subcategoryIds,
      }).subscribe({
        next: () => {
          this.toastr.success(
            this.translate.instant('partners.detail.approveSuccess'),
            this.translate.instant('partners.detail.success')
          );
          this.loadPartner(partner.id.toString());
          this.loadChangeLogs(partner.id.toString(), 0, this.logsPageSize());
        },
        error: () => this.toastr.error(
          this.translate.instant('partners.detail.approveError'),
          this.translate.instant('common.error')
        ),
      });
    });
  }

  rejectPartner(): void {
    const partner = this.partner();
    if (!partner) return;
    const reason = prompt(this.translate.instant('partners.detail.rejectPrompt', { name: partner.businessName }));
    if (reason) {
      this.partnersService.rejectPartner(partner.id.toString(), reason).subscribe({
        next: () => { this.toastr.success(this.translate.instant('partners.detail.rejectSuccess'), this.translate.instant('partners.detail.success')); this.loadPartner(partner.id.toString()); },
        error: () => this.toastr.error(this.translate.instant('partners.detail.rejectError'), this.translate.instant('common.error')),
      });
    }
  }

  activatePartner(): void {
    const partner = this.partner();
    if (!partner) return;
    if (confirm(this.translate.instant('partners.detail.activateConfirm', { name: partner.businessName }))) {
      this.partnersService.activatePartner(partner.id.toString()).subscribe({
        next: () => { this.toastr.success(this.translate.instant('partners.detail.activateSuccess'), this.translate.instant('partners.detail.success')); this.loadPartner(partner.id.toString()); },
        error: () => this.toastr.error(this.translate.instant('partners.detail.activateError'), this.translate.instant('common.error')),
      });
    }
  }

  suspendPartner(): void {
    const partner = this.partner();
    if (!partner) return;
    const reason = prompt(this.translate.instant('partners.detail.suspendConfirm', { name: partner.brandName || partner.businessName }));
    if (reason?.trim()) {
      this.partnersService.suspendPartner(partner.id.toString(), reason.trim()).subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('partners.detail.suspendSuccess'), this.translate.instant('partners.detail.success'));
          this.loadPartner(partner.id.toString());
          this.loadChangeLogs(partner.id.toString(), 0, this.logsPageSize());
        },
        error: () => this.toastr.error(this.translate.instant('partners.detail.suspendError'), this.translate.instant('common.error')),
      });
    }
  }

  deactivatePartner(): void {
    const partner = this.partner();
    if (!partner) return;
    const reason = prompt(this.translate.instant('partners.detail.deactivateConfirm', { name: partner.brandName || partner.businessName }));
    if (reason?.trim()) {
      this.partnersService.deactivatePartner(partner.id.toString(), reason.trim()).subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('partners.detail.deactivateSuccess'), this.translate.instant('partners.detail.success'));
          this.loadPartner(partner.id.toString());
          this.loadChangeLogs(partner.id.toString(), 0, this.logsPageSize());
        },
        error: () => this.toastr.error(this.translate.instant('partners.detail.deactivateError'), this.translate.instant('common.error')),
      });
    }
  }

  parseOpeningHours(): any[] {
    try {
      const partner = this.partner();
      if (!partner?.openingHoursDisplay) return [];
      return JSON.parse(partner.openingHoursDisplay);
    } catch { return []; }
  }

  openEditDialog(): void {
    const partner = this.partner();
    if (!partner) return;
    this.dialog.open(PartnerEditDialogComponent, { width: '660px', maxHeight: '90vh', data: { partner } as PartnerEditDialogData })
      .afterClosed().subscribe((result) => {
        if (result) {
          this.partnersService.updatePartner(partner.id.toString(), result).subscribe({
            next: () => { this.toastr.success(this.translate.instant('partners.edit.successMessage', { name: partner.brandName || partner.businessName }), this.translate.instant('partners.edit.successTitle')); this.loadPartner(partner.id.toString()); },
            error: () => this.toastr.error(this.translate.instant('partners.edit.errorMessage'), this.translate.instant('common.error')),
          });
        }
      });
  }

  goBack(): void { this.router.navigate(['/partners']); }

  getPartnerIdDisplay(): string {
    const p = this.partner();
    return p?.id ? `SL-${String(p.id).padStart(5, '0')}` : '—';
  }

  getTypeLabel(type: string): string {
    const key = `partners.types.${(type || 'other').toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : type || '—';
  }

  resolveCategoryNames(rawIds: string | null): string[] {
    if (!rawIds?.trim()) return [];
    const ids  = rawIds.split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n));
    const cats = this.allCategories();
    return ids.map(id => { const c = cats.find(c => c.id === id); return c ? (c.nameI18n?.['fr'] || c.nameI18n?.['en'] || `ID:${id}`) : `ID:${id}`; });
  }

  resolveCategoryMetas(rawIds: string | null): { name: string; bg: string | null; color: string | null }[] {
    if (!rawIds?.trim()) return [];
    const ids  = rawIds.split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n));
    const cats = this.allCategories();
    return ids.map(id => { const c = cats.find(c => c.id === id); return { name: c ? (c.nameI18n?.['fr'] || c.nameI18n?.['en'] || `ID:${id}`) : `ID:${id}`, bg: c?.backgroundColor ?? null, color: c?.textColor ?? null }; });
  }

  getCategoryNames(rawIds: string | null): string { return this.resolveCategoryMetas(rawIds).map(m => m.name).join(', '); }

  getMainCategoryName(rawIds: string | null): string {
    if (!rawIds?.trim()) return '';
    const firstId = parseInt(rawIds.split(',')[0].trim(), 10);
    if (isNaN(firstId)) return '';
    const cat = this.allCategories().find(c => c.id === firstId);
    return cat ? (cat.nameI18n?.['fr'] || cat.nameI18n?.['en'] || `ID:${firstId}`) : `ID:${firstId}`;
  }

  getSubCategoryNames(rawIds: string | null): string {
    if (!rawIds?.trim()) return '';
    const parts = rawIds.split(',').slice(1).map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n));
    if (!parts.length) return '';
    return parts.map(id => { const c = this.allCategories().find(c => c.id === id); return c ? (c.nameI18n?.['fr'] || c.nameI18n?.['en'] || `ID:${id}`) : `ID:${id}`; }).join(', ');
  }

  hasSubCategories(rawIds: string | null): boolean { return !!rawIds?.trim() && rawIds.split(',').length > 1; }

  getActionLabel(action: string): string {
    const key = `partners.detail.logs.actions.${action}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : (action ?? '');
  }

  getAdminName(log: any): string {
    if (log?.adminName) return log.adminName;
    if (log?.adminId)   return `Admin #${log.adminId}`;
    return this.translate.instant('common.system') || 'Système';
  }

  getAdminAvatarColor(log: any): string {
    const colors = ['#E31E24', '#10B981', '#3B82F6', '#8B5CF6', '#F59E0B', '#EF4444', '#06B6D4', '#EC4899'];
    return colors[(this.getAdminName(log).charCodeAt(0) || 0) % colors.length];
  }

  getAdminRoleColor(log: any): string {
    const role = (log?.adminRole || '').toUpperCase();
    if (role.includes('SUPER'))   return '#E31E24';
    if (role.includes('SUPPORT')) return '#10B981';
    if (role.includes('MANAGER')) return '#3B82F6';
    if (role.includes('FINANCE')) return '#F59E0B';
    return '#64748B';
  }

  saveNote(): void {
    const p = this.partner();
    if (!p?.id) return;
    this.partnersService.saveInternalNotes(String(p.id), this.internalNotes || '').subscribe({
      next: () => this.toastr.success(this.translate.instant('partners.detail.noteSaved'), this.translate.instant('partners.detail.internalNotes')),
      error: () => this.toastr.error(this.translate.instant('partners.detail.noteError'), this.translate.instant('common.error')),
    });
  }
}