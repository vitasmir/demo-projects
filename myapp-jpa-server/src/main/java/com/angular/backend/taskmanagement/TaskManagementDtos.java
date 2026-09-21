package com.angular.backend.taskmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TaskManagementDtos {

    private TaskManagementDtos() {
    }

    public record UserResponse(String id, String username, String displayName, boolean admin, List<Long> accessibleProjectIds) {
    }

        public record CreateUserRequest(
            String username,
            String displayName,
            String firstName,
            String lastName,
            String email,
            String password,
            Boolean admin) {
    }

        public record CreateProjectRequest(String name, String description) {
        }

        public record ProjectResponse(Long id, String name, String description) {
        }

    public record ProjectAccessOverviewResponse(List<UserResponse> users, List<ProjectResponse> projects) {
    }

    public record UpdateUserProjectAccessRequest(List<Long> projectIds) {
    }

        public record CreateSprintRequest(Long projectId, String name, LocalDate startDate, LocalDate endDate) {
    }

        public record SprintResponse(Long id, Long projectId, String name, LocalDate startDate, LocalDate endDate) {
    }

        public record CreateStoryRequest(Long projectId, Long sprintId, String title, String description, Integer difficulty, String colorHex) {
    }

        public record UpdateStoryRequest(String title, String description) {
    }

        public record StoryResponse(Long id, Integer storyNumber, Long projectId, Long sprintId, String title, String description, Integer difficulty, boolean isCompleted, String status, int taskCount, int completedTaskCount, String colorHex) {
        }

        public record AssignStoryToSprintRequest(Long sprintId) {
        }

    public record CreateTaskRequest(
            Long storyId,
            String title,
            String description,
            String definitionOfDone,
            String colorHex,
            String creatorUserId,
            String assigneeUserId,
            String reviewerUserId) {
    }

        public record CreateStoryDiscussionMessageRequest(
            String authorUserId,
            Long parentMessageId,
            String content) {
        }

        public record MarkStoryDiscussionReadRequest(String userId) {
        }

            public record StoryDiscussionRealtimeEvent(
                String eventType,
                Long projectId,
                Long storyId,
                String actorUserId,
                List<Long> unreadStoryIds,
                LocalDateTime occurredAt) {
            }

        public record StoryDiscussionMessageResponse(
            Long id,
            Long storyId,
            Long parentMessageId,
            String authorUserId,
            String authorDisplayName,
            String content,
            LocalDateTime createdAt,
            List<StoryDiscussionMessageResponse> replies) {
        }
        
        public record CreateTaskDiscussionMessageRequest(
            String authorUserId,
            Long parentMessageId,
            String content) {
        }

        public record MarkTaskDiscussionReadRequest(String userId) {
        }

        public record TaskDiscussionRealtimeEvent(
            String eventType,
            Long projectId,
            Long taskId,
            String actorUserId,
            List<Long> unreadTaskIds,
            LocalDateTime occurredAt) {
        }

        public record TaskDiscussionMessageResponse(
            Long id,
            Long taskId,
            Long parentMessageId,
            String authorUserId,
            String authorDisplayName,
            String content,
            LocalDateTime createdAt,
            List<TaskDiscussionMessageResponse> replies) {
        }

    public record TaskResponse(
            Long id,
            Integer taskNumber,
            String taskKey,
            Long storyId,
            String title,
            String description,
            String definitionOfDone,
            String colorHex,
            TaskStatus status,
            String creatorUserId,
            String creatorDisplayName,
            String assigneeUserId,
            String assigneeDisplayName,
            String reviewerUserId,
            String reviewerDisplayName,
            String reviewComment,
            LocalDateTime completedAt) {
    }

    public record UpdateTaskStatusRequest(TaskStatus status, String reviewComment) {
    }

        public record UpdateTaskRequest(
            String title,
            String description,
            String definitionOfDone) {
        }

    public record ReturnTaskRequest(String reviewComment) {
    }

    public record AddCodeReferenceRequest(String commitHash, String repositoryUrl, String note) {
    }

    public record CodeReferenceResponse(Long id, Long taskId, String commitHash, String repositoryUrl, String note,
            LocalDateTime createdAt) {
    }

    public record BurnoutPoint(LocalDate date, long completedPoints, long remainingPoints, List<String> completedStories) {
    }

    public record SprintDashboardResponse(
            SprintResponse sprint,
            List<StoryResponse> userStories,
            List<TaskResponse> tasks,
            int velocity,
            List<BurnoutPoint> burnoutChart) {
    }
}
