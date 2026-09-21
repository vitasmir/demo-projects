import { registerLocaleData } from '@angular/common';
import localeCs from '@angular/common/locales/cs';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

registerLocaleData(localeCs);

bootstrapApplication(AppComponent, appConfig)
  .catch((err: any) => console.error(err));