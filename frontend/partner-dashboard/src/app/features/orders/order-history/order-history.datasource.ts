// src/app/features/orders/order-history/order-history.datasource.ts
import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { Order } from '../models/order.model';
import { OrdersService, PartnerHistoryFilters } from '../services/orders.service';

export class OrderHistoryDataSource extends DataSource<Order> {
  private readonly data$    = new BehaviorSubject<Order[]>([]);
  readonly total$   = new BehaviorSubject<number>(0);
  readonly loading$ = new BehaviorSubject<boolean>(false);
  private loadSub?: Subscription;

  constructor(private readonly ordersService: OrdersService) {
    super();
  }

  connect(): Observable<Order[]> {
    return this.data$.asObservable();
  }

  disconnect(): void {
    this.loadSub?.unsubscribe();
    this.loadSub = undefined;
  }

  load(filters: PartnerHistoryFilters): void {
    this.loadSub?.unsubscribe();
    this.loading$.next(true);
    this.loadSub = this.ordersService.getHistory(filters).subscribe({
      next: (res) => {
        this.data$.next(res.items as Order[]);
        this.total$.next(res.total);
        this.loading$.next(false);
      },
      error: () => {
        this.data$.next([]);
        this.total$.next(0);
        this.loading$.next(false);
      },
    });
  }
}
