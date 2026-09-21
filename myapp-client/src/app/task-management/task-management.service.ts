import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  BurnoutPoint,
  CodeReference,
  ProjectAccessOverview,
  StoryDiscussionMessage, TaskDiscussionMessage,
  TmProject,
  Sprint,
  SprintDashboard,
  TaskItem,
  TaskStatus,
  TmUser,
  UserStory
} from './task-management.model';

@Injectable({
  providedIn: 'root'
})
export class TaskManagementService {
  private readonly baseUrl = '/api/task-management';
  private readonly currentUserStorageKey = 'tm_current_user';
  private currentUserCache: TmUser | null = null;

  constructor(private readonly http: HttpClient) { }

  syncCurrentUserFromJwt(): Observable<TmUser> {
    return this.http.get<TmUser>(`${this.baseUrl}/auth/me`)
      .pipe(tap(user => this.setCurrentUser(user)));
  }

  getCurrentUser(): TmUser | null {
    if (this.currentUserCache != null) {
      return this.currentUserCache;
    }

    try {
      const raw = localStorage.getItem(this.currentUserStorageKey);
      if (raw == null || raw.trim() === '') {
        return null;
      }

      const parsed = JSON.parse(raw) as TmUser;
      if (parsed == null || parsed.id == null || parsed.username == null || parsed.displayName == null) {
        return null;
      }

      this.currentUserCache = parsed;
      return parsed;
    } catch {
      return null;
    }
  }

  setCurrentUser(user: TmUser | null): void {
    this.currentUserCache = user;

    if (user == null) {
      localStorage.removeItem(this.currentUserStorageKey);
      return;
    }

    localStorage.setItem(this.currentUserStorageKey, JSON.stringify(user));
  }

  logout(): void {
    this.setCurrentUser(null);
  }

  createProject(name: string, description: string): Observable<TmProject> {
    return this.http.post<TmProject>(`${this.baseUrl}/projects`, { name, description });
  }

  getProjects(): Observable<TmProject[]> {
    return this.http.get<TmProject[]>(`${this.baseUrl}/projects`);
  }

  getProjectAccessOverview(): Observable<ProjectAccessOverview> {
    return this.http.get<ProjectAccessOverview>(`${this.baseUrl}/admin/project-access`);
  }

  updateUserProjectAccess(userId: string, projectIds: number[]): Observable<TmUser> {
    return this.http.put<TmUser>(`${this.baseUrl}/admin/project-access/users/${userId}`, { projectIds });
  }

  deleteManagedUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/project-access/users/${userId}`);
  }

  deleteProject(projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/projects/${projectId}`);
  }

  createSprint(projectId: number, name: string, startDate: string, endDate: string): Observable<Sprint> {
    return this.http.post<Sprint>(`${this.baseUrl}/sprints`, {
      projectId,
      name,
      startDate: this.toApiDate(startDate),
      endDate: this.toApiDate(endDate)
    });
  }

  getSprints(projectId: number): Observable<Sprint[]> {
    return this.http.get<Sprint[]>(`${this.baseUrl}/sprints`, { params: { projectId } });
  }

  getUsers(projectId?: number): Observable<TmUser[]> {
    return this.http.get<TmUser[]>(`${this.baseUrl}/users`, {
      params: projectId == null ? {} : { projectId }
    });
  }

  registerUser(payload: {
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }): Observable<TmUser> {
    return this.http.post<TmUser>(`${this.baseUrl}/register`, {
      ...payload,
      admin: false
    });
  }

  createUser(payload: {
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    admin: boolean;
  }): Observable<TmUser> {
    return this.http.post<TmUser>(`${this.baseUrl}/users`, payload);
  }

  createStory(projectId: number, title: string, description: string, difficulty: number, colorHex: string): Observable<UserStory> {
    return this.http.post<UserStory>(`${this.baseUrl}/stories`, { projectId, title, description, difficulty, colorHex });
  }

  getProductBacklog(projectId: number): Observable<UserStory[]> {
    return this.http.get<UserStory[]>(`${this.baseUrl}/stories/backlog`, { params: { projectId } });
  }

  updateStory(storyId: number, payload: {
    title: string;
    description: string;
  }): Observable<UserStory> {
    return this.http.put<UserStory>(`${this.baseUrl}/stories/${storyId}`, payload);
  }

  assignStoryToSprint(storyId: number, sprintId: number | null): Observable<UserStory> {
    return this.http.put<UserStory>(`${this.baseUrl}/stories/${storyId}/assign-sprint`, { sprintId });
  }

  deleteStory(storyId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/stories/${storyId}`);
  }

  getUnreadStoryDiscussionIds(projectId: number, userId: string): Observable<number[]> {
    return this.http.get<number[]>(`${this.baseUrl}/projects/${projectId}/story-discussions/unread`, {
      params: { userId }
    });
  }

  getStoryDiscussionMessages(storyId: number): Observable<StoryDiscussionMessage[]> {
    return this.http.get<StoryDiscussionMessage[]>(`${this.baseUrl}/stories/${storyId}/discussion/messages`);
  }

  createStoryDiscussionMessage(payload: {
    storyId: number;
    authorUserId: string;
    parentMessageId: number | null;
    content: string;
  }): Observable<StoryDiscussionMessage> {
    const { storyId, ...body } = payload;
    return this.http.post<StoryDiscussionMessage>(`${this.baseUrl}/stories/${storyId}/discussion/messages`, body);
  }

  markStoryDiscussionRead(storyId: number, userId: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/stories/${storyId}/discussion/read`, { userId });
  }

  getUnreadTaskDiscussionIds(projectId: number, userId: string): Observable<number[]> {
    return this.http.get<number[]>(`${this.baseUrl}/projects/${projectId}/task-discussions/unread`, {
      params: { userId }
    });
  }

  getTaskDiscussionMessages(taskId: number): Observable<TaskDiscussionMessage[]> {
    return this.http.get<TaskDiscussionMessage[]>(`${this.baseUrl}/tasks/${taskId}/discussion/messages`);
  }

  createTaskDiscussionMessage(payload: {
    taskId: number;
    authorUserId: string;
    parentMessageId: number | null;
    content: string;
  }): Observable<TaskDiscussionMessage> {
    const { taskId, ...body } = payload;
    return this.http.post<TaskDiscussionMessage>(`${this.baseUrl}/tasks/${taskId}/discussion/messages`, body);
  }

  markTaskDiscussionRead(taskId: number, userId: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/tasks/${taskId}/discussion/read`, { userId });
  }

  createTask(payload: {
    storyId: number;
    title: string;
    description: string;
    definitionOfDone: string;
    colorHex: string;
    creatorUserId: string;
    assigneeUserId: string;
    reviewerUserId: string;
  }): Observable<TaskItem> {
    return this.http.post<TaskItem>(`${this.baseUrl}/tasks`, payload);
  }

  updateTaskStatus(taskId: number, status: TaskStatus, reviewComment: string): Observable<TaskItem> {
    return this.http.put<TaskItem>(`${this.baseUrl}/tasks/${taskId}/status`, { status, reviewComment });
  }

  updateTask(taskId: number, payload: {
    title: string;
    description: string;
    definitionOfDone: string;
  }): Observable<TaskItem> {
    return this.http.put<TaskItem>(`${this.baseUrl}/tasks/${taskId}`, payload);
  }

  returnTask(taskId: number, reviewComment: string): Observable<TaskItem> {
    return this.http.put<TaskItem>(`${this.baseUrl}/tasks/${taskId}/return`, { reviewComment });
  }

  addCodeReference(taskId: number, commitHash: string, repositoryUrl: string, note: string): Observable<CodeReference> {
    return this.http.post<CodeReference>(`${this.baseUrl}/tasks/${taskId}/references`, {
      commitHash,
      repositoryUrl,
      note
    });
  }

  getCodeReferences(taskId: number): Observable<CodeReference[]> {
    return this.http.get<CodeReference[]>(`${this.baseUrl}/tasks/${taskId}/references`);
  }

  getSprintDashboard(sprintId: number): Observable<SprintDashboard> {
    return this.http.get<SprintDashboard>(`${this.baseUrl}/dashboard/sprints/${sprintId}`)
      .pipe(map(dashboard => this.normalizeSprintDashboard(dashboard)));
  }

  private normalizeSprintDashboard(dashboard: SprintDashboard): SprintDashboard {
    const existingChart = Array.isArray(dashboard.burnoutChart) ? dashboard.burnoutChart : [];
    if (existingChart.length > 0) {
      return dashboard;
    }

    const fallbackChart = this.buildFallbackBurnoutChart(dashboard);
    return {
      ...dashboard,
      burnoutChart: fallbackChart
    };
  }

  private buildFallbackBurnoutChart(dashboard: SprintDashboard): BurnoutPoint[] {
    const stories = dashboard.userStories ?? [];
    const tasks = dashboard.tasks ?? [];
    const sprintStart = this.parseDateValue(dashboard.sprint?.startDate ?? null);
    const sprintEnd = this.parseDateValue(dashboard.sprint?.endDate ?? null);
    const today = this.startOfDay(new Date());
    const completionEvents = this.buildStoryCompletionEvents(stories, tasks);

    let chartStart = sprintStart;
    let chartEnd = sprintEnd != null && sprintEnd.getTime() < today.getTime() ? sprintEnd : today;

    const earliestCompletion = completionEvents[0]?.date ?? null;
    const latestCompletion = completionEvents.length > 0 ? completionEvents[completionEvents.length - 1].date : null;

    if (earliestCompletion != null && (chartStart == null || earliestCompletion.getTime() < chartStart.getTime())) {
      chartStart = this.addDays(earliestCompletion, -1);
    }

    if (latestCompletion != null && (chartEnd == null || latestCompletion.getTime() > chartEnd.getTime())) {
      chartEnd = latestCompletion;
    }

    if (chartStart == null || chartEnd == null || chartEnd.getTime() < chartStart.getTime()) {
      if (earliestCompletion == null) {
        return [];
      }

      chartStart = this.addDays(earliestCompletion, -1);
      chartEnd = latestCompletion ?? earliestCompletion;
    }

    const totalStoryPoints = stories.reduce((sum, story) => sum + (Number(story.difficulty) || 0), 0);
    const completionsByDate = new Map<string, { storyPoints: number; storyTitles: string[] }>();

    completionEvents.forEach(event => {
      const key = this.toIsoDate(event.date);
      const current = completionsByDate.get(key) ?? { storyPoints: 0, storyTitles: [] };
      current.storyPoints += event.storyPoints;
      current.storyTitles.push(event.storyTitle);
      completionsByDate.set(key, current);
    });

    const points: BurnoutPoint[] = [];
    let completedPoints = 0;

    for (let cursor = new Date(chartStart.getTime()); cursor.getTime() <= chartEnd.getTime(); cursor = this.addDays(cursor, 1)) {
      const key = this.toIsoDate(cursor);
      const completedToday = completionsByDate.get(key);
      if (completedToday != null) {
        completedPoints += completedToday.storyPoints;
      }

      points.push({
        date: key,
        completedPoints,
        remainingPoints: Math.max(totalStoryPoints - completedPoints, 0),
        completedStories: completedToday?.storyTitles ?? []
      });
    }

    return points;
  }

  private buildStoryCompletionEvents(stories: UserStory[], tasks: TaskItem[]): Array<{ date: Date; storyPoints: number; storyTitle: string }> {
    const tasksByStoryId = new Map<number, TaskItem[]>();
    tasks.forEach(task => {
      const storyTasks = tasksByStoryId.get(task.storyId) ?? [];
      storyTasks.push(task);
      tasksByStoryId.set(task.storyId, storyTasks);
    });

    return stories
      .map(story => {
        const storyTasks = tasksByStoryId.get(story.id) ?? [];
        const isCompleted = story.isCompleted === true
          || (storyTasks.length > 0 && storyTasks.every(task => task.status === 'DONE'));

        if (!isCompleted) {
          return null;
        }

        const completionDates = storyTasks
          .map(task => this.parseDateValue(task.completedAt))
          .filter((date): date is Date => date != null)
          .sort((a, b) => a.getTime() - b.getTime());

        const completionDate = completionDates[completionDates.length - 1] ?? null;
        if (completionDate == null) {
          return null;
        }

        const rawTitle = (story.title ?? '').trim().replace(/^#\d+\s*/, '');
        return {
          date: completionDate,
          storyPoints: Number(story.difficulty) || 0,
          storyTitle: `#${story.storyNumber} ${rawTitle}`.trim()
        };
      })
      .filter((event): event is { date: Date; storyPoints: number; storyTitle: string } => event != null)
      .sort((a, b) => a.date.getTime() - b.date.getTime());
  }

  private parseDateValue(value: string | number[] | null | undefined): Date | null {
    if (value == null) {
      return null;
    }

    if (Array.isArray(value)) {
      const [year, month, day] = value;
      if (![year, month, day].every(Number.isInteger)) {
        return null;
      }

      const parsedArrayDate = new Date(year, month - 1, day);
      return Number.isNaN(parsedArrayDate.getTime()) ? null : this.startOfDay(parsedArrayDate);
    }

    const raw = value.trim();
    if (raw === '') {
      return null;
    }

    const parsed = raw.includes('T') ? new Date(raw) : new Date(`${raw}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return this.startOfDay(parsed);
  }

  private startOfDay(value: Date): Date {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate());
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

  private toApiDate(value: string): string {
    const normalized = value.trim();
    const match = normalized.match(/^(\d{4})[-\.](\d{1,2})[-\.](\d{1,2})$/);
    if (match == null) {
      return value;
    }

    const [, yearValue, monthValue, dayValue] = match;
    const month = monthValue.padStart(2, '0');
    const day = dayValue.padStart(2, '0');
    return `${yearValue}-${month}-${day}`;
  }
}
