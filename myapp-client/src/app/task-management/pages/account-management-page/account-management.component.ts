import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { TmUser } from '../../task-management.model';
import { TaskManagementService } from '../../task-management.service';
import { HttpErrorResponse } from '@angular/common/http';

type AccountForm = {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  admin: boolean;
};

@Component({
  selector: 'app-account-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './account-management.component.html',
  styleUrl: './account-management.component.scss'
})
export class AccountManagementComponent implements OnInit {
  currentUser: TmUser | null = null;
  initializingCurrentUser = true;
  saving = false;
  errorMessage = '';
  successMessage = '';
  intent: 'signup' | 'admin' = 'signup';

  signupForm: AccountForm = this.emptyForm(false);
  adminCreateForm: AccountForm = this.emptyForm(true);

  constructor(
    private readonly authService: AuthService,
    private readonly taskService: TaskManagementService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.intent = params.get('mode') === 'admin' ? 'admin' : 'signup';
      this.cdr.markForCheck();
    });

    this.initializeCurrentUserFromJwt();
  }

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.taskService.logout();
    this.authService.logout();
  }

  goToProjects(): void {
    void this.router.navigate(['/task-management/projects']);
  }

  createSignupAccount(): void {
    const validationMessage = this.validateForm(this.signupForm, false);
    if (validationMessage !== null) {
      this.errorMessage = validationMessage;
      this.successMessage = '';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.taskService.registerUser({
      username: this.signupForm.username.trim(),
      firstName: this.signupForm.firstName.trim(),
      lastName: this.signupForm.lastName.trim(),
      email: this.signupForm.email.trim(),
      password: this.signupForm.password
    }).subscribe({
      next: () => {
        this.signupForm = this.emptyForm(false);
        this.saving = false;
        this.successMessage = 'Účet byl vytvořen v Keycloaku. Můžeš se přihlásit.';
        this.cdr.markForCheck();
      },
      error: err => {
        this.saving = false;
        this.errorMessage = this.resolveApiError(err, 'Registrace účtu selhala.');
        this.cdr.markForCheck();
      }
    });
  }

  createAdminManagedAccount(): void {
    if (this.currentUser?.admin !== true) {
      this.errorMessage = 'Účty může vytvářet jen admin.';
      return;
    }

    const validationMessage = this.validateForm(this.adminCreateForm, true);
    if (validationMessage !== null) {
      this.errorMessage = validationMessage;
      this.successMessage = '';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.taskService.createUser({
      username: this.adminCreateForm.username.trim(),
      firstName: this.adminCreateForm.firstName.trim(),
      lastName: this.adminCreateForm.lastName.trim(),
      email: this.adminCreateForm.email.trim(),
      password: this.adminCreateForm.password,
      admin: this.adminCreateForm.admin
    }).subscribe({
      next: () => {
        this.adminCreateForm = this.emptyForm(true);
        this.saving = false;
        this.successMessage = 'Nový účet byl vytvořen v Keycloaku i Task Managementu.';
        this.cdr.markForCheck();
      },
      error: err => {
        this.saving = false;
        this.errorMessage = this.resolveApiError(err, 'Vytvoření účtu selhalo.');
        this.cdr.markForCheck();
      }
    });
  }

  signupPasswordHints(): string[] {
    return this.passwordHints(this.signupForm.password);
  }

  adminPasswordHints(): string[] {
    return this.passwordHints(this.adminCreateForm.password);
  }

  private initializeCurrentUserFromJwt(): void {
    if (!this.authService.isAuthenticated()) {
      this.initializingCurrentUser = false;
      this.currentUser = null;
      this.taskService.logout();
      this.cdr.markForCheck();
      return;
    }

    this.initializingCurrentUser = true;
    this.taskService.syncCurrentUserFromJwt().subscribe({
      next: user => {
        this.currentUser = user;
        this.initializingCurrentUser = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.currentUser = null;
        this.initializingCurrentUser = false;
        this.errorMessage = this.resolveApiError(err, 'Nepodařilo se načíst Task Management uživatele z JWT tokenu.');
        this.cdr.markForCheck();
      }
    });
  }

  private validateForm(form: AccountForm, allowAdmin: boolean): string | null {
    const username = form.username.trim();
    const firstName = form.firstName.trim();
    const lastName = form.lastName.trim();
    const email = form.email.trim();

    if (username === '' || firstName === '' || lastName === '' || email === '' || form.password.trim() === '' || form.confirmPassword.trim() === '') {
      return 'Vyplň Username, jméno, příjmení, email, heslo a potvrzení hesla.';
    }

    if (!/^[a-zA-Z0-9._@-]{4,40}$/.test(username)) {
      return 'Username musí mít 4-40 znaků a smí obsahovat jen písmena, čísla, tečku, podtržítko, pomlčku nebo @.';
    }

    if (firstName.length > 80 || lastName.length > 80) {
      return 'Jméno a příjmení mohou mít nejvýše 80 znaků.';
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return 'Zadej platný email.';
    }

    if (form.password !== form.confirmPassword) {
      return 'Heslo a potvrzení hesla se neshodují.';
    }

    if (!allowAdmin && form.admin) {
      return 'Veřejná registrace neumí vytvořit admin účet.';
    }

    return null;
  }

  private passwordHints(password: string): string[] {
    void password;
    return [];
  }

  private emptyForm(admin: boolean): AccountForm {
    return {
      username: '',
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
      admin
    };
  }

  private resolveApiError(err: unknown, fallback: string): string {
    const httpError = err as HttpErrorResponse;
    if (httpError == null) {
      return fallback;
    }

    if (httpError.status === 0) {
      return 'Backend není dostupný. Ověř, že běží server na portu 8080/9090.';
    }

    if (httpError.status === 401) {
      return 'Nejdřív se přihlas přes Keycloak.';
    }

    if (httpError.status === 403) {
      return 'Nemáš oprávnění vytvořit tento typ účtu.';
    }

    const errorPayload = httpError.error as { message?: string; error?: string; detail?: string } | undefined;
    return errorPayload?.message ?? errorPayload?.detail ?? errorPayload?.error ?? fallback;
  }
}