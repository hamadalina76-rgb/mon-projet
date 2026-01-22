// src/app/features/reviews/flagged-reviews/flagged-reviews.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-flagged-reviews',
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
        <h2>{{ 'reviews.flagged' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Flagged reviews for review</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class FlaggedReviewsComponent implements OnInit {
  ngOnInit(): void {}
}
