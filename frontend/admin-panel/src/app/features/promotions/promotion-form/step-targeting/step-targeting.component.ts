import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormGroup, FormArray } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { CategoriesService } from '../../../categories/services/categories.service';
import { PartnersService } from '../../../partners/services/partners.service';
import { Category } from '@core/models/category.model';
import { Zone } from '@core/models/zone.model';
import { RuleBuilderComponent } from '../rule-builder/rule-builder.component';

@Component({
  selector: 'app-step-targeting',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    MatCheckboxModule, MatRadioModule, MatDividerModule, TranslateModule,
    RuleBuilderComponent,
  ],
  templateUrl: './step-targeting.component.html',
  styleUrls: ['./step-targeting.component.scss'],
})
export class StepTargetingComponent implements OnInit, OnDestroy {
  @Input() formGroup!: FormGroup;
  @Input() promotionType = '';
  @Input() rulesArray!: FormArray;

  @Output() partnersLoaded = new EventEmitter<{ id: number; name: string }[]>();
  @Output() categoriesLoaded = new EventEmitter<{ id: number; name: string }[]>();
  @Output() zonesLoaded = new EventEmitter<{ id: number; name: string }[]>();

  private categoriesService = inject(CategoriesService);
  private partnersService = inject(PartnersService);
  private destroy$ = new Subject<void>();

  // Partners
  partners: { id: number; name: string; logo?: string }[] = [];
  filteredPartners: { id: number; name: string; logo?: string }[] = [];
  partnerSearch = '';
  allPartners = true;

  // Categories
  categories: Category[] = [];
  allCategories = true;

  // Zones
  zones: Zone[] = [];
  filteredZones: Zone[] = [];
  zoneSearch = '';
  allZones = true;

  ngOnInit(): void {
    this.loadPartners();
    this.loadCategories();
    this.loadZones();
    // Determine modes from existing data
    const partnerIds = this.formGroup.get('applicablePartnerIds')?.value;
    this.allPartners = !partnerIds || partnerIds.length === 0;

    const catIds = this.formGroup.get('applicableCategoryIds')?.value;
    this.allCategories = !catIds || catIds.length === 0;

    const zoneIds = this.formGroup.get('applicableZoneIds')?.value;
    this.allZones = !zoneIds || zoneIds.length === 0;
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  // ── Partners ──────────────────────────────────────────────────
  private loadPartners(): void {
    this.partnersService.getPartners(0, 200).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        const list = res?.data?.content ?? res?.content ?? (Array.isArray(res) ? res : []);
        this.partners = list.map((p: any) => ({
          id: p.id,
          name: p.businessName || p.brandName || p.name || `Partner #${p.id}`,
          logo: p.logo || null,
        }));
        this.filteredPartners = [...this.partners];
        this.partnersLoaded.emit(this.partners.map(p => ({ id: p.id, name: p.name })));
      },
    });
  }

  filterPartners(event: Event): void {
    const q = (event.target as HTMLInputElement).value.toLowerCase().trim();
    this.partnerSearch = q;
    this.filteredPartners = q
      ? this.partners.filter(p => p.name.toLowerCase().includes(q))
      : [...this.partners];
  }

  onPartnerModeChange(allPartners: boolean): void {
    this.allPartners = allPartners;
    if (allPartners) {
      this.formGroup.get('applicablePartnerIds')?.setValue([]);
    }
  }

  getPartnerInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || '?';
  }

  // ── Categories ────────────────────────────────────────────────
  private loadCategories(): void {
    this.categoriesService.getCategories().pipe(takeUntil(this.destroy$)).subscribe({
      next: (cats) => {
        this.categories = cats;
        this.categoriesLoaded.emit(cats.map(c => ({ id: c.id, name: c.nameI18n['fr'] || c.nameI18n['en'] || `Catégorie #${c.id}` })));
      },
    });
  }

  onCategoryToggle(catId: number, checked: boolean): void {
    const ctrl = this.formGroup.get('applicableCategoryIds');
    const current: number[] = ctrl?.value || [];
    if (checked) {
      ctrl?.setValue([...current, catId]);
    } else {
      ctrl?.setValue(current.filter((id: number) => id !== catId));
    }
  }

  isCategorySelected(catId: number): boolean {
    return (this.formGroup.get('applicableCategoryIds')?.value || []).includes(catId);
  }

  onCategoryModeChange(all: boolean): void {
    this.allCategories = all;
    if (all) {
      this.formGroup.get('applicableCategoryIds')?.setValue([]);
    }
  }

  // ── Zones ─────────────────────────────────────────────────────
  private loadZones(): void {
    this.partnersService.getAllZones().pipe(takeUntil(this.destroy$)).subscribe({
      next: (zones) => {
        this.zones = zones;
        this.filteredZones = zones;
        this.zonesLoaded.emit(zones.map(z => ({ id: z.id, name: z.name })));
      },
    });
  }

  onZoneModeChange(all: boolean): void {
    this.allZones = all;
    if (all) {
      this.formGroup.get('applicableZoneIds')?.setValue([]);
    }
  }

  filterZones(): void {
    const q = this.zoneSearch.toLowerCase().trim();
    this.filteredZones = q
      ? this.zones.filter(z => z.name.toLowerCase().includes(q))
      : [...this.zones];
  }
}
