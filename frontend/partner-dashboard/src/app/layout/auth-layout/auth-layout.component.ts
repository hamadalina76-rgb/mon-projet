// src/app/layout/auth-layout/auth-layout.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div class="auth-layout">
      <div class="auth-container">
        <div class="auth-brand">
          <div class="logo-text">🚀 SpeedLine</div>
          <h1>Partner Dashboard</h1>
        </div>
        <div class="auth-content">
          <router-outlet></router-outlet>
        </div>
      </div>
      <div class="auth-bg">
        <div class="overlay"></div>
      </div>
    </div>
  `,
  styleUrls: ['./auth-layout.component.scss'],
})
export class AuthLayoutComponent {}
