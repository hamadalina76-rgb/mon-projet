// src/app/layout/auth-layout/auth-layout.component.ts
// The auth pages (login, forgot-password) are full-page designs.
// This layout just provides a transparent wrapper with a router-outlet.
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet></router-outlet>`,
  styles: [`:host { display: block; min-height: 100vh; }`],
})
export class AuthLayoutComponent {}
