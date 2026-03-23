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
import { MatTooltipModule } from '@angular/material/tooltip';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PartnersService } from '../services/partners.service';
import { PartnerEditDialogComponent, PartnerEditDialogData } from '../partner-edit-dialog/partner-edit-dialog.component';
import { CommissionSetupDialogComponent, CommissionSetupResult } from '../partner-approval/commission-setup-dialog.component';
import { CommissionSetupDialogComponent, CommissionSetupData, CommissionSetupResult } from '../partner-approval/commission-setup-dialog.component';
import { CategoriesService } from '@features/categories/services/categories.service';
import { Category } from '@core/models/category.model';
import { Zone } from '@core/models/zone.model';
import { ToastrService } from 'ngx-toastr';
import { environment } from '@environments/environment';
import { ReasonDialogComponent } from '@shared/components/reason-dialog/reason-dialog.component';
import { WebSocketNotification, WebSocketService } from '@core/services/websocket.service';
import { AdminService } from '@core/services/admin.service';
import { Subscription } from 'rxjs';

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
    MatTooltipModule,
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
  private wsService         = inject(WebSocketService);
  private adminService      = inject(AdminService);
  private wsSub: Subscription | null = null;

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef;
  @ViewChild('zonesMapContainer', { static: false }) zonesMapContainer!: ElementRef;

  partner       = signal<any>(null);
  allCategories = signal<Category[]>([]);
  loading       = signal(false);
  error         = signal<string | null>(null);
  internalNotes = '';
  selectedTabIndex = signal(0);

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
  changeLogsError   = signal<string | null>(null);
  readonly LOGS_PAGE_SIZE = 10;
  // Pagination serveur : on charge uniquement la page courante
  logsPageSize      = signal(this.LOGS_PAGE_SIZE);
  logsCurrentPage   = signal(0);
  logsTotalElements = signal(0);
  readonly logsDisplayedColumns = ['changedAt', 'action', 'admin', 'modifications'];

  // ── Menu / Products (admin verification) ───────────────────────────────
  partnerMenuCategories = signal<any[]>([]);
  partnerMenuProducts = signal<any[]>([]);
  partnerMenuLoading = signal(false);
  partnerMenuError = signal<string | null>(null);
  partnerMenuPage = signal(0);
  partnerMenuPageSize = signal(20);
  partnerMenuTotalElements = signal(0);
  partnerMenuTotalPages = signal(0);
  partnerMenuCategoryFilter = signal<number | null>(null);
  partnerMenuModerationFilter = signal<string>('ALL');
  partnerMenuTableView = signal<'PRODUCTS' | 'HISTORY'>('PRODUCTS');
  partnerMenuSearch = signal('');
  partnerMenuSearchValue = '';
  partnerMenuSearchDebounce: ReturnType<typeof setTimeout> | null = null;
  productAuditLogs = signal<any[]>([]);
  productAuditLoading = signal(false);
  productAuditError  = signal<string | null>(null);
  productAuditTotalElements = signal(0);
  productAuditPage = signal(0);
  productAuditPageSize = signal(5);

  // Etat calculé pour l'affichage Menu/History dans ListPageComponent
  menuLoading = computed(() =>
    this.partnerMenuTableView() === 'PRODUCTS'
      ? this.partnerMenuLoading()
      : this.productAuditLoading()
  );
  menuError = computed(() =>
    this.partnerMenuTableView() === 'PRODUCTS'
      ? this.partnerMenuError()
      : this.productAuditError()
  );
  menuEmpty = computed(() => {
    if (this.menuLoading()) return false;
    if (this.menuError()) return false;
    return this.partnerMenuTableView() === 'PRODUCTS'
      ? (this.partnerMenuProducts().length === 0)
      : (this.filteredProductAuditLogs().length === 0);
  });

  logsEmpty = computed(() =>
    !this.changeLogsLoading() && !this.changeLogsError() && this.logsTotalElements() === 0
  );

  historyFilterAction = signal<string>('');
  historyFilterActorType = signal<string>('');
  historyFilterActorId = signal<number | null>(null);
  historyFilterProductId = signal<number | null>(null);
  historyFilterDateFrom = signal<string>('');
  historyFilterDateTo = signal<string>('');
  historyFilterAdminFullName = signal<string>('');
  historyFilterActorIdValue: string | number = '';
  historyFilterProductIdValue = '';
  historyFilterAdminFullNameValue = '';

  filteredProductAuditLogs = computed(() => {
    const rows = this.productAuditLogs() ?? [];
    const query = (this.historyFilterAdminFullName() || '').trim().toLowerCase();
    if (!query) return rows;
    return rows.filter((row: any) => {
      if (String(row?.actorType || '').toUpperCase() !== 'ADMIN') return false;
      const name = this.getHistoryActorFullName(row).toLowerCase();
      return name.includes(query);
    });
  });

  // Cache pour éviter de recalculer/parse du JSON à chaque détection de changement
  // (sinon le template appelle getParsedHistoryDiffs() plusieurs fois par ligne et freeze l'UI).
  private historyDiffsCache = new Map<string, Array<{ field: string; before: string; after: string }>>();
  private historyDiffsComputing = false;
  historyDiffsVersion = signal(0);
  private historyDiffsRequestId = 0;
  private adminNameCacheByUserId = new Map<number, string>();
  private adminNameRequestsInFlight = new Set<number>();

  // ── Filtres logs ──────────────────────────────────────────────────────────

  logsFilterAction   = signal<string>('');
  logsFilterAdminFullName = signal<string>('');
  logsFilterDateFrom = signal<string>('');
  logsFilterDateTo   = signal<string>('');  logsSearchText    = signal<string>(''); // Texte de recherche
  // Valeurs ngModel — pont two-way vers les signaux
  logsFilterActionValue: string        = '';
  logsFilterAdminFullNameValue: string = '';
  logsFilterDateFromValue: string      = '';
  logsFilterDateToValue: string        = '';
  logsFilterDateValue: string          = ''; // Date unique pour le filtre simplifiéé

  hasActiveFilters = computed(() =>
    !!(this.logsFilterAction()   ||
       this.logsFilterAdminFullName() ||
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
        (log.statusAfter ?? '').toLowerCase().includes(search) ||
        (log.reason ?? '').toLowerCase().includes(search) ||
        (log.changesBefore ?? '').toLowerCase().includes(search) ||
        (log.changesAfter ?? '').toLowerCase().includes(search);
      return matchAction && matchDate && matchSearch;
    });
  });

  /** Tranche visible selon la page courante */
  // Le backend renvoie déjà le bon "page" => on n'ajoute pas de slice côté front.
  paginatedChangeLogs = computed(() => this.filteredChangeLogs());

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
    this.changeLogsError.set(null);

    this.partnersService.getPartnerChangeLogs(id, page, size).subscribe({
      next: (partnerLogs) => {
        const partnerContent = (partnerLogs?.content ?? [])
          .map((log: any) => this.normalizePartnerHistoryLog(log))
          .sort((a: any, b: any) => {
            const da = new Date(a?.changedAt ?? 0).getTime();
            const db = new Date(b?.changedAt ?? 0).getTime();
            return db - da;
          });

        // Afficher toutes les entrées (y compris "Système") pour ne pas masquer
        // l'historique du toggle d'autorisation.
        this.changeLogs.set(partnerContent);
        this.logsTotalElements.set(partnerLogs?.totalElements ?? partnerContent.length);
        this.changeLogsLoading.set(false);
        this.hydrateAdminNamesForPartnerLogs(partnerContent);
      },
      error: () => {
        this.changeLogsError.set('Impossible de charger l\'historique du partenaire.');
        this.changeLogsLoading.set(false);
      },
    });
  }

  private normalizePartnerHistoryLog(log: any): any {
    return {
      ...log,
      changedAt: log?.changedAt,
      entityType: 'PARTNER',
    };
  }

  private normalizeProductHistoryLog(log: any): any {
    return {
      id: `product-${log?.id ?? Date.now()}`,
      adminId: log?.adminId,
      adminName: log?.actorType === 'PARTNER' ? 'Partenaire' : (log?.adminName || (log?.actorId ? `Admin #${log.actorId}` : 'Système')),
      action: log?.action,
      reason: log?.reason,
      changedAt: log?.createdAt,
      changesBefore: log?.changesBefore,
      changesAfter: log?.changesAfter,
      entityType: 'PRODUCT',
    };
  }

  hasStatusChanged(log: any): boolean {
    const before = log?.statusBefore ?? null;
    const after = log?.statusAfter ?? null;
    if (before == null && after == null) return false;
    return before !== after;
  }

  hasCommissionChanged(log: any): boolean {
    const beforeType = log?.commissionTypeBefore ?? null;
    const afterType = log?.commissionTypeAfter ?? null;

    const beforeRate = log?.commissionRateBefore != null ? String(log.commissionRateBefore) : null;
    const afterRate = log?.commissionRateAfter != null ? String(log.commissionRateAfter) : null;

    if (beforeType == null && afterType == null && beforeRate == null && afterRate == null) return false;
    return beforeType !== afterType || beforeRate !== afterRate;
  }

  hasCategoryChanged(log: any): boolean {
    const before = log?.categoryIdsBefore ?? null;
    const after = log?.categoryIdsAfter ?? null;
    if (before == null && after == null) return false;
    return before !== after;
  }

  hasProductEditPermissionChanged(log: any): boolean {
    const before = log?.productEditPermissionBefore ?? null;
    const after = log?.productEditPermissionAfter ?? null;
    // Le toggle doit avoir une valeur après-coup pour pouvoir être affiché.
    if (after == null) return false;
    return before !== after;
  }

  getHistoryProductDiffs(log: any): Array<{ field: string; before: string; after: string }> {
    const before = this.parseJsonSafe(log?.changesBefore);
    const after = this.parseJsonSafe(log?.changesAfter);
    const keys = new Set<string>([...Object.keys(before), ...Object.keys(after)]);
    const diffs: Array<{ field: string; before: string; after: string }> = [];
    keys.forEach((k) => {
      const b = this.stringifyDiffValue(before[k]);
      const a = this.stringifyDiffValue(after[k]);
      if (!this.areEquivalentDiffValues(before[k], after[k], b, a)) {
        diffs.push({ field: this.labelDiffField(k), before: b || '—', after: a || '—' });
      }
    });
    return diffs.slice(0, 4);
  }

  private hasBackendLogFilters(): boolean {
    // Le filtre texte (logsSearchText) est côté frontend, pas côté backend.
    return !!(
      this.logsFilterAction() ||
      this.logsFilterAdminFullName() ||
      this.logsFilterDateFrom() ||
      this.logsFilterDateTo()
    );
  }

  applyLogsFilters(): void {
    this.logsCurrentPage.set(0);
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;
    this.changeLogsLoading.set(true);
    this.changeLogsError.set(null);
    this.partnersService.getPartnerChangeLogsFiltered(id, 0, this.logsPageSize(), {
      action: this.logsFilterAction() || undefined,
      adminFullName: this.logsFilterAdminFullName() || undefined,
      dateFrom: this.logsFilterDateFrom() || undefined,
      dateTo: this.logsFilterDateTo() || undefined,
    }).subscribe({
      next: (partnerLogs) => {
        const partnerContent = (partnerLogs?.content ?? [])
          .map((log: any) => this.normalizePartnerHistoryLog(log))
          .sort((a: any, b: any) => {
            const da = new Date(a?.changedAt ?? 0).getTime();
            const db = new Date(b?.changedAt ?? 0).getTime();
            return db - da;
          });
        this.changeLogs.set(partnerContent);
        this.logsTotalElements.set(partnerLogs?.totalElements ?? partnerContent.length);
        this.changeLogsLoading.set(false);
        this.hydrateAdminNamesForPartnerLogs(partnerContent);
      },
      error: () => {
        this.changeLogsError.set('Impossible de charger l\'historique du partenaire.');
        this.changeLogsLoading.set(false);
      },
    });
  }

  logsSearchTextChanged(text: string): void {
    this.logsSearchText.set(text);
    this.logsCurrentPage.set(0);
  }

  resetLogsFilters(): void {
    this.logsFilterAction.set('');
    this.logsFilterAdminFullName.set('');
    this.logsFilterDateFrom.set('');
    this.logsFilterDateTo.set('');
    this.logsSearchText.set('');
    this.logsFilterActionValue   = '';
    this.logsFilterAdminFullNameValue = '';
    this.logsFilterDateFromValue = '';
    this.logsFilterDateToValue   = '';
    this.logsFilterDateValue     = '';
    this.logsCurrentPage.set(0);
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadChangeLogs(id, 0, this.logsPageSize());
  }

  onLogsPageChange(event: PageEvent): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.logsCurrentPage.set(event.pageIndex);
    this.logsPageSize.set(event.pageSize);

    // Recharge côté backend pour pagination correcte.
    if (this.hasBackendLogFilters()) {
      this.partnersService.getPartnerChangeLogsFiltered(id, event.pageIndex, event.pageSize, {
        action: this.logsFilterAction() || undefined,
        adminFullName: this.logsFilterAdminFullName() || undefined,
        dateFrom: this.logsFilterDateFrom() || undefined,
        dateTo: this.logsFilterDateTo() || undefined,
      }).subscribe({
        next: (partnerLogs) => {
          const partnerContent = (partnerLogs?.content ?? [])
            .map((log: any) => this.normalizePartnerHistoryLog(log))
            .sort((a: any, b: any) => {
              const da = new Date(a?.changedAt ?? 0).getTime();
              const db = new Date(b?.changedAt ?? 0).getTime();
              return db - da;
            });
          this.changeLogs.set(partnerContent);
          this.logsTotalElements.set(partnerLogs?.totalElements ?? partnerContent.length);
          this.changeLogsLoading.set(false);
          this.hydrateAdminNamesForPartnerLogs(partnerContent);
        },
        error: () => {
          this.changeLogsError.set('Impossible de charger l\'historique du partenaire.');
          this.changeLogsLoading.set(false);
        }
      });
    } else {
      this.loadChangeLogs(id, event.pageIndex, event.pageSize);
    }
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.applyRouteTabPreferences();
    this.wsService.connect();
    this.wsSub = this.wsService.onAdminNotification.subscribe((notif: WebSocketNotification) => {
      const currentPartnerId = this.route.snapshot.paramMap.get('id');
      const notifPartnerId = String(notif?.data?.['partnerId'] ?? '');
      const action = String(notif?.data?.['action'] ?? '');
      const isReviewProduct =
        action === 'REVIEW_PRODUCT' ||
        action === 'PRODUCT_REQUEST_SUBMITTED' ||
        action === 'REVIEW_PRODUCT_SUBMITTED';

      if (!currentPartnerId || notifPartnerId !== currentPartnerId || !isReviewProduct) return;
      this.loadPartnerMenuProducts(currentPartnerId, 0, this.partnerMenuPageSize());
      this.loadProductAuditLogs(currentPartnerId, 0, this.productAuditPageSize());
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPartner(id);
      this.loadChangeLogs(id, 0, this.logsPageSize());
      this.loadProductAuditLogs(id, 0, this.productAuditPageSize());
      this.loadZonesData(id);
      this.loadProductAuditLogs(id, 0, 20);
    }
    this.categoriesService.getCategories().subscribe({
      next: (cats) => this.allCategories.set(cats),
      error: () => {},
    });
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    if (this.map) { this.map.remove(); this.map = null; }
    this.wsSub?.unsubscribe();
    this.wsSub = null;
    if (this.partnerMenuSearchDebounce) clearTimeout(this.partnerMenuSearchDebounce);
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

        // Load partner menu categories & products for verification (Menu / Products tab)
        this.loadPartnerMenu(id);
      },
      error: (err) => {
        console.error('Failed to load partner:', err);
        this.error.set('Failed to load partner details');
        this.loading.set(false);
      },
    });
  }

  private loadPartnerMenu(partnerId: string): void {
    this.partnerMenuLoading.set(true);
    this.partnerMenuError.set(null);

    // 1) Categories
    this.partnersService.getPartnerMenuCategories(partnerId).subscribe({
      next: (cats) => {
        this.partnerMenuCategories.set(cats ?? []);
      },
      error: () => {
        this.partnerMenuCategories.set([]);
      },
    });

    // 2) Products (server-side pagination)
    this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());

    const qp = this.route.snapshot.queryParamMap;
    if ((qp.get('tab') || '').toLowerCase() === 'menu') {
      const view = (qp.get('view') || '').toLowerCase();
      // UX: au refresh, on affiche toujours "Tous" puis l'utilisateur applique le filtre
      // via le bouton "Filtrer". On ignore donc volontairement le query param `moderation`
      // pour éviter un état bloquant (ex: rester sur "En attente" après recharge).
      this.partnerMenuModerationFilter.set('ALL');

      // Important: si le paramètre `view` n'est pas fourni (cas le plus fréquent quand on clique
      // sur les boutons "Tableau Produits / Historique Modifications" sans changer l'URL),
      // on conserve l'état courant (partnerMenuTableView) au lieu de forcer PRODUCTS.
      if (view === 'history') {
        this.partnerMenuTableView.set('HISTORY');
        this.loadProductAuditLogs(partnerId, 0, this.productAuditPageSize());
      } else if (view) {
        // Paramètre `view` explicite et différent de "history" => on revient à PRODUCTS.
        this.partnerMenuTableView.set('PRODUCTS');
        this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());
      } else {
        // Pas de `view` dans l'URL => on recharge la table en fonction de la vue courante.
        if (this.partnerMenuTableView() === 'HISTORY') {
          this.loadProductAuditLogs(partnerId, 0, this.productAuditPageSize());
        } else {
          this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());
        }
      }
    }
  }

  private applyRouteTabPreferences(): void {
    const tab = (this.route.snapshot.queryParamMap.get('tab') || '').toLowerCase();
    if (tab === 'menu') {
      this.selectedTabIndex.set(2);
      return;
    }
    if (tab === 'history') {
      this.selectedTabIndex.set(4);
      return;
    }
    this.selectedTabIndex.set(0);
  }

  private loadPartnerMenuProducts(partnerId: string, page: number, size: number): void {
    this.partnerMenuLoading.set(true);
    this.partnerMenuError.set(null);
    const request$ = this.partnersService.getPartnerMenuProducts(
      partnerId,
      page,
      size,
      this.partnerMenuCategoryFilter(),
      this.partnerMenuSearch(),
      this.partnerMenuModerationFilter()
    );

    request$.subscribe({
        next: (res) => {
          this.partnerMenuProducts.set(res?.content ?? []);
          this.partnerMenuTotalElements.set(res?.totalElements ?? 0);
          this.partnerMenuTotalPages.set(res?.totalPages ?? 0);
          this.partnerMenuPage.set(res?.number ?? page);
          this.partnerMenuPageSize.set(res?.size ?? size);
          this.partnerMenuError.set(null);
          this.partnerMenuLoading.set(false);
        },
        error: () => {
          this.partnerMenuProducts.set([]);
          this.partnerMenuTotalElements.set(0);
          this.partnerMenuTotalPages.set(0);
          this.partnerMenuLoading.set(false);
          this.partnerMenuError.set('Impossible de charger les produits du partenaire.');
        },
      });
  }

  onMenuProductsListPageChange(event: { page: number; pageSize: number }): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    const zeroBasedPage = Math.max(0, (event.page ?? 1) - 1);
    if (this.partnerMenuTableView() === 'PRODUCTS') {
      this.loadPartnerMenuProducts(partnerId, zeroBasedPage, event.pageSize ?? this.partnerMenuPageSize());
      this.scrollToTopOfMenu();
      return;
    }
    this.loadProductAuditLogs(partnerId, zeroBasedPage, event.pageSize ?? this.productAuditPageSize());
    this.scrollToTopOfMenu();
  }

  private scrollToTopOfMenu(): void {
    // Simple UX improvement: keep user context when paging.
    // The page is already mostly above fold, so we just scroll to top.
    try {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch {
      // ignore
    }
  }

  onPartnerMenuTableViewChange(view: 'PRODUCTS' | 'HISTORY'): void {
    this.partnerMenuTableView.set(view);
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    if (view === 'HISTORY') {
      this.loadProductAuditLogs(partnerId, 0, this.productAuditPageSize());
    } else {
      // Stop heavy diff computations when leaving the HISTORY view.
      this.historyDiffsComputing = false;
      this.historyDiffsRequestId++;
      this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());
    }
  }

  applyHistoryFilters(): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    const productIdRaw = (this.historyFilterProductIdValue || '').trim();
    const parsedId = productIdRaw ? Number(productIdRaw) : null;
    this.historyFilterProductId.set(Number.isFinite(parsedId as number) ? parsedId : null);
    this.loadProductAuditLogs(partnerId, 0, this.productAuditPageSize());
  }

  resetHistoryFilters(): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    this.historyFilterAction.set('');
    this.historyFilterActorType.set('');
    this.historyFilterActorId.set(null);
    this.historyFilterActorIdValue = '';
    this.historyFilterProductId.set(null);
    this.historyFilterProductIdValue = '';
    this.historyFilterDateFrom.set('');
    this.historyFilterDateTo.set('');
    this.historyFilterAdminFullName.set('');
    this.historyFilterAdminFullNameValue = '';
    this.loadProductAuditLogs(partnerId, 0, this.productAuditPageSize());
  }

  onProductCategoryFilterChange(value: number | null): void {
    this.partnerMenuCategoryFilter.set(value);
    this.autoApplyProductFilters();
  }

  onProductModerationFilterChange(value: string): void {
    this.partnerMenuModerationFilter.set(value);
    this.autoApplyProductFilters();
  }

  onProductSearchInputChange(value: string): void {
    this.partnerMenuSearchValue = value;
    if (this.partnerMenuSearchDebounce) clearTimeout(this.partnerMenuSearchDebounce);
    this.partnerMenuSearchDebounce = setTimeout(() => this.autoApplyProductFilters(), 350);
  }

  onHistoryFilterActionChange(value: string): void {
    this.historyFilterAction.set(value);
    this.applyHistoryFilters();
  }

  onHistoryFilterActorTypeChange(value: string): void {
    this.historyFilterActorType.set(value);
    if (value !== 'ADMIN') {
      this.historyFilterActorId.set(null);
      this.historyFilterActorIdValue = '';
    }
    this.applyHistoryFilters();
  }

  onHistoryFilterActorIdChange(value: string | number): void {
    this.historyFilterActorIdValue = value;
    const raw = String(value ?? '').trim();
    if (!raw) {
      this.historyFilterActorId.set(null);
      this.applyHistoryFilters();
      return;
    }
    const parsed = Number(raw);
    this.historyFilterActorId.set(Number.isFinite(parsed) ? parsed : null);
    if (this.historyFilterActorId() != null) this.historyFilterActorType.set('ADMIN');
    this.applyHistoryFilters();
  }

  onHistoryFilterProductIdInput(value: string): void {
    this.historyFilterProductIdValue = value;
    this.applyHistoryFilters();
  }

  onHistoryFilterDateFromChange(value: string): void {
    this.historyFilterDateFrom.set(value);
    this.applyHistoryFilters();
  }

  onHistoryFilterDateToChange(value: string): void {
    this.historyFilterDateTo.set(value);
    this.applyHistoryFilters();
  }

  onHistoryFilterAdminFullNameChange(value: string): void {
    this.historyFilterAdminFullNameValue = value ?? '';
    this.historyFilterAdminFullName.set(value ?? '');
    // UX: appliquer directement sans cliquer sur "Filtrer"
    this.applyHistoryFilters();
  }

  applyMenuProductsFilters(): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    this.partnerMenuSearch.set((this.partnerMenuSearchValue || '').trim());
    this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());
  }

  private autoApplyProductFilters(): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    this.partnerMenuSearch.set((this.partnerMenuSearchValue || '').trim());
    this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());
  }

  resetMenuProductsFilters(): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    this.partnerMenuCategoryFilter.set(null);
    this.partnerMenuModerationFilter.set('ALL');
    this.partnerMenuSearchValue = '';
    this.partnerMenuSearch.set('');
    this.loadPartnerMenuProducts(partnerId, 0, this.partnerMenuPageSize());
  }

  getMenuCategoryName(categoryId: number | null | undefined): string {
    if (categoryId == null) return 'Sans catégorie';
    const cat = this.partnerMenuCategories().find(c => c?.id === categoryId);
    return cat?.name ?? `Catégorie #${categoryId}`;
  }

  getModerationLabel(status: string | null | undefined): string {
    if (status === 'PENDING') return 'En attente';
    if (status === 'REJECTED') return 'Rejeté';
    return 'Validé';
  }

  getProductImage(product: any): string {
    return product?.imageUrl || product?.image || '';
  }

  getProductTags(product: any): string[] {
    const raw = (product?.tags ?? '').toString().trim();
    if (!raw) return [];
    return raw.split(',').map((t: string) => t.trim()).filter((t: string) => !!t);
  }

  hasPromotion(product: any): boolean {
    return !!(product?.promotionLabel || product?.discountPercentage);
  }

  getCategoryImage(categoryId: number | null | undefined): string {
    if (categoryId == null) return '';
    const cat = this.partnerMenuCategories().find(c => c?.id === categoryId);
    return cat?.imageUrl || cat?.iconUrl || '';
  }

  getProductDiffEntries(product: any): Array<{ field: string; before: string; after: string }> {
    const before = this.parseJsonSafe(product?.lastChangesBefore);
    const after = this.parseJsonSafe(product?.lastChangesAfter);
    const fields = new Set<string>([...Object.keys(before), ...Object.keys(after)]);
    const entries: Array<{ field: string; before: string; after: string }> = [];
    fields.forEach((field) => {
      const prev = this.stringifyDiffValue(before[field]);
      const next = this.stringifyDiffValue(after[field]);
      if (!this.areEquivalentDiffValues(before[field], after[field], prev, next)) {
        entries.push({ field: this.labelDiffField(field), before: prev || '—', after: next || '—' });
      }
    });
    return entries;
  }

  getOrderedPartnerMenuProducts(): any[] {
    const rows = this.partnerMenuProducts() ?? [];
    return [...rows].sort((a: any, b: any) => {
      const rank = (status: string | null | undefined) => status === 'PENDING' ? 0 : status === 'REJECTED' ? 1 : 2;
      const byStatus = rank(a?.moderationStatus) - rank(b?.moderationStatus);
      if (byStatus !== 0) return byStatus;
      const da = new Date(a?.updatedAt || a?.createdAt || 0).getTime();
      const db = new Date(b?.updatedAt || b?.createdAt || 0).getTime();
      return db - da;
    });
  }

  getPendingCountInTable(): number {
    return (this.partnerMenuProducts() ?? []).filter((p: any) => p?.moderationStatus === 'PENDING').length;
  }

  hasProductChanges(product: any): boolean {
    return this.getProductDiffEntries(product).length > 0;
  }

  private parseJsonSafe(raw: any): Record<string, any> {
    if (!raw || typeof raw !== 'string') return {};
    try {
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? parsed : {};
    } catch {
      return {};
    }
  }

  private stringifyDiffValue(value: any): string {
    if (value == null) return '';
    if (typeof value === 'boolean') return value ? 'Oui' : 'Non';
    if (typeof value === 'number') return Number.isInteger(value) ? String(value) : String(value);
    if (typeof value === 'string') {
      const v = value.trim();
      const n = Number(v);
      if (v !== '' && !Number.isNaN(n)) {
        // Prevent false diffs like "160163.00" vs "160163".
        return Number.isInteger(n) ? String(n) : String(n);
      }
      return v;
    }
    return String(value).trim();
  }

  private areEquivalentDiffValues(beforeRaw: any, afterRaw: any, beforeStr?: string, afterStr?: string): boolean {
    if (beforeRaw === afterRaw) return true;

    const b = (beforeStr ?? this.stringifyDiffValue(beforeRaw)).trim();
    const a = (afterStr ?? this.stringifyDiffValue(afterRaw)).trim();
    if (b === a) return true;
    if (!b || !a) return false;

    const bn = Number(b);
    const an = Number(a);
    if (!Number.isNaN(bn) && !Number.isNaN(an)) {
      return bn === an;
    }
    return false;
  }

  private labelDiffField(field: string): string {
    const labels: Record<string, string> = {
      name: 'Nom',
      description: 'Description',
      price: 'Prix',
      originalPrice: 'Prix original',
      categoryId: 'Catégorie',
      imageUrl: 'Image',
      tags: 'Tags',
      isAvailable: 'Disponibilité',
      isPopular: 'Populaire',
      preparationTimeMin: 'Temps préparation',
      stockQuantity: 'Stock',
      promotionLabel: 'Promotion',
      promotionStartDate: 'Début promotion',
      promotionEndDate: 'Fin promotion',
      discountPercentage: 'Réduction',
      isVegetarian: 'Végétarien',
      isVegan: 'Vegan',
      isHalal: 'Halal',
      isGlutenFree: 'Sans gluten',
      spicyLevel: 'Niveau épicé',
      isNew: 'Nouveau',
      isFeatured: 'Mis en avant',
      status: 'Statut produit',
      moderationStatus: 'Statut modération',
    };
    return labels[field] || field;
  }

  private loadProductAuditLogs(partnerId: string, page: number, size: number): void {
    this.productAuditLoading.set(true);
    this.productAuditError.set(null);
    this.historyDiffsCache.clear();
    this.historyDiffsComputing = true;
    this.historyDiffsRequestId++;
    this.historyDiffsVersion.set(0);
    const requestId = this.historyDiffsRequestId;
    this.partnersService.getPartnerProductHistoryBackups(partnerId, page, size, {
      action: this.historyFilterAction() || undefined,
      actorType: this.historyFilterActorType() || undefined,
      // Disable filtering by admin fullName/id (prevents unnecessary failures/calls).
      actorId: null,
      adminFullName: this.historyFilterAdminFullName() || undefined,
      productId: this.historyFilterProductId(),
      dateFrom: this.historyFilterDateFrom() || undefined,
      dateTo: this.historyFilterDateTo() || undefined,
    }).subscribe({
      next: (res) => {
        const rows = res?.content ?? [];
        this.productAuditLogs.set(rows);
        this.productAuditTotalElements.set(res?.totalElements ?? 0);
        this.productAuditPage.set(res?.number ?? page);
        // L'UI doit refléter la taille réellement demandée (size).
        // Éviter de "reset" à une valeur retournée inattendue (ex: 10),
        // sinon le sélecteur affiche une valeur différente de celle attendue.
        this.productAuditPageSize.set(size);
        this.productAuditError.set(null);
        this.productAuditLoading.set(false);
        this.hydrateAdminNamesForProductLogs(rows);
        // Precompute diffs asynchronously to avoid blocking UI thread.
        this.precomputeHistoryDiffsAsync(requestId);
      },
      error: () => {
        this.productAuditLogs.set([]);
        this.productAuditTotalElements.set(0);
        this.productAuditLoading.set(false);
        this.productAuditError.set('Impossible de charger l\'historique des produits.');
        this.historyDiffsComputing = false;
        this.historyDiffsVersion.update(v => v + 1);
      }
    });
  }

  getPartnerDisplayNameWithId(): string {
    const p = this.partner();
    const name = p?.businessName || p?.brandName || 'Partenaire';
    const id = p?.id ? `#${p.id}` : '#-';
    return `${name} (${id})`;
  }

  getParsedHistoryDiffs(row: any): Array<{ field: string; before: string; after: string }> {
    const key = row?.id != null ? String(row.id) : '';
    if (!key) return [];
    // Register signal dependency so that template updates when cache is filled.
    this.historyDiffsVersion();
    return this.historyDiffsCache.get(key) ?? [];
  }

  isHistoryDiffsCached(row: any): boolean {
    const key = row?.id != null ? String(row.id) : '';
    if (!key) return false;
    return this.historyDiffsCache.has(key);
  }

  private async precomputeHistoryDiffsAsync(requestId: number): Promise<void> {
    if (this.historyDiffsComputing !== true) return;
    const rows = this.productAuditLogs();
    if (!rows?.length) {
      this.historyDiffsComputing = false;
      return;
    }

    // Give the browser one tick to paint the table before heavy JSON parsing.
    await new Promise<void>((r) => setTimeout(() => r(), 0));

    let index = 0;
    const timeBudgetMs = 10; // keep UI responsive by limiting synchronous work

    const processChunk = async (): Promise<void> => {
      if (this.historyDiffsRequestId !== requestId) return; // outdated computation
      const start = performance.now();
      let didWork = false;

      while (index < rows.length && (performance.now() - start) < timeBudgetMs) {
        const row = rows[index++];
        if (this.historyDiffsRequestId !== requestId) return;
        const key = row?.id != null ? String(row.id) : '';
        if (!key || this.historyDiffsCache.has(key)) continue;

        const before = this.parseJsonSafe(row?.changesBefore);
        const after = this.parseJsonSafe(row?.changesAfter);
        const keys = new Set<string>([...Object.keys(before), ...Object.keys(after)]);
        const output: Array<{ field: string; before: string; after: string }> = [];
        keys.forEach((k) => {
          const b = this.stringifyDiffValue(before[k]);
          const a = this.stringifyDiffValue(after[k]);
          if (!this.areEquivalentDiffValues(before[k], after[k], b, a)) {
            output.push({ field: this.labelDiffField(k), before: b || '—', after: a || '—' });
          }
        });
        if (key) this.historyDiffsCache.set(key, output);
        didWork = true;
      }

      if (didWork) this.historyDiffsVersion.update(v => v + 1);

      if (index < rows.length) {
        await new Promise<void>((r) => setTimeout(() => r(), 0));
        return processChunk();
      }

      this.historyDiffsComputing = false;
      this.historyDiffsVersion.update(v => v + 1);
    };

    await processChunk();
  }

  getHistoryAdminOptions(): Array<{ id: number; name: string }> {
    const options = new Map<number, string>();
    for (const row of this.productAuditLogs()) {
      if (String(row?.actorType || '').toUpperCase() !== 'ADMIN') continue;
      const id = Number(row?.actorId);
      if (!Number.isFinite(id)) continue;
      const displayName = (row?.adminName || '').toString().trim() || 'Admin';
      if (!options.has(id)) options.set(id, displayName);
    }
    // Ensure the selected admin always exists in the dropdown options.
    // After APPROVE/REJECT the current page may no longer contain entries for that admin,
    // which can make Angular Material appear "stuck".
    const selectedId = this.historyFilterActorId();
    if (selectedId != null && Number.isFinite(selectedId) && !options.has(selectedId)) {
      options.set(selectedId, 'Admin');
    }
    return Array.from(options.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  getHistoryActorLabel(row: any): string {
    const actorType = String(row?.actorType || '').toUpperCase();
    if (actorType === 'PARTNER') return 'Partenaire';
    if (actorType === 'ADMIN') {
      const adminName = this.getResolvedAdminName(row?.actorId, row?.adminName);
      return adminName || 'Admin';
    }
    return 'Système';
  }

  getHistoryActorFullName(row: any): string {
    const actorType = String(row?.actorType || '').toUpperCase();
    if (actorType === 'PARTNER') return this.getPartnerDisplayNameWithId();
    if (actorType === 'ADMIN') {
      const adminName = this.getResolvedAdminName(row?.actorId, row?.adminName);
      return adminName || 'Admin';
    }
    return 'Système';
  }

  getDiffDisplayValue(fieldLabel: string, rawValue: string): string {
    if (!rawValue || rawValue === '—') return '—';
    if (fieldLabel === 'Catégorie') {
      const id = Number(rawValue);
      if (!Number.isNaN(id)) return this.getMenuCategoryName(id);
    }
    return rawValue;
  }

  isImageDiff(fieldLabel: string, rawValue: string): boolean {
    if (fieldLabel !== 'Image') return false;
    if (!rawValue || rawValue === '—') return false;
    return /^https?:\/\//i.test(rawValue);
  }

  approveProductFromMenu(productId: number): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    this.partnersService.approveProduct(productId).subscribe({
      next: () => {
        this.toastr.success('Produit validé avec succès');
        this.loadPartnerMenuProducts(partnerId, this.partnerMenuPage(), this.partnerMenuPageSize());
      },
      error: () => this.toastr.error('Erreur lors de la validation du produit'),
    });
  }

  rejectProductFromMenu(productId: number): void {
    const partnerId = this.route.snapshot.paramMap.get('id');
    if (!partnerId) return;
    this.dialog.open(ReasonDialogComponent, {
      width: '520px',
      data: {
        title: 'Rejeter ce produit',
        message: 'Saisissez un motif de rejet clair pour le partenaire.',
        placeholder: 'Motif du rejet',
        confirmLabel: 'Rejeter',
        cancelLabel: 'Annuler',
        minLength: 3,
      },
    }).afterClosed().subscribe((reason: string | null) => {
      if (!reason) return;
      this.partnersService.rejectProduct(productId, reason).subscribe({
        next: () => {
          this.toastr.success('Produit rejeté');
          this.loadPartnerMenuProducts(partnerId, this.partnerMenuPage(), this.partnerMenuPageSize());
        },
        error: () => this.toastr.error('Erreur lors du rejet du produit'),
      });
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

    // During approval we also configure commission/categories (and the auto-approval permission for product updates).
    const dialogRef = this.dialog.open(CommissionSetupDialogComponent, {
      width: '620px',
      disableClose: true,
      data: {
        partnerId: partner.id.toString(),
        partnerName: partner.businessName || partner.brandName || 'Partenaire',
      },
    });

    dialogRef.afterClosed().subscribe((result: CommissionSetupResult | undefined) => {
      if (!result) return;

      const approvalData = {
        commissionType: result.commissionType,
        commissionRate: result.commissionRate,
        categoryId: result.categoryId,
        subcategoryIds: result.subcategoryIds,
        allowProductUpdatesWithoutApproval: result.allowProductUpdatesWithoutApproval,
      };

      this.partnersService.approvePartnerWithCommission(partner.id.toString(), approvalData).subscribe({
        next: () => {
          this.toastr.success(
            this.translate.instant('partners.detail.approveSuccess'),
            this.translate.instant('partners.detail.success')
          );
          this.loadPartner(partner.id.toString());
        },
        error: () => this.toastr.error(this.translate.instant('partners.detail.approveError'), this.translate.instant('common.error')),
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
    this.dialog.open(ReasonDialogComponent, {
      width: '520px',
      data: {
        title: this.translate.instant('partners.approval.reject'),
        message: this.translate.instant('partners.detail.rejectPrompt', { name: partner.businessName }),
        placeholder: 'Motif du rejet',
        confirmLabel: this.translate.instant('partners.approval.reject'),
        cancelLabel: this.translate.instant('common.cancel'),
        minLength: 3,
      },
    }).afterClosed().subscribe((reason: string | null) => {
      if (!reason) return;
      this.partnersService.rejectPartner(partner.id.toString(), reason).subscribe({
        next: () => { this.toastr.success(this.translate.instant('partners.detail.rejectSuccess'), this.translate.instant('partners.detail.success')); this.loadPartner(partner.id.toString()); },
        error: () => this.toastr.error(this.translate.instant('partners.detail.rejectError'), this.translate.instant('common.error')),
      });
    });
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
    this.dialog.open(ReasonDialogComponent, {
      width: '520px',
      data: {
        title: this.translate.instant('partners.detail.suspend'),
        message: this.translate.instant('partners.detail.suspendConfirm', { name: partner.brandName || partner.businessName }),
        placeholder: 'Motif de suspension',
        confirmLabel: this.translate.instant('partners.detail.suspend'),
        cancelLabel: this.translate.instant('common.cancel'),
        minLength: 3,
      },
    }).afterClosed().subscribe((reason: string | null) => {
      if (!reason) return;
      this.partnersService.suspendPartner(partner.id.toString(), reason.trim()).subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('partners.detail.suspendSuccess'), this.translate.instant('partners.detail.success'));
          this.loadPartner(partner.id.toString());
          this.loadChangeLogs(partner.id.toString(), 0, this.logsPageSize());
        },
        error: () => this.toastr.error(this.translate.instant('partners.detail.suspendError'), this.translate.instant('common.error')),
      });
    });
  }

  deactivatePartner(): void {
    const partner = this.partner();
    if (!partner) return;
    this.dialog.open(ReasonDialogComponent, {
      width: '520px',
      data: {
        title: this.translate.instant('partners.detail.deactivate'),
        message: this.translate.instant('partners.detail.deactivateConfirm', { name: partner.brandName || partner.businessName }),
        placeholder: 'Motif de désactivation',
        confirmLabel: this.translate.instant('partners.detail.deactivate'),
        cancelLabel: this.translate.instant('common.cancel'),
        minLength: 3,
      },
    }).afterClosed().subscribe((reason: string | null) => {
      if (!reason) return;
      this.partnersService.deactivatePartner(partner.id.toString(), reason.trim()).subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('partners.detail.deactivateSuccess'), this.translate.instant('partners.detail.success'));
          this.loadPartner(partner.id.toString());
          this.loadChangeLogs(partner.id.toString(), 0, this.logsPageSize());
        },
        error: () => this.toastr.error(this.translate.instant('partners.detail.deactivateError'), this.translate.instant('common.error')),
      });
    });
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
          const partnerIdStr = partner.id.toString();
          const menuView = this.partnerMenuTableView();
          this.partnersService.updatePartner(partner.id.toString(), result).subscribe({
            next: () => {
              this.toastr.success(
                this.translate.instant('partners.edit.successMessage', { name: partner.brandName || partner.businessName }),
                this.translate.instant('partners.edit.successTitle')
              );
              // Recharge le partenaire + recharge l'historique demandé
              this.loadPartner(partnerIdStr);
              this.loadChangeLogs(partnerIdStr, 0, this.logsPageSize());
              if (menuView === 'HISTORY') {
                // Restaure explicitement la vue HISTORY (évite le retour sur PRODUCTS après reload)
                this.partnerMenuTableView.set('HISTORY');
                this.loadProductAuditLogs(partnerIdStr, 0, this.productAuditPageSize());
              } else {
                this.partnerMenuTableView.set('PRODUCTS');
              }
            },
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
    const customLabels: Record<string, string> = {
      PRODUCT_SUBMITTED: 'Produit soumis',
      PRODUCT_RESUBMITTED: 'Produit modifié (re-soumis)',
      PRODUCT_PROMOTION_UPDATED: 'Promotion mise à jour',
    };
    if (customLabels[action]) return customLabels[action];
    const key = `partners.detail.logs.actions.${action}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : (action ?? '');
  }

  getAdminName(log: any): string {
    const resolved = this.getResolvedAdminName(log?.adminId, log?.adminName);
    if (resolved) return resolved;
    if (log?.adminId)   return 'Admin';
    return this.translate.instant('common.system') || 'Système';
  }

  private getResolvedAdminName(adminUserIdRaw: any, backendNameRaw: any): string {
    const backendName = (backendNameRaw || '').toString().trim();
    if (backendName && !/^Admin\s*#\d+$/i.test(backendName)) return backendName;
    const adminUserId = Number(adminUserIdRaw);
    if (Number.isFinite(adminUserId)) {
      const cached = this.adminNameCacheByUserId.get(adminUserId);
      if (cached && !/^Admin\s*#\d+$/i.test(cached)) return cached;
    }
    return '';
  }

  private hydrateAdminNamesForPartnerLogs(logs: any[]): void {
    const ids = Array.from(new Set(
      (logs ?? [])
        .filter((l: any) => l?.adminId != null)
        .map((l: any) => Number(l.adminId))
        .filter((id: number) => Number.isFinite(id))
    ));
    ids.forEach((id) => this.fetchAdminFullNameByUserId(id, 'PARTNER'));
  }

  private hydrateAdminNamesForProductLogs(rows: any[]): void {
    const ids = Array.from(new Set(
      (rows ?? [])
        .filter((r: any) => String(r?.actorType || '').toUpperCase() === 'ADMIN' && r?.actorId != null)
        .map((r: any) => Number(r.actorId))
        .filter((id: number) => Number.isFinite(id))
    ));
    ids.forEach((id) => this.fetchAdminFullNameByUserId(id, 'PRODUCT'));
  }

  private fetchAdminFullNameByUserId(adminUserId: number, target: 'PARTNER' | 'PRODUCT'): void {
    if (!Number.isFinite(adminUserId)) return;
    if (this.adminNameRequestsInFlight.has(adminUserId)) return;
    const cached = this.adminNameCacheByUserId.get(adminUserId);
    if (cached && !/^Admin\s*#\d+$/i.test(cached)) return;

    this.adminNameRequestsInFlight.add(adminUserId);
    this.adminService.getAdminByUserId(adminUserId).subscribe({
      next: (admin) => {
        const fullName = (admin?.fullName || '').trim();
        if (fullName) {
          this.adminNameCacheByUserId.set(adminUserId, fullName);
          if (target === 'PARTNER') {
            this.changeLogs.update((list) => (list ?? []).map((l: any) =>
              Number(l?.adminId) === adminUserId ? { ...l, adminName: fullName } : l
            ));
          } else {
            this.productAuditLogs.update((list) => (list ?? []).map((r: any) =>
              Number(r?.actorId) === adminUserId && String(r?.actorType || '').toUpperCase() === 'ADMIN'
                ? { ...r, adminName: fullName }
                : r
            ));
          }
        }
        this.adminNameRequestsInFlight.delete(adminUserId);
      },
      error: () => {
        this.adminNameRequestsInFlight.delete(adminUserId);
      },
    });
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