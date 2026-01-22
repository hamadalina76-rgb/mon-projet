// src/app/features/settings/email-templates/email-templates.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-email-templates',
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
        <h2>{{ 'settings.emailTemplates' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Email template management</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class EmailTemplatesComponent implements OnInit {
  ngOnInit(): void {}
}
