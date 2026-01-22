// src/app/features/partners/partner-verification/partner-verification.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TranslateModule } from '@ngx-translate/core';
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'app-partner-verification',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    TranslateModule,
  ],
  templateUrl: './partner-verification.component.html',
  styleUrls: ['./partner-verification.component.scss'],
})
export class PartnerVerificationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private partnersService = inject(PartnersService);

  partner: any = null;
  documents: any[] = [];
  loading = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPartner(id);
    }
  }

  loadPartner(id: string): void {
    this.loading = true;
    this.partnersService.getPartner(id).subscribe({
      next: (partner) => {
        this.partner = partner;
        this.documents = partner.documents || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  verifyDocument(documentId: string): void {
    // TODO: Implement
  }

  viewDocument(doc: any): void {
    window.open(doc.url, '_blank');
  }

  saveVerification(): void {
    console.log('Saving verification...');
    // TODO: Implement save verification
  }
}
