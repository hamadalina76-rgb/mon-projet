// src/app/features/zones/zones-list/zones-list.component.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';

interface Zone {
    id: string;
    name: string;
    city: string;
    isActive: boolean;
    createdAt: string;
}

@Component({
    selector: 'app-zones-list',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatTableModule,
        MatPaginatorModule,
        MatProgressSpinnerModule,
        TranslateModule,
    ],
    template: `
    <div class="zones-list-container">
      <div class="page-header">
        <h1>{{ 'zones.title' | translate }}</h1>
        <button mat-raised-button color="primary" routerLink="editor">
          <mat-icon>add</mat-icon>
          {{ 'zones.addZone' | translate }}
        </button>
      </div>

      @if (loading()) {
        <div class="loading-container">
          <mat-spinner></mat-spinner>
        </div>
      } @else {
        <mat-card>
          <mat-card-content>
            <table mat-table [dataSource]="zones()" class="zones-table">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>{{ 'zones.name' | translate }}</th>
                <td mat-cell *matCellDef="let zone">{{ zone.name }}</td>
              </ng-container>

              <ng-container matColumnDef="city">
                <th mat-header-cell *matHeaderCellDef>{{ 'zones.city' | translate }}</th>
                <td mat-cell *matCellDef="let zone">{{ zone.city }}</td>
              </ng-container>

              <ng-container matColumnDef="isActive">
                <th mat-header-cell *matHeaderCellDef>{{ 'zones.status' | translate }}</th>
                <td mat-cell *matCellDef="let zone">
                  <span class="status-badge" [class.active]="zone.isActive">
                    {{ zone.isActive ? 'Active' : 'Inactive' }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>{{ 'common.actions' | translate }}</th>
                <td mat-cell *matCellDef="let zone">
                  <button mat-icon-button [routerLink]="[zone.id, 'edit']">
                    <mat-icon>edit</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
    styles: [`
    .zones-list-container {
      padding: 1.5rem;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;

      h1 {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 600;
      }
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 3rem;
    }

    .zones-table {
      width: 100%;
    }

    .status-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 1rem;
      font-size: 0.75rem;
      background: var(--bg-secondary);
      color: var(--text-secondary);

      &.active {
        background: var(--success-bg);
        color: var(--success);
      }
    }
  `],
})
export class ZonesListComponent implements OnInit {
    loading = signal(false);
    zones = signal<Zone[]>([]);
    displayedColumns = ['name', 'city', 'isActive', 'actions'];

    ngOnInit(): void {
        this.loadZones();
    }

    private loadZones(): void {
        this.loading.set(true);
        // Simulated data - replace with API call
        setTimeout(() => {
            this.zones.set([
                { id: '1', name: 'Zone Centre', city: 'Tunis', isActive: true, createdAt: '2024-01-15' },
                { id: '2', name: 'Zone Nord', city: 'Tunis', isActive: true, createdAt: '2024-01-16' },
                { id: '3', name: 'Zone Sud', city: 'Sfax', isActive: false, createdAt: '2024-01-17' },
            ]);
            this.loading.set(false);
        }, 500);
    }
}
