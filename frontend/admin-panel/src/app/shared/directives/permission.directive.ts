// src/app/shared/directives/permission.directive.ts
import { Directive, Input, TemplateRef, ViewContainerRef, inject, effect } from '@angular/core';
import { AuthService } from '@core/services/auth.service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective {
  private templateRef = inject(TemplateRef<any>);
  private viewContainer = inject(ViewContainerRef);
  private authService = inject(AuthService);

  private hasView = false;
  private requiredPermission = '';

  @Input() set appHasPermission(permission: string) {
    this.requiredPermission = permission;
    this.updateView();
  }

  constructor() {
    effect(() => {
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
    if (!user || !this.requiredPermission) return false;
    return user.permissions?.includes(this.requiredPermission) ?? false;
  }
}
