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
import { TranslateModule } from '@ngx-translate/core';
import { SettingsService } from '../services/settings.service';

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
    TranslateModule,
  ],
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss'],
})
export class GeneralSettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private settingsService = inject(SettingsService);

  settingsForm: FormGroup = this.fb.group({
    platformName: ['SpeedLine'],
    contactEmail: [''],
    contactPhone: [''],
    defaultLanguage: ['fr'],
    defaultCurrency: ['MAD'],
    maintenanceMode: [false],
  });

  loading = false;

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    // TODO: Implement
  }

  saveSettings(): void {
    // TODO: Implement
  }
}
