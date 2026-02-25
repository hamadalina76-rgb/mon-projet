// src/app/core/services/activity-log.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { AdminActivityLog } from '@core/models/user.model';

@Injectable({ providedIn: 'root' })
export class ActivityLogService {
  private api = inject(ApiService);
  private authService = inject(AuthService);

  private logs: AdminActivityLog[] = [];

  /**
   * Log an admin action locally and send to server
   */
  log(action: string, resource: string, resourceId?: string, details?: string): void {
    const currentUser = this.authService.currentUser();
    
    const entry: AdminActivityLog = {
      id: crypto.randomUUID(),
      adminId: currentUser?.id?.toString() || '',
      adminName: `${currentUser?.firstName || ''} ${currentUser?.lastName || ''}`.trim() || 'Unknown',
      action,
      resource,
      resourceId,
      details,
      timestamp: new Date().toISOString(),
    };

    this.logs.push(entry);

    // Fire-and-forget to server
    this.api.post('admin/activity-logs', entry).subscribe({
      error: () => {
        /* silently ignore if server is down */
      },
    });
  }

  /**
   * Get activity logs from server
   */
  getActivityLogs(page = 0, size = 25): Observable<any> {
    return this.api.get(`admin/activity-logs?page=${page}&size=${size}`);
  }

  /**
   * Get activity logs for a resource and resource id (e.g. customers/123)
   */
  getActivityLogsByResource(resource: string, resourceId: string, page = 0, size = 25): Observable<any> {
    return this.api.get(`admin/activity-logs/by-resource/${resource}/${resourceId}?page=${page}&size=${size}`);
  }

  /**
   * Get local logs (useful for debugging)
   */
  getLocalLogs(): AdminActivityLog[] {
    return [...this.logs];
  }
}
