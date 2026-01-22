// src/app/features/partners/partners-list/partners-list.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'app-partners-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
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
  templateUrl: './partners-list.component.html',
  styleUrls: ['./partners-list.component.scss'],
})
export class PartnersListComponent implements OnInit {
  private partnersService = inject(PartnersService);

  partners: any[] = [];
  loading = false;
  searchTerm = '';
  currentPage = 1;
  pageSize = 20;
  totalPartners = 0;

  ngOnInit(): void {
    this.loadPartners();
  }

  loadPartners(): void {
    // TODO: Implement
  }
}
