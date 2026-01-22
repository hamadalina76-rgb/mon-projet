// src/app/features/users/admins/permissions-manager/permissions-manager.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-permissions-manager',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCheckboxModule,
    MatExpansionModule,
    TranslateModule,
  ],
  templateUrl: './permissions-manager.component.html',
  styleUrls: ['./permissions-manager.component.scss'],
})
export class PermissionsManagerComponent {
  @Input() permissions: string[] = [];
  @Output() permissionsChange = new EventEmitter<string[]>();

  // TODO: Implement permissions management
}
