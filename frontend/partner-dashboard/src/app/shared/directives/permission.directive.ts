// src/app/shared/directives/permission.directive.ts - Angular 19
import { Directive, TemplateRef, ViewContainerRef, inject, input, effect } from '@angular/core';
import { AuthService } from '@core/services/auth.service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective {
  // Angular 19 Signal Input
  requiredRoles = input<string[]>([], { alias: 'appHasPermission' });

  private templateRef = inject(TemplateRef<unknown>);
  private viewContainer = inject(ViewContainerRef);
  private authService = inject(AuthService);

  constructor() {
    effect(() => {
      this.updateView();
    });
  }

  private updateView(): void {
    const userRole = this.authService.getUserRole();
    const roles = this.requiredRoles();

    this.viewContainer.clear();

    if (!roles || roles.length === 0) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      return;
    }

    if (userRole && roles.includes(userRole)) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}
