// src/app/features/support/live-chat/live-chat.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-live-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatListModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'support.liveChat' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <div class="chat-container">
          <mat-list class="chat-list">
            <mat-list-item *ngFor="let chat of activeChats">
              {{ chat.user }} - {{ chat.message }}
            </mat-list-item>
          </mat-list>
          <div class="chat-input">
            <mat-form-field>
              <input matInput placeholder="Type a message..." [(ngModel)]="message">
            </mat-form-field>
            <button mat-icon-button (click)="sendMessage()">
              <mat-icon>send</mat-icon>
            </button>
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .chat-container {
      height: 500px;
      display: flex;
      flex-direction: column;
    }
    .chat-list {
      flex: 1;
      overflow-y: auto;
    }
    .chat-input {
      display: flex;
      gap: 8px;
      padding-top: 16px;
    }
  `]
})
export class LiveChatComponent implements OnInit {
  activeChats: any[] = [];
  message = '';

  ngOnInit(): void {
    this.loadChats();
  }

  loadChats(): void {
    // TODO: Implement chat loading
    this.activeChats = [];
  }

  sendMessage(): void {
    console.log('Sending message:', this.message);
    this.message = '';
  }
}
