import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';
import { TmUser } from '../../task-management.model';
import { TaskManagementService } from '../../task-management.service';
import { AccountManagementComponent } from './account-management.component';

describe('AccountManagementComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let taskService: jasmine.SpyObj<TaskManagementService>;
  let queryParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const adminUser: TmUser = {
    id: 'admin-1',
    username: 'demo',
    displayName: 'Demo User',
    admin: true
  };

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'logout', 'isAuthenticated']);
    taskService = jasmine.createSpyObj<TaskManagementService>('TaskManagementService', [
      'logout',
      'syncCurrentUserFromJwt',
      'registerUser',
      'createUser'
    ]);

    authService.isAuthenticated.and.returnValue(false);
    taskService.syncCurrentUserFromJwt.and.returnValue(of(adminUser));
    taskService.registerUser.and.returnValue(of(adminUser));
    taskService.createUser.and.returnValue(of(adminUser));

    queryParamMap$ = new BehaviorSubject(convertToParamMap({ mode: 'signup' }));

    await TestBed.configureTestingModule({
      imports: [AccountManagementComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: TaskManagementService, useValue: taskService },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: queryParamMap$.asObservable() }
        }
      ]
    }).compileComponents();
  });

  it('should allow short signup passwords when other fields are valid', () => {
    const fixture = TestBed.createComponent(AccountManagementComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.signupForm.username = 'new.user';
    component.signupForm.firstName = 'New';
    component.signupForm.lastName = 'User';
    component.signupForm.email = 'new.user@example.com';
    component.signupForm.password = 'weak';
    component.signupForm.confirmPassword = 'weak';

    component.createSignupAccount();

    expect(taskService.registerUser).toHaveBeenCalledWith({
      username: 'new.user',
      firstName: 'New',
      lastName: 'User',
      email: 'new.user@example.com',
      password: 'weak'
    });
    expect(component.errorMessage).toBe('');
  });

  it('should send the supplied identity fields for signup', () => {
    const fixture = TestBed.createComponent(AccountManagementComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.signupForm.username = 'new.user';
    component.signupForm.firstName = 'New';
    component.signupForm.lastName = 'User';
    component.signupForm.email = 'new.user@example.com';
    component.signupForm.password = 'ValidPass12';
    component.signupForm.confirmPassword = 'ValidPass12';

    component.createSignupAccount();

    expect(taskService.registerUser).toHaveBeenCalledWith({
      username: 'new.user',
      firstName: 'New',
      lastName: 'User',
      email: 'new.user@example.com',
      password: 'ValidPass12'
    });
  });

  it('should register a new account with a valid signup form', () => {
    const fixture = TestBed.createComponent(AccountManagementComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.signupForm.username = 'new.user';
    component.signupForm.firstName = 'New';
    component.signupForm.lastName = 'User';
    component.signupForm.email = 'new.user@example.com';
    component.signupForm.password = 'ValidPass12';
    component.signupForm.confirmPassword = 'ValidPass12';

    component.createSignupAccount();

    expect(taskService.registerUser).toHaveBeenCalledWith({
      username: 'new.user',
      firstName: 'New',
      lastName: 'User',
      email: 'new.user@example.com',
      password: 'ValidPass12'
    });
    expect(component.successMessage).toContain('Účet byl vytvořen');
  });

  it('should create an admin-managed account when the current user is admin', () => {
    authService.isAuthenticated.and.returnValue(true);
    queryParamMap$.next(convertToParamMap({ mode: 'admin' }));

    const fixture = TestBed.createComponent(AccountManagementComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.adminCreateForm.username = 'managed.admin';
    component.adminCreateForm.firstName = 'Managed';
    component.adminCreateForm.lastName = 'Admin';
    component.adminCreateForm.email = 'managed.admin@example.com';
    component.adminCreateForm.password = 'simplepass1';
    component.adminCreateForm.confirmPassword = 'simplepass1';
    component.adminCreateForm.admin = true;

    component.createAdminManagedAccount();

    expect(taskService.createUser).toHaveBeenCalledWith({
      username: 'managed.admin',
      firstName: 'Managed',
      lastName: 'Admin',
      email: 'managed.admin@example.com',
      password: 'simplepass1',
      admin: true
    });
    expect(component.successMessage).toContain('Nový účet byl vytvořen');
  });
});