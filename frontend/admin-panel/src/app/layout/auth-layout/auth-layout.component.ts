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
      <div class="auth-branding">
        <div class="branding-content">
          <img src="assets/images/logo-white.svg" alt="SpeedLine Admin" class="logo" />
          <h1>SpeedLine Admin</h1>
          <p>Panneau d'administration de la plateforme de livraison</p>
        </div>
      </div>
      <div class="auth-content">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [
    `
      .auth-layout {
        display: flex;
        min-height: 100vh;
      }

      .auth-branding {
        flex: 1;
        background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        padding: 2rem;

        @media (max-width: 1024px) {
          display: none;
        }
      }

      .branding-content {
        text-align: center;
        max-width: 400px;

        .logo {
          width: 120px;
          margin-bottom: 2rem;
        }

        h1 {
          font-size: 2.5rem;
          font-weight: 700;
          margin: 0 0 1rem;
        }

        p {
          font-size: 1.125rem;
          opacity: 0.9;
          margin: 0;
        }
      }

      .auth-content {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 2rem;
        background: var(--bg-main);
      }
    `,
  ],
})
export class AuthLayoutComponent {}
