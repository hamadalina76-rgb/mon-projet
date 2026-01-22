// src/app/features/support/faq-manager/faq-manager.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { SupportService } from '../services/support.service';

@Component({
  selector: 'app-faq-manager',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatExpansionModule,
    MatDialogModule,
    TranslateModule,
  ],
  templateUrl: './faq-manager.component.html',
  styleUrls: ['./faq-manager.component.scss'],
})
export class FaqManagerComponent implements OnInit {
  private supportService = inject(SupportService);
  private dialog = inject(MatDialog);

  faqs: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadFAQs();
  }

  loadFAQs(): void {
    this.loading = true;
    // TODO: Implement loading FAQs
    this.faqs = [];
    this.loading = false;
  }

  openCreateDialog(): void {
    // TODO: Implement
  }

  editFAQ(faq: any): void {
    console.log('Editing FAQ:', faq);
    // TODO: Implement edit FAQ
  }

  deleteFAQ(id: string): void {
    if (confirm('Are you sure you want to delete this FAQ?')) {
      console.log('Deleting FAQ:', id);
      // TODO: Implement delete FAQ
    }
  }
}
