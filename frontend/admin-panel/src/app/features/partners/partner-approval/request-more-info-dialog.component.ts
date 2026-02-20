// src/app/features/partners/partner-approval/request-more-info-dialog.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-request-more-info-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    TranslateModule,
  ],
  templateUrl: './request-more-info-dialog.component.html',
  styleUrls: ['./request-more-info-dialog.component.scss'],
})
export class RequestMoreInfoDialogComponent {
  private dialogRef = inject(MatDialogRef<RequestMoreInfoDialogComponent>);
  message = '';

  cancel(): void {
    this.dialogRef.close(false);
  }

  send(): void {
    if (this.message.trim()) {
      this.dialogRef.close(this.message.trim());
    }
  }
}
