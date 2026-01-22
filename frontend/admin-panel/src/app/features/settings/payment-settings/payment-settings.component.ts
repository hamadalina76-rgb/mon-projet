// src/app/features/settings/payment-settings/payment-settings.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { SettingsService } from '../services/settings.service';

@Component({
  selector: 'app-payment-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
    TranslateModule,
  ],
  templateUrl: './payment-settings.component.html',
  styleUrls: ['./payment-settings.component.scss'],
})
export class PaymentSettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private settingsService = inject(SettingsService);

  paymentForm: FormGroup = this.fb.group({
    platformCommission: [15],
    courierCommission: [80],
    cashPaymentEnabled: [true],
    cardPaymentEnabled: [true],
    paymentGateway: ['stripe'],
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
