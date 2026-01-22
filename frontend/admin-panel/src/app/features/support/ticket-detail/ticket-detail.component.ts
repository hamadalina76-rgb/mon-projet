// src/app/features/support/ticket-detail/ticket-detail.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { SupportService } from '../services/support.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule,
    TranslateModule,
  ],
  templateUrl: './ticket-detail.component.html',
  styleUrls: ['./ticket-detail.component.scss'],
})
export class TicketDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private supportService = inject(SupportService);

  ticket: any = null;
  loading = false;
  responseMessage = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTicket(id);
    }
  }

  loadTicket(id: string): void {
    this.loading = true;
    this.supportService.getTicket(id).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  assignToMe(): void {
    console.log('Assigning ticket to me...');
    // TODO: Implement assign to me
  }

  resolveTicket(): void {
    if (confirm('Are you sure you want to resolve this ticket?')) {
      console.log('Resolving ticket...');
      // TODO: Implement resolve ticket
    }
  }

  sendResponse(): void {
    // TODO: Implement
  }
}
