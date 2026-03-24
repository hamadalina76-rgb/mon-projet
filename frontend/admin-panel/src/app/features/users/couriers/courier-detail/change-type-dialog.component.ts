import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-change-type-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatRippleModule,
    TranslateModule,
  ],
  templateUrl: './change-type-dialog.component.html',
  styleUrl: './change-type-dialog.component.scss',
})
export class ChangeTypeDialogComponent {
  private dialogRef = inject(MatDialogRef<ChangeTypeDialogComponent>);
  currentType: string = inject(MAT_DIALOG_DATA);
  selected = signal<'INTERNAL' | 'EXTERNAL' | null>(this.currentType as any ?? null);

  onConfirm(): void {
    const type = this.selected();
    if (type && type !== this.currentType) this.dialogRef.close(type);
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
