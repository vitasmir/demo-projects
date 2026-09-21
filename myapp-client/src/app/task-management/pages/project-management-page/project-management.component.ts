import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { TmProject, TmUser } from '../../task-management.model';
import { TaskManagementService } from '../../task-management.service';

type DeleteDialogState =
  | { kind: 'project'; project: TmProject };

@Component({
  selector: 'app-project-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-management.component.html',
  styleUrl: './project-management.component.scss'
})
export class ProjectManagementComponent implements OnInit {
  errorMessage = '';
  currentUser: TmUser | null = null;
  projectsLoaded = false;
  initializingCurrentUser = true;
  deleteDialog: DeleteDialogState | null = null;
  deleteDialogSubmitting = false;

  projectForm = {
    name: '',
    description: ''
  };

  projects: TmProject[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly taskService: TaskManagementService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router
  ) { }

  ngOnInit(): void {
    this.initializeCurrentUserFromJwt();
  }

  logout(): void {
    this.taskService.logout();
    this.authService.logout();
    this.currentUser = null;
    this.errorMessage = '';
    this.projects = [];
    this.projectsLoaded = false;
    this.deleteDialog = null;
    this.deleteDialogSubmitting = false;
    this.projectForm = {
      name: '',
      description: ''
    };
    this.cdr.markForCheck();
  }

  private initializeCurrentUserFromJwt(): void {
    if (!this.authService.isAuthenticated()) {
      this.initializingCurrentUser = false;
      this.currentUser = null;
      this.projectsLoaded = false;
      this.taskService.logout();
      this.cdr.markForCheck();
      return;
    }

    this.initializingCurrentUser = true;
    this.errorMessage = '';
    this.taskService.syncCurrentUserFromJwt().subscribe({
      next: user => {
        this.currentUser = user;
        this.initializingCurrentUser = false;
        this.loadProjects();
      },
      error: err => {
        this.currentUser = null;
        this.projectsLoaded = false;
        this.initializingCurrentUser = false;
        this.errorMessage = this.resolveApiError(err, 'Nepodařilo se načíst Task Management uživatele z JWT tokenu.');
        this.cdr.markForCheck();
      }
    });
  }

  loadProjects(): void {
    if (this.currentUser == null) {
      this.projects = [];
      this.projectsLoaded = false;
      return;
    }

    this.projectsLoaded = false;
    this.taskService.getProjects().subscribe({
      next: projects => {
        this.projects = [...projects].sort((a, b) => a.name.localeCompare(b.name, 'cs', { sensitivity: 'base' }));
        this.projectsLoaded = true;
        this.cdr.markForCheck();
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Nepodařilo se načíst projekty.');
        this.projectsLoaded = true;
        this.cdr.markForCheck();
      }
    });
  }

  createProject(): void {
    if (this.currentUser == null) {
      this.errorMessage = 'Nejdříve se přihlas.';
      return;
    }

    this.errorMessage = '';
    const name = this.projectForm.name.trim();
    const description = this.projectForm.description.trim();

    if (name === '' || description === '') {
      this.errorMessage = 'Vyplň název projektu i popis projektu.';
      return;
    }

    this.taskService.createProject(name, description).subscribe({
      next: createdProject => {
        this.projectForm = { name: '', description: '' };
        this.projects = [...this.projects, createdProject]
          .sort((a, b) => a.name.localeCompare(b.name, 'cs', { sensitivity: 'base' }));
        this.cdr.markForCheck();
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Vytvoření projektu selhalo.');
        this.cdr.markForCheck();
      }
    });
  }

  deleteProject(project: TmProject): void {
    if (this.currentUser == null) {
      this.errorMessage = 'Nejdříve se přihlas.';
      return;
    }

    if (this.currentUser.admin !== true) {
      this.errorMessage = 'Projekt může smazat jen admin.';
      return;
    }

    this.deleteDialog = { kind: 'project', project };
    this.cdr.markForCheck();
  }

  confirmDeleteDialog(): void {
    if (this.deleteDialog == null || this.deleteDialogSubmitting) {
      return;
    }

    this.confirmProjectDeletion(this.deleteDialog.project);
  }

  closeDeleteDialog(): void {
    if (this.deleteDialogSubmitting) {
      return;
    }

    this.deleteDialog = null;
    this.cdr.markForCheck();
  }

  deleteDialogTitle(): string {
    return 'Potvrzení smazání projektu';
  }

  deleteDialogMessage(): string {
    if (this.deleteDialog != null) {
      return `Opravdu chceš smazat projekt "${this.deleteDialog.project.name}"?`;
    }

    return '';
  }

  deleteDialogConfirmLabel(): string {
    return this.deleteDialogSubmitting ? 'Mažu...' : 'Potvrdit smazání';
  }

  private confirmProjectDeletion(project: TmProject): void {
    this.errorMessage = '';
    this.deleteDialogSubmitting = true;
    this.deleteDialog = null;
    this.cdr.markForCheck();

    this.taskService.deleteProject(project.id).subscribe({
      next: () => {
        this.projects = this.projects.filter(item => item.id !== project.id);
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Smazání projektu selhalo.');
        this.deleteDialogSubmitting = false;
        this.cdr.markForCheck();
      },
      complete: () => {
        this.deleteDialogSubmitting = false;
        this.cdr.markForCheck();
      }
    });
  }

  openProject(project: TmProject): void {
    if (this.currentUser == null) {
      this.errorMessage = 'Nejdříve se přihlas.';
      return;
    }

    this.router.navigate(['/task-management'], {
      queryParams: {
        projectId: project.id,
        view: 'sprints'
      }
    });
  }

  openProjectAdministration(): void {
    if (this.currentUser?.admin !== true) {
      this.errorMessage = 'Administraci projektů může otevřít jen admin.';
      return;
    }

    void this.router.navigate(['/task-management/project-access']);
  }

  openAccountManagement(mode: 'signup' | 'admin'): void {
    void this.router.navigate(['/task-management/accounts'], {
      queryParams: { mode }
    });
  }

  private resolveApiError(err: unknown, fallback: string): string {
    const httpError = err as HttpErrorResponse;
    if (httpError == null) {
      return fallback;
    }

    if (httpError.status === 0) {
      return 'Backend není dostupný. Ověř, že běží server na portu 8080/9090.';
    }

    if (httpError.status === 403) {
      return 'Nemáš oprávnění pro zvolený projekt nebo administraci projektu.';
    }

    if (httpError.status === 401) {
      return 'Nejdřív se přihlas přes Keycloak.';
    }

    const errorPayload = httpError.error as { message?: string; error?: string; detail?: string } | undefined;
    return errorPayload?.message ?? errorPayload?.detail ?? errorPayload?.error ?? fallback;
  }
}