// src/app/features/notifications/send-notification/send-notification.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-send-notification',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'notifications.send' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="notificationForm" (ngSubmit)="sendNotification()">
          <mat-form-field>
            <mat-label>Title</mat-label>
            <input matInput formControlName="title">
          </mat-form-field>
          <mat-form-field>
            <mat-label>Message</mat-label>
            <textarea matInput formControlName="message" rows="4"></textarea>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Target Audience</mat-label>
            <mat-select formControlName="audience">
              <mat-option value="all">All Users</mat-option>
              <mat-option value="customers">Customers</mat-option>
              <mat-option value="partners">Partners</mat-option>
            </mat-select>
          </mat-form-field>
          <button mat-raised-button color="primary" type="submit">
            Send Notification
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      max-width: 600px;
    }
  `]
})
export class SendNotificationComponent {
  notificationForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.notificationForm = this.fb.group({
      title: [''],
      message: [''],
      audience: ['all'],
    });
  }

  sendNotification(): void {
    console.log('Sending notification:', this.notificationForm.value);
  }
}
