package com.angular.backend.taskmanagement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.angular.backend.taskmanagement.TaskManagementDtos.AddCodeReferenceRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.AssignStoryToSprintRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateStoryDiscussionMessageRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateUserRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateProjectRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CodeReferenceResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateSprintRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateStoryRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateTaskRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.MarkStoryDiscussionReadRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.ProjectResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.ProjectAccessOverviewResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.ReturnTaskRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.SprintDashboardResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.SprintResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.StoryDiscussionMessageResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateTaskDiscussionMessageRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.MarkTaskDiscussionReadRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.TaskDiscussionMessageResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.StoryResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.TaskResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateUserProjectAccessRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateStoryRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateTaskRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateTaskStatusRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UserResponse;

@RestController
@RequestMapping("/api/task-management")
public class TaskManagementController {

    private final TaskManagementService service;

    public TaskManagementController(TaskManagementService service) {
        this.service = service;
    }

    @GetMapping("/auth/me")
    public UserResponse syncCurrentUserFromJwt(@AuthenticationPrincipal Jwt jwt) {
        return service.syncCurrentUserFromJwt(jwt);
    }

    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(@RequestBody CreateSprintRequest request) {
        return service.createSprint(request);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@RequestBody CreateProjectRequest request) {
        return service.createProject(request);
    }

    @GetMapping("/projects")
    public List<ProjectResponse> getProjects() {
        return service.getProjects();
    }

    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long projectId) {
        service.deleteProject(projectId);
    }

    @GetMapping("/sprints")
    public List<SprintResponse> getSprints(@RequestParam Long projectId) {
        return service.getSprints(projectId);
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers(@RequestParam(required = false) Long projectId) {
        return service.getUsers(projectId);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@RequestBody CreateUserRequest request) {
        return service.registerUser(request);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.createUser(request, jwt);
    }

    @GetMapping("/admin/project-access")
    public ProjectAccessOverviewResponse getProjectAccessOverview() {
        return service.getProjectAccessOverview();
    }

    @PutMapping("/admin/project-access/users/{userId}")
    public UserResponse updateUserProjectAccess(
            @PathVariable String userId,
            @RequestBody(required = false) UpdateUserProjectAccessRequest request) {
        return service.updateUserProjectAccess(userId, request);
    }

    @DeleteMapping("/admin/project-access/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteManagedUser(@PathVariable String userId) {
        service.deleteManagedUser(userId);
    }

    @PostMapping("/stories")
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse createStory(@RequestBody CreateStoryRequest request) {
        return service.createStory(request);
    }

    @GetMapping("/stories")
    public List<StoryResponse> getStoriesBySprint(@RequestParam Long sprintId) {
        return service.getStoriesBySprint(sprintId);
    }

    @GetMapping("/stories/backlog")
    public List<StoryResponse> getProductBacklog(@RequestParam Long projectId) {
        return service.getProductBacklog(projectId);
    }

    @PutMapping("/stories/{storyId}")
    public StoryResponse updateStory(@PathVariable Long storyId, @RequestBody UpdateStoryRequest request) {
        return service.updateStory(storyId, request);
    }

    @PutMapping("/stories/{storyId}/assign-sprint")
    public StoryResponse assignStoryToSprint(@PathVariable Long storyId, @RequestBody AssignStoryToSprintRequest request) {
        return service.assignStoryToSprint(storyId, request);
    }

    @DeleteMapping("/stories/{storyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStory(@PathVariable Long storyId) {
        service.deleteStory(storyId);
    }

    @GetMapping("/projects/{projectId}/story-discussions/unread")
    public List<Long> getUnreadStoryDiscussionIds(@PathVariable Long projectId, @RequestParam String userId) {
        return service.getUnreadStoryDiscussionIds(projectId, userId);
    }

    @GetMapping("/stories/{storyId}/discussion/messages")
    public List<StoryDiscussionMessageResponse> getStoryDiscussionMessages(@PathVariable Long storyId) {
        return service.getStoryDiscussionMessages(storyId);
    }

    @PostMapping("/stories/{storyId}/discussion/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public StoryDiscussionMessageResponse createStoryDiscussionMessage(
            @PathVariable Long storyId,
            @RequestBody CreateStoryDiscussionMessageRequest request) {
        return service.createStoryDiscussionMessage(storyId, request);
    }

    @PutMapping("/stories/{storyId}/discussion/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markStoryDiscussionRead(
            @PathVariable Long storyId,
            @RequestBody MarkStoryDiscussionReadRequest request) {
        service.markStoryDiscussionRead(storyId, request);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@RequestBody CreateTaskRequest request) {
        return service.createTask(request);
    }

    @PutMapping("/tasks/{taskId}/status")
    public TaskResponse updateTaskStatus(@PathVariable Long taskId, @RequestBody UpdateTaskStatusRequest request) {
        return service.updateTaskStatus(taskId, request);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse updateTask(@PathVariable Long taskId, @RequestBody UpdateTaskRequest request) {
        return service.updateTask(taskId, request);
    }

    @PutMapping("/tasks/{taskId}/return")
    public TaskResponse returnTask(@PathVariable Long taskId, @RequestBody(required = false) ReturnTaskRequest request) {
        return service.returnTaskToCreator(taskId, request);
    }

    @PostMapping("/tasks/{taskId}/references")
    @ResponseStatus(HttpStatus.CREATED)
    public CodeReferenceResponse addCodeReference(
            @PathVariable Long taskId,
            @RequestBody AddCodeReferenceRequest request) {
        return service.addCodeReference(taskId, request);
    }

    @GetMapping("/tasks/{taskId}/references")
    public List<CodeReferenceResponse> getCodeReferences(@PathVariable Long taskId) {
        return service.getCodeReferences(taskId);
    }

    @GetMapping("/dashboard/sprints/{sprintId}")
    public SprintDashboardResponse getSprintDashboard(@PathVariable Long sprintId) {
        return service.getSprintDashboard(sprintId);
    }

    @GetMapping("/projects/{projectId}/task-discussions/unread")
    public List<Long> getUnreadTaskDiscussionIds(@PathVariable Long projectId, @RequestParam String userId) {
        return service.getUnreadTaskDiscussionIds(projectId, userId);
    }

    @GetMapping("/tasks/{taskId}/discussion/messages")
    public List<TaskDiscussionMessageResponse> getTaskDiscussionMessages(@PathVariable Long taskId) {
        return service.getTaskDiscussionMessages(taskId);
    }

    @PostMapping("/tasks/{taskId}/discussion/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDiscussionMessageResponse createTaskDiscussionMessage(
            @PathVariable Long taskId,
            @RequestBody CreateTaskDiscussionMessageRequest request) {
        return service.createTaskDiscussionMessage(taskId, request);
    }

    @PutMapping("/tasks/{taskId}/discussion/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markTaskDiscussionRead(
            @PathVariable Long taskId,
            @RequestBody MarkTaskDiscussionReadRequest request) {
        service.markTaskDiscussionRead(taskId, request);
    }
}
