import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, firstValueFrom, Subscription } from 'rxjs';
import {
  BurnoutPoint,
  Sprint,
  SprintDashboard,
  StoryDiscussionMessage,
  TaskItem,
  TaskStatus,
  TmUser,
  UserStory
} from './task-management.model';
import { TaskManagementService } from './task-management.service';

type FlattenedStoryDiscussionMessage = StoryDiscussionMessage & { depth: number };
type StoryDragSource = 'backlog' | 'sprint';
type BurnoutHoverState = {
  date: string;
  x: number;
  popupLeftPercent: number;
  completedStories: string[];
  incompleteStories: string[];
};

type TopMenu = 'sprints' | 'stories' | 'tasks' | 'my-tasks';

@Injectable({
  providedIn: 'root'
})
export class TaskManagementStateService {
  activeTopMenu: TopMenu = 'sprints';
  taskDisplayMode: 'all' | 'mine' = 'all';
  readonly allowedStoryPoints = [5, 10, 20, 50, 100];
  readonly statuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
  readonly burnoutSvgWidth = 760;
  readonly burnoutSvgHeight = 280;
  readonly burnoutPadding = { top: 20, right: 20, bottom: 40, left: 48 };

  loading = false;
  errorMessage = '';
  draggingTaskId: number | null = null;
  dragOverStatus: TaskStatus | null = null;
  draggingStoryId: number | null = null;
  draggingStorySource: StoryDragSource | null = null;
  sprintStoryDropActive = false;
  backlogStoryDropActive = false;

  currentUser: TmUser | null = null;
  selectedProjectName = '';

  sprintForm = {
    name: '',
    startDate: '',
    endDate: ''
  };

  storyForm = {
    title: '',
    description: '',
    difficulty: 10,
    colorHex: '#FFF8DC'
  };

  taskForm = {
    storyId: 0,
    title: '',
    description: '',
    definitionOfDone: '',
    colorHex: '#FFF8DC',
    assigneeUserId: '',
    reviewerUserId: ''
  };

  sprints: Sprint[] = [];
  backlogStories: UserStory[] = [];
  users: TmUser[] = [];
  selectedProjectId: number | null = null;
  selectedSprintId: number | null = null;
  selectedStoryId: number | null = null;
  dashboard: SprintDashboard | null = null;
  myTasksLoading = false;
  discussionDialogOpen = false;
  discussionLoading = false;
  discussionSubmitting = false;
  discussionStory: UserStory | null = null;
  discussionMessages: StoryDiscussionMessage[] = [];
  discussionFlatMessages: FlattenedStoryDiscussionMessage[] = [];
  discussionDraft = '';
  discussionReplyTo: FlattenedStoryDiscussionMessage | null = null;
  unreadDiscussionStoryIds = new Set<number>();
  burnoutHover: BurnoutHoverState | null = null;

  private myTasksOverview: TaskItem[] = [];
  private myTaskStoriesById = new Map<number, UserStory>();
  private myTaskSprintDisplayByTaskId = new Map<number, number>();
  private myTasksLoadedProjectId: number | null = null;
  private routeSubscription?: Subscription;

  constructor(
    private readonly taskService: TaskManagementService,
    private readonly router: Router
  ) {}

  init(route: ActivatedRoute, defaultMenu: TopMenu = 'sprints'): void {
    this.currentUser = this.taskService.getCurrentUser();
    if (this.currentUser != null) {
      this.taskForm.assigneeUserId = this.currentUser.id;
      this.taskForm.reviewerUserId = this.currentUser.id;
      this.loadUsers();
    }

    this.routeSubscription?.unsubscribe();
    this.routeSubscription = route.queryParamMap.subscribe(params => {
      const rawProjectId = params.get('projectId');
      const projectId = rawProjectId == null ? null : Number(rawProjectId);
      const requestedView = params.get('view');

      if (requestedView === 'stories' || requestedView === 'tasks' || requestedView === 'sprints' || requestedView === 'my-tasks') {
        this.activeTopMenu = requestedView;
      }

      if (projectId == null || !Number.isFinite(projectId) || projectId <= 0) {
        this.selectedProjectId = null;
        this.selectedProjectName = '';
        this.resetProjectContext();
        return;
      }

      if (this.selectedProjectId === projectId) {
        const shouldReloadProjectData =
          this.selectedProjectName.trim() === ''
          || this.sprints.length === 0
          || (this.backlogStories.length === 0 && this.dashboard == null);

        if (shouldReloadProjectData) {
          this.loadSelectedProjectAndData();
        }
        return;
      }

      if (requestedView == null) {
        this.activeTopMenu = defaultMenu;
      }

      this.closeStoryDiscussion();
      this.selectedProjectId = projectId;
      this.selectedStoryId = null;
      this.taskForm.storyId = 0;
      this.loadSelectedProjectAndData();
    });
  }

  setActiveTopMenu(menu: TopMenu): void {
    this.activeTopMenu = menu;
    if (menu === 'my-tasks') {
      void this.loadMyTasksOverview();
    }
  }

  logout(): void {
    this.taskService.logout();
    this.currentUser = null;
    this.errorMessage = '';
    this.loading = false;
    this.users = [];
    this.myTasksOverview = [];
    this.myTaskStoriesById.clear();
    this.myTaskSprintDisplayByTaskId.clear();
    this.myTasksLoadedProjectId = null;
    this.taskForm.assigneeUserId = '';
    this.taskForm.reviewerUserId = '';
    this.unreadDiscussionStoryIds.clear();
    this.closeStoryDiscussion();
  }

  goToProjects(): void {
    void this.router.navigate(['/task-management/projects']);
  }

  navigateToMenu(menu: TopMenu): void {
    this.activeTopMenu = menu;
    if (menu === 'my-tasks') {
      void this.loadMyTasksOverview();
    }

    void this.router.navigate(['/task-management', menu], {
      queryParams: this.selectedProjectId != null ? { projectId: this.selectedProjectId } : {},
      queryParamsHandling: this.selectedProjectId != null ? 'merge' : ''
    });
  }

  loadUsers(): void {
    this.taskService.getUsers().subscribe({
      next: data => {
        this.users = [...data].sort((a, b) =>
          a.displayName.localeCompare(b.displayName, 'cs', { sensitivity: 'base' })
        );

        if (this.currentUser != null) {
          const exists = this.users.some(user => user.id === this.currentUser!.id);
          if (!exists) {
            this.taskService.logout();
            this.currentUser = null;
            this.users = [];
            this.taskForm.assigneeUserId = '';
            this.taskForm.reviewerUserId = '';
            this.unreadDiscussionStoryIds.clear();
            this.closeStoryDiscussion();
            this.errorMessage = 'Uložené přihlášení už není platné. Přihlas se znovu.';
            return;
          }
        }

        if (this.taskForm.assigneeUserId === '' && this.currentUser != null) {
          this.taskForm.assigneeUserId = this.currentUser.id;
        }
        if (this.taskForm.reviewerUserId === '' && this.currentUser != null) {
          this.taskForm.reviewerUserId = this.currentUser.id;
        }
      },
      error: () => {
        this.errorMessage = 'Nepodařilo se načíst seznam uživatelů.';
      }
    });
  }

  loadBacklog(): void {
    if (this.selectedProjectId == null) {
      this.backlogStories = [];
      return;
    }

    this.taskService.getProductBacklog(this.selectedProjectId).subscribe({
      next: stories => {
        this.backlogStories = stories;
      },
      error: () => {
        this.errorMessage = 'Nepodařilo se načíst product backlog.';
      }
    });
  }

  loadSprints(): void {
    if (this.selectedProjectId == null) {
      this.sprints = [];
      this.dashboard = null;
      this.selectedSprintId = null;
      return;
    }

    this.taskService.getSprints(this.selectedProjectId).subscribe({
      next: data => {
        this.sprints = data;
        if (this.sprints.length > 0) {
          this.selectSprint(this.preferredSprintId(this.sprints));
        } else {
          this.selectedSprintId = null;
          this.dashboard = null;
          this.selectedStoryId = null;
          this.taskForm.storyId = 0;
        }
      },
      error: () => {
        this.errorMessage = 'Nepodařilo se načíst sprinty.';
      }
    });
  }

  createSprint(): void {
    this.errorMessage = '';
    if (this.selectedProjectId == null) {
      this.errorMessage = 'Nejprve vytvoř nebo vyber projekt.';
      return;
    }

    const name = this.sprintForm.name.trim();
    const startDate = this.sprintForm.startDate;
    const endDate = this.sprintForm.endDate;

    if (name === '' || startDate === '' || endDate === '') {
      this.errorMessage = 'Vyplň název sprintu, datum od a datum do.';
      return;
    }

    if (new Date(endDate) < new Date(startDate)) {
      this.errorMessage = 'Datum konce sprintu musí být stejné nebo pozdější než datum začátku.';
      return;
    }

    this.taskService.createSprint(this.selectedProjectId, name, startDate, endDate)
      .subscribe({
        next: sprint => {
          this.sprints = [...this.sprints, sprint];
          this.selectSprint(sprint.id);
          this.sprintForm = { name: '', startDate: '', endDate: '' };
        },
        error: err => {
          this.errorMessage = this.resolveApiError(err, 'Vytvoření sprintu selhalo.');
        }
      });
  }

  selectSprint(sprintId: number): void {
    const selected = Number(sprintId);
    if (!Number.isFinite(selected) || selected <= 0) {
      this.selectedSprintId = null;
      this.dashboard = null;
      return;
    }

    this.selectedSprintId = selected;
    this.loadDashboard();
  }

  onMenuSprintChange(sprintId: number): void {
    const selected = Number(sprintId);
    if (!Number.isFinite(selected) || selected <= 0) {
      return;
    }

    this.selectSprint(selected);
    this.selectedStoryId = null;
    this.taskForm.storyId = 0;
    this.navigateToMenu('stories');
  }

  onMenuStoryChange(storyId: number, mode: 'all' | 'mine' = 'all'): void {
    const id = Number(storyId);
    if (!id || id <= 0) {
      this.selectedStoryId = null;
      return;
    }

    this.selectedStoryId = id;
    this.taskForm.storyId = id;
    this.taskDisplayMode = mode;
    this.navigateToMenu('tasks');
  }

  setTaskDisplayMode(mode: 'all' | 'mine'): void {
    this.taskDisplayMode = mode;
  }

  loadDashboard(): void {
    if (this.selectedProjectId == null || this.selectedSprintId == null) {
      this.dashboard = null;
      return;
    }

    this.taskService.getSprintDashboard(this.selectedSprintId)
      .subscribe({
        next: data => {
          this.dashboard = data;
          const selectedStoryExists = data.userStories.some(story => story.id === this.taskForm.storyId);
          if (!selectedStoryExists) {
            this.taskForm.storyId = 0;
          }

          const selectedMenuStoryExists = data.userStories.some(story => story.id === this.selectedStoryId);
          if (!selectedMenuStoryExists) {
            this.selectedStoryId = null;
          }
        },
        error: () => {
          this.errorMessage = 'Dashboard se nepodařilo načíst.';
        }
      });
  }

  createStory(): void {
    this.errorMessage = '';
    if (this.selectedProjectId == null) {
      this.errorMessage = 'Nejprve vyber projekt.';
      return;
    }

    const title = this.storyForm.title.trim();
    const description = this.storyForm.description.trim();

    if (title === '' || description === '') {
      this.errorMessage = 'Vyplň titulek a popis user story.';
      return;
    }

    this.taskService.createStory(
      this.selectedProjectId,
      title,
      description,
      this.storyForm.difficulty,
      this.storyForm.colorHex
    ).subscribe({
      next: () => {
        this.storyForm.title = '';
        this.storyForm.description = '';
        this.storyForm.difficulty = 10;
        this.storyForm.colorHex = '#FFF8DC';
        this.loadBacklog();
        this.loadDashboard();
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Vytvoření user story selhalo.');
      }
    });
  }

  deleteBacklogStory(story: UserStory): void {
    const confirmed = window.confirm(`Opravdu chceš smazat funkci "${story.title}" z product backlogu?`);
    if (!confirmed) {
      return;
    }

    this.taskService.deleteStory(story.id).subscribe({
      next: () => {
        this.backlogStories = this.backlogStories.filter(item => item.id !== story.id);
        this.errorMessage = '';
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Smazání funkce z product backlogu selhalo.');
      }
    });
  }

  renameStory(story: UserStory): void {
    this.errorMessage = '';
    const promptValue = window.prompt('Nový název user story:', story.title);
    if (promptValue == null) {
      return;
    }

    const newTitle = promptValue.trim();
    if (newTitle === '') {
      this.errorMessage = 'Název user story nesmí být prázdný.';
      return;
    }

    if (newTitle === story.title.trim()) {
      return;
    }

    this.taskService.updateStory(story.id, {
      title: newTitle,
      description: story.description
    }).subscribe({
      next: () => {
        this.loadBacklog();
        this.loadDashboard();
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Přejmenování user story selhalo.');
      }
    });
  }

  createTask(): void {
    if (this.currentUser == null) {
      this.errorMessage = 'Nejdříve se přihlas.';
      return;
    }

    this.errorMessage = '';
    if (this.taskForm.storyId <= 0) {
      this.errorMessage = 'Nejdříve vytvoř user story.';
      return;
    }

    const title = this.taskForm.title.trim();
    const description = this.taskForm.description.trim();
    const definitionOfDone = this.taskForm.definitionOfDone.trim();

    if (title === '' || description === '' || definitionOfDone === '') {
      this.errorMessage = 'Vyplň název tasku, zadání úkolu i definici splnění.';
      return;
    }

    const creatorUserId = this.currentUser.id;
    const assigneeUserId = this.taskForm.assigneeUserId || creatorUserId;
    const reviewerUserId = this.taskForm.reviewerUserId || creatorUserId;

    const creatorExists = this.users.some(user => user.id === creatorUserId);
    if (!creatorExists) {
      this.taskService.logout();
      this.currentUser = null;
      this.taskForm.assigneeUserId = '';
      this.taskForm.reviewerUserId = '';
      this.errorMessage = 'Uložené přihlášení už není platné. Přihlas se znovu.';
      return;
    }

    const assigneeExists = this.users.some(user => user.id === assigneeUserId);
    const reviewerExists = this.users.some(user => user.id === reviewerUserId);
    if (!assigneeExists || !reviewerExists) {
      this.errorMessage = 'Vyber platného řešitele a reviewera.';
      return;
    }

    this.taskService.createTask({
      storyId: this.taskForm.storyId,
      title,
      description,
      definitionOfDone,
      colorHex: this.taskForm.colorHex,
      creatorUserId,
      assigneeUserId,
      reviewerUserId
    }).subscribe({
      next: () => {
        this.taskForm.title = '';
        this.taskForm.description = '';
        this.taskForm.definitionOfDone = '';
        this.loadDashboard();
        this.refreshMyTasksOverviewIfNeeded();
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Vytvoření tasku selhalo.');
      }
    });
  }

  canEditTask(task: TaskItem): boolean {
    return task.status === 'TODO';
  }

  canChangeStatus(task: TaskItem, _toStatus?: TaskStatus): boolean {
    if (this.currentUser == null) {
      return false;
    }

    if (this.currentUser.admin === true) {
      return true;
    }

    const uid = this.currentUser.id;
    return task.creatorUserId === uid || task.assigneeUserId === uid || task.reviewerUserId === uid;
  }

  editTask(task: TaskItem): void {
    this.errorMessage = '';

    if (!this.canEditTask(task)) {
      this.errorMessage = 'Task lze upravit jen ve stavu Nedotčené (TODO).';
      return;
    }

    const titleInput = window.prompt('Upravit název tasku:', task.title);
    if (titleInput == null) {
      return;
    }

    const descriptionInput = window.prompt('Upravit zadání tasku:', task.description);
    if (descriptionInput == null) {
      return;
    }

    const definitionInput = window.prompt('Upravit definici splnění:', task.definitionOfDone);
    if (definitionInput == null) {
      return;
    }

    const title = titleInput.trim();
    const description = descriptionInput.trim();
    const definitionOfDone = definitionInput.trim();

    if (title === '' || description === '' || definitionOfDone === '') {
      this.errorMessage = 'Název, zadání i definice splnění musí být vyplněné.';
      return;
    }

    this.taskService.updateTask(task.id, {
      title,
      description,
      definitionOfDone
    }).subscribe({
      next: () => {
        this.loadDashboard();
        this.refreshMyTasksOverviewIfNeeded();
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Úprava tasku selhala.');
      }
    });
  }

  updateTaskStatus(task: TaskItem, status: TaskStatus): void {
    if (!this.canChangeStatus(task, status)) {
      this.errorMessage = 'Nemáš oprávnění změnit stav tohoto tasku.';
      return;
    }

    const previousStatus = task.status;
    task.status = status;

    this.taskService.updateTaskStatus(task.id, status, task.reviewComment ?? '')
      .subscribe({
        next: () => {
          this.loadDashboard();
          this.refreshMyTasksOverviewIfNeeded();
        },
        error: () => {
          task.status = previousStatus;
          this.errorMessage = 'Změna stavu tasku selhala.';
        }
      });
  }

  onTaskDragStart(event: DragEvent, task: TaskItem): void {
    if (event.dataTransfer != null) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', String(task.id));
      event.dataTransfer.setData('text', String(task.id));
    }

    setTimeout(() => {
      this.draggingTaskId = task.id;
    });
  }

  onTaskDragEnd(): void {
    this.dragOverStatus = null;
    setTimeout(() => {
      this.draggingTaskId = null;
    }, 50);
  }

  onStatusDragOver(event: DragEvent, status: TaskStatus): void {
    event.preventDefault();
    if (this.dragOverStatus !== status) {
      this.dragOverStatus = status;
    }
    if (event.dataTransfer != null) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onStatusDragLeave(event: DragEvent): void {
    const currentTarget = event.currentTarget as HTMLElement | null;
    const relatedTarget = event.relatedTarget as Node | null;

    if (currentTarget && relatedTarget && currentTarget.contains(relatedTarget)) {
      return;
    }

    this.dragOverStatus = null;
  }

  onStatusDrop(event: DragEvent, status: TaskStatus): void {
    event.preventDefault();

    const rawDraggedId =
      event.dataTransfer?.getData('text/plain') ||
      event.dataTransfer?.getData('text') ||
      '';

    const parsedDraggedId = rawDraggedId.trim() === '' ? null : Number(rawDraggedId);
    const draggedIdFromTransfer = parsedDraggedId != null && Number.isFinite(parsedDraggedId)
      ? parsedDraggedId
      : null;

    const draggedTaskId = this.draggingTaskId ?? draggedIdFromTransfer;
    this.dragOverStatus = null;

    if (draggedTaskId == null) {
      return;
    }

    const task = (this.dashboard?.tasks ?? []).find(item => item.id === draggedTaskId);
    if (task == null || task.status === status) {
      this.draggingTaskId = null;
      return;
    }

    this.updateTaskStatus(task, status);
    this.draggingTaskId = null;
  }

  onBacklogStoryDragStart(event: DragEvent, story: UserStory, source: StoryDragSource = 'backlog'): void {
    if (event.dataTransfer != null) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', String(story.id));
      event.dataTransfer.setData('text', String(story.id));
    }

    this.draggingStoryId = story.id;
    this.draggingStorySource = source;
  }

  onBacklogStoryDragEnd(): void {
    this.sprintStoryDropActive = false;
    this.backlogStoryDropActive = false;
    setTimeout(() => {
      this.draggingStoryId = null;
      this.draggingStorySource = null;
    }, 50);
  }

  onSprintStoryDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.sprintStoryDropActive) {
      this.sprintStoryDropActive = true;
    }
    if (event.dataTransfer != null) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onSprintStoryDragLeave(event: DragEvent): void {
    const currentTarget = event.currentTarget as HTMLElement | null;
    const relatedTarget = event.relatedTarget as Node | null;

    if (currentTarget && relatedTarget && currentTarget.contains(relatedTarget)) {
      return;
    }

    this.sprintStoryDropActive = false;
  }

  onSprintStoryDrop(event: DragEvent): void {
    event.preventDefault();
    this.sprintStoryDropActive = false;

    if (this.selectedSprintId == null) {
      this.errorMessage = 'Nejdřív vyber sprint pro naplánování story.';
      return;
    }

    const rawDraggedId =
      event.dataTransfer?.getData('text/plain') ||
      event.dataTransfer?.getData('text') ||
      '';

    const parsedDraggedId = rawDraggedId.trim() === '' ? null : Number(rawDraggedId);
    const draggedIdFromTransfer = parsedDraggedId != null && Number.isFinite(parsedDraggedId)
      ? parsedDraggedId
      : null;

    const storyId = this.draggingStoryId ?? draggedIdFromTransfer;
    if (storyId == null) {
      return;
    }

    const story = this.draggingStorySource === 'sprint'
      ? null
      : this.backlogStories.find(item => item.id === storyId);
    if (story == null) {
      this.draggingStoryId = null;
      this.draggingStorySource = null;
      return;
    }

    this.taskService.assignStoryToSprint(story.id, this.selectedSprintId)
      .subscribe({
        next: () => {
          this.draggingStoryId = null;
          this.draggingStorySource = null;
          this.loadBacklog();
          this.loadDashboard();
        },
        error: err => {
          this.draggingStoryId = null;
          this.draggingStorySource = null;
          this.errorMessage = this.resolveApiError(err, 'Přesun story do sprintu selhal.');
        }
      });
  }

  onBacklogStoryDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.backlogStoryDropActive) {
      this.backlogStoryDropActive = true;
    }
    if (event.dataTransfer != null) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onBacklogStoryDragLeave(event: DragEvent): void {
    const currentTarget = event.currentTarget as HTMLElement | null;
    const relatedTarget = event.relatedTarget as Node | null;

    if (currentTarget && relatedTarget && currentTarget.contains(relatedTarget)) {
      return;
    }

    this.backlogStoryDropActive = false;
  }

  onBacklogStoryDrop(event: DragEvent): void {
    event.preventDefault();
    this.backlogStoryDropActive = false;

    const rawDraggedId =
      event.dataTransfer?.getData('text/plain') ||
      event.dataTransfer?.getData('text') ||
      '';

    const parsedDraggedId = rawDraggedId.trim() === '' ? null : Number(rawDraggedId);
    const draggedIdFromTransfer = parsedDraggedId != null && Number.isFinite(parsedDraggedId)
      ? parsedDraggedId
      : null;

    const storyId = this.draggingStoryId ?? draggedIdFromTransfer;
    if (storyId == null) {
      return;
    }

    const storyInSprint = this.draggingStorySource === 'backlog'
      ? null
      : this.dashboard?.userStories.find(item => item.id === storyId);
    if (storyInSprint == null) {
      this.draggingStoryId = null;
      this.draggingStorySource = null;
      return;
    }

    this.taskService.assignStoryToSprint(storyInSprint.id, null)
      .subscribe({
        next: () => {
          this.draggingStoryId = null;
          this.draggingStorySource = null;
          if (this.selectedStoryId === storyInSprint.id) {
            this.selectedStoryId = null;
            this.taskForm.storyId = 0;
          }
          this.loadBacklog();
          this.loadDashboard();
        },
        error: err => {
          this.draggingStoryId = null;
          this.draggingStorySource = null;
          this.errorMessage = this.resolveApiError(err, 'Přesun story zpět do backlogu selhal.');
        }
      });
  }

  returnTask(task: TaskItem): void {
    if (!this.canChangeStatus(task, 'IN_PROGRESS')) {
      this.errorMessage = 'Nemáš oprávnění vrátit tento task tvůrci.';
      return;
    }

    this.taskService.returnTask(task.id, 'Kontrola neproběhla s očekávaným výsledkem.')
      .subscribe({
        next: () => {
          this.loadDashboard();
          this.refreshMyTasksOverviewIfNeeded();
        },
        error: () => {
          this.errorMessage = 'Vrácení tasku selhalo.';
        }
      });
  }

  storyById(storyId: number): UserStory | undefined {
    return this.dashboard?.userStories.find(s => s.id === storyId);
  }

  myTaskStoryById(storyId: number): UserStory | undefined {
    return this.storyById(storyId) ?? this.myTaskStoriesById.get(storyId);
  }

  storyDisplayNumber(storyId: number): number {
    return this.myTaskStoryById(storyId)?.storyNumber ?? storyId;
  }

  myTaskSprintDisplayNumber(taskId: number): number | null {
    return this.myTaskSprintDisplayByTaskId.get(taskId) ?? null;
  }

  sprintDisplayNumber(sprintOrId: Sprint | number | null | undefined): number | null {
    if (sprintOrId == null) {
      return null;
    }

    const sprintId = typeof sprintOrId === 'number' ? sprintOrId : sprintOrId.id;
    const index = this.sprints.findIndex(sprint => sprint.id === sprintId);
    return index >= 0 ? index + 1 : null;
  }

  selectedSprintDisplayNumber(): number | null {
    return this.sprintDisplayNumber(this.selectedSprintId);
  }

  selectedStory(): UserStory | undefined {
    if (this.selectedStoryId == null) {
      return undefined;
    }
    return this.storyById(this.selectedStoryId);
  }

  sprintCompletedStories(): UserStory[] {
    return (this.dashboard?.userStories ?? []).filter(story => this.storyProgressCategory(story) === 'completed');
  }

  sprintInProgressStories(): UserStory[] {
    return (this.dashboard?.userStories ?? []).filter(story => this.storyProgressCategory(story) === 'in-progress');
  }

  sprintNotStartedStories(): UserStory[] {
    return (this.dashboard?.userStories ?? []).filter(story => this.storyProgressCategory(story) === 'not-started');
  }

  tasksForSelectedStory(): TaskItem[] {
    const tasks = this.dashboard?.tasks ?? [];
    if (this.selectedStoryId == null) {
      return [];
    }

    const selectedStoryTasks = tasks.filter(task => task.storyId === this.selectedStoryId);
    if (this.taskDisplayMode === 'mine' && this.currentUser != null) {
      return selectedStoryTasks.filter(task => task.assigneeUserId === this.currentUser!.id);
    }

    return selectedStoryTasks;
  }

  untouchedTasks(): TaskItem[] {
    return this.scopedTasks().filter(task => task.status === 'TODO');
  }

  inProgressTasks(): TaskItem[] {
    return this.scopedTasks().filter(task => task.status === 'IN_PROGRESS');
  }

  controlTasks(): TaskItem[] {
    return this.scopedTasks().filter(task => task.status === 'IN_REVIEW');
  }

  doneTasks(): TaskItem[] {
    return this.scopedTasks().filter(task => task.status === 'DONE');
  }

  storyTaskCount(storyId: number): number {
    return (this.dashboard?.tasks ?? []).filter(task => task.storyId === storyId).length;
  }

  storyCompletedTaskCount(storyId: number): number {
    return (this.dashboard?.tasks ?? []).filter(task => task.storyId === storyId && task.status === 'DONE').length;
  }

  myTasks(): TaskItem[] {
    if (this.currentUser == null) {
      return [];
    }

    if (this.myTasksLoadedProjectId === this.selectedProjectId) {
      return this.myTasksOverview;
    }

    if (this.dashboard == null) {
      return [];
    }

    return (this.dashboard.tasks ?? []).filter(task => task.assigneeUserId === this.currentUser!.id && task.status !== 'DONE');
  }

  async openMyTask(task: TaskItem): Promise<void> {
    this.errorMessage = '';

    if (this.selectedProjectId == null) {
      this.errorMessage = 'Nejdřív vyber projekt.';
      return;
    }

    const openTaskInCurrentDashboard = this.dashboard?.tasks.some(item => item.id === task.id) === true
      && this.dashboard?.userStories.some(story => story.id === task.storyId) === true;

    if (openTaskInCurrentDashboard) {
      this.selectedStoryId = task.storyId;
      this.taskForm.storyId = task.storyId;
      this.taskDisplayMode = 'mine';
      this.navigateToMenu('tasks');
      return;
    }

    this.loading = true;

    try {
      const sprints = await firstValueFrom(this.taskService.getSprints(this.selectedProjectId));

      for (const sprint of sprints) {
        const sprintDashboard = await firstValueFrom(this.taskService.getSprintDashboard(sprint.id));
        const taskExists = sprintDashboard.tasks.some(item => item.id === task.id);
        const storyExists = sprintDashboard.userStories.some(story => story.id === task.storyId);

        if (!taskExists || !storyExists) {
          continue;
        }

        this.selectedSprintId = sprint.id;
        this.dashboard = sprintDashboard;
        this.selectedStoryId = task.storyId;
        this.taskForm.storyId = task.storyId;
        this.taskDisplayMode = 'mine';
        this.navigateToMenu('tasks');
        return;
      }

      this.errorMessage = 'Nepodařilo se najít sprint obsahující vybraný task.';
    } catch (err) {
      this.errorMessage = this.resolveApiError(err, 'Otevření tasku selhalo.');
    } finally {
      this.loading = false;
    }
  }

  openStoryDiscussion(story: UserStory, event?: Event): void {
    event?.stopPropagation();

    if (this.currentUser == null) {
      this.errorMessage = 'Nejdříve se přihlas.';
      return;
    }

    this.errorMessage = '';
    this.discussionDialogOpen = true;
    this.discussionStory = story;
    this.discussionMessages = [];
    this.discussionFlatMessages = [];
    this.discussionDraft = '';
    this.discussionReplyTo = null;
    this.loadStoryDiscussionMessages(true);
  }

  closeStoryDiscussion(): void {
    this.discussionDialogOpen = false;
    this.discussionLoading = false;
    this.discussionSubmitting = false;
    this.discussionStory = null;
    this.discussionMessages = [];
    this.discussionFlatMessages = [];
    this.discussionDraft = '';
    this.discussionReplyTo = null;
  }

  hasUnreadStoryDiscussion(storyId: number): boolean {
    return this.unreadDiscussionStoryIds.has(storyId);
  }

  replyToStoryDiscussionMessage(message: FlattenedStoryDiscussionMessage): void {
    this.discussionReplyTo = message;
  }

  cancelStoryDiscussionReply(): void {
    this.discussionReplyTo = null;
  }

  submitStoryDiscussionMessage(): void {
    if (this.currentUser == null) {
      this.errorMessage = 'Nejdříve se přihlas.';
      return;
    }

    if (this.discussionStory == null) {
      this.errorMessage = 'Nejdřív vyber user story pro diskusi.';
      return;
    }

    const content = this.discussionDraft.trim();
    if (content === '') {
      this.errorMessage = 'Napiš zprávu do diskuse.';
      return;
    }

    this.discussionSubmitting = true;
    this.taskService.createStoryDiscussionMessage({
      storyId: this.discussionStory.id,
      authorUserId: this.currentUser.id,
      parentMessageId: this.discussionReplyTo?.id ?? null,
      content
    }).pipe(finalize(() => {
      this.discussionSubmitting = false;
    })).subscribe({
      next: () => {
        this.discussionDraft = '';
        this.discussionReplyTo = null;
        this.loadStoryDiscussionMessages(false);
      },
      error: err => {
        this.errorMessage = this.resolveApiError(err, 'Odeslání zprávy do diskuse selhalo.');
      }
    });
  }

  discussionAuthorLabel(message: FlattenedStoryDiscussionMessage): string {
    if (this.currentUser != null && message.authorUserId === this.currentUser.id) {
      return `${message.authorDisplayName} (ty)`;
    }

    return message.authorDisplayName;
  }

  burnoutDayCount(points: BurnoutPoint[]): number {
    return this.burnoutDisplayPoints(points).length;
  }

  burnoutTotalStoryPoints(points: BurnoutPoint[]): number {
    return this.maxBurnoutValue(points);
  }

  burnoutSeries(points: BurnoutPoint[]): Array<{ key: string; x: number; yRemaining: number; yCompleted: number; date: string; remaining: number; completed: number; burnedSincePrevious: number; completedStories: string[]; tooltip: string; completedTooltip: string; showOnXAxis: boolean }> {
    const displayPoints = this.burnoutDisplayPoints(points);
    if (displayPoints.length === 0) {
      return [];
    }

    const maxValue = this.maxBurnoutValue(points);
    const chartWidth = this.burnoutSvgWidth - this.burnoutPadding.left - this.burnoutPadding.right;
    const chartHeight = this.burnoutSvgHeight - this.burnoutPadding.top - this.burnoutPadding.bottom;
    const denominator = Math.max(displayPoints.length - 1, 1);
    const completedStoriesSoFar: string[] = [];
    const completedStoriesSet = new Set<string>();
    const step = displayPoints.length > 20 ? 3 : (displayPoints.length > 10 ? 2 : 1);

    return displayPoints.map((point, index) => {
      (point.completedStories ?? []).forEach(storyTitle => {
        if (storyTitle !== '' && !completedStoriesSet.has(storyTitle)) {
          completedStoriesSet.add(storyTitle);
          completedStoriesSoFar.push(storyTitle);
        }
      });

      const x = this.burnoutPadding.left + (index / denominator) * chartWidth;
      const yRemaining = this.burnoutPadding.top + (1 - point.remainingPoints / maxValue) * chartHeight;
      const yCompleted = this.burnoutPadding.top + (1 - point.completedPoints / maxValue) * chartHeight;
      const burnedSincePrevious = index === 0
        ? Math.max(maxValue - point.remainingPoints, 0)
        : Math.max(displayPoints[index - 1].remainingPoints - point.remainingPoints, 0);
      const completedStories = [...completedStoriesSoFar];

      return {
        key: `${point.date}-${index}`,
        x,
        yRemaining,
        yCompleted,
        date: point.date,
        remaining: point.remainingPoints,
        completed: point.completedPoints,
        burnedSincePrevious,
        completedStories,
        tooltip: this.buildBurnoutPointTooltip(point.date, point.remainingPoints, point.completedPoints, burnedSincePrevious, completedStories),
        completedTooltip: this.buildBurnoutCompletedTooltip(point.date, point.completedPoints, burnedSincePrevious, completedStories),
        showOnXAxis: index % step === 0
      };
    });
  }

  burnoutLineSegments(points: BurnoutPoint[], kind: 'remaining' | 'completed' = 'remaining'): Array<{ key: string; x1: number; y1: number; x2: number; y2: number; tooltip: string }> {
    if (points.length < 2) {
      const series = this.burnoutSeries(points);
      if (kind !== 'remaining' || series.length === 0) {
        return [];
      }

      const first = series[0];
      const initialDrop = Math.max(this.maxBurnoutValue(points) - first.remaining, 0);
      return initialDrop > 0
        ? [{
            key: `${first.key}-${kind}-initial-vertical-segment`,
            x1: first.x,
            y1: this.burnoutPadding.top,
            x2: first.x,
            y2: first.yRemaining,
            tooltip: first.tooltip
          }]
        : [];
    }

    const series = this.burnoutSeries(points);
    const segments: Array<{ key: string; x1: number; y1: number; x2: number; y2: number; tooltip: string }> = [];

    if (kind === 'remaining' && series.length > 0) {
      const first = series[0];
      const initialDrop = Math.max(this.maxBurnoutValue(points) - first.remaining, 0);
      if (initialDrop > 0) {
        segments.push({
          key: `${first.key}-${kind}-initial-vertical-segment`,
          x1: first.x,
          y1: this.burnoutPadding.top,
          x2: first.x,
          y2: first.yRemaining,
          tooltip: first.tooltip
        });
      }
    }

    for (let index = 1; index < series.length; index++) {
      const previous = series[index - 1];
      const current = series[index];
      const previousY = kind === 'remaining' ? previous.yRemaining : previous.yCompleted;
      const currentY = kind === 'remaining' ? current.yRemaining : current.yCompleted;

      segments.push({
        key: `${current.key}-${kind}-horizontal-segment`,
        x1: previous.x,
        y1: previousY,
        x2: current.x,
        y2: previousY,
        tooltip: kind === 'remaining' ? previous.tooltip : previous.completedTooltip
      });

      segments.push({
        key: `${current.key}-${kind}-vertical-segment`,
        x1: current.x,
        y1: previousY,
        x2: current.x,
        y2: currentY,
        tooltip: kind === 'remaining' ? current.tooltip : current.completedTooltip
      });
    }

    return segments;
  }

  burnoutPath(points: BurnoutPoint[], kind: 'remaining' | 'completed'): string {
    const series = this.burnoutSeries(points);
    if (series.length === 0) {
      return '';
    }

    const first = series[0];
    const firstY = kind === 'remaining' ? first.yRemaining : first.yCompleted;
    const pathParts: string[] = [];

    if (kind === 'remaining') {
      const initialDrop = Math.max(this.maxBurnoutValue(points) - first.remaining, 0);
      if (initialDrop > 0) {
        pathParts.push(`M ${first.x.toFixed(1)} ${this.burnoutPadding.top.toFixed(1)}`);
        pathParts.push(`L ${first.x.toFixed(1)} ${firstY.toFixed(1)}`);
      } else {
        pathParts.push(`M ${first.x.toFixed(1)} ${firstY.toFixed(1)}`);
      }
    } else {
      pathParts.push(`M ${first.x.toFixed(1)} ${firstY.toFixed(1)}`);
    }

    for (let i = 1; i < series.length; i++) {
      const previous = series[i - 1];
      const current = series[i];
      const previousY = kind === 'remaining' ? previous.yRemaining : previous.yCompleted;
      const currentY = kind === 'remaining' ? current.yRemaining : current.yCompleted;
      pathParts.push(`L ${current.x.toFixed(1)} ${previousY.toFixed(1)}`);
      pathParts.push(`L ${current.x.toFixed(1)} ${currentY.toFixed(1)}`);
    }

    return pathParts.join(' ');
  }

  idealBurnoutPath(points: BurnoutPoint[]): string {
    if (points.length === 0) {
      return '';
    }

    const startX = this.burnoutPadding.left;
    const startY = this.burnoutPadding.top;
    const endX = this.burnoutSvgWidth - this.burnoutPadding.right;
    const endY = this.burnoutSvgHeight - this.burnoutPadding.bottom;

    return `M ${startX.toFixed(1)} ${startY.toFixed(1)} L ${endX.toFixed(1)} ${endY.toFixed(1)}`;
  }

  burnoutYGrid(points: BurnoutPoint[]): number[] {
    const values = this.burnoutYAxisValues(points);
    const axisMax = values[0] ?? 1;
    const chartHeight = this.burnoutSvgHeight - this.burnoutPadding.top - this.burnoutPadding.bottom;

    return values.map(value => this.burnoutPadding.top + (1 - value / axisMax) * chartHeight);
  }

  burnoutYValue(y: number, points: BurnoutPoint[]): number {
    const grid = this.burnoutYGrid(points);
    const values = this.burnoutYAxisValues(points);
    if (grid.length === 0 || values.length === 0) {
      return 0;
    }

    let nearestIndex = 0;
    let nearestDistance = Math.abs(grid[0] - y);
    for (let index = 1; index < grid.length; index++) {
      const distance = Math.abs(grid[index] - y);
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearestIndex = index;
      }
    }

    return values[nearestIndex] ?? 0;
  }

  burnoutDateLabels(points: BurnoutPoint[]): Array<{ key: string; date: string; text: string; dayMonth: string; leftPercent: number; align: 'start' | 'center' | 'end' }> {
    const displayPoints = this.burnoutDisplayPoints(points);
    if (displayPoints.length === 0) {
      return [];
    }

    const series = this.burnoutSeries(points);

    return displayPoints.map((point, index) => {
      const x = series[index]?.x ?? this.burnoutPadding.left;
      const leftPercent = (x / this.burnoutSvgWidth) * 100;
      const align: 'start' | 'center' | 'end' = 'center';

      if (series[index] && !series[index].showOnXAxis) {
        return null;
      }

      return {
        key: `${point.date}-${index}`,
        date: point.date,
        text: String(index + 1),
        dayMonth: this.formatDayMonth(point.date),
        leftPercent,
        align
      };
    }).filter((item): item is NonNullable<typeof item> => item !== null);
  }

  isBurnoutToday(date: string): boolean {
    const parsedDate = this.parseIsoDate(date);
    if (parsedDate == null) {
      return false;
    }

    return this.toIsoDate(parsedDate) === this.toIsoDate(new Date());
  }

  onBurnoutMouseMove(event: MouseEvent, points: BurnoutPoint[]): void {
    const series = this.burnoutSeries(points);
    if (series.length === 0) {
      this.burnoutHover = null;
      return;
    }

    const target = event.currentTarget as SVGElement | null;
    const rect = target?.getBoundingClientRect();
    if (rect == null || rect.width <= 0) {
      this.burnoutHover = null;
      return;
    }

    const relativeX = ((event.clientX - rect.left) / rect.width) * this.burnoutSvgWidth;
    const hovered = series.reduce((nearest, current) => {
      const nearestDistance = Math.abs(nearest.x - relativeX);
      const currentDistance = Math.abs(current.x - relativeX);
      return currentDistance < nearestDistance ? current : nearest;
    });

    const allStories = (this.dashboard?.userStories ?? []).map(story => this.storyDisplayTitle(story));
    const completedByDay = hovered.completedStories ?? [];
    const completedSet = new Set(completedByDay.map(story => this.normalizeStoryTitle(story)));
    const completedStories = allStories.filter(story => completedSet.has(this.normalizeStoryTitle(story)));

    completedByDay.forEach(story => {
      const normalized = this.normalizeStoryTitle(story);
      if (!completedStories.some(item => this.normalizeStoryTitle(item) === normalized)) {
        completedStories.push(story);
      }
    });

    const incompleteStories = allStories.filter(story => !completedSet.has(this.normalizeStoryTitle(story)));
    const popupLeftPercent = Math.min(82, Math.max(18, (hovered.x / this.burnoutSvgWidth) * 100));

    this.burnoutHover = {
      date: hovered.date,
      x: hovered.x,
      popupLeftPercent,
      completedStories,
      incompleteStories
    };
  }

  clearBurnoutHover(): void {
    this.burnoutHover = null;
  }

  formatDate(value: string | null | undefined): string {
    if (value == null || value === '') {
      return '-';
    }

    const date = this.parseIsoDate(value);
    if (date == null || Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleDateString('cs-CZ', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatDateTime(value: unknown): string {
    if (value == null || value === '') {
      return '-';
    }

    const date = this.parseIsoDate(value);
    if (date == null) {
      return String(value);
    }

    const datePart = date.toLocaleDateString('cs-CZ', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    const timePart = date.toLocaleTimeString('cs-CZ', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    return `${datePart} ${timePart}`;
  }

  formatDiscussionDateTime(value: unknown): string {
    if (value == null || value === '') {
      return '-';
    }

    const date = this.parseIsoDate(value);
    if (date == null) {
      return String(value);
    }

    const timePart = date.toLocaleTimeString('cs-CZ', {
      hour: '2-digit',
      minute: '2-digit'
    });
    const datePart = date.toLocaleDateString('cs-CZ', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });

    return `${timePart} ${datePart}`;
  }

  resolveApiError(err: unknown, fallback: string): string {
    const httpError = err as HttpErrorResponse;
    if (httpError == null) {
      return fallback;
    }

    if (httpError.status === 0) {
      return 'Backend není dostupný. Ověř, že běží server na portu 8080.';
    }

    if (httpError.status === 409) {
      return 'Uživatel s tímto username už existuje.';
    }

    if (httpError.status === 401) {
      return 'Neplatné přihlašovací údaje.';
    }

    if (httpError.status === 400) {
      return 'Neplatný požadavek. Zkontroluj vyplněná pole.';
    }

    if (httpError.status === 404) {
      return 'Vybraná user story nebo uživatel už v backendu neexistuje. Přihlas se znovu a obnov data projektu.';
    }

    const errorPayload = httpError.error as { message?: string; error?: string; detail?: string } | undefined;
    return errorPayload?.message ?? errorPayload?.detail ?? errorPayload?.error ?? fallback;
  }

  private preferredSprintId(sprints: Sprint[]): number {
    if (this.selectedSprintId != null) {
      const alreadySelected = sprints.find(sprint => sprint.id === this.selectedSprintId);
      if (alreadySelected != null) {
        return alreadySelected.id;
      }
    }

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const activeSprint = sprints.find(sprint => {
      const start = this.toDayOnly(sprint.startDate);
      const end = this.toDayOnly(sprint.endDate);
      return start != null && end != null && start <= today && today <= end;
    });

    if (activeSprint != null) {
      return activeSprint.id;
    }

    const nearestUpcoming = sprints
      .map(sprint => ({ sprint, start: this.toDayOnly(sprint.startDate) }))
      .filter(item => item.start != null && item.start > today)
      .sort((a, b) => a.start!.getTime() - b.start!.getTime())[0];

    if (nearestUpcoming != null) {
      return nearestUpcoming.sprint.id;
    }

    const latestPast = sprints
      .map(sprint => ({ sprint, end: this.toDayOnly(sprint.endDate) }))
      .filter(item => item.end != null)
      .sort((a, b) => b.end!.getTime() - a.end!.getTime())[0];

    return latestPast?.sprint.id ?? sprints[0].id;
  }

  private toDayOnly(value: Date | string | null | undefined): Date | null {
    if (value == null || value === '') {
      return null;
    }

    const parsed = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate());
  }

  private scopedTasks(): TaskItem[] {
    const tasks = this.dashboard?.tasks ?? [];
    let scoped = tasks;

    if (this.selectedStoryId != null) {
      scoped = scoped.filter(task => task.storyId === this.selectedStoryId);
    }

    if (this.taskDisplayMode === 'mine' && this.currentUser != null) {
      scoped = scoped.filter(task => task.assigneeUserId === this.currentUser!.id);
    }

    return scoped;
  }

  private loadStoryDiscussionMessages(markAsRead: boolean): void {
    if (this.discussionStory == null) {
      return;
    }

    this.discussionLoading = true;
    this.taskService.getStoryDiscussionMessages(this.discussionStory.id)
      .pipe(finalize(() => {
        this.discussionLoading = false;
      }))
      .subscribe({
        next: messages => {
          this.discussionMessages = messages;
          this.discussionFlatMessages = this.flattenStoryDiscussionMessages(messages);
          if (markAsRead) {
            this.markOpenedStoryDiscussionAsRead();
          }
        },
        error: err => {
          this.errorMessage = this.resolveApiError(err, 'Načtení diskuse selhalo.');
        }
      });
  }

  private flattenStoryDiscussionMessages(
    messages: StoryDiscussionMessage[],
    depth = 0
  ): FlattenedStoryDiscussionMessage[] {
    return messages.flatMap(message => [
      { ...message, depth },
      ...this.flattenStoryDiscussionMessages(message.replies ?? [], depth + 1)
    ]);
  }

  private markOpenedStoryDiscussionAsRead(): void {
    if (this.currentUser == null || this.discussionStory == null) {
      return;
    }

    const discussionStoryId = this.discussionStory.id;
    this.taskService.markStoryDiscussionRead(discussionStoryId, this.currentUser.id)
      .subscribe({
        next: () => {
          this.refreshUnreadStoryDiscussionIds();
        },
        error: () => {
          // Keep UI usable
        }
      });
  }

  private refreshUnreadStoryDiscussionIds(): void {
    if (this.currentUser == null || this.selectedProjectId == null) {
      this.unreadDiscussionStoryIds.clear();
      return;
    }

    this.taskService.getUnreadStoryDiscussionIds(this.selectedProjectId, this.currentUser.id)
      .subscribe({
        next: storyIds => {
          this.unreadDiscussionStoryIds = new Set(storyIds);
        },
        error: () => {
          // do nothing
        }
      });
  }

  private resetProjectContext(): void {
    this.sprints = [];
    this.backlogStories = [];
    this.dashboard = null;
    this.myTasksLoading = false;
    this.myTasksOverview = [];
    this.myTaskStoriesById.clear();
    this.myTaskSprintDisplayByTaskId.clear();
    this.myTasksLoadedProjectId = null;
    this.selectedSprintId = null;
    this.selectedStoryId = null;
    this.taskForm.storyId = 0;
    this.sprintStoryDropActive = false;
    this.backlogStoryDropActive = false;
    this.draggingStoryId = null;
    this.draggingStorySource = null;
    this.unreadDiscussionStoryIds.clear();
    this.closeStoryDiscussion();
  }

  private loadSelectedProjectAndData(): void {
    if (this.selectedProjectId == null) {
      this.resetProjectContext();
      return;
    }

    this.taskService.getProjects().subscribe({
      next: projects => {
        const selectedProject = projects.find(project => project.id === this.selectedProjectId);
        if (selectedProject == null) {
          this.errorMessage = 'Vybraný projekt neexistuje. Vyber jiný projekt.';
          this.selectedProjectName = '';
          this.resetProjectContext();
          return;
        }

        this.selectedProjectName = selectedProject.name;
        this.errorMessage = '';
        this.refreshUnreadStoryDiscussionIds();
        this.loadBacklog();
        this.loadSprints();
        if (this.activeTopMenu === 'my-tasks') {
          void this.loadMyTasksOverview();
        }
      },
      error: () => {
        this.errorMessage = 'Nepodařilo se načíst vybraný projekt.';
        this.selectedProjectName = '';
        this.resetProjectContext();
      }
    });
  }

  private refreshMyTasksOverviewIfNeeded(): void {
    if (this.selectedProjectId == null || this.currentUser == null) {
      return;
    }

    if (this.activeTopMenu === 'my-tasks' || this.myTasksLoadedProjectId === this.selectedProjectId) {
      void this.loadMyTasksOverview();
    }
  }

  private async loadMyTasksOverview(): Promise<void> {
    if (this.selectedProjectId == null || this.currentUser == null) {
      this.myTasksOverview = [];
      this.myTaskStoriesById.clear();
      this.myTaskSprintDisplayByTaskId.clear();
      this.myTasksLoadedProjectId = null;
      this.myTasksLoading = false;
      return;
    }

    const projectId = this.selectedProjectId;
    const currentUserId = this.currentUser.id;
    this.myTasksLoading = true;
    this.errorMessage = '';

    try {
      const sprints = await firstValueFrom(this.taskService.getSprints(projectId));

      if (this.selectedProjectId !== projectId || this.currentUser?.id !== currentUserId) {
        return;
      }

      const dashboards = await Promise.all(
        sprints.map(async sprint => ({
          sprint,
          dashboard: await firstValueFrom(this.taskService.getSprintDashboard(sprint.id))
        }))
      );

      if (this.selectedProjectId !== projectId || this.currentUser?.id !== currentUserId) {
        return;
      }

      const storiesById = new Map<number, UserStory>();
      const sprintDisplayByTaskId = new Map<number, number>();
      const tasks = dashboards
        .flatMap(({ sprint, dashboard }) => {
          dashboard.userStories.forEach(story => storiesById.set(story.id, story));
          return dashboard.tasks
            .filter(task => task.assigneeUserId === currentUserId && task.status !== 'DONE')
            .map(task => {
              sprintDisplayByTaskId.set(task.id, this.sprintDisplayNumber(sprint) ?? 0);
              return task;
            });
        })
        .sort((left, right) => {
          const leftSprint = sprintDisplayByTaskId.get(left.id) ?? 0;
          const rightSprint = sprintDisplayByTaskId.get(right.id) ?? 0;
          if (leftSprint !== rightSprint) {
            return leftSprint - rightSprint;
          }

          const leftStory = storiesById.get(left.storyId)?.storyNumber ?? 0;
          const rightStory = storiesById.get(right.storyId)?.storyNumber ?? 0;
          if (leftStory !== rightStory) {
            return leftStory - rightStory;
          }

          return left.taskNumber - right.taskNumber;
        });

      this.myTasksOverview = tasks;
      this.myTaskStoriesById = storiesById;
      this.myTaskSprintDisplayByTaskId = sprintDisplayByTaskId;
      this.myTasksLoadedProjectId = projectId;
    } catch (err) {
      this.myTasksOverview = [];
      this.myTaskStoriesById.clear();
      this.myTaskSprintDisplayByTaskId.clear();
      this.myTasksLoadedProjectId = null;
      this.errorMessage = this.resolveApiError(err, 'Načtení mých tasků selhalo.');
    } finally {
      if (this.selectedProjectId === projectId && this.currentUser?.id === currentUserId) {
        this.myTasksLoading = false;
      }
    }
  }

  private maxBurnoutValue(points: BurnoutPoint[]): number {
    const totalStoryPoints = points.reduce((maxValue, point) => Math.max(maxValue, point.remainingPoints + point.completedPoints), 0);
    return Math.max(totalStoryPoints, 1);
  }

  private burnoutYAxisValues(points: BurnoutPoint[]): number[] {
    const step = this.burnoutYAxisStep(points);
    const axisMax = Math.max(step, Math.ceil(this.maxBurnoutValue(points) / step) * step);
    const values: number[] = [];

    for (let value = axisMax; value >= 0; value -= step) {
      values.push(value);
    }

    if (values[values.length - 1] !== 0) {
      values.push(0);
    }

    return values;
  }

  private burnoutYAxisStep(points: BurnoutPoint[]): 5 | 10 | 20 {
    const maxValue = this.maxBurnoutValue(points);
    if (maxValue <= 40) {
      return 5;
    }

    if (maxValue <= 100) {
      return 10;
    }

    return 20;
  }

  private buildBurnoutPointTooltip(date: string, remaining: number, completed: number, burnedSincePrevious: number, completedStories: string[]): string {
    const completedStoriesLabel = completedStories.length === 0
      ? 'Aktuálně dokončené user stories: žádné'
      : `Aktuálně dokončené user stories: ${completedStories.join(', ')}`;

    return `${this.formatDate(date)} · zbývá ${remaining} SP · dokončeno ${completed} SP · spáleno od minulého bodu ${burnedSincePrevious} SP\n${completedStoriesLabel}`;
  }

  private buildBurnoutCompletedTooltip(date: string, completed: number, burnedSincePrevious: number, completedStories: string[]): string {
    const completedStoriesLabel = completedStories.length === 0
      ? 'Aktuálně dokončené user stories: žádné'
      : `Aktuálně dokončené user stories: ${completedStories.join(', ')}`;

    return `${this.formatDate(date)} · dokončeno ${completed} SP · spáleno od minulého bodu ${burnedSincePrevious} SP\n${completedStoriesLabel}`;
  }

  private resolveBurnoutTimeline(points: BurnoutPoint[]): { start: Date | null; end: Date | null; totalDays: number } {
    const pointDates = points
      .map(point => this.parseIsoDate(point.date))
      .filter((date): date is Date => date != null)
      .sort((a, b) => a.getTime() - b.getTime());

    const pointStart = pointDates[0] ?? null;
    const pointEnd = pointDates[pointDates.length - 1] ?? null;
    const sprintStart = this.parseIsoDate(this.dashboard?.sprint.startDate);
    const sprintEnd = this.parseIsoDate(this.dashboard?.sprint.endDate);

    const usePointTimeline = pointStart != null && pointEnd != null && (
      sprintStart == null
      || sprintEnd == null
      || pointStart.getTime() < sprintStart.getTime()
      || pointEnd.getTime() > sprintEnd.getTime()
    );

    const start = usePointTimeline ? pointStart : (sprintStart ?? pointStart);
    const end = usePointTimeline ? pointEnd : (sprintEnd ?? pointEnd);
    const totalDays = this.countDaysInclusive(start, end);

    return {
      start,
      end,
      totalDays: totalDays > 0 ? totalDays : points.length
    };
  }

  private burnoutDisplayPoints(points: BurnoutPoint[]): BurnoutPoint[] {
    if (points.length === 0) {
      return [];
    }

    const normalizedPoints = points
      .map(point => {
        const parsedDate = this.parseIsoDate(point.date);
        return parsedDate == null
          ? null
          : {
              ...point,
              date: this.toIsoDate(parsedDate),
              completedStories: Array.isArray(point.completedStories) ? point.completedStories : []
            };
      })
      .filter((point): point is BurnoutPoint => point != null)
      .sort((left, right) => {
        const leftTime = this.parseIsoDate(left.date)?.getTime() ?? 0;
        const rightTime = this.parseIsoDate(right.date)?.getTime() ?? 0;
        return leftTime - rightTime;
      });

    const timeline = this.resolveBurnoutTimeline(normalizedPoints);
    if (timeline.start == null || timeline.totalDays <= 0) {
      return normalizedPoints;
    }

    const pointsByDate = new Map<string, BurnoutPoint>();
    normalizedPoints.forEach(point => {
      pointsByDate.set(point.date, point);
    });

    const totalStoryPoints = this.maxBurnoutValue(normalizedPoints);
    const displayPoints: BurnoutPoint[] = [];
    let completedPoints = 0;
    let remainingPoints = totalStoryPoints;

    for (let offset = 0; offset < timeline.totalDays; offset++) {
      const currentDate = this.addDays(timeline.start, offset);
      const date = this.toIsoDate(currentDate);
      const point = pointsByDate.get(date);

      if (point != null) {
        completedPoints = point.completedPoints;
        remainingPoints = point.remainingPoints;
      }

      displayPoints.push({
        date,
        completedPoints,
        remainingPoints,
        completedStories: point?.completedStories ?? []
      });
    }

    return displayPoints;
  }

  private parseIsoDate(value: unknown): Date | null {
    if (value == null) {
      return null;
    }

    if (value instanceof Date) {
      return Number.isNaN(value.getTime()) ? null : new Date(value.getTime());
    }

    if (typeof value === 'string') {
      const raw = value.trim();
      if (raw === '') {
        return null;
      }

      const parsed = raw.includes('T')
        ? new Date(raw)
        : new Date(`${raw}T00:00:00`);
      if (Number.isNaN(parsed.getTime())) {
        return null;
      }
      return parsed;
    }

    if (Array.isArray(value) && value.length >= 3) {
      const [year, month, day, hour = 0, minute = 0, second = 0, fraction = 0] = value;
      if (
        typeof year === 'number'
        && typeof month === 'number'
        && typeof day === 'number'
        && typeof hour === 'number'
        && typeof minute === 'number'
        && typeof second === 'number'
        && typeof fraction === 'number'
      ) {
        const millisecond = fraction > 999
          ? Math.floor(fraction / 1_000_000)
          : fraction;
        const parsed = new Date(year, month - 1, day, hour, minute, second, millisecond);
        return Number.isNaN(parsed.getTime()) ? null : parsed;
      }
    }

    const parsed = new Date(String(value));
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return parsed;
  }

  private countDaysInclusive(start: Date | null, end: Date | null): number {
    if (start == null || end == null || end < start) {
      return 0;
    }

    const startUtc = Date.UTC(start.getFullYear(), start.getMonth(), start.getDate());
    const endUtc = Date.UTC(end.getFullYear(), end.getMonth(), end.getDate());
    return Math.floor((endUtc - startUtc) / 86_400_000) + 1;
  }

  private addDays(value: Date, days: number): Date {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate() + days);
  }

  private toIsoDate(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDayMonth(value: string): string {
    const parsed = this.parseIsoDate(value);
    if (parsed == null) {
      return '';
    }

    const day = String(parsed.getDate()).padStart(2, '0');
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    return `${day}.${month}.`;
  }

  private storyDisplayTitle(story: UserStory): string {
    const cleanTitle = (story.title ?? '').trim().replace(/^#\d+\s*/, '');
    const titlePart = cleanTitle === '' ? 'Bez názvu' : cleanTitle;
    return `#${story.storyNumber} ${titlePart}`;
  }

  private normalizeStoryTitle(value: string): string {
    return value.trim().replace(/\s+/g, ' ').toLowerCase();
  }

  private storyProgressCategory(story: UserStory): 'completed' | 'in-progress' | 'not-started' {
    const normalizedStatus = (story.status ?? '').trim().toLowerCase();

    if (normalizedStatus === 'completed' || story.isCompleted === true) {
      return 'completed';
    }

    if (normalizedStatus === 'in progress' || normalizedStatus === 'in_progress') {
      return 'in-progress';
    }

    return 'not-started';
  }
}
