// src/app/shared/directives/permission.directive.ts
import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  inject,
  effect,
} from '@angular/core';
import { AuthService } from '@core/services/auth.service';

/**
 * Structural directive to show/hide elements based on permissions.
 *
 * Usage:
 *   <button *appHasPermission="'payments:view'">Payments</button>
 *   <div *appHasPermission="['orders:view','orders:manage']">...</div>
 */
@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective {
  private templateRef = inject(TemplateRef<any>);
  private viewContainer = inject(ViewContainerRef);
  private authService = inject(AuthService);

  private hasView = false;
  private requiredPermissions: string[] = [];

  @Input() set appHasPermission(permission: string | string[]) {
    this.requiredPermissions = Array.isArray(permission)
      ? permission
      : [permission];
    this.updateView();
  }

  constructor() {
    effect(() => {
      // Re-evaluate whenever the user signal changes
      this.authService.currentUser();
      this.updateView();
    });
  }

  private updateView(): void {
    const hasPermission = this.checkPermission();

    if (hasPermission && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!hasPermission && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }

  private checkPermission(): boolean {
    const user = this.authService.currentUser();
    if (!user || this.requiredPermissions.length === 0) return false;
    return this.authService.hasAnyPermission(this.requiredPermissions);
  }
}

/**
 * Structural directive to show/hide elements based on roles.
 *
 * Usage:
 *   <button *appHasRole="'SUPER_ADMIN'">Super only</button>
 *   <div *appHasRole="['SUPER_ADMIN','FINANCE_ADMIN']">...</div>
 */
@Directive({
  selector: '[appHasRole]',
  standalone: true,
})
export class HasRoleDirective {
  private templateRef = inject(TemplateRef<any>);
  private viewContainer = inject(ViewContainerRef);
  private authService = inject(AuthService);

  private hasView = false;
  private requiredRoles: string[] = [];

  @Input() set appHasRole(role: string | string[]) {
    this.requiredRoles = Array.isArray(role) ? role : [role];
    this.updateView();
  }

  constructor() {
    effect(() => {
      this.authService.currentUser();
      this.updateView();
    });
  }

  private updateView(): void {
    const hasRole = this.checkRole();

    if (hasRole && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!hasRole && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }

  private checkRole(): boolean {
    const user = this.authService.currentUser();
    if (!user || this.requiredRoles.length === 0) return false;
    return this.requiredRoles.includes(user.role);
  }
}
