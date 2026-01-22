// src/app/features/notifications/push-notifications/push-notifications.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { NotificationsService } from '../services/notifications.service';

@Component({
  selector: 'app-push-notifications',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    TranslateModule,
  ],
  templateUrl: './push-notifications.component.html',
  styleUrls: ['./push-notifications.component.scss'],
})
export class PushNotificationsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private notificationsService = inject(NotificationsService);

  notificationForm: FormGroup = this.fb.group({
    title: ['', Validators.required],
    message: ['', Validators.required],
    targetAudience: ['all', Validators.required],
    targetType: ['customers'],
  });

  loading = false;

  ngOnInit(): void {}

  sendNotification(): void {
    // TODO: Implement
  }
}
