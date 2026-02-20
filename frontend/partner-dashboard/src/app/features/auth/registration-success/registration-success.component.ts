// registration-success.component.ts - Language is persisted from login page (partnerLang)
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-registration-success',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  templateUrl: './registration-success.component.html',
  styleUrls: ['./registration-success.component.scss'],
})
export class RegistrationSuccessComponent implements OnInit {
  private translate = inject(TranslateService);

  ngOnInit(): void {
    const savedLang = localStorage.getItem('partnerLang') || 'fr';
    this.translate.use(savedLang);
    document.documentElement.setAttribute('lang', savedLang);
    document.documentElement.setAttribute('dir', savedLang === 'ar' ? 'rtl' : 'ltr');
  }
}
