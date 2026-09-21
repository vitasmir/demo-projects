import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AppComponent } from './app.component';
import { AuthService } from './auth/auth.service';

@Component({ template: '' })
class DummyRouteComponent {}

describe('AppComponent', () => {
  let router: Router;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'login',
      'logout',
      'isAuthenticated',
      'isInitialized',
      'getUsername'
    ]);
    authService.isAuthenticated.and.returnValue(false);
    authService.isInitialized.and.returnValue(true);
    authService.getUsername.and.returnValue(null);

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([
          { path: 'task-management', component: DummyRouteComponent },
          { path: 'task-management/accounts', component: DummyRouteComponent },
          { path: 'task-management/projects', component: DummyRouteComponent },
          { path: 'task-management/project-access', component: DummyRouteComponent }
        ]),
        { provide: AuthService, useValue: authService },
        {
          provide: TranslateService,
          useValue: {
            addLangs: jasmine.createSpy('addLangs'),
            setDefaultLang: jasmine.createSpy('setDefaultLang'),
            use: jasmine.createSpy('use')
          }
        }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should detect the public task-management accounts route', async () => {
    await router.navigateByUrl('/task-management/accounts');

    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;

    expect(component.isPublicTaskManagementRoute()).toBeTrue();
    expect(component.isTaskManagementRoute()).toBeFalse();
  });

  it('should detect non-public task-management routes separately', async () => {
    authService.isAuthenticated.and.returnValue(true);
    await router.navigateByUrl('/task-management/projects');

    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;

    expect(component.isTaskManagementRoute()).toBeTrue();
    expect(component.isPublicTaskManagementRoute()).toBeFalse();
    expect(component.isProjectManagementRoute()).toBeTrue();
  });

  it('should detect the sprint dashboard route as project management', async () => {
    authService.isAuthenticated.and.returnValue(true);
    await router.navigateByUrl('/task-management?projectId=5&view=sprints');

    const fixture = TestBed.createComponent(AppComponent);
    const component = fixture.componentInstance;

    expect(component.isTaskManagementRoute()).toBeTrue();
    expect(component.isProjectManagementRoute()).toBeTrue();
    expect(component.isPublicTaskManagementRoute()).toBeFalse();
  });
});
