// src/app/features/users/couriers/courier-detail/courier-detail.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { CouriersService } from '../services/couriers.service';

@Component({
  selector: 'app-courier-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  templateUrl: './courier-detail.component.html',
  styleUrls: ['./courier-detail.component.scss'],
})
export class CourierDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private couriersService = inject(CouriersService);

  courier: any = null;
  loading = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCourier(id);
    }
  }

  loadCourier(id: string): void {
    // TODO: Implement
  }
}
