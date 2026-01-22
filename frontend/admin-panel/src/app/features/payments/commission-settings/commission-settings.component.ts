// src/app/features/payments/commission-settings/commission-settings.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-commission-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'settings.commissions' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <div class="settings-form">
          <mat-form-field>
            <mat-label>Partner Commission (%)</mat-label>
            <input matInput type="number" [(ngModel)]="partnerCommission">
          </mat-form-field>
          <mat-form-field>
            <mat-label>Courier Commission (%)</mat-label>
            <input matInput type="number" [(ngModel)]="courierCommission">
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="saveSettings()">
            Save Settings
          </button>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .settings-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      max-width: 400px;
    }
  `]
})
export class CommissionSettingsComponent implements OnInit {
  partnerCommission = 10;
  courierCommission = 15;

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    // TODO: Implement loading settings
  }

  saveSettings(): void {
    console.log('Saving commission settings...');
    // TODO: Implement save settings
  }
}
