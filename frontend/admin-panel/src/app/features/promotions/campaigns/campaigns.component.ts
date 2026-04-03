// src/app/features/promotions/campaigns/campaigns.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { PromotionsService } from '../services/promotions.service';

@Component({
  selector: 'app-campaigns',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  templateUrl: './campaigns.component.html',
  styleUrls: ['./campaigns.component.scss'],
})
export class CampaignsComponent implements OnInit {
  private promotionsService = inject(PromotionsService);

  campaigns: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadCampaigns();
  }

  loadCampaigns(): void {
    // TODO: Implement
  }

  deleteCampaign(id: number): void {
    // TODO: Implement
  }
}
