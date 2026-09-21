import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { convertToParamMap, ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { TaskManagementStateService } from './task-management.state';
import { TaskManagementService } from './task-management.service';
import { Sprint, SprintDashboard, TmProject, TmUser } from './task-management.model';

describe('TaskManagementStateService', () => {
  let service: TaskManagementStateService;
  let taskService: jasmine.SpyObj<TaskManagementService>;
  let router: jasmine.SpyObj<Router>;
  let queryParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let routeStub: ActivatedRoute;

  const currentUser: TmUser = {
    id: 'u1',
    username: 'tester',
    displayName: 'Tester'
  };

  const project: TmProject = {
    id: 5,
    name: 'Alpha',
    description: 'Project Alpha'
  };

  const sprint: Sprint = {
    id: 10,
    projectId: 5,
    name: 'Sprint 1',
    startDate: '2026-03-20',
    endDate: '2026-03-28'
  };

  const dashboard: SprintDashboard = {
    sprint,
    userStories: [{
      id: 100,
      storyNumber: 1,
      projectId: 5,
      sprintId: 10,
      title: 'Story one',
      description: 'Description',
      difficulty: 10,
      status: 'in progress',
      isCompleted: false,
      colorHex: '#FFF8DC'
    }],
    tasks: [{
      id: 200,
      taskNumber: 1,
      taskKey: 'task#1',
      storyId: 100,
      title: 'Task one',
      description: 'Desc',
      definitionOfDone: 'DoD',
      colorHex: '#FFF8DC',
      status: 'TODO',
      creatorUserId: 'u1',
      creatorDisplayName: 'Tester',
      assigneeUserId: 'u1',
      assigneeDisplayName: 'Tester',
      reviewerUserId: 'u1',
      reviewerDisplayName: 'Tester',
      reviewComment: null,
      completedAt: null
    }],
    velocity: 0,
    burnoutChart: []
  };

  beforeEach(() => {
    queryParamMap$ = new BehaviorSubject(convertToParamMap({}));
    routeStub = {
      queryParamMap: queryParamMap$.asObservable()
    } as ActivatedRoute;

    taskService = jasmine.createSpyObj<TaskManagementService>('TaskManagementService', [
      'getCurrentUser',
      'getUsers',
      'getProjects',
      'getProductBacklog',
      'getSprints',
      'getSprintDashboard',
      'createTask',
      'logout'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    taskService.getCurrentUser.and.returnValue(currentUser);
    taskService.getUsers.and.returnValue(of([currentUser]));
    taskService.getProjects.and.returnValue(of([project]));
    taskService.getProductBacklog.and.returnValue(of([]));
    taskService.getSprints.and.returnValue(of([sprint]));
    taskService.getSprintDashboard.and.returnValue(of(dashboard));
    taskService.createTask.and.returnValue(of(dashboard.tasks[0]));

    TestBed.configureTestingModule({
      providers: [
        TaskManagementStateService,
        { provide: TaskManagementService, useValue: taskService },
        { provide: Router, useValue: router }
      ]
    });

    service = TestBed.inject(TaskManagementStateService);
  });

  it('should initialize current user and load project data from query params', () => {
    service.init(routeStub);
    queryParamMap$.next(convertToParamMap({ projectId: '5' }));

    expect(service.currentUser).toEqual(currentUser);
    expect(taskService.getUsers).toHaveBeenCalled();
    expect(service.selectedProjectId).toBe(5);
    expect(service.selectedProjectName).toBe('Alpha');
    expect(taskService.getProductBacklog).toHaveBeenCalledWith(5);
    expect(taskService.getSprints).toHaveBeenCalledWith(5);
    expect(taskService.getSprintDashboard).toHaveBeenCalledWith(10);
    expect(service.selectedSprintId).toBe(10);
    expect(service.dashboard).toEqual(dashboard);
  });

  it('should change story context and navigate to tasks menu', () => {
    service.selectedProjectId = 5;

    service.onMenuStoryChange(100, 'mine');

    expect(service.selectedStoryId).toBe(100);
    expect(service.taskForm.storyId).toBe(100);
    expect(service.taskDisplayMode).toBe('mine');
    expect(service.activeTopMenu).toBe('tasks');
    expect(router.navigate).toHaveBeenCalledWith(['/task-management', 'tasks'], {
      queryParams: { projectId: 5 },
      queryParamsHandling: 'merge'
    });
  });

  it('should validate task creation before calling api', () => {
    service.currentUser = currentUser;
    service.taskForm.storyId = 0;

    service.createTask();

    expect(service.errorMessage).toBe('Nejdříve vytvoř user story.');
    expect(taskService.createTask).not.toHaveBeenCalled();
  });

  it('should create task and reload dashboard when form is valid', () => {
    service.currentUser = currentUser;
    service.selectedProjectId = 5;
    service.selectedSprintId = 10;
    service.taskForm.storyId = 100;
    service.taskForm.title = ' Implementace ';
    service.taskForm.description = ' Popis tasku ';
    service.taskForm.definitionOfDone = ' Hotovo ';
    service.taskForm.assigneeUserId = '';
    service.taskForm.reviewerUserId = '';

    service.createTask();

    expect(taskService.createTask).toHaveBeenCalledWith(jasmine.objectContaining({
      storyId: 100,
      title: 'Implementace',
      description: 'Popis tasku',
      definitionOfDone: 'Hotovo',
      creatorUserId: 'u1',
      assigneeUserId: 'u1',
      reviewerUserId: 'u1'
    }));
    expect(taskService.getSprintDashboard).toHaveBeenCalledWith(10);
    expect(service.taskForm.title).toBe('');
    expect(service.taskForm.description).toBe('');
    expect(service.taskForm.definitionOfDone).toBe('');
  });

  it('should map api error payload to a user-friendly message', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { message: 'Duplicitní záznam.' }
    });

    expect(service.resolveApiError(error, 'Fallback')).toBe('Uživatel s tímto username už existuje.');
  });

  it('should fallback to backend message for unknown statuses', () => {
    const error = new HttpErrorResponse({
      status: 422,
      error: { message: 'Custom backend message.' }
    });

    expect(service.resolveApiError(error, 'Fallback')).toBe('Custom backend message.');
  });

  it('should set error when loading selected project fails', () => {
    taskService.getProjects.and.returnValue(throwError(() => new Error('boom')));

    service.init(routeStub);
    queryParamMap$.next(convertToParamMap({ projectId: '5' }));

    expect(service.errorMessage).toBe('Nepodařilo se načíst vybraný projekt.');
    expect(service.selectedProjectName).toBe('');
    expect(service.dashboard).toBeNull();
  });
});