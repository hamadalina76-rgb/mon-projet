// src/main.ts
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import localeEnGb from '@angular/common/locales/en-GB';
import localeArTn from '@angular/common/locales/ar-TN';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

registerLocaleData(localeFr);
registerLocaleData(localeEnGb);
registerLocaleData(localeArTn);

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
