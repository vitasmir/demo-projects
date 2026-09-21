import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { ProjectAccessOverview, TmProject, TmUser } from '../../task-management.model';
import { TaskManagementService } from '../../task-management.service';

type DeleteDialogState = { user: TmUser };

@Component({
  selector: 'app-project-access-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-access-management.component.html',
  styleUrl: './project-access-management.component.scss'
})
export class ProjectAccessManagementComponent implements OnInit {
  errorMessage = '';
  currentUser: TmUser | null = null;
  loadingCurrentUser = true;
  adminLoading = false;
  adminSaving = false;
  adminOverview: ProjectAccessOverview = { users: [], projects: [] };
  selectedAdminUserId = '';
  selectedAdminProjectIds = new Set<number>();
  deleteDialog: DeleteDialogState | null = null;
  deleteDialogSubmitting = false;

  constructor(
    private readonly authService: AuthService,
    private readonly taskService: TaskManagementService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.initializeCurrentUserFromJwt();
  }

  openProjects(): void {
    void this.router.navigate(['/task-management/projects']);
  }

  openAccountManagement(): void {
    void this.router.navigate(['/task-management/accounts'], {
      queryParams: { mode: 'admin' }
    });
  }

  selectAdminUser(userId: string): void {
    this.selectedAdminUserId = userId;
    this.syncSelectedAdminProjectIds();
    this.cdr.markForCheck();
  }

  selectedAdminUser(): TmUser | null {
    return this.adminOverview.users.find(user => user.id === this.selectedAdminUserId) ?? null;
  }

  isProjectAssignedToSelectedUser(projectId: number): boolean {
    return this.selectedAdminProjectIds.has(projectId);
  }

  onSelectedUserProjectChange(projectId: number, checked: boolean): void {
    const next = new Set(this.selectedAdminProjectIds);
    if (checked) {
      next.add(projectId);
    } else {
      next.delete(projectId);
    }

    this.selectedAdminProjectIds = next;
    this.cdr.markForCheck();
  }

  saveSelectedUserProjectAccess(): void {
    if (this.currentUser?.admin !== true) {
      this.errorMessage = 'Uložení přístupů může provést jen admin.';
      return;
    }

    const selectedUser = this.selectedAdminUser();
    if (selectedUser == null) {
      this.errorMessage = 'Vyber uživatele, kterému chceš upravit přístupy.';
      return;
    }

    this.errorMessage = '';
    this.adminSaving = true;
    const projectIds = this.adminOverview.projects
      .filter(project => this.selectedAdminProjectIds.has(project.id))
      .map(project => project.id);

    this.taskService.updateUserProjectAccess(selectedUser.id, projectIds).subscribe({
      next: updatedUser => {
        this.adminOverview = {
          ...this.adminOverview,
          users: this.adminOverview.users.map(user => user.id === updatedUser.id ? updatedUser : user)
        };
        this.syncSelectedAdminProjectIds();

        if (this.currentUser?.id === updatedUser.id) {
          this.currentUser = {
            ...this.currentUser,
            ...updatedUser
          };
        }

        this.adminSaving = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.adminSaving = false;
        this.errorMessage = this.resolveApiError(err, 'Uložení oprávnění k projektům selhalo.');
        this.cdr.markForCheck();
      },
      complete: () => {
        this.adminSaving = false;
        this.cdr.markForCheck();
      }
    });
  }

  deleteAdminUser(user: TmUser): void {
    if (this.currentUser?.admin !== true) {
      this.errorMessage = 'Mazání uživatelů může provést jen admin.';
      return;
    }

    this.deleteDialog = { user };
    this.cdr.markForCheck();
  }

  confirmDeleteDialog(): void {
    if (this.deleteDialog == null || this.deleteDialogSubmitting) {
      return;
    }

    this.errorMessage = '';
    this.adminSaving = true;
    this.deleteDialogSubmitting = true;
    const user = this.deleteDialog.user;
    this.deleteDialog = null;
    this.cdr.markForCheck();

    this.taskService.deleteManagedUser(user.id).subscribe({
      next: () => {
        const remainingUsers = this.adminOverview.users.filter(item => item.id !== user.id);
        this.adminOverview = {
          ...this.adminOverview,
          users: remainingUsers
        };

        if (this.selectedAdminUserId === user.id) {
          this.selectedAdminUserId = remainingUsers[0]?.id ?? '';
          this.syncSelectedAdminProjectIds();
        }
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Smazání uživatele selhalo.');
        this.adminSaving = false;
        this.deleteDialogSubmitting = false;
        this.cdr.markForCheck();
      },
      complete: () => {
        this.adminSaving = false;
        this.deleteDialogSubmitting = false;
        this.cdr.markForCheck();
      }
    });
  }

  closeDeleteDialog(): void {
    if (this.deleteDialogSubmitting) {
      return;
    }

    this.deleteDialog = null;
    this.cdr.markForCheck();
  }

  deleteDialogMessage(): string {
    if (this.deleteDialog == null) {
      return '';
    }

    return `Opravdu chceš smazat uživatele "${this.deleteDialog.user.displayName}"?`;
  }

  deleteDialogConfirmLabel(): string {
    return this.deleteDialogSubmitting ? 'Mažu...' : 'Potvrdit smazání';
  }

  trackByUserId(_index: number, user: TmUser): string {
    return user.id;
  }

  trackByProjectId(_index: number, project: TmProject): number {
    return project.id;
  }

  private initializeCurrentUserFromJwt(): void {
    if (!this.authService.isAuthenticated()) {
      this.loadingCurrentUser = false;
      this.currentUser = null;
      this.taskService.logout();
      this.cdr.markForCheck();
      return;
    }

    this.loadingCurrentUser = true;
    this.errorMessage = '';
    this.taskService.syncCurrentUserFromJwt().subscribe({
      next: user => {
        this.currentUser = user;
        this.loadingCurrentUser = false;
        if (user.admin === true) {
          this.loadProjectAdministration();
          return;
        }

        this.errorMessage = 'Administraci projektů může otevřít jen admin.';
        this.cdr.markForCheck();
      },
      error: err => {
        this.currentUser = null;
        this.loadingCurrentUser = false;
        this.errorMessage = this.resolveApiError(err, 'Nepodařilo se načíst Task Management uživatele z JWT tokenu.');
        this.cdr.markForCheck();
      }
    });
  }

  private loadProjectAdministration(): void {
    if (this.currentUser?.admin !== true) {
      return;
    }

    this.adminLoading = true;
    this.errorMessage = '';
    this.taskService.getProjectAccessOverview().subscribe({
      next: overview => {
        const users = [...overview.users].sort((a, b) => a.displayName.localeCompare(b.displayName, 'cs', { sensitivity: 'base' }));
        const projects = [...overview.projects].sort((a, b) => a.name.localeCompare(b.name, 'cs', { sensitivity: 'base' }));

        this.adminOverview = { users, projects };

        if (this.selectedAdminUserId === '' || !users.some(user => user.id === this.selectedAdminUserId)) {
          this.selectedAdminUserId = users[0]?.id ?? '';
        }

        this.syncSelectedAdminProjectIds();
        this.adminLoading = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.adminLoading = false;
        this.errorMessage = this.resolveApiError(err, 'Nepodařilo se načíst administraci projektů.');
        this.cdr.markForCheck();
      }
    });
  }

  private syncSelectedAdminProjectIds(): void {
    const selectedUser = this.selectedAdminUser();
    this.selectedAdminProjectIds = new Set(selectedUser?.accessibleProjectIds ?? []);
  }

  private resolveApiError(err: unknown, fallback: string): string {
    const httpError = err as HttpErrorResponse;
    if (httpError == null) {
      return fallback;
    }

    const payload = httpError.error as { message?: string; error?: string; detail?: string } | string | null;
    if (typeof payload === 'string' && payload.trim() !== '') {
      return payload;
    }

    if (payload != null && typeof payload === 'object') {
      const message = payload.message ?? payload.detail ?? payload.error;
      if (message != null && message.trim() !== '') {
        return message;
      }
    }

    if (httpError.message != null && httpError.message.trim() !== '') {
      return httpError.message;
    }

    return fallback;
  }
}