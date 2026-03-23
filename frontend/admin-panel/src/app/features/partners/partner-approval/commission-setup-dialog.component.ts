import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CategoriesService } from '@features/categories/services/categories.service';
import { Category } from '@core/models/category.model';
import { Zone } from '@core/models/zone.model';
import { PartnersService } from '../services/partners.service';

export interface CommissionSetupData {
  partnerId: string;
  partnerName: string;
}

export interface CommissionSetupResult {
  commissionType: 'PERCENTAGE' | 'MARKUP';
  commissionRate: number;
  categoryId: number;
  subcategoryIds: number[];
  allowProductUpdatesWithoutApproval: boolean;
  zoneIds: number[];
}

@Component({
  selector: 'app-commission-setup-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatDividerModule,
    TranslateModule
  ],
  templateUrl: './commission-setup-dialog.component.html',
  styleUrls: ['./commission-setup-dialog.component.scss']
})
export class CommissionSetupDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private translate = inject(TranslateService);
  private categoriesService = inject(CategoriesService);
  private partnersService = inject(PartnersService);
  private dialogRef = inject(MatDialogRef<CommissionSetupDialogComponent>);
  
  data: CommissionSetupData = inject(MAT_DIALOG_DATA);

  commissionForm: FormGroup;
  
  allCategories = signal<Category[]>([]);
  allZones = signal<Zone[]>([]);
  selectedSubcategoryIds = signal<number[]>([]);
  selectedZoneIds = signal<number[]>([]);
  selectedCategoryId = signal<number | null>(null);
  selectedCommissionTypeValue = signal<string>('PERCENTAGE');
  formIsValid = signal<boolean>(false);

  // Computed properties
  rootCategories = computed(() => 
    this.allCategories().filter(cat => !cat.parentId)
  );

  selectedCommissionType = computed(() => this.selectedCommissionTypeValue());

  selectedCategory = computed(() => {
    const categoryId = this.selectedCategoryId();
    if (!categoryId) return null;
    return this.allCategories().find(cat => Number(cat.id) === Number(categoryId)) || null;
  });

  subcategories = computed(() => {
    const selectedCat = this.selectedCategory();
    if (!selectedCat) return [];
    return this.allCategories().filter(cat => 
      cat.parentId != null && Number(cat.parentId) === Number(selectedCat.id)
    );
  });

  isFormValid = computed(() => this.formIsValid());

  constructor() {
    this.commissionForm = this.fb.group({
      commissionType: ['PERCENTAGE', Validators.required],
      commissionRate: [null, [Validators.required, Validators.min(0.1), Validators.max(100)]],
      categoryId: [null, Validators.required],
      allowProductUpdatesWithoutApproval: [false]
    });

    this.commissionForm.valueChanges.subscribe(() => {
      this.formIsValid.set(this.commissionForm.valid);
    });

    this.commissionForm.get('commissionType')!.valueChanges.subscribe(val => {
      this.selectedCommissionTypeValue.set(val);
    });
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadZones();
  }

  private loadCategories(): void {
    this.categoriesService.getCategories().subscribe({
      next: (categories) => {
        const activeCategories = categories.filter(cat => cat.isActive);
        this.allCategories.set(activeCategories);
      },
      error: (err) => console.error('Erreur lors du chargement des catégories:', err)
    });
  }

  private loadZones(): void {
    this.partnersService.getAllZones().subscribe({
      next: (zones) => this.allZones.set(zones),
      error: (err) => console.error('Erreur lors du chargement des zones:', err)
    });
  }

  isZoneSelected(zoneId: number): boolean {
    return this.selectedZoneIds().includes(zoneId);
  }

  toggleZone(zoneId: number): void {
    const current = this.selectedZoneIds();
    if (current.includes(zoneId)) {
      this.selectedZoneIds.set(current.filter(id => id !== zoneId));
    } else {
      this.selectedZoneIds.set([...current, zoneId]);
    }
  }

  getCategoryName(category: Category): string {
    const lang = this.translate.currentLang || 'fr';
    return category.nameI18n[lang] || 
           category.nameI18n['fr'] || 
           category.nameI18n['en'] || 
           Object.values(category.nameI18n)[0] || 
           'Sans nom';
  }

  onCategoryChange(categoryId: number): void {
    this.selectedCategoryId.set(categoryId);
    this.selectedSubcategoryIds.set([]);
    this.commissionForm.patchValue({ categoryId });
  }

  trackByCategoryId(index: number, category: Category): number {
    return category.id;
  }

  trackBySubcategoryId(index: number, subcategory: Category): number {
    return subcategory.id;
  }

  toggleSubcategory(subcategoryId: number): void {
    const currentIds = this.selectedSubcategoryIds();
    if (currentIds.includes(subcategoryId)) {
      this.selectedSubcategoryIds.set(currentIds.filter(id => id !== subcategoryId));
    } else {
      this.selectedSubcategoryIds.set([...currentIds, subcategoryId]);
    }
  }

  isSubcategorySelected(subcategoryId: number): boolean {
    return this.selectedSubcategoryIds().includes(subcategoryId);
  }

  onConfirm(): void {
    if (!this.isFormValid()) return;

    const formValue = this.commissionForm.value;
    const result: CommissionSetupResult = {
      commissionType: formValue.commissionType,
      commissionRate: formValue.commissionRate,
      categoryId: formValue.categoryId,
      subcategoryIds: this.selectedSubcategoryIds(),
      zoneIds: this.selectedZoneIds(),
      allowProductUpdatesWithoutApproval: !!formValue.allowProductUpdatesWithoutApproval
    };

    this.dialogRef.close(result);
  }
}