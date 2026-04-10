import { Component, OnInit, OnDestroy, AfterViewInit, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule, MatTabChangeEvent } from '@angular/material/tabs';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import mapboxgl from 'mapbox-gl';
import { environment } from '@environments/environment';
import { OrdersService } from '../services/orders.service';
import { WebSocketService, CourierPositionEvent, OrderNoteEvent } from '@core/services/websocket.service';
import { AdminOrder, CourierPosition, InternalNote, NoteVisibility, ORDER_STATUS_CONFIG, OrderStatus } from '../models/admin-order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    MatTabsModule, MatDialogModule, MatSnackBarModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private ordersService = inject(OrdersService);
  private wsService = inject(WebSocketService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  order: AdminOrder | null = null;
  loading = true;
  refreshing = false;
  statusConfig = ORDER_STATUS_CONFIG;
  selectedTabIndex = 0;

  // Cancel / Refund dialog state
  showCancelDialog = false;
  showRefundDialog = false;
  cancelReason = '';
  refundAmount = 0;
  actionLoading = false;

  // Force status dialog state
  showForceStatusDialog = false;
  newStatus: OrderStatus = 'CONFIRMED';
  forceStatusNotes = '';

  // Assign courier dialog state
  showAssignCourierDialog = false;
  newCourierId: number | null = null;

  // Contact dialog state
  showContactDialog = false;
  contactTarget: 'customer' | 'partner' | 'courier' = 'customer';
  contactMessage = '';

  // Note dialog state
  showNoteDialog = false;
  noteText = '';
  noteVisibility: NoteVisibility = 'ADMIN_ONLY';

  // Internal notes thread
  notes: InternalNote[] = [];
  notesLoading = false;
  noteInputText = '';
  noteInputVisibility: NoteVisibility = 'ADMIN_ONLY';
  noteSending = false;
  notesCount = 0;

  // WS notes subscription
  private notesSub: Subscription | null = null;
  private notesUnsubscribe: (() => void) | null = null;

  /** Allowed status transitions for admin force-status */
  readonly ALLOWED_TRANSITIONS: Record<string, OrderStatus[]> = {
    PENDING: ['CONFIRMED', 'PREPARING', 'CANCELLED'],
    CONFIRMED: ['PREPARING', 'CANCELLED'],
    PREPARING: ['READY_FOR_PICKUP', 'CANCELLED'],
    READY_FOR_PICKUP: ['PICKED_UP', 'CANCELLED'],
    PICKED_UP: ['IN_DELIVERY', 'CANCELLED'],
    IN_DELIVERY: ['DELIVERED', 'CANCELLED'],
  };

  // Map state
  private map: mapboxgl.Map | null = null;
  private mapInitialized = false;
  private partnerMarker: mapboxgl.Marker | null = null;
  private deliveryMarker: mapboxgl.Marker | null = null;
  private courierMarker: mapboxgl.Marker | null = null;
  courierPosition: CourierPosition | null = null;

  // WS tracking
  private trackingUnsubscribe: (() => void) | null = null;
  private trackingSub: Subscription | null = null;

  getStatusProp(prop: 'color' | 'bg' | 'icon'): string {
    const cfg = this.statusConfig[this.order?.status as OrderStatus];
    return cfg ? cfg[prop] : '';
  }

  /** Ordered pipeline steps */
  readonly pipeline = [
    'PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP', 'PICKED_UP', 'IN_DELIVERY', 'DELIVERED',
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadOrder(id);
  }

  ngOnDestroy(): void {
    this.destroyMap();
    this.stopTracking();
    this.notesSub?.unsubscribe();
    this.notesUnsubscribe?.();
  }

  loadOrder(id: string): void {
    this.loading = true;
    this.ordersService.getOrder(+id).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
        this.refreshing = false;
        // If map tab is active, re-render
        if (this.selectedTabIndex === 2 && this.mapInitialized) {
          setTimeout(() => this.setupMapMarkers(), 100);
        }
        // Start or stop tracking
        if (order.status === 'IN_DELIVERY' && order.courierId) {
          this.startTracking(order.id, order.courierId);
        } else {
          this.stopTracking();
        }
        // Load notes
        this.loadNotes(order.id);
        // Subscribe to real-time notes from other admins
        this.subscribeToNotes(order.id);
      },
      error: () => { this.loading = false; this.refreshing = false; },
    });
  }

  refresh(): void {
    if (!this.order) return;
    this.refreshing = true;
    this.loadOrder(String(this.order.id));
  }

  /** Returns step index of current status in the pipeline (0-based). -1 if cancelled. */
  get currentStepIndex(): number {
    if (!this.order) return -1;
    if (this.order.status === 'CANCELLED') return -1;
    return this.pipeline.indexOf(this.order.status);
  }

  getStepState(i: number): 'done' | 'active' | 'upcoming' {
    const cur = this.currentStepIndex;
    if (cur < 0) return 'upcoming';
    if (i < cur) return 'done';
    if (i === cur) return 'active';
    return 'upcoming';
  }

  /** Timeline icon per action type */
  getTimelineIcon(entry: any): string {
    const map: Record<string, string> = {
      PENDING: 'schedule', CONFIRMED: 'thumb_up', PREPARING: 'restaurant',
      READY_FOR_PICKUP: 'inventory_2', PICKED_UP: 'local_shipping',
      IN_DELIVERY: 'delivery_dining', DELIVERED: 'check_circle', CANCELLED: 'cancel',
    };
    return map[entry.status] || 'fiber_manual_record';
  }

  getTimelineDotClass(entry: any): string {
    const map: Record<string, string> = {
      DELIVERED: 'dot-success', CANCELLED: 'dot-danger',
      PREPARING: 'dot-warning', READY_FOR_PICKUP: 'dot-warning',
      CONFIRMED: 'dot-info', PICKED_UP: 'dot-info', IN_DELIVERY: 'dot-info',
      PENDING: 'dot-default',
    };
    return map[entry.status] || 'dot-default';
  }

  // ── Tab events ──────────────────────────────────────
  onTabChange(event: MatTabChangeEvent): void {
    this.selectedTabIndex = event.index;
    if (event.index === 2) {
      setTimeout(() => this.initMap(), 200);
    }
  }

  // ── Map ─────────────────────────────────────────────
  private initMap(): void {
    if (this.mapInitialized || !this.order) return;
    const container = document.getElementById('order-map');
    if (!container) return;

    mapboxgl.accessToken = environment.mapboxToken;

    const deliveryLat = this.order.deliveryAddress?.latitude ?? this.order.deliveryLatitude;
    const deliveryLng = this.order.deliveryAddress?.longitude ?? this.order.deliveryLongitude;
    const centerLng = deliveryLng ?? 10.1815;
    const centerLat = deliveryLat ?? 36.8065;

    this.map = new mapboxgl.Map({
      container: 'order-map',
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [centerLng, centerLat],
      zoom: 13,
    });

    this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');

    this.map.on('load', () => {
      this.mapInitialized = true;
      this.setupMapMarkers();
    });
  }

  private setupMapMarkers(): void {
    if (!this.map || !this.order) return;

    // Clear existing
    this.partnerMarker?.remove();
    this.deliveryMarker?.remove();
    this.courierMarker?.remove();

    const bounds = new mapboxgl.LngLatBounds();
    let hasMarkers = false;

    // Delivery address marker (green)
    const deliveryLat = this.order.deliveryAddress?.latitude ?? this.order.deliveryLatitude;
    const deliveryLng = this.order.deliveryAddress?.longitude ?? this.order.deliveryLongitude;
    if (deliveryLat && deliveryLng) {
      const el = this.createMarkerEl('#2ecc71', 'person_pin_circle');
      this.deliveryMarker = new mapboxgl.Marker({ element: el })
        .setLngLat([deliveryLng, deliveryLat])
        .setPopup(new mapboxgl.Popup({ offset: 25 }).setHTML(
          `<strong>${this.translate.instant('orderDetail.customer')}</strong><br>${this.order.deliveryAddress?.formattedAddress || this.order.customerName}`
        ))
        .addTo(this.map!);
      bounds.extend([deliveryLng, deliveryLat]);
      hasMarkers = true;
    }

    // Partner marker (red) — use partner address coords if available
    // The API doesn't return partner lat/lng directly, but we place a marker if the delivery address exists
    // For now, offset slightly from delivery to indicate partner location
    // In production, partnerAddress would have coordinates

    // Courier marker (blue) — only if IN_DELIVERY
    if (this.courierPosition) {
      this.addCourierMarker(this.courierPosition.lat, this.courierPosition.lng);
      bounds.extend([this.courierPosition.lng, this.courierPosition.lat]);
      hasMarkers = true;
    }

    if (hasMarkers) {
      if (bounds.getNorthEast().lng === bounds.getSouthWest().lng) {
        this.map!.setCenter(bounds.getCenter());
        this.map!.setZoom(14);
      } else {
        this.map!.fitBounds(bounds, { padding: 60, maxZoom: 16 });
      }
    }
  }

  private createMarkerEl(color: string, icon: string): HTMLElement {
    const el = document.createElement('div');
    el.style.cssText = `
      width: 32px; height: 32px; border-radius: 50%;
      background: ${color}; border: 3px solid #fff;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
      display: flex; align-items: center; justify-content: center;
      cursor: pointer;
    `;
    const iconEl = document.createElement('span');
    iconEl.className = 'material-icons';
    iconEl.style.cssText = 'font-size: 16px; color: #fff;';
    iconEl.textContent = icon;
    el.appendChild(iconEl);
    return el;
  }

  private addCourierMarker(lat: number, lng: number): void {
    if (!this.map) return;
    const el = this.createMarkerEl('#3498db', 'delivery_dining');
    el.classList.add('courier-marker-pulse');
    this.courierMarker = new mapboxgl.Marker({ element: el })
      .setLngLat([lng, lat])
      .setPopup(new mapboxgl.Popup({ offset: 25 }).setHTML(
        `<strong>${this.translate.instant('orderDetail.courier')}</strong><br>${this.order?.courierName || ''}`
      ))
      .addTo(this.map);
  }

  private destroyMap(): void {
    this.partnerMarker?.remove();
    this.deliveryMarker?.remove();
    this.courierMarker?.remove();
    this.map?.remove();
    this.map = null;
    this.mapInitialized = false;
  }

  // ── GPS Tracking ────────────────────────────────────
  private startTracking(orderId: number, courierId: number): void {
    this.stopTracking();

    // Fetch initial position via REST
    this.ordersService.getCourierPosition(courierId).subscribe({
      next: (pos) => {
        if (pos) {
          this.courierPosition = pos;
          if (this.mapInitialized) this.updateCourierOnMap(pos.lat, pos.lng);
        }
      },
      error: () => { /* courier position not available yet */ },
    });

    // Subscribe to WebSocket for live updates
    const tracking = this.wsService.subscribeToCourierTracking(orderId);
    this.trackingUnsubscribe = tracking.unsubscribe;
    this.trackingSub = tracking.positions$.subscribe((pos: CourierPositionEvent) => {
      this.courierPosition = { lat: pos.lat, lng: pos.lng, heading: pos.heading, speed: pos.speed };
      if (this.mapInitialized) this.updateCourierOnMap(pos.lat, pos.lng);
    });
  }

  private stopTracking(): void {
    this.trackingSub?.unsubscribe();
    this.trackingSub = null;
    this.trackingUnsubscribe?.();
    this.trackingUnsubscribe = null;
  }

  private updateCourierOnMap(lat: number, lng: number): void {
    if (this.courierMarker) {
      this.courierMarker.setLngLat([lng, lat]);
    } else {
      this.addCourierMarker(lat, lng);
    }
  }

  // ── Admin Actions ───────────────────────────────────
  get canCancel(): boolean {
    return !!this.order?.isCancellable ||
      ['PENDING', 'CONFIRMED', 'PREPARING'].includes(this.order?.status || '');
  }

  get canRefund(): boolean {
    return this.order?.status === 'DELIVERED' || this.order?.status === 'CANCELLED';
  }

  get canForceStatus(): boolean {
    if (!this.order) return false;
    const transitions = this.ALLOWED_TRANSITIONS[this.order.status];
    return !!transitions && transitions.length > 0;
  }

  get availableTransitions(): OrderStatus[] {
    if (!this.order) return [];
    return this.ALLOWED_TRANSITIONS[this.order.status] || [];
  }

  get canAssignCourier(): boolean {
    return this.order?.status === 'READY_FOR_PICKUP';
  }

  get canContact(): boolean {
    return !!this.order;
  }

  get canAddNote(): boolean {
    return !!this.order;
  }

  openCancelDialog(): void {
    this.cancelReason = '';
    this.showCancelDialog = true;
  }

  closeCancelDialog(): void {
    this.showCancelDialog = false;
  }

  confirmCancel(): void {
    if (!this.order || !this.cancelReason.trim()) return;
    this.actionLoading = true;
    this.ordersService.cancelOrder(String(this.order.id), this.cancelReason).subscribe({
      next: () => {
        this.actionLoading = false;
        this.showCancelDialog = false;
        this.snackBar.open(this.translate.instant('orderDetail.cancelSuccess'), '✕', { duration: 3000 });
        this.refresh();
      },
      error: () => {
        this.actionLoading = false;
        this.snackBar.open(this.translate.instant('orderDetail.cancelError'), '✕', { duration: 3000 });
      },
    });
  }

  openRefundDialog(): void {
    this.refundAmount = this.order?.total || 0;
    this.showRefundDialog = true;
  }

  closeRefundDialog(): void {
    this.showRefundDialog = false;
  }

  confirmRefund(): void {
    if (!this.order || this.refundAmount <= 0) return;
    this.actionLoading = true;
    this.ordersService.refundOrder(String(this.order.id), this.refundAmount).subscribe({
      next: () => {
        this.actionLoading = false;
        this.showRefundDialog = false;
        this.snackBar.open(this.translate.instant('orderDetail.refundSuccess'), '✕', { duration: 3000 });
        this.refresh();
      },
      error: () => {
        this.actionLoading = false;
        this.snackBar.open(this.translate.instant('orderDetail.refundError'), '✕', { duration: 3000 });
      },
    });
  }

  // ── Force Status ────────────────────────────────────
  openForceStatusDialog(): void {
    const transitions = this.availableTransitions;
    this.newStatus = transitions.length ? transitions[0] : 'CONFIRMED';
    this.forceStatusNotes = '';
    this.showForceStatusDialog = true;
  }

  closeForceStatusDialog(): void {
    this.showForceStatusDialog = false;
  }

  confirmForceStatus(): void {
    if (!this.order) return;
    this.actionLoading = true;
    this.ordersService.forceStatus(String(this.order.id), this.newStatus, this.forceStatusNotes).subscribe({
      next: () => {
        this.actionLoading = false;
        this.showForceStatusDialog = false;
        this.snackBar.open(this.translate.instant('orderDetail.forceStatusSuccess'), '✕', { duration: 3000 });
        this.refresh();
      },
      error: () => {
        this.actionLoading = false;
        this.snackBar.open(this.translate.instant('orderDetail.forceStatusError'), '✕', { duration: 3000 });
      },
    });
  }

  // ── Assign Courier ──────────────────────────────────
  openAssignCourierDialog(): void {
    this.newCourierId = null;
    this.showAssignCourierDialog = true;
  }

  closeAssignCourierDialog(): void {
    this.showAssignCourierDialog = false;
  }

  confirmAssignCourier(): void {
    if (!this.order || !this.newCourierId) return;
    this.actionLoading = true;
    this.ordersService.assignCourier(String(this.order.id), this.newCourierId).subscribe({
      next: () => {
        this.actionLoading = false;
        this.showAssignCourierDialog = false;
        this.snackBar.open(this.translate.instant('orderDetail.assignSuccess'), '✕', { duration: 3000 });
        this.refresh();
      },
      error: () => {
        this.actionLoading = false;
        this.snackBar.open(this.translate.instant('orderDetail.assignError'), '✕', { duration: 3000 });
      },
    });
  }

  // ── Contact ─────────────────────────────────────────
  openContactDialog(target: 'customer' | 'partner' | 'courier'): void {
    this.contactTarget = target;
    this.contactMessage = '';
    this.showContactDialog = true;
  }

  closeContactDialog(): void {
    this.showContactDialog = false;
  }

  confirmContact(): void {
    if (!this.order || !this.contactMessage.trim()) return;
    let targetUserId = 0;
    if (this.contactTarget === 'customer') targetUserId = this.order.customerId;
    else if (this.contactTarget === 'partner') targetUserId = this.order.partnerId;
    else if (this.contactTarget === 'courier') targetUserId = this.order.courierId || 0;

    const title = this.translate.instant('orderDetail.adminMessageTitle', {
      orderNumber: this.order.orderNumber,
    });
    const data = {
      orderId: this.order.id,
      orderNumber: this.order.orderNumber,
      action: 'ADMIN_CONTACT',
      senderRole: 'ADMIN',
    };
    this.actionLoading = true;
    this.ordersService.sendNotification(targetUserId, title, this.contactMessage, data).subscribe({
      next: () => {
        this.actionLoading = false;
        this.showContactDialog = false;
        this.snackBar.open(this.translate.instant('orderDetail.contactSuccess'), '✕', { duration: 3000 });
      },
      error: () => {
        this.actionLoading = false;
        this.snackBar.open(this.translate.instant('orderDetail.contactError'), '✕', { duration: 3000 });
      },
    });
  }

  // ── Internal Notes ──────────────────────────────────
  private subscribeToNotes(orderId: number): void {
    // Clean up previous subscription
    this.notesSub?.unsubscribe();
    this.notesUnsubscribe?.();

    const ws = this.wsService.subscribeToOrderNotes(orderId);
    this.notesUnsubscribe = ws.unsubscribe;
    this.notesSub = ws.notes$.subscribe((note: OrderNoteEvent) => {
      // Avoid duplicating notes we just sent (already in the list via optimistic update)
      const exists = this.notes.some(n => n.id === note.id);
      if (!exists) {
        this.notes = [...this.notes, {
          ...note,
          isPending: false,
        } as InternalNote];
        this.notesCount = this.notes.length;
      }
    });
  }

  loadNotes(orderId: number): void {
    this.notesLoading = true;
    this.ordersService.getNotes(orderId).subscribe({
      next: (notes) => {
        this.notes = notes;
        this.notesCount = notes.length;
        this.notesLoading = false;
      },
      error: () => {
        this.notes = [];
        this.notesCount = 0;
        this.notesLoading = false;
      },
    });
  }

  sendNote(): void {
    if (!this.order || !this.noteInputText.trim()) return;
    const tempId = 'temp-' + Date.now();
    const optimisticNote: InternalNote = {
      id: 0,
      orderId: this.order.id,
      content: this.noteInputText.trim(),
      visibility: this.noteInputVisibility,
      authorId: 0,
      authorName: 'Moi',
      authorRole: 'ADMIN',
      createdAt: new Date().toISOString(),
      isPending: true,
      tempId,
    };

    this.notes = [...this.notes, optimisticNote];
    this.notesCount = this.notes.length;
    const content = this.noteInputText.trim();
    const visibility = this.noteInputVisibility;
    this.noteInputText = '';
    this.noteSending = true;

    this.ordersService.addNote(this.order.id, content, visibility).subscribe({
      next: (savedNote) => {
        this.notes = this.notes.map(n => n.tempId === tempId ? { ...savedNote, isPending: false } : n);
        this.notesCount = this.notes.length;
        this.noteSending = false;
      },
      error: () => {
        this.notes = this.notes.filter(n => n.tempId !== tempId);
        this.notesCount = this.notes.length;
        this.noteSending = false;
        this.snackBar.open(this.translate.instant('orderDetail.noteSendError'), '✕', { duration: 3000 });
      },
    });
  }

  deleteNote(noteId: number): void {
    if (!this.order) return;
    this.ordersService.deleteNote(this.order.id, noteId).subscribe({
      next: () => {
        this.notes = this.notes.filter(n => n.id !== noteId);
        this.notesCount = this.notes.length;
        this.snackBar.open(this.translate.instant('orderDetail.noteDeleted'), '✕', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open(this.translate.instant('orderDetail.noteDeleteError'), '✕', { duration: 3000 });
      },
    });
  }

  onNoteKeydown(event: KeyboardEvent): void {
    if (event.ctrlKey && event.key === 'Enter') {
      event.preventDefault();
      this.sendNote();
    }
  }

  getRelativeTime(dateStr: string): string {
    const now = new Date();
    const date = new Date(dateStr);
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return this.translate.instant('orderDetail.justNow');
    if (diffMin < 60) return this.translate.instant('orderDetail.minutesAgo', { count: diffMin });
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return this.translate.instant('orderDetail.hoursAgo', { count: diffHours });
    const diffDays = Math.floor(diffHours / 24);
    return this.translate.instant('orderDetail.daysAgo', { count: diffDays });
  }

  // Keep the old dialog methods for the action-grid button
  openNoteDialog(): void {
    this.noteText = '';
    this.showNoteDialog = false;
    this.selectedTabIndex = 5; // Switch to notes tab
  }

  closeNoteDialog(): void {
    this.showNoteDialog = false;
  }

  confirmNote(): void {
    // Redirect to the in-tab send
    this.noteInputText = this.noteText;
    this.sendNote();
    this.showNoteDialog = false;
  }
}
