// src/app/features/settings/notification-settings/notification-settings.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-notification-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatSlideToggleModule,
    MatButtonModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'settings.notifications' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <div class="settings-list">
          <mat-slide-toggle [(ngModel)]="emailNotifications">
            Email Notifications
          </mat-slide-toggle>
          <mat-slide-toggle [(ngModel)]="pushNotifications">
            Push Notifications
          </mat-slide-toggle>
          <mat-slide-toggle [(ngModel)]="smsNotifications">
            SMS Notifications
          </mat-slide-toggle>
        </div>
        <button mat-raised-button color="primary" (click)="saveSettings()">
          Save Settings
        </button>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .settings-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-bottom: 24px;
    }
  `]
})
export class NotificationSettingsComponent implements OnInit {
  emailNotifications = true;
  pushNotifications = true;
  smsNotifications = false;

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    // TODO: Implement
  }

  saveSettings(): void {
    console.log('Saving notification settings...');
  }
}
