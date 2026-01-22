// src/app/features/support/tickets-list/tickets-list.component.ts
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
import { MatBadgeModule } from '@angular/material/badge';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { SupportService } from '../services/support.service';

@Component({
  selector: 'app-tickets-list',
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
    MatBadgeModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './tickets-list.component.html',
  styleUrls: ['./tickets-list.component.scss'],
})
export class TicketsListComponent implements OnInit {
  private supportService = inject(SupportService);

  tickets: any[] = [];
  loading = false;
  statusFilter = 'ALL';
  openCount = 0;

  columns = [
    { key: 'id', label: 'ID' },
    { key: 'subject', label: 'Sujet' },
    { key: 'customer', label: 'Client' },
    { key: 'priority', label: 'Priorité' },
    { key: 'status', label: 'Statut' },
    { key: 'createdAt', label: 'Créé le' },
  ];

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    // TODO: Implement
  }
}

