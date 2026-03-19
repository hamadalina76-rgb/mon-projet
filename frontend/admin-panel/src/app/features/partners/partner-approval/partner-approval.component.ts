// src/app/features/partners/partner-approval/partner-approval.component.ts
import { Component, OnInit, OnDestroy, inject, signal, computed, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { PartnersService } from '../services/partners.service';
import { CategoriesService } from '@features/categories/services/categories.service';
import { Category } from '@core/models/category.model';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { RejectDialogComponent } from './reject-dialog.component';
import { RequestMoreInfoDialogComponent } from './request-more-info-dialog.component';
import { CommissionSetupDialogComponent, CommissionSetupData, CommissionSetupResult } from './commission-setup-dialog.component';
import { environment } from '@environments/environment';

declare const mapboxgl: any;

@Component({
  selector: 'app-partner-approval',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    TranslateModule,
  ],
  templateUrl: './partner-approval.component.html',
  styleUrls: ['./partner-approval.component.scss'],
})
export class PartnerApprovalComponent implements OnInit, AfterViewInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private partnersService = inject(PartnersService);
  private categoriesService = inject(CategoriesService);
  private dialog = inject(MatDialog);

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef;

  partner = signal<any>(null);
  allCategories = signal<Category[]>([]);
  loading = signal(false);
  actionLoading = signal(false);
  private map: any = null;
  private marker: any = null;

  /** URLs des photos (photosJson est un JSON array de strings) */
  photoUrls = computed(() => {
    const p = this.partner();
    const raw = p?.photosJson;
    if (!raw) return [];
    try {
      const arr = JSON.parse(raw) as string[];
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  });

  /** IDs de catégories du partenaire (premier = catégorie racine, reste = sous-catégories) */
  partnerCategoryIds = computed<number[]>(() => {
    const p = this.partner();
    if (!p) return [];
    const ids = p.categoryIds;
    if (Array.isArray(ids)) return ids.map((n: any) => Number(n)).filter((n: number) => !isNaN(n));
    if (typeof ids === 'string' && ids.trim()) {
      return ids.split(',').map((s: string) => parseInt(s.trim(), 10)).filter((n: number) => !isNaN(n));
    }
    return [];
  });

  /** Catégorie principale (racine) — premier ID de la liste */
  partnerMainCategory = computed<Category | null>(() => {
    const ids = this.partnerCategoryIds();
    if (ids.length === 0) return null;
    return this.allCategories().find(c => c.id === ids[0]) ?? null;
  });

  /** Sous-catégories — tous les IDs sauf le premier */
  partnerSubcategories = computed<Category[]>(() => {
    const ids = this.partnerCategoryIds();
    if (ids.length <= 1) return [];
    const subIds = ids.slice(1);
    return this.allCategories().filter(c => subIds.includes(c.id));
  });

  /** Label du type de commission */
  commissionTypeLabel = computed<string>(() => {
    const type = this.partner()?.commissionType;
    if (type === 'PERCENTAGE') return 'Pourcentage';
    if (type === 'MARKUP') return 'Markup';
    return type ?? '';
  });

  /** Format date de soumission */
  formattedSubmissionDate = computed(() => {
    const p = this.partner();
    if (!p?.createdAt) return '-';
    try {
      const date = new Date(p.createdAt);
      return date.toLocaleDateString('fr-FR', { year: 'numeric', month: 'long', day: 'numeric' });
    } catch {
      return p.createdAt;
    }
  });

  /** Badge de statut avec classe CSS appropriée */
  statusBadgeClass = computed(() => {
    const status = this.partner()?.status?.toUpperCase();
    if (status === 'DOCUMENTS_MISSING') return 'documents-missing';
    if (status === 'PENDING') return 'pending';
    if (status === 'ACTIVE' || status === 'APPROVED') return 'approved';
    if (status === 'REJECTED') return 'rejected';
    if (status === 'SUSPENDED') return 'suspended';
    return 'pending';
  });

  /** Texte du badge de statut */
  statusBadgeText = computed(() => {
    const status = this.partner()?.status?.toUpperCase();
    if (status === 'DOCUMENTS_MISSING') return 'partners.approval.statusDocumentsMissing';
    if (status === 'PENDING') return 'partners.approval.statusPending';
    if (status === 'ACTIVE' || status === 'APPROVED') return 'partners.approval.statusApproved';
    if (status === 'REJECTED') return 'partners.approval.statusRejected';
    if (status === 'SUSPENDED') return 'partners.approval.statusSuspended';
    return 'partners.approval.statusPending';
  });

  /** ID partenaire formaté (SPDL-XXX) */
  partnerIdFormatted = computed(() => {
    const p = this.partner();
    if (!p?.id) return '-';
    return `SPDL-${p.id.toString().padStart(5, '0')}`;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPartner(id);
    }
    this.categoriesService.getCategories().subscribe({
      next: (cats) => this.allCategories.set(cats),
      error: () => {}, // non-bloquant
    });
  }

  ngAfterViewInit(): void {
    // Map initialized after partner data loads
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.marker = null;
    }
  }

  loadPartner(id: string): void {
    this.loading.set(true);
    this.partnersService.getPartner(id).subscribe({
      next: (data: any) => {
        this.partner.set(data);
        this.loading.set(false);
        setTimeout(() => this.initializeMap(), 150);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/partners']);
      },
    });
  }

  private initializeMap(): void {
    const partner = this.partner();
    if (!partner?.latitude || !partner?.longitude || !this.mapContainer?.nativeElement) return;

    const token = environment.mapboxToken;
    if (typeof mapboxgl === 'undefined' || !token) return;

    try {
      if (this.map) {
        this.map.remove();
        this.map = null;
      }
      mapboxgl.accessToken = token;

      this.map = new mapboxgl.Map({
        container: this.mapContainer.nativeElement,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [Number(partner.longitude), Number(partner.latitude)],
        zoom: 15,
      });

      this.marker = new mapboxgl.Marker({ color: environment.mapboxToken ? '#E31E24' : '#E31E24' })
        .setLngLat([Number(partner.longitude), Number(partner.latitude)])
        .setPopup(
          new mapboxgl.Popup().setHTML(
            `<div style="padding: 8px; font-family: Inter, sans-serif;">
              <strong>${(partner.businessName || partner.brandName || '').replace(/</g, '&lt;')}</strong><br/>
              ${(partner.address || '').replace(/</g, '&lt;')}<br/>
              ${(partner.city || '')}${partner.postalCode ? ', ' + partner.postalCode : ''}
            </div>`
          )
        )
        .addTo(this.map);

      this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');
      setTimeout(() => this.map?.resize(), 100);
    } catch (e) {
      console.warn('Map init error:', e);
    }
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
      if (result) {
        this.actionLoading.set(true);
        
        // Create approval data with commission information
        const approvalData = {
          commissionType: result.commissionType,
          commissionRate: result.commissionRate,
          categoryId: result.categoryId,
          subcategoryIds: result.subcategoryIds
        };

        this.partnersService.approvePartnerWithCommission(partner.id.toString(), approvalData).subscribe({
          next: () => {
            // Assign zones if selected
            if (result.zoneIds && result.zoneIds.length > 0) {
              this.partnersService.assignZones(partner.id.toString(), result.zoneIds).subscribe({
                error: (e) => console.error('Zone assignment failed:', e)
              });
            }
            this.actionLoading.set(false);
            this.router.navigate(['/partners']);
          },
          error: () => this.actionLoading.set(false),
        });
      }
    });
  }

  rejectPartner(): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, {
      width: '480px',
    });

    dialogRef.afterClosed().subscribe((reason: string | false) => {
      if (reason) {
        this.actionLoading.set(true);
        this.partnersService.rejectPartner(this.partner().id.toString(), reason).subscribe({
          next: () => {
            this.actionLoading.set(false);
            this.router.navigate(['/partners']);
          },
          error: () => this.actionLoading.set(false),
        });
      }
    });
  }

  requestMoreInfo(): void {
    const dialogRef = this.dialog.open(RequestMoreInfoDialogComponent, {
      width: '520px',
    });

    dialogRef.afterClosed().subscribe((message: string | false) => {
      if (message) {
        this.actionLoading.set(true);
        this.partnersService.requestMoreInfo(this.partner().id.toString(), message).subscribe({
          next: () => {
            this.actionLoading.set(false);
            this.router.navigate(['/partners']);
          },
          error: () => this.actionLoading.set(false),
        });
      }
    });
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      RESTAURANT: 'Restaurant',
      FAST_FOOD: 'Fast Food',
      CAFE: 'Café',
      BAKERY: 'Boulangerie',
      GROCERY: 'Épicerie',
      PHARMACY: 'Pharmacie',
      FLORIST: 'Fleuriste',
      OTHER: 'Autre',
    };
    return labels[type] || type || '-';
  }

  getLegalStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      SARL: 'SARL',
      SA: 'SA',
      AUTO_ENTREPRENEUR: 'Auto-Entrepreneur',
      SUARL: 'SUARL',
      SAS: 'SAS',
    };
    return labels[status] || status || '-';
  }

  getOpeningHoursDisplay(oh: string): string[] {
    if (!oh || typeof oh !== 'string') return [];
    try {
      const parsed = JSON.parse(oh);
      const arr = Array.isArray(parsed) ? parsed : [parsed];
      if (arr.length === 0) return [];
      return arr.slice(0, 7).map((s: any) => {
        const d = s.day || '-';
        if (s.isClosed) return `${d}: Fermé`;
        const slot = s.slots?.[0];
        const open = slot?.open || '-';
        const close = slot?.close || '-';
        return `${d}: ${open}–${close}`;
      });
    } catch {
      return [oh];
    }
  }
}
