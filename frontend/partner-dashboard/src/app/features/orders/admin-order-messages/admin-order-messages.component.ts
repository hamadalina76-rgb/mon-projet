import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';

/** Entrée API notifications (alignée sur le header partenaire). */
interface AdminContactRow {
  id: string;
  title: string;
  message: string;
  createdAt: string;
  isRead: boolean;
  orderId: string;
  orderNumber?: string;
}

@Component({
  selector: 'app-admin-order-messages',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './admin-order-messages.component.html',
  styleUrl: './admin-order-messages.component.scss',
})
export class AdminOrderMessagesComponent implements OnInit {
  private auth = inject(AuthService);
  private notificationsApi = inject(NotificationService);
  private router = inject(Router);

  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly rows = signal<AdminContactRow[]>([]);

  ngOnInit(): void {
    this.loadMessages();
  }

  private loadMessages(): void {
    const user = this.auth.currentUser();
    if (!user?.id) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.loadError.set(false);

    this.notificationsApi.getNotifications(Number(user.id), 0, 100).subscribe({
      next: (res) => {
        const content = (res?.content ?? []) as any[];
        const mapped: AdminContactRow[] = content
          .filter((n) => n?.data?.action === 'ADMIN_CONTACT')
          .map((n) => {
            const oid = n.data?.orderId ?? n.data?.id;
            return {
              id: String(n.id),
              title: n.title ?? '',
              message: n.message ?? '',
              createdAt: n.createdAt,
              isRead: !!n.isRead,
              orderId: oid != null ? String(oid) : '',
              orderNumber: n.data?.orderNumber != null ? String(n.data.orderNumber) : undefined,
            };
          })
          .filter((r) => r.orderId.length > 0);
        this.rows.set(mapped);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  goToOrder(row: AdminContactRow): void {
    this.router.navigate(['/orders', row.orderId]);
  }

  refresh(): void {
    this.loadMessages();
  }
}
