import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Angular 19';

  constructor(
    private router: Router,
    private translate: TranslateService,
    private authService: AuthService
  ) {
    this.translate.addLangs(['cs', 'en']);
    this.translate.setDefaultLang('en');
    this.translate.use('en');
  }

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  isAuthInitialized(): boolean {
    return this.authService.isInitialized();
  }

  username(): string | null {
    return this.authService.getUsername();
  }

  isTaskManagementRoute(): boolean {
    const path = this.router.url.split('?')[0];

    return path === '/task-management'
      || path.startsWith('/task-management/projects')
      || path.startsWith('/task-management/project-access');
  }

  isProjectManagementRoute(): boolean {
    return this.isTaskManagementRoute();
  }

  isPublicTaskManagementRoute(): boolean {
    const path = this.router.url.split('?')[0];
    return path === '/task-management/accounts';
  }
}
