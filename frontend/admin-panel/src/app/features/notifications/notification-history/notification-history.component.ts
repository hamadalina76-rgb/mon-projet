// src/app/features/notifications/notification-history/notification-history.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { NotificationsService } from '../services/notifications.service';

@Component({
  selector: 'app-notification-history',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './notification-history.component.html',
  styleUrls: ['./notification-history.component.scss'],
})
export class NotificationHistoryComponent implements OnInit {
  private notificationsService = inject(NotificationsService);

  notifications: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    // TODO: Implement
  }
}
