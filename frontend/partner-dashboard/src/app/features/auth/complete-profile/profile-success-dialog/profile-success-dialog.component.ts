import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-profile-success-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, TranslateModule],
  templateUrl: './profile-success-dialog.component.html',
  styleUrls: ['./profile-success-dialog.component.scss'],
})
export class ProfileSuccessDialogComponent {
  private dialogRef = inject(MatDialogRef<ProfileSuccessDialogComponent>);

  close(): void {
    this.dialogRef.close(true);
  }
}
