// src/app/features/users/couriers/courier-approval/courier-approval.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { CouriersService } from '../services/couriers.service';

@Component({
  selector: 'app-courier-approval',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './courier-approval.component.html',
  styleUrls: ['./courier-approval.component.scss'],
})
export class CourierApprovalComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
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

  approveCourier(): void {
    // TODO: Implement
  }

  rejectCourier(): void {
    // TODO: Implement
  }
}
