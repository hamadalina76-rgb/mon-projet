// src/app/features/settings/general-settings/general-settings.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { SettingsService, GeneralSettingsResponse } from '../services/settings.service';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-general-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    TranslateModule,
    RouterModule,
  ],
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss'],
})
export class GeneralSettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private settingsService = inject(SettingsService);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);

  settingsForm: FormGroup = this.fb.group({
    platformName: ['SpeedLine'],
    contactEmail: [''],
    contactPhone: [''],
    defaultLanguage: ['fr'],
    defaultCurrency: ['MAD'],
    maintenanceMode: [false],
    appEnabled: [true],
  });

  loading = false;
  saving = false;
  togglingApp = false;

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.settingsService.getGeneralSettings().subscribe({
      next: (settings) => {
        this.settingsForm.patchValue(settings);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  saveSettings(): void {
    this.saving = true;
    // appEnabled is controlled exclusively via toggleAppEnabled() + confirmation dialog,
    // never sent through the regular Save button to avoid bypassing the confirmation.
    const { appEnabled, ...payload } = this.settingsForm.value;
    this.settingsService.updateGeneralSettings(payload).subscribe({
      next: (settings) => {
        this.settingsForm.patchValue(settings);
        this.saving = false;
        this.toastr.success(this.translate.instant('settings.general.saveSuccess'));
      },
      error: () => {
        this.saving = false;
        this.toastr.error(this.translate.instant('common.error'));
      },
    });
  }

  /** Toggle the entire application on/off with a confirmation dialog */
  toggleAppEnabled(): void {
    const currentlyEnabled = this.settingsForm.value.appEnabled;
    const newState = !currentlyEnabled;

    const data: ConfirmationDialogData = {
      title: this.translate.instant(newState
        ? 'settings.general.enableAppTitle'
        : 'settings.general.disableAppTitle'),
      message: this.translate.instant(newState
        ? 'settings.general.enableAppMessage'
        : 'settings.general.disableAppMessage'),
      confirmLabel: this.translate.instant(newState
        ? 'settings.general.enableAppConfirm'
        : 'settings.general.disableAppConfirm'),
      cancelLabel: this.translate.instant('common.cancel'),
      type: newState ? 'info' : 'danger',
      icon: newState ? 'power' : 'power_off',
    };

    this.dialog.open(ConfirmationDialogComponent, { data, width: '440px' })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        this.togglingApp = true;
        this.settingsService.updateGeneralSettings({ appEnabled: newState }).subscribe({
          next: (settings) => {
            this.settingsForm.patchValue(settings);
            this.togglingApp = false;
            this.toastr.success(this.translate.instant(
              newState ? 'settings.general.appEnabledSuccess' : 'settings.general.appDisabledSuccess'
            ));
          },
          error: () => {
            this.togglingApp = false;
            this.toastr.error(this.translate.instant('common.error'));
          },
        });
      });
  }
}
