// src/app/features/users/customers/customers-list/customers-list.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { CustomersService } from '../services/customers.service';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-customers-list',
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
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './customers-list.component.html',
  styleUrls: ['./customers-list.component.scss'],
})
export class CustomersListComponent implements OnInit {
  private customersService = inject(CustomersService);

  customers: any[] = [];
  loading = false;
  searchTerm = '';
  currentPage = 1;
  pageSize = 20;
  totalCustomers = 0;
  statusFilter = 'ALL';

  columns = [
    { key: 'name', label: 'Nom' },
    { key: 'email', label: 'Email' },
    { key: 'phone', label: 'Téléphone' },
    { key: 'status', label: 'Statut' },
    { key: 'createdAt', label: 'Date d\'inscription' },
  ];

  actions = [
    { icon: 'visibility', action: 'view', label: 'common.view' },
    { icon: 'block', action: 'block', label: 'common.block' },
  ];

  private searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.loadCustomers();
    this.setupSearch();
  }

  setupSearch(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => {
        // TODO: Implement search
      });
  }

  loadCustomers(): void {
    // TODO: Implement load customers
  }

  onSearchChange(term: string): void {
    this.searchSubject.next(term);
  }

  exportCustomers(): void {
    // TODO: Implement export
  }

  filterByStatus(status: string): void {
    this.statusFilter = status;
    this.loadCustomers();
  }

  onAction(event: { action: string; row: any }): void {
    // TODO: Handle action
  }

  onPageChange(event: any): void {
    this.currentPage = event.pageIndex + 1;
    this.loadCustomers();
  }
}

