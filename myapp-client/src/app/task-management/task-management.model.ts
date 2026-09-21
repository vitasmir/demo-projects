export interface TmUser {
  id: string;
  username: string;
  displayName: string;
  admin?: boolean;
  accessibleProjectIds?: number[];
}

export interface TmProject {
  id: number;
  name: string;
  description: string;
}

export interface ProjectAccessOverview {
  users: TmUser[];
  projects: TmProject[];
}

export interface Sprint {
  id: number;
  projectId: number;
  name: string;
  startDate: string;
  endDate: string;
}

export interface UserStory {
  id: number;
  storyNumber: number;
  projectId: number;
  sprintId: number | null;
  title: string;
  description: string;
  difficulty: 5 | 10 | 20 | 50 | 100;
  isCompleted?: boolean;
  status?: string;
  taskCount?: number;
  completedTaskCount?: number;
  colorHex: string;
}

export interface StoryDiscussionMessage {
  id: number;
  storyId: number;
  parentMessageId: number | null;
  authorUserId: string;
  authorDisplayName: string;
  content: string;
  createdAt: unknown;
  replies: StoryDiscussionMessage[];
}

export interface StoryDiscussionRealtimeEvent {
  eventType: 'MESSAGE_CREATED' | 'READ_STATE_UPDATED';
  projectId: number;
  storyId: number;
  actorUserId: string;
  unreadStoryIds?: number[] | null;
  occurredAt: unknown;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'RETURNED';

export interface TaskItem {
  id: number;
  taskNumber: number;
  taskKey: string;
  storyId: number;
  title: string;
  description: string;
  definitionOfDone: string;
  colorHex: string;
  status: TaskStatus;
  creatorUserId: string;
  creatorDisplayName: string;
  assigneeUserId: string;
  assigneeDisplayName: string;
  reviewerUserId: string;
  reviewerDisplayName: string;
  reviewComment: string | null;
  completedAt: string | null;
}

export interface BurnoutPoint {
  date: string;
  completedPoints: number;
  remainingPoints: number;
  completedStories: string[];
}

export interface SprintDashboard {
  sprint: Sprint;
  userStories: UserStory[];
  tasks: TaskItem[];
  velocity: number;
  burnoutChart: BurnoutPoint[];
}

export interface CodeReference {
  id: number;
  taskId: number;
  commitHash: string;
  repositoryUrl: string | null;
  note: string | null;
  createdAt: string;
}

export interface TaskDiscussionMessage {
  id: number;
  taskId: number;
  parentMessageId: number | null;
  authorUserId: string;
  authorDisplayName: string;
  content: string;
  createdAt: unknown;
  replies: TaskDiscussionMessage[];
}

export interface TaskDiscussionRealtimeEvent {
  eventType: 'MESSAGE_CREATED' | 'READ_STATE_UPDATED';
  projectId: number;
  taskId: number;
  actorUserId: string;
  unreadTaskIds?: number[] | null;
  occurredAt: unknown;
}
