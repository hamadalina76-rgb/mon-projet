// src/app/features/reviews/review-moderation/review-moderation.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-review-moderation',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'reviews.moderation' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Review moderation queue</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class ReviewModerationComponent implements OnInit {
  ngOnInit(): void {}
}
