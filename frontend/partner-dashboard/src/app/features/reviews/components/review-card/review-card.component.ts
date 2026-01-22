// src/app/features/reviews/components/review-card/review-card.component.ts - Angular 19
import { Component, input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-review-card',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './review-card.component.html',
  styleUrls: ['./review-card.component.scss'],
})
export class ReviewCardComponent {
  // Angular 19 Signal Inputs
  review = input.required<any>();
  
  // Angular 19 Outputs
  reply = output<{ id: string; reply: string }>();

  // Signals
  showReplyForm = signal(false);
  replyText = signal('');

  // Computed
  hasReply = computed(() => !!this.review()?.reply);
  rating = computed(() => this.review()?.rating ?? 0);
  stars = computed(() => Array(5).fill(0).map((_, i) => i < this.rating()));

  submitReply(): void {
    const text = this.replyText().trim();
    if (text) {
      this.reply.emit({ id: this.review().id, reply: text });
      this.showReplyForm.set(false);
      this.replyText.set('');
    }
  }

  updateReplyText(value: string): void {
    this.replyText.set(value);
  }
}
