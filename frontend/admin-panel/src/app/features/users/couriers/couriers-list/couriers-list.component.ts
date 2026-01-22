// src/app/features/users/couriers/couriers-list/couriers-list.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { CouriersService } from '../services/couriers.service';

@Component({
  selector: 'app-couriers-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatChipsModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './couriers-list.component.html',
  styleUrls: ['./couriers-list.component.scss'],
})
export class CouriersListComponent implements OnInit {
  private couriersService = inject(CouriersService);

  couriers: any[] = [];
  loading = false;
  searchTerm = '';
  currentPage = 1;
  pageSize = 20;
  totalCouriers = 0;

  ngOnInit(): void {
    this.loadCouriers();
  }

  loadCouriers(): void {
    // TODO: Implement
  }
}
