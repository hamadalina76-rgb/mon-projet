// src/app/shared/components/data-table/data-table.component.ts
import { Component, Input, Output, EventEmitter, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { SelectionModel } from '@angular/cdk/collections';
import { TranslateModule } from '@ngx-translate/core';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  type?: 'text' | 'date' | 'currency' | 'status' | 'actions';
  format?: (value: any) => string;
}

export interface TableAction {
  icon: string;
  label: string;
  action: string;
  color?: string;
  condition?: (row: any) => boolean;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    TranslateModule,
  ],
  template: `
    @if (loading) {
      <mat-progress-bar mode="indeterminate"></mat-progress-bar>
    }

    <div class="table-container">
      <table mat-table [dataSource]="dataSource" matSort (matSortChange)="onSort($event)">
        <!-- Selection Column -->
        @if (selectable) {
          <ng-container matColumnDef="select">
            <th mat-header-cell *matHeaderCellDef>
              <mat-checkbox
                (change)="$event ? toggleAllRows() : null"
                [checked]="selection.hasValue() && isAllSelected()"
                [indeterminate]="selection.hasValue() && !isAllSelected()"
              >
              </mat-checkbox>
            </th>
            <td mat-cell *matCellDef="let row">
              <mat-checkbox
                (click)="$event.stopPropagation()"
                (change)="$event ? selection.toggle(row) : null"
                [checked]="selection.isSelected(row)"
              >
              </mat-checkbox>
            </td>
          </ng-container>
        }

        <!-- Dynamic Columns -->
        @for (column of columns; track column.key) {
          <ng-container [matColumnDef]="column.key">
            <th mat-header-cell *matHeaderCellDef [mat-sort-header]="column.sortable ? column.key : ''">
              {{ column.label | translate }}
            </th>
            <td mat-cell *matCellDef="let row">
              @switch (column.type) {
                @case ('status') {
                  <span class="status-badge" [attr.data-status]="row[column.key]">
                    {{ row[column.key] }}
                  </span>
                }
                @case ('currency') {
                  {{ row[column.key] | currency:'TND':'symbol':'1.2-2' }}
                }
                @case ('date') {
                  {{ row[column.key] | date:'dd/MM/yyyy HH:mm' }}
                }
                @case ('actions') {
                  <button mat-icon-button [matMenuTriggerFor]="actionsMenu">
                    <mat-icon>more_vert</mat-icon>
                  </button>
                  <mat-menu #actionsMenu="matMenu">
                    @for (action of actions; track action.action) {
                      @if (!action.condition || action.condition(row)) {
                        <button mat-menu-item (click)="onAction(action.action, row)">
                          <mat-icon [style.color]="action.color">{{ action.icon }}</mat-icon>
                          <span>{{ action.label | translate }}</span>
                        </button>
                      }
                    }
                  </mat-menu>
                }
                @default {
                  {{ column.format ? column.format(row[column.key]) : row[column.key] }}
                }
              }
            </td>
          </ng-container>
        }

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr
          mat-row
          *matRowDef="let row; columns: displayedColumns"
          [class.selected]="selection.isSelected(row)"
          (click)="rowClick.emit(row)"
        ></tr>
      </table>

      @if (dataSource.data.length === 0 && !loading) {
        <div class="empty-state">
          <mat-icon>inbox</mat-icon>
          <p>{{ 'common.noData' | translate }}</p>
        </div>
      }
    </div>

    <mat-paginator
      [length]="totalItems"
      [pageSize]="pageSize"
      [pageSizeOptions]="pageSizeOptions"
      [pageIndex]="pageIndex"
      (page)="onPageChange($event)"
      showFirstLastButtons
    >
    </mat-paginator>
  `,
  styles: [
    `
      .table-container {
        overflow-x: auto;
      }

      table {
        width: 100%;
      }

      .selected {
        background-color: rgba(79, 70, 229, 0.08);
      }

      .status-badge {
        padding: 0.25rem 0.75rem;
        border-radius: 100px;
        font-size: 0.75rem;
        font-weight: 500;

        &[data-status='active'],
        &[data-status='approved'],
        &[data-status='completed'] {
          background-color: rgba(16, 185, 129, 0.1);
          color: #10b981;
        }

        &[data-status='pending'] {
          background-color: rgba(245, 158, 11, 0.1);
          color: #f59e0b;
        }

        &[data-status='rejected'],
        &[data-status='cancelled'],
        &[data-status='inactive'] {
          background-color: rgba(239, 68, 68, 0.1);
          color: #ef4444;
        }
      }

      .empty-state {
        padding: 3rem;
        text-align: center;
        color: var(--text-secondary);

        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          margin-bottom: 1rem;
        }
      }
    `,
  ],
})
export class DataTableComponent<T> implements AfterViewInit {
  @Input() set data(value: T[]) {
    this.dataSource.data = value;
  }
  @Input() columns: TableColumn[] = [];
  @Input() actions: TableAction[] = [];
  @Input() loading = false;
  @Input() selectable = false;
  @Input() totalItems = 0;
  @Input() pageSize = 20;
  @Input() pageIndex = 0;
  @Input() pageSizeOptions = [10, 20, 50, 100];

  @Output() actionClick = new EventEmitter<{ action: string; row: T }>();
  @Output() rowClick = new EventEmitter<T>();
  @Output() selectionChange = new EventEmitter<T[]>();
  @Output() sortChange = new EventEmitter<Sort>();
  @Output() pageChange = new EventEmitter<PageEvent>();

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  dataSource = new MatTableDataSource<T>([]);
  selection = new SelectionModel<T>(true, []);

  get displayedColumns(): string[] {
    const cols = this.columns.map((c) => c.key);
    return this.selectable ? ['select', ...cols] : cols;
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.selection.select(...this.dataSource.data);
    }
    this.selectionChange.emit(this.selection.selected);
  }

  onAction(action: string, row: T): void {
    this.actionClick.emit({ action, row });
  }

  onSort(sort: Sort): void {
    this.sortChange.emit(sort);
  }

  onPageChange(event: PageEvent): void {
    this.pageChange.emit(event);
  }
}
