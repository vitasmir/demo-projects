import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER, ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from "@ngx-translate/core";
import { provideTranslateHttpLoader } from "@ngx-translate/http-loader";
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { AuthService } from './auth/auth.service';
import { authInterceptor, errorInterceptor, spinnerInterceptor } from './shared/interceptor';

function initializeAuth(authService: AuthService): () => Promise<void> {
  return () => authService.initAuth();
}

export const appConfig: ApplicationConfig = {

  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
      //withInterceptors([spinnerInterceptor])
    ),
    provideOAuthClient(),
    provideAnimations(),
    provideZonelessChangeDetection(),
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [AuthService],
      useFactory: initializeAuth
    },

    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: '/i18n/',
        suffix: '.json'
      }),
      fallbackLang: 'cz',
      lang: 'cz'
    })
  ]
};
