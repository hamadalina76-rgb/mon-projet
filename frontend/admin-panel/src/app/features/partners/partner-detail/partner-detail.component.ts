// src/app/features/partners/partner-detail/partner-detail.component.ts
import { Component, OnInit, OnDestroy, inject, signal, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
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
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PartnersService } from '../services/partners.service';
import { ToastrService } from 'ngx-toastr';
import { environment } from '@environments/environment';

// Mapbox GL loaded from CDN (index.html)
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
    TranslateModule,
  ],
  templateUrl: './partner-detail.component.html',
  styleUrls: ['./partner-detail.component.scss'],
})
export class PartnerDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private partnersService = inject(PartnersService);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef;

  partner = signal<any>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  internalNotes = '';

  private map: any = null;
  private marker: any = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPartner(id);
    }
  }

  ngAfterViewInit(): void {
    // Map will be initialized after partner data is loaded
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  loadPartner(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.partnersService.getPartner(id).subscribe({
      next: (data) => {
        this.partner.set(data);
        // Load internal notes if available
        if (data.internalNotes) {
          this.internalNotes = data.internalNotes;
        }
        this.loading.set(false);
        
        // Initialize map after partner data is loaded
        setTimeout(() => this.initializeMap(), 100);
      },
      error: (err) => {
        console.error('Failed to load partner:', err);
        this.error.set('Failed to load partner details');
        this.loading.set(false);
      }
    });
  }

  private initializeMap(): void {
    const partner = this.partner();
    if (!partner || !partner.latitude || !partner.longitude || !this.mapContainer) {
      console.warn('Cannot initialize map: missing location data or map container');
      return;
    }

    const token = environment.mapboxToken;
    if (typeof mapboxgl === 'undefined' || !token) {
      console.warn('Mapbox GL or mapboxToken not available. Set mapboxToken in environment.');
      return;
    }

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

      this.marker = new mapboxgl.Marker({ color: '#FF6B35' })
        .setLngLat([Number(partner.longitude), Number(partner.latitude)])
        .setPopup(
          new mapboxgl.Popup().setHTML(
            `<div style="padding: 8px;">
              <strong>${(partner.businessName || partner.brandName || '').replace(/</g, '&lt;')}</strong><br/>
              ${(partner.address || '').replace(/</g, '&lt;')}<br/>
              ${(partner.city || '').replace(/</g, '&lt;')}${partner.postalCode ? ', ' + partner.postalCode : ''}
            </div>`
          )
        )
        .addTo(this.map);

      this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');
      setTimeout(() => this.map?.resize(), 100);
    } catch (error) {
      console.error('Error initializing map:', error);
    }
  }

  approvePartner(): void {
    const partner = this.partner();
    if (!partner) return;

    const message = this.translate.instant('partners.detail.approveConfirm', { name: partner.businessName });
    if (confirm(message)) {
      this.partnersService.approvePartner(partner.id.toString()).subscribe({
        next: () => {
          this.toastr.success(
            this.translate.instant('partners.detail.approveSuccess'),
            this.translate.instant('partners.detail.success')
          );
          this.loadPartner(partner.id.toString());
        },
        error: (err) => {
          console.error('Failed to approve partner:', err);
          this.toastr.error(
            this.translate.instant('partners.detail.approveError'),
            this.translate.instant('common.error')
          );
        }
      });
    }
  }

  rejectPartner(): void {
    const partner = this.partner();
    if (!partner) return;

    const promptMessage = this.translate.instant('partners.detail.rejectPrompt', { name: partner.businessName });
    const reason = prompt(promptMessage);
    if (reason) {
      this.partnersService.rejectPartner(partner.id.toString(), reason).subscribe({
        next: () => {
          this.toastr.success(
            this.translate.instant('partners.detail.rejectSuccess'),
            this.translate.instant('partners.detail.success')
          );
          this.loadPartner(partner.id.toString());
        },
        error: (err) => {
          console.error('Failed to reject partner:', err);
          this.toastr.error(
            this.translate.instant('partners.detail.rejectError'),
            this.translate.instant('common.error')
          );
        }
      });
    }
  }

  activatePartner(): void {
    const partner = this.partner();
    if (!partner) return;

    const message = this.translate.instant('partners.detail.activateConfirm', { name: partner.businessName });
    if (confirm(message)) {
      this.partnersService.activatePartner(partner.id.toString()).subscribe({
        next: () => {
          this.toastr.success(
            this.translate.instant('partners.detail.activateSuccess'),
            this.translate.instant('partners.detail.success')
          );
          this.loadPartner(partner.id.toString());
        },
        error: (err) => {
          console.error('Failed to activate partner:', err);
          this.toastr.error(
            this.translate.instant('partners.detail.activateError'),
            this.translate.instant('common.error')
          );
        }
      });
    }
  }

  parseOpeningHours(): any[] {
    try {
      const partner = this.partner();
      if (!partner || !partner.openingHoursDisplay) {
        return [];
      }
      return JSON.parse(partner.openingHoursDisplay);
    } catch (e) {
      console.error('Failed to parse opening hours:', e);
      return [];
    }
  }

  goBack(): void {
    this.router.navigate(['/partners']);
  }

  getPartnerIdDisplay(): string {
    const p = this.partner();
    return p?.id ? `SL-${String(p.id).padStart(5, '0')}` : '—';
  }

  getTypeLabel(type: string): string {
    const key = `partners.types.${(type || 'other').toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : type || '—';
  }

  saveNote(): void {
    const p = this.partner();
    if (!p || !p.id) {
      return;
    }

    this.partnersService.saveInternalNotes(String(p.id), this.internalNotes || '').subscribe({
      next: () => {
        this.toastr.success(
          this.translate.instant('partners.detail.noteSaved'),
          this.translate.instant('partners.detail.internalNotes')
        );
      },
      error: (err) => {
        console.error('Failed to save internal note:', err);
        this.toastr.error(
          this.translate.instant('partners.detail.noteError'),
          this.translate.instant('common.error')
        );
      },
    });
  }
}
