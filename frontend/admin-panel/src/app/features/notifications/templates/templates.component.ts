// src/app/features/notifications/templates/templates.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    TranslateModule,
    DataTableComponent,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'notifications.templates' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <app-data-table [data]="templates" [loading]="loading"></app-data-table>
      </mat-card-content>
    </mat-card>
  `,
})
export class TemplatesComponent implements OnInit {
  templates: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    // TODO: Implement
    this.templates = [];
    this.loading = false;
  }
}
