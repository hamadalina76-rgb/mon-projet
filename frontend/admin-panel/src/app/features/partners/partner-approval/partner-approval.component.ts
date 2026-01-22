// src/app/features/partners/partner-approval/partner-approval.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'app-partner-approval',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './partner-approval.component.html',
  styleUrls: ['./partner-approval.component.scss'],
})
export class PartnerApprovalComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private partnersService = inject(PartnersService);

  partner: any = null;
  loading = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPartner(id);
    }
  }

  loadPartner(id: string): void {
    // TODO: Implement
  }

  approvePartner(): void {
    // TODO: Implement
  }

  rejectPartner(): void {
    // TODO: Implement
  }
}
