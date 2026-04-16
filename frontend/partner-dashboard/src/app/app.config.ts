// src/app/app.config.ts
import { ApplicationConfig, importProvidersFrom, APP_INITIALIZER, LOCALE_ID } from '@angular/core';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { Observable, firstValueFrom } from 'rxjs';
import { SocketIoModule, SocketIoConfig } from 'ngx-socket-io';

import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { routes } from './app.routes';
import { authInterceptor } from '@core/interceptors/auth.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { loadingInterceptor } from '@core/interceptors/loading.interceptor';
import { environment } from '@environments/environment';
import { partnerAppLocaleId } from '@core/i18n/locale-id.factory';

// Socket.IO Configuration
const socketConfig: SocketIoConfig = {
  url: environment.wsUrl,
  options: {
    autoConnect: false,
    transports: ['websocket'],
  },
};

// Custom Translation Loader
export class CustomTranslateLoader implements TranslateLoader {
  constructor(private http: HttpClient) {}

  getTranslation(lang: string): Observable<any> {
    return this.http.get(`./assets/i18n/${lang}.json`);
  }
}

export function HttpLoaderFactory(http: HttpClient) {
  return new CustomTranslateLoader(http);
}

export function initTranslations(translate: TranslateService) {
  return () => firstValueFrom(translate.use(environment.defaultLanguage));
}

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useFactory: partnerAppLocaleId },
    { provide: MAT_DATE_LOCALE, useFactory: partnerAppLocaleId },
    { provide: APP_INITIALIZER, useFactory: initTranslations, deps: [TranslateService], multi: true },
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor, loadingInterceptor])
    ),
    provideAnimations(),
    provideCharts(withDefaultRegisterables()),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
    }),
    importProvidersFrom(
      TranslateModule.forRoot({
        defaultLanguage: environment.defaultLanguage,
        loader: {
          provide: TranslateLoader,
          useFactory: HttpLoaderFactory,
          deps: [HttpClient],
        },
      }),
      SocketIoModule.forRoot(socketConfig)
    ),
  ],
};
