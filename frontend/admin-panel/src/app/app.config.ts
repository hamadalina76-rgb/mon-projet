// src/app/app.config.ts
import { ApplicationConfig, importProvidersFrom, APP_INITIALIZER, LOCALE_ID } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { TranslateModule, TranslateService, TranslateLoader } from '@ngx-translate/core';
import { TranslateHttpLoader, TRANSLATE_HTTP_LOADER_CONFIG } from '@ngx-translate/http-loader';
import { firstValueFrom } from 'rxjs';
import { MAT_DATE_LOCALE, MAT_DATE_FORMATS, DateAdapter } from '@angular/material/core';
import { NativeDateAdapter } from '@angular/material/core';
// import { SocketIoModule, SocketIoConfig } from 'ngx-socket-io';

import { routes } from './app.routes';
import { authInterceptor } from '@core/interceptors/auth.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { loadingInterceptor } from '@core/interceptors/loading.interceptor';
import { RuntimeConfigService } from '@core/services/runtime-config.service';
import { environment } from '@environments/environment';
import { adminAppLocaleId } from '@core/i18n/locale-id.factory';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

const CUSTOM_DATE_FORMATS = {
  parse: { dateInput: 'dd/MM/yyyy' },
  display: {
    dateInput: { day: '2-digit', month: '2-digit', year: 'numeric' } as Intl.DateTimeFormatOptions,
    monthYearLabel: { month: 'long', year: 'numeric' } as Intl.DateTimeFormatOptions,
    dateA11yLabel: { day: 'numeric', month: 'long', year: 'numeric' } as Intl.DateTimeFormatOptions,
    monthYearA11yLabel: { month: 'long', year: 'numeric' } as Intl.DateTimeFormatOptions,
  },
};

// const socketConfig: SocketIoConfig = {
//   url: environment.wsUrl,
//   options: {
//     autoConnect: false,
//     transports: ['websocket'],
//   },
// };

// Initialize translations before app starts
export function initializeTranslations(translate: TranslateService) {
  return async (): Promise<void> => {
    const savedLang = localStorage.getItem('language') || environment.defaultLanguage;
    translate.setDefaultLang(environment.defaultLanguage);
    translate.addLangs(environment.supportedLanguages);
    document.documentElement.dir = savedLang === 'ar' ? 'rtl' : 'ltr';
    
    // Load translations before app starts
    await firstValueFrom(translate.use(savedLang));
  };
}

export function initializeRuntimeConfig(runtimeConfigService: RuntimeConfigService) {
  return async (): Promise<void> => {
    await runtimeConfigService.load();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useFactory: adminAppLocaleId },
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor, loadingInterceptor])
    ),
    provideAnimations(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
    }),
    // Configure TranslateHttpLoader
    {
      provide: TRANSLATE_HTTP_LOADER_CONFIG,
      useValue: {
        prefix: './assets/i18n/',
        suffix: '.json',
      },
    },
    importProvidersFrom(
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateHttpLoader,
        },
      }),
      // SocketIoModule.forRoot(socketConfig)
    ),
    // Initialize translations at app startup
    {
      provide: APP_INITIALIZER,
      useFactory: initializeRuntimeConfig,
      deps: [RuntimeConfigService],
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeTranslations,
      deps: [TranslateService],
      multi: true,
    },
    { provide: MAT_DATE_LOCALE, useFactory: adminAppLocaleId },
    { provide: MAT_DATE_FORMATS, useValue: CUSTOM_DATE_FORMATS },
    { provide: DateAdapter, useClass: NativeDateAdapter },
    provideCharts(withDefaultRegisterables()),
  ],
};
