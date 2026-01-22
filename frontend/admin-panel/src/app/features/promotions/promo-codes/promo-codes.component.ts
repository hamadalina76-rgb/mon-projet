// src/app/features/promotions/promo-codes/promo-codes.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { PromotionsService } from '../services/promotions.service';

@Component({
  selector: 'app-promo-codes',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './promo-codes.component.html',
  styleUrls: ['./promo-codes.component.scss'],
})
export class PromoCodesComponent implements OnInit {
  private promotionsService = inject(PromotionsService);
  private dialog = inject(MatDialog);

  promoCodes: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadPromoCodes();
  }

  loadPromoCodes(): void {
    // TODO: Implement
  }

  openCreateDialog(): void {
    // TODO: Implement
  }
}
