package com.angular.backend.taskmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.angular.backend.taskmanagement.TaskManagementDtos.AddCodeReferenceRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.AssignStoryToSprintRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.BurnoutPoint;
import com.angular.backend.taskmanagement.TaskManagementDtos.CodeReferenceResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateProjectRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateSprintRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateStoryDiscussionMessageRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateStoryRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateTaskDiscussionMessageRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateTaskRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.CreateUserRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.MarkStoryDiscussionReadRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.MarkTaskDiscussionReadRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.ProjectAccessOverviewResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.ProjectResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.ReturnTaskRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.SprintDashboardResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.SprintResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.StoryDiscussionMessageResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.StoryDiscussionRealtimeEvent;
import com.angular.backend.taskmanagement.TaskManagementDtos.StoryResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.TaskDiscussionMessageResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.TaskDiscussionRealtimeEvent;
import com.angular.backend.taskmanagement.TaskManagementDtos.TaskResponse;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateStoryRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateTaskRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateTaskStatusRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UpdateUserProjectAccessRequest;
import com.angular.backend.taskmanagement.TaskManagementDtos.UserResponse;

@Service
public class TaskManagementService {

    private static final Set<Integer> ALLOWED_STORY_POINTS = Set.of(5, 10, 20, 50, 100);
        private static final Set<String> ADMIN_ROLE_NAMES = Set.of(
            "admin",
            "role_admin",
            "task-management-admin",
            "task_management_admin",
            "task.admin",
            "tm_admin");
    private static final String DISCUSSION_EVENT_MESSAGE_CREATED = "MESSAGE_CREATED";
    private static final String DISCUSSION_EVENT_READ_STATE_UPDATED = "READ_STATE_UPDATED";

    private final TaskManagementUserRepository userRepository;
    private final TmProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final UserStoryRepository storyRepository;
    private final TaskItemRepository taskRepository;
    private final TaskCodeReferenceRepository codeReferenceRepository;
    private final StoryDiscussionMessageRepository storyDiscussionMessageRepository;
    private final StoryDiscussionReadStateRepository storyDiscussionReadStateRepository;
    private final TaskDiscussionMessageRepository taskDiscussionMessageRepository;
    private final TaskDiscussionReadStateRepository taskDiscussionReadStateRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KeycloakUserProvisioningService keycloakUserProvisioningService;

    public TaskManagementService(
            TaskManagementUserRepository userRepository,
            TmProjectRepository projectRepository,
            SprintRepository sprintRepository,
            UserStoryRepository storyRepository,
            TaskItemRepository taskRepository,
            TaskCodeReferenceRepository codeReferenceRepository,
            StoryDiscussionMessageRepository storyDiscussionMessageRepository,
            StoryDiscussionReadStateRepository storyDiscussionReadStateRepository,
            TaskDiscussionMessageRepository taskDiscussionMessageRepository,
            TaskDiscussionReadStateRepository taskDiscussionReadStateRepository,
            SimpMessagingTemplate messagingTemplate,
            KeycloakUserProvisioningService keycloakUserProvisioningService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.storyRepository = storyRepository;
        this.taskRepository = taskRepository;
        this.codeReferenceRepository = codeReferenceRepository;
        this.storyDiscussionMessageRepository = storyDiscussionMessageRepository;
        this.storyDiscussionReadStateRepository = storyDiscussionReadStateRepository;
        this.taskDiscussionMessageRepository = taskDiscussionMessageRepository;
        this.taskDiscussionReadStateRepository = taskDiscussionReadStateRepository;
        this.messagingTemplate = messagingTemplate;
        this.keycloakUserProvisioningService = keycloakUserProvisioningService;
    }

    @Transactional
    public UserResponse syncCurrentUserFromJwt(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token is required");
        }

        TaskManagementUser user = syncJwtBackedUser(jwt);
        return toUserResponse(user, findAccessibleProjectIds(user.getId()));
    }

    @Transactional
    public UserResponse registerUser(CreateUserRequest request) {
        validateCreateUserRequest(request);

        if (Boolean.TRUE.equals(request.admin())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nemáš oprávnění vytvořit admin účet.");
        }

        return createManagedUser(
            request.username().trim(),
            normalizeDisplayName(request.firstName(), request.lastName()),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.password().trim(),
            false);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, Jwt jwt) {
        validateCreateUserRequest(request);

        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token is required");
        }

        String currentUsername = extractJwtUsername(jwt);
        TaskManagementUser currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        boolean currentIsAdmin = hasAdminRole(jwt)
            || (currentUser != null && currentUser.isAdmin())
            || keycloakUserProvisioningService.isBootstrapAdminUsername(currentUsername)
            || keycloakUserProvisioningService.userHasAdminRole(currentUsername);

        if (!currentIsAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nemáš oprávnění vytvářet uživatelské účty.");
        }

        return createManagedUser(
                request.username().trim(),
                normalizeDisplayName(request.firstName(), request.lastName()),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password().trim(),
                Boolean.TRUE.equals(request.admin()));
    }

    private void validateCreateUserRequest(CreateUserRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.firstName())
            || isBlank(request.lastName()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Username, first name, last name, email and password are required");
        }

        String username = request.username().trim();
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String email = request.email().trim();
        String displayName = normalizeDisplayName(request.firstName(), request.lastName());
        String password = request.password();

        if (!username.matches("^[a-zA-Z0-9._@-]{4,40}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username musí mít 4-40 znaků a smí obsahovat jen písmena, čísla, tečku, podtržítko, pomlčku nebo @.");
        }

        if (displayName.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zobrazované jméno může mít nejvýše 80 znaků.");
        }

        if (firstName.length() > 80 || lastName.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jméno a příjmení mohou mít nejvýše 80 znaků.");
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email nemá platný formát.");
        }

        if (password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
    }

    private UserResponse createManagedUser(
            String requestedUsername,
            String requestedDisplayName,
            String requestedFirstName,
            String requestedLastName,
            String requestedEmail,
            String requestedPassword,
            boolean requestedAdmin) {
        if (userRepository.existsByUsername(requestedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        String keycloakUserId = keycloakUserProvisioningService.createUser(
                requestedUsername,
                requestedFirstName,
                requestedLastName,
                requestedEmail,
                requestedPassword,
                requestedAdmin);

        try {
            TaskManagementUser user = new TaskManagementUser();
            user.setUsername(requestedUsername);
            user.setDisplayName(requestedDisplayName);
            user.setAdmin(requestedAdmin);

            TaskManagementUser savedUser = userRepository.save(user);
            return toUserResponse(savedUser, findAccessibleProjectIds(savedUser.getId()));
        } catch (RuntimeException exception) {
            keycloakUserProvisioningService.deleteUser(keycloakUserId);
            throw exception;
        }
    }

    private String normalizeDisplayName(String firstName, String lastName) {
        String nameFromParts = String.join(" ", firstName == null ? "" : firstName.trim(), lastName == null ? "" : lastName.trim()).trim();
        return nameFromParts;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        if (request == null || isBlank(request.name()) || isBlank(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name and description are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(true);

        TmProject project = new TmProject();
        project.setName(request.name().trim());
        project.setDescription(request.description().trim());
        if (accessContext.user() != null) {
            project.getMembers().add(accessContext.user());
        }
        return toProjectResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        AccessContext accessContext = resolveCurrentAccessContext(false);
        List<TmProject> projects = accessContext.admin()
                ? projectRepository.findAllByOrderByNameAscIdAsc()
                : accessContext.user() == null
                        ? List.of()
                        : projectRepository.findDistinctByMembersIdOrderByNameAscIdAsc(accessContext.user().getId());

        return projects.stream()
                .map(this::toProjectResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(Long projectId) {
        AccessContext accessContext = resolveCurrentAccessContext(false);

        if (projectId != null) {
            requireProjectAccess(projectId, accessContext);
            return userRepository.findUsersVisibleForProject(projectId).stream()
                    .map(user -> toUserResponse(user, List.of()))
                    .toList();
        }

        if (accessContext.admin()) {
            return userRepository.findAllByOrderByDisplayNameAscUsernameAsc().stream()
                    .map(user -> toUserResponse(user, List.of()))
                    .toList();
        }

        if (accessContext.user() == null) {
            return List.of();
        }

        return List.of(toUserResponse(accessContext.user(), findAccessibleProjectIds(accessContext.user().getId())));
    }

    @Transactional(readOnly = true)
    public ProjectAccessOverviewResponse getProjectAccessOverview() {
        AccessContext accessContext = resolveCurrentAccessContext(false);
        assertAdmin(accessContext);

        List<TmProject> projects = new ArrayList<>(projectRepository.findAllWithMembers());
        projects.sort(Comparator
                .comparing(TmProject::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TmProject::getId));

        List<UserResponse> users = userRepository.findAllByOrderByDisplayNameAscUsernameAsc().stream()
            .map(user -> toUserResponse(user, findAccessibleProjectIds(user.getId())))
                .toList();

        return new ProjectAccessOverviewResponse(
                users,
                projects.stream().map(this::toProjectResponse).toList());
    }

    @Transactional
    public UserResponse updateUserProjectAccess(String userId, UpdateUserProjectAccessRequest request) {
        AccessContext accessContext = resolveCurrentAccessContext(false);
        assertAdmin(accessContext);

        if (isBlank(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
 
        TaskManagementUser user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Set<Long> requestedProjectIds = new LinkedHashSet<>();
        if (request != null && request.projectIds() != null) {
            requestedProjectIds.addAll(request.projectIds().stream().filter(projectId -> projectId != null).toList());
        }

        List<TmProject> projects = new ArrayList<>(projectRepository.findAllWithMembers());
        Map<Long, TmProject> projectsById = new LinkedHashMap<>();
        for (TmProject project : projects) {
            projectsById.put(project.getId(), project);
        }

        for (Long projectId : requestedProjectIds) {
            if (!projectsById.containsKey(projectId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
            }
        }

        List<TmProject> changedProjects = new ArrayList<>();
        for (TmProject project : projects) {
            boolean shouldHaveAccess = requestedProjectIds.contains(project.getId());
            boolean hasAccess = hasStoredProjectMembership(project, user);

            if (shouldHaveAccess && !hasAccess) {
                changedProjects.add(project);
                project.getMembers().add(user);
                continue;
            }

            if (!shouldHaveAccess && hasAccess) {
                project.getMembers().removeIf(member -> member.getId().equals(user.getId()));
                changedProjects.add(project);
            }
        }

        for (TmProject changedProject : changedProjects) {
            projectRepository.save(changedProject);
        }

        List<Long> accessibleProjectIds = findAccessibleProjectIds(user.getId());

        return toUserResponse(user, accessibleProjectIds);
    }

    @Transactional
    public void deleteManagedUser(String userId) {
        AccessContext accessContext = resolveCurrentAccessContext(false);
        assertAdmin(accessContext);

        if (isBlank(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        String normalizedUserId = userId.trim();
        TaskManagementUser user = userRepository.findById(normalizedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (accessContext.user() != null && normalizedUserId.equals(accessContext.user().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nemůžeš smazat právě přihlášený účet.");
        }

        if (taskRepository.existsByAnyUserAssignment(normalizedUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Uživatele nelze smazat, protože je navázaný na tasky jako creator, assignee nebo reviewer.");
        }

        if (storyDiscussionMessageRepository.existsByAuthorId(normalizedUserId)
                || taskDiscussionMessageRepository.existsByAuthorId(normalizedUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Uživatele nelze smazat, protože je autorem zpráv v diskusi.");
        }

        List<TmProject> projects = new ArrayList<>(projectRepository.findAllWithMembers());
        List<TmProject> changedProjects = new ArrayList<>();
        for (TmProject project : projects) {
            boolean removed = project.getMembers().removeIf(member -> normalizedUserId.equals(member.getId()));
            if (removed) {
                changedProjects.add(project);
            }
        }

        if (!changedProjects.isEmpty()) {
            projectRepository.saveAll(changedProjects);
        }

        storyDiscussionReadStateRepository.deleteByUserId(normalizedUserId);
        taskDiscussionReadStateRepository.deleteByUserId(normalizedUserId);
        userRepository.delete(user);
        keycloakUserProvisioningService.deleteUserByUsername(user.getUsername());
    }

    private TaskManagementUser updateJwtBackedUser(TaskManagementUser user, String displayName, boolean adminFromJwt) {
        boolean changed = false;
        boolean effectiveAdmin = user.isAdmin() || adminFromJwt;

        if (!displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
            changed = true;
        }

        if (user.isAdmin() != effectiveAdmin) {
            user.setAdmin(effectiveAdmin);
            changed = true;
        }

        return changed ? userRepository.save(user) : user;
    }

    private TaskManagementUser createJwtBackedUser(String username, String displayName, boolean admin) {
        TaskManagementUser user = new TaskManagementUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setAdmin(admin);
        return userRepository.save(user);
    }

    private TaskManagementUser syncJwtBackedUser(Jwt jwt) {
        String username = extractJwtUsername(jwt);
        String displayName = extractJwtDisplayName(jwt, username);
        boolean admin = hasAdminRole(jwt);

        return userRepository.findByUsername(username)
                .map(existingUser -> updateJwtBackedUser(existingUser, displayName, admin))
                .orElseGet(() -> createJwtBackedUser(username, displayName, admin));
    }

    private String extractJwtUsername(Jwt jwt) {
        String username = firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                jwt.getSubject());

        if (isBlank(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JWT token does not contain a usable username");
        }

        return username.trim();
    }

    private String extractJwtDisplayName(Jwt jwt, String fallbackUsername) {
        String displayName = firstNonBlank(
                jwt.getClaimAsString("name"),
                formatFirstAndLastName(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name")),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                fallbackUsername);

        return displayName.trim();
    }

    private String formatFirstAndLastName(String firstName, String lastName) {
        String combinedName = String.format("%s %s",
                firstName == null ? "" : firstName.trim(),
                lastName == null ? "" : lastName.trim()).trim();
        return combinedName.isEmpty() ? null : combinedName;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }

        return null;
    }

    @Transactional
    public void deleteProject(Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId is required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);
        assertAdmin(accessContext);

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        projectRepository.deleteById(projectId);
    }

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request) {
        if (request == null || request.projectId() == null || isBlank(request.name()) || request.startDate() == null || request.endDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint name and dates are required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint end date must be after start date");
        }

        TmProject project = requireProjectAccess(request.projectId(), resolveCurrentAccessContext(false));

        Sprint sprint = new Sprint();
        sprint.setName(request.name().trim());
        sprint.setProject(project);
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());

        return toSprintResponse(sprintRepository.save(sprint));
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprints(Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId is required");
        }

        requireProjectAccess(projectId, resolveCurrentAccessContext(false));

        return sprintRepository.findByProjectIdOrderByStartDateAscIdAsc(projectId)
                .stream()
                .map(this::toSprintResponse)
                .toList();
    }

    @Transactional
    public StoryResponse createStory(CreateStoryRequest request) {
        if (request == null || request.projectId() == null || isBlank(request.title()) || isBlank(request.description())
                || request.difficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user story payload");
        }
        if (!ALLOWED_STORY_POINTS.contains(request.difficulty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty must be one of 5,10,20,50,100");
        }

        TmProject project = requireProjectAccess(request.projectId(), resolveCurrentAccessContext(false));

        Sprint sprint = null;
        if (request.sprintId() != null) {
            sprint = sprintRepository.findById(request.sprintId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
            if (!sprint.getProject().getId().equals(project.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint does not belong to selected project");
            }
        }

        UserStory story = new UserStory();
        story.setProject(project);
        story.setStoryNumber(nextStoryNumber(project.getId()));
        story.setSprint(sprint);
        story.setTitle(request.title().trim());
        story.setDescription(request.description().trim());
        story.setDifficulty(request.difficulty());
        story.setColorHex(isBlank(request.colorHex()) ? "#FFF8DC" : request.colorHex().trim());

        UserStory saved = storyRepository.save(story);
        return toStoryResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesBySprint(Long sprintId) {
        if (sprintId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sprintId is required");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        requireProjectAccess(sprint.getProject().getId(), resolveCurrentAccessContext(false));

        List<UserStory> stories = storyRepository.findBySprintIdOrderByStoryNumberAsc(sprintId);
        List<TaskItem> tasks = taskRepository.findByStorySprintIdOrderByTaskNumberAsc(sprintId);
        return stories.stream()
                .filter(story -> story.getProject().getId().equals(sprint.getProject().getId()))
                .map(s -> toStoryResponse(s, tasks))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getProductBacklog(Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId is required");
        }
        requireProjectAccess(projectId, resolveCurrentAccessContext(false));

        List<TaskItem> backlogTasks = taskRepository.findByBacklogStoryProjectIdOrderByTaskNumberAsc(projectId);

        return storyRepository.findByProjectIdAndSprintIsNullOrderByStoryNumberAsc(projectId)
                .stream()
                .map(story -> toStoryResponse(story, backlogTasks))
                .toList();
    }

    @Transactional
    public StoryResponse updateStory(Long storyId, UpdateStoryRequest request) {
        if (storyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storyId is required");
        }

        if (request == null || isBlank(request.title()) || isBlank(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Story title and description are required");
        }

        UserStory story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        requireProjectAccess(story.getProject().getId(), resolveCurrentAccessContext(false));

        story.setTitle(request.title().trim());
        story.setDescription(request.description().trim());
        UserStory saved = storyRepository.save(story);

        List<TaskItem> sprintTasks = saved.getSprint() == null
            ? taskRepository.findByStoryIdOrderByTaskNumberAsc(saved.getId())
            : taskRepository.findByStorySprintIdOrderByTaskNumberAsc(saved.getSprint().getId());

        return toStoryResponse(saved, sprintTasks);
    }

    @Transactional
    public StoryResponse assignStoryToSprint(Long storyId, AssignStoryToSprintRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment payload is required");
        }

        UserStory story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        requireProjectAccess(story.getProject().getId(), resolveCurrentAccessContext(false));

        Sprint targetSprint = null;
        if (request.sprintId() != null) {
            targetSprint = sprintRepository.findById(request.sprintId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));

            if (!targetSprint.getProject().getId().equals(story.getProject().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Story and sprint must belong to same project");
            }
        }

        story.setSprint(targetSprint);
        UserStory saved = storyRepository.save(story);

        List<TaskItem> sprintTasks = saved.getSprint() == null
            ? taskRepository.findByStoryIdOrderByTaskNumberAsc(saved.getId())
            : taskRepository.findByStorySprintIdOrderByTaskNumberAsc(saved.getSprint().getId());
        return toStoryResponse(saved, sprintTasks);
    }

    @Transactional
    public void deleteStory(Long storyId) {
        if (storyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storyId is required");
        }

        UserStory story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        requireProjectAccess(story.getProject().getId(), resolveCurrentAccessContext(false));

        if (story.getSprint() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Story is in sprint. Move it back to product backlog first.");
        }

        if (taskRepository.existsByStoryId(storyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Story already has tasks and cannot be deleted.");
        }

        storyRepository.delete(story);
    }

    @Transactional(readOnly = true)
    public List<Long> getUnreadStoryDiscussionIds(Long projectId, String userId) {
        if (projectId == null || isBlank(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId and userId are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);
        requireProjectAccess(projectId, accessContext);
        requireUserActionAccess(userId, accessContext);

        return findUnreadStoryDiscussionIds(projectId, userId.trim());
    }

    private List<Long> findUnreadStoryDiscussionIds(Long projectId, String userId) {
        String normalizedUserId = userId.trim();

        Map<Long, LocalDateTime> latestForeignMessageByStoryId = new LinkedHashMap<>();
        for (StoryDiscussionMessage message : storyDiscussionMessageRepository
                .findByStoryProjectIdAndAuthorIdNotOrderByCreatedAtDesc(projectId, normalizedUserId)) {
            latestForeignMessageByStoryId.putIfAbsent(message.getStory().getId(), message.getCreatedAt());
        }

        Map<Long, LocalDateTime> lastReadByStoryId = new LinkedHashMap<>();
        for (StoryDiscussionReadState readState : storyDiscussionReadStateRepository
                .findByUserIdAndStoryProjectId(normalizedUserId, projectId)) {
            lastReadByStoryId.put(readState.getStory().getId(), readState.getLastReadAt());
        }

        List<Long> unreadStoryIds = new ArrayList<>();
        for (UserStory story : storyRepository.findByProjectIdOrderByStoryNumberAsc(projectId)) {
            LocalDateTime latestForeignMessageAt = latestForeignMessageByStoryId.get(story.getId());
            if (latestForeignMessageAt == null) {
                continue;
            }

            LocalDateTime lastReadAt = lastReadByStoryId.get(story.getId());
            if (lastReadAt == null || latestForeignMessageAt.isAfter(lastReadAt)) {
                unreadStoryIds.add(story.getId());
            }
        }

        return unreadStoryIds;
    }

    @Transactional(readOnly = true)
    public List<StoryDiscussionMessageResponse> getStoryDiscussionMessages(Long storyId) {
        UserStory story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        requireProjectAccess(story.getProject().getId(), resolveCurrentAccessContext(false));

        List<StoryDiscussionMessage> messages = storyDiscussionMessageRepository.findByStoryIdOrderByCreatedAtAscIdAsc(story.getId());
        Map<Long, List<StoryDiscussionMessage>> childrenByParentId = new LinkedHashMap<>();
        List<StoryDiscussionMessage> rootMessages = new ArrayList<>();

        for (StoryDiscussionMessage message : messages) {
            Long parentId = message.getParentMessage() == null ? null : message.getParentMessage().getId();
            if (parentId == null) {
                rootMessages.add(message);
                continue;
            }

            childrenByParentId.computeIfAbsent(parentId, key -> new ArrayList<>()).add(message);
        }

        return rootMessages.stream()
                .map(message -> toStoryDiscussionMessageResponse(message, childrenByParentId))
                .toList();
    }

    @Transactional
    public StoryDiscussionMessageResponse createStoryDiscussionMessage(Long storyId, CreateStoryDiscussionMessageRequest request) {
        if (storyId == null || request == null || isBlank(request.authorUserId()) || isBlank(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storyId, authorUserId and content are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);

        UserStory story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        requireProjectAccess(story.getProject().getId(), accessContext);

        TaskManagementUser author = requireProjectUser(story.getProject(), request.authorUserId(), "Author");
        requireUserActionAccess(author.getId(), accessContext);

        StoryDiscussionMessage parentMessage = null;
        if (request.parentMessageId() != null) {
            parentMessage = storyDiscussionMessageRepository.findById(request.parentMessageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent message not found"));
            if (!parentMessage.getStory().getId().equals(story.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent message does not belong to selected story");
            }
        }

        StoryDiscussionMessage message = new StoryDiscussionMessage();
        message.setStory(story);
        message.setAuthor(author);
        message.setParentMessage(parentMessage);
        message.setContent(request.content().trim());

        StoryDiscussionMessage saved = storyDiscussionMessageRepository.save(message);
        saveStoryDiscussionReadState(story, author);
        StoryDiscussionMessageResponse response = toStoryDiscussionMessageResponse(saved, Map.of());
        List<Long> unreadStoryIds = findUnreadStoryDiscussionIds(story.getProject().getId(), author.getId());
        broadcastStoryDiscussionEvent(story, DISCUSSION_EVENT_MESSAGE_CREATED, author.getId(), unreadStoryIds);
        return response;
    }

    @Transactional
    public void markStoryDiscussionRead(Long storyId, MarkStoryDiscussionReadRequest request) {
        if (storyId == null || request == null || isBlank(request.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storyId and userId are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);

        UserStory story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        requireProjectAccess(story.getProject().getId(), accessContext);

        requireUserActionAccess(request.userId(), accessContext);

        TaskManagementUser user = userRepository.findById(request.userId().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        saveStoryDiscussionReadState(story, user);
        List<Long> unreadStoryIds = findUnreadStoryDiscussionIds(story.getProject().getId(), user.getId());
        broadcastStoryDiscussionEvent(story, DISCUSSION_EVENT_READ_STATE_UPDATED, user.getId(), unreadStoryIds);
    }

    @Transactional(readOnly = true)
    public List<Long> getUnreadTaskDiscussionIds(Long projectId, String userId) {
        if (projectId == null || isBlank(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId and userId are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);
        requireProjectAccess(projectId, accessContext);
        requireUserActionAccess(userId, accessContext);

        return findUnreadTaskDiscussionIds(projectId, userId.trim());
    }

    private List<Long> findUnreadTaskDiscussionIds(Long projectId, String userId) {
        String normalizedUserId = userId.trim();

        Map<Long, LocalDateTime> latestForeignMessageByTaskId = new LinkedHashMap<>();
        for (TaskDiscussionMessage message : taskDiscussionMessageRepository
                .findByTaskStoryProjectIdAndAuthorIdNotOrderByCreatedAtDesc(projectId, normalizedUserId)) {
            latestForeignMessageByTaskId.putIfAbsent(message.getTask().getId(), message.getCreatedAt());
        }

        Map<Long, LocalDateTime> lastReadByTaskId = new LinkedHashMap<>();
        for (TaskDiscussionReadState readState : taskDiscussionReadStateRepository
                .findByUserIdAndTaskStoryProjectId(normalizedUserId, projectId)) {
            lastReadByTaskId.put(readState.getTask().getId(), readState.getLastReadAt());
        }

        List<Long> unreadTaskIds = new ArrayList<>();
        for (Map.Entry<Long, LocalDateTime> entry : latestForeignMessageByTaskId.entrySet()) {
            Long taskId = entry.getKey();
            LocalDateTime latestMessageAt = entry.getValue();
            LocalDateTime lastReadAt = lastReadByTaskId.get(taskId);

            if (lastReadAt == null || latestMessageAt.isAfter(lastReadAt)) {
                unreadTaskIds.add(taskId);
            }
        }
        return unreadTaskIds;
    }

    @Transactional(readOnly = true)
    public List<TaskDiscussionMessageResponse> getTaskDiscussionMessages(Long taskId) {
        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProjectAccess(task.getStory().getProject().getId(), resolveCurrentAccessContext(false));

        List<TaskDiscussionMessage> messages = taskDiscussionMessageRepository.findByTaskIdOrderByCreatedAtAscIdAsc(task.getId());
        Map<Long, List<TaskDiscussionMessage>> childrenByParentId = new LinkedHashMap<>();
        List<TaskDiscussionMessage> rootMessages = new ArrayList<>();

        for (TaskDiscussionMessage message : messages) {
            Long parentId = message.getParentMessage() == null ? null : message.getParentMessage().getId();
            if (parentId == null) {
                rootMessages.add(message);
                continue;
            }
            childrenByParentId.computeIfAbsent(parentId, k -> new ArrayList<>()).add(message);
        }

        return rootMessages.stream()
                .map(message -> toTaskDiscussionMessageResponse(message, childrenByParentId))
                .toList();
    }

    @Transactional
    public TaskDiscussionMessageResponse createTaskDiscussionMessage(Long taskId, CreateTaskDiscussionMessageRequest request) {
        if (taskId == null || request == null || isBlank(request.authorUserId()) || isBlank(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId, authorUserId and content are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);

        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProjectAccess(task.getStory().getProject().getId(), accessContext);

        TaskManagementUser author = requireProjectUser(task.getStory().getProject(), request.authorUserId(), "Author");
        requireUserActionAccess(author.getId(), accessContext);

        TaskDiscussionMessage parentMessage = null;
        if (request.parentMessageId() != null) {
            parentMessage = taskDiscussionMessageRepository.findById(request.parentMessageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent message not found"));
            if (!parentMessage.getTask().getId().equals(task.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent message does not belong to selected task");
            }
        }

        TaskDiscussionMessage message = new TaskDiscussionMessage();
        message.setTask(task);
        message.setAuthor(author);
        message.setParentMessage(parentMessage);
        message.setContent(request.content().trim());

        TaskDiscussionMessage saved = taskDiscussionMessageRepository.save(message);
        saveTaskDiscussionReadState(task, author);
        TaskDiscussionMessageResponse response = toTaskDiscussionMessageResponse(saved, Map.of());
        List<Long> unreadTaskIds = findUnreadTaskDiscussionIds(task.getStory().getProject().getId(), author.getId());
        broadcastTaskDiscussionEvent(task, DISCUSSION_EVENT_MESSAGE_CREATED, author.getId(), unreadTaskIds);
        return response;
    }

    @Transactional
    public void markTaskDiscussionRead(Long taskId, MarkTaskDiscussionReadRequest request) {
        if (taskId == null || request == null || isBlank(request.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId and userId are required");
        }

        AccessContext accessContext = resolveCurrentAccessContext(false);

        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProjectAccess(task.getStory().getProject().getId(), accessContext);

        requireUserActionAccess(request.userId(), accessContext);

        TaskManagementUser user = userRepository.findById(request.userId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        saveTaskDiscussionReadState(task, user);
        List<Long> unreadTaskIds = findUnreadTaskDiscussionIds(task.getStory().getProject().getId(), user.getId());
        broadcastTaskDiscussionEvent(task, DISCUSSION_EVENT_READ_STATE_UPDATED, user.getId(), unreadTaskIds);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        if (request == null
                || request.storyId() == null
                || isBlank(request.title())
                || isBlank(request.description())
                || isBlank(request.definitionOfDone())
                || isBlank(request.creatorUserId())
                || isBlank(request.assigneeUserId())
                || isBlank(request.reviewerUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task payload");
        }

            AccessContext accessContext = resolveCurrentAccessContext(false);

        UserStory story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
            TmProject project = requireProjectAccess(story.getProject().getId(), accessContext);

        if (story.getSprint() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create task for backlog story. Assign story to sprint first.");
        }

            TaskManagementUser creator = requireProjectUser(project, request.creatorUserId(), "Creator");
            requireUserActionAccess(creator.getId(), accessContext);

            TaskManagementUser assignee = requireProjectUser(project, request.assigneeUserId(), "Assignee");

            TaskManagementUser reviewer = requireProjectUser(project, request.reviewerUserId(), "Reviewer");

        TaskItem task = new TaskItem();
        task.setStory(story);
        int nextTaskNumber = nextTaskNumber(story.getId());
        task.setTaskNumber(nextTaskNumber);
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setDefinitionOfDone(request.definitionOfDone().trim());
        task.setColorHex(isBlank(request.colorHex()) ? "#FFF8DC" : request.colorHex().trim());
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setReviewer(reviewer);
        task.setStatus(TaskStatus.TODO);
        task.setTaskKey(generateTaskKey(nextTaskNumber));

        TaskItem saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task status is required");
        }

        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        AccessContext accessContext = resolveCurrentAccessContext(false);
        requireProjectAccess(task.getStory().getProject().getId(), accessContext);
        requireTaskStateChangeAccess(task, accessContext);

        task.setStatus(request.status());
        task.setReviewComment(request.reviewComment());
        if (request.status() == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
        return toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request) {
        if (taskId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId is required");
        }

        if (request == null
                || isBlank(request.title())
                || isBlank(request.description())
                || isBlank(request.definitionOfDone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task title, description and definition of done are required");
        }

        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProjectAccess(task.getStory().getProject().getId(), resolveCurrentAccessContext(false));

        if (task.getStatus() != TaskStatus.TODO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task can be edited only in TODO state");
        }

        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setDefinitionOfDone(request.definitionOfDone().trim());

        return toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse returnTaskToCreator(Long taskId, ReturnTaskRequest request) {
        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        AccessContext accessContext = resolveCurrentAccessContext(false);
        requireProjectAccess(task.getStory().getProject().getId(), accessContext);
        requireTaskStateChangeAccess(task, accessContext);

        // When returning a task to the creator, mark it as back in progress
        // so it appears in the 'Rozpracované' column in the UI.
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setAssignee(task.getCreator());
        task.setReviewComment(request == null ? null : request.reviewComment());
        task.setCompletedAt(null);

        return toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public CodeReferenceResponse addCodeReference(Long taskId, AddCodeReferenceRequest request) {
        if (request == null || isBlank(request.commitHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commit hash is required");
        }

        TaskItem task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProjectAccess(task.getStory().getProject().getId(), resolveCurrentAccessContext(false));

        TaskCodeReference ref = new TaskCodeReference();
        ref.setTask(task);
        ref.setCommitHash(request.commitHash().trim());
        ref.setRepositoryUrl(isBlank(request.repositoryUrl()) ? null : request.repositoryUrl().trim());
        ref.setNote(isBlank(request.note()) ? null : request.note().trim());

        return toCodeReferenceResponse(codeReferenceRepository.save(ref));
    }

    @Transactional(readOnly = true)
    public List<CodeReferenceResponse> getCodeReferences(Long taskId) {
        TaskItem task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        requireProjectAccess(task.getStory().getProject().getId(), resolveCurrentAccessContext(false));

        return codeReferenceRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(this::toCodeReferenceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SprintDashboardResponse getSprintDashboard(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        requireProjectAccess(sprint.getProject().getId(), resolveCurrentAccessContext(false));

        List<UserStory> stories = storyRepository.findBySprintIdOrderByStoryNumberAsc(sprintId);
        List<TaskItem> tasks = taskRepository.findByStorySprintIdOrderByTaskNumberAsc(sprintId);

        int velocity = stories.stream()
            .filter(story -> {
                List<TaskItem> storyTasks = tasks.stream()
                    .filter(task -> task.getStory().getId().equals(story.getId()))
                    .toList();
                return !storyTasks.isEmpty() && storyTasks.stream().allMatch(task -> task.getStatus() == TaskStatus.DONE);
            })
                .mapToInt(UserStory::getDifficulty)
                .sum();

        int totalStoryPoints = stories.stream()
            .mapToInt(UserStory::getDifficulty)
            .sum();

        List<StoryCompletionInfo> completedStories = stories.stream()
            .map(story -> {
                List<TaskItem> storyTasks = tasks.stream()
                    .filter(task -> task.getStory().getId().equals(story.getId()))
                    .toList();

                boolean isCompleted = !storyTasks.isEmpty()
                    && storyTasks.stream().allMatch(task -> task.getStatus() == TaskStatus.DONE);
                if (!isCompleted) {
                return null;
                }

                LocalDate completionDate = storyTasks.stream()
                    .map(TaskItem::getCompletedAt)
                    .filter(completedAt -> completedAt != null)
                    .map(LocalDateTime::toLocalDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

                if (completionDate == null) {
                return null;
                }

                String rawStoryTitle = story.getTitle() == null ? "" : story.getTitle().trim();
                String sanitizedStoryTitle = rawStoryTitle.replaceFirst("^#\\d+\\s*", "");

                return new StoryCompletionInfo(story.getDifficulty(), completionDate, story.getStoryNumber(), sanitizedStoryTitle);
            })
            .filter(item -> item != null)
            .toList();

        LocalDate today = LocalDate.now();
        LocalDate endPoint = today.isBefore(sprint.getEndDate()) ? today : sprint.getEndDate();

        List<BurnoutPoint> burnout;
        if (endPoint.isBefore(sprint.getStartDate())) {
            burnout = List.of();
        } else {
            burnout = sprint.getStartDate().datesUntil(endPoint.plusDays(1))
                    .map(day -> {
                long completed = completedStories.stream()
                    .filter(story -> !story.completedAt().isAfter(day))
                    .mapToLong(StoryCompletionInfo::storyPoints)
                    .sum();
                long remaining = Math.max(totalStoryPoints - completed, 0);

                List<String> completedStoryTitles = completedStories.stream()
                    .filter(story -> story.completedAt().isEqual(day))
                    .map(story -> "#" + story.storyNumber() + " " + story.storyTitle())
                    .toList();

                        return new BurnoutPoint(day, completed, remaining, completedStoryTitles);
                    })
                    .toList();
        }

        return new SprintDashboardResponse(
                toSprintResponse(sprint),
                stories.stream().map(story -> toStoryResponse(story, tasks)).toList(),
                tasks.stream().map(this::toTaskResponse).toList(),
                velocity,
                burnout);
    }

    private record StoryCompletionInfo(int storyPoints, LocalDate completedAt, Integer storyNumber, String storyTitle) {
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AccessContext resolveCurrentAccessContext(boolean syncCurrentUser) {
        Jwt jwt = extractCurrentJwt();
        String username = extractJwtUsername(jwt);
        TaskManagementUser storedUser = userRepository.findByUsername(username).orElse(null);
        TaskManagementUser user = syncCurrentUser
                ? syncJwtBackedUser(jwt)
            : storedUser;
        boolean admin = hasAdminRole(jwt) || (user != null && user.isAdmin());
        return new AccessContext(user, username, admin);
    }

    private Jwt extractCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token is required");
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }

        if (authentication.getPrincipal() instanceof Jwt jwtPrincipal) {
            return jwtPrincipal;
        }

        if (authentication.getCredentials() instanceof Jwt jwtCredentials) {
            return jwtCredentials;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token is required");
    }

    private boolean hasAdminRole(Jwt jwt) {
        Set<String> normalizedRoles = new LinkedHashSet<>();
        collectRoleValuesFromClaim(jwt.getClaim("realm_access"), normalizedRoles);

        Object resourceAccessClaim = jwt.getClaim("resource_access");
        if (resourceAccessClaim instanceof Map<?, ?> resourceAccess) {
            for (Object clientAccess : resourceAccess.values()) {
                collectRoleValuesFromClaim(clientAccess, normalizedRoles);
            }
        }

        return normalizedRoles.stream().anyMatch(ADMIN_ROLE_NAMES::contains);
    }

    private void collectRoleValuesFromClaim(Object claimValue, Set<String> roles) {
        if (!(claimValue instanceof Map<?, ?> claimMap)) {
            return;
        }

        Object rawRoles = claimMap.get("roles");
        if (!(rawRoles instanceof Iterable<?> iterableRoles)) {
            return;
        }

        for (Object rawRole : iterableRoles) {
            if (rawRole instanceof String role && !role.isBlank()) {
                roles.add(role.trim().toLowerCase());
            }
        }
    }

    private TmProject requireProjectAccess(Long projectId, AccessContext accessContext) {
        TmProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (accessContext.admin()) {
            return project;
        }

        if (accessContext.user() == null || !projectRepository.existsByIdAndMembersId(projectId, accessContext.user().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemáš oprávnění vstoupit do vybraného projektu.");
        }

        return project;
    }

    private void assertAdmin(AccessContext accessContext) {
        if (!accessContext.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Jen admin může spravovat přístupy k projektům.");
        }
    }

    private void requireUserActionAccess(String requestedUserId, AccessContext accessContext) {
        String normalizedUserId = requestedUserId == null ? null : requestedUserId.trim();
        if (isBlank(normalizedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        if (accessContext.admin()) {
            if (!userRepository.existsById(normalizedUserId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            return;
        }

        if (accessContext.user() == null || !accessContext.user().getId().equals(normalizedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemáš oprávnění pracovat jménem jiného uživatele.");
        }
    }

    private TaskManagementUser requireProjectUser(TmProject project, String userId, String roleLabel) {
        String normalizedUserId = userId == null ? null : userId.trim();
        if (isBlank(normalizedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, roleLabel + " is required");
        }

        TaskManagementUser user = userRepository.findById(normalizedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, roleLabel + " not found"));

        if (!hasEffectiveProjectAccess(project, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, roleLabel + " does not have access to selected project");
        }

        return user;
    }

    private void requireTaskStateChangeAccess(TaskItem task, AccessContext accessContext) {
        if (accessContext == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemáš oprávnění změnit stav tasku.");
        }

        if (accessContext.admin()) {
            return;
        }

        TaskManagementUser current = accessContext.user();
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemáš oprávnění změnit stav tasku.");
        }

        String uid = current.getId();
        boolean allowed = uid.equals(task.getAssignee().getId())
                || uid.equals(task.getCreator().getId())
                || uid.equals(task.getReviewer().getId());

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemáš oprávnění změnit stav tasku.");
        }
    }

    private boolean hasEffectiveProjectAccess(TmProject project, TaskManagementUser user) {
        if (user.isAdmin()) {
            return true;
        }

        return hasStoredProjectMembership(project, user);
    }

    private boolean hasStoredProjectMembership(TmProject project, TaskManagementUser user) {
        return project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    private Map<String, List<Long>> buildAccessibleProjectIdsByUser(List<TmProject> projects) {
        Map<String, List<Long>> accessibleProjectIdsByUserId = new LinkedHashMap<>();
        for (TmProject project : projects) {
            for (TaskManagementUser member : project.getMembers()) {
                accessibleProjectIdsByUserId
                        .computeIfAbsent(member.getId(), ignored -> new ArrayList<>())
                        .add(project.getId());
            }
        }

        return accessibleProjectIdsByUserId;
    }

    private List<Long> findAccessibleProjectIds(String userId) {
        return projectRepository.findDistinctByMembersIdOrderByNameAscIdAsc(userId).stream()
                .map(TmProject::getId)
                .toList();
    }

    private int nextStoryNumber(Long projectId) {
        return storyRepository.findMaxStoryNumberByProjectId(projectId) + 1;
    }

    private int nextTaskNumber(Long storyId) {
        return taskRepository.findMaxTaskNumberByStoryId(storyId) + 1;
    }

    private String generateTaskKey(int taskNumber) {
        return "task#" + taskNumber;
    }

    private UserResponse toUserResponse(TaskManagementUser user, List<Long> accessibleProjectIds) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isAdmin(),
                accessibleProjectIds == null ? List.of() : accessibleProjectIds);
    }

    private SprintResponse toSprintResponse(Sprint sprint) {
        return new SprintResponse(sprint.getId(), sprint.getProject().getId(), sprint.getName(), sprint.getStartDate(), sprint.getEndDate());
    }

    private StoryResponse toStoryResponse(UserStory story, List<TaskItem> sprintTasks) {
        String rawTitle = story.getTitle() == null ? "" : story.getTitle().trim();
        String sanitizedTitle = rawTitle.replaceFirst("^#\\d+\\s*", "");

        List<TaskItem> storyTasks = sprintTasks == null ? List.of()
                : sprintTasks.stream().filter(t -> t.getStory().getId().equals(story.getId())).toList();

        int taskCount = storyTasks.size();
        int completedTaskCount = (int) storyTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        boolean isCompleted = !storyTasks.isEmpty() && storyTasks.stream().allMatch(t -> t.getStatus() == TaskStatus.DONE);

        String status;
        if (storyTasks.isEmpty()) {
            status = "not yet started";
        } else if (isCompleted) {
            status = "completed";
        } else {
            boolean allTodo = storyTasks.stream().allMatch(t -> t.getStatus() == TaskStatus.TODO);
            status = allTodo ? "not yet started" : "in progress";
        }

        return new StoryResponse(
                story.getId(),
            story.getStoryNumber(),
                story.getProject().getId(),
                story.getSprint() == null ? null : story.getSprint().getId(),
                sanitizedTitle,
                story.getDescription(),
                story.getDifficulty(),
                isCompleted,
                status,
                taskCount,
                completedTaskCount,
                story.getColorHex() != null ? story.getColorHex() : "#FFF8DC");
    }

    private ProjectResponse toProjectResponse(TmProject project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription());
    }

    private TaskResponse toTaskResponse(TaskItem task) {
        String rawTitle = task.getTitle() == null ? "" : task.getTitle().trim();
        String sanitizedTitle = rawTitle;
        if (task.getTaskKey() != null && !task.getTaskKey().isBlank()) {
            sanitizedTitle = sanitizedTitle.replace(task.getTaskKey(), "").trim();
        }
        return new TaskResponse(
                task.getId(),
            task.getTaskNumber(),
                task.getTaskKey(),
                task.getStory().getId(),
                sanitizedTitle,
                task.getDescription(),
                task.getDefinitionOfDone(),
                task.getColorHex(),
                task.getStatus(),
                task.getCreator().getId(),
                task.getCreator().getDisplayName(),
                task.getAssignee().getId(),
                task.getAssignee().getDisplayName(),
                task.getReviewer().getId(),
                task.getReviewer().getDisplayName(),
                task.getReviewComment(),
                task.getCompletedAt());
    }

    private CodeReferenceResponse toCodeReferenceResponse(TaskCodeReference reference) {
        return new CodeReferenceResponse(
                reference.getId(),
                reference.getTask().getId(),
                reference.getCommitHash(),
                reference.getRepositoryUrl(),
                reference.getNote(),
                reference.getCreatedAt());
    }

            private StoryDiscussionMessageResponse toStoryDiscussionMessageResponse(
                StoryDiscussionMessage message,
                Map<Long, List<StoryDiscussionMessage>> childrenByParentId) {
            List<StoryDiscussionMessageResponse> replies = childrenByParentId
                .getOrDefault(message.getId(), List.of())
                .stream()
                .map(child -> toStoryDiscussionMessageResponse(child, childrenByParentId))
                .toList();

            return new StoryDiscussionMessageResponse(
                message.getId(),
                message.getStory().getId(),
                message.getParentMessage() == null ? null : message.getParentMessage().getId(),
                message.getAuthor().getId(),
                message.getAuthor().getDisplayName(),
                message.getContent(),
                message.getCreatedAt(),
                replies);
            }

            private void saveStoryDiscussionReadState(UserStory story, TaskManagementUser user) {
            StoryDiscussionReadState readState = storyDiscussionReadStateRepository
                .findByStoryIdAndUserId(story.getId(), user.getId())
                .orElseGet(StoryDiscussionReadState::new);

            readState.setStory(story);
            readState.setUser(user);
            readState.setLastReadAt(LocalDateTime.now());
            storyDiscussionReadStateRepository.save(readState);
            }

                private void broadcastStoryDiscussionEvent(UserStory story, String eventType, String actorUserId, List<Long> unreadStoryIds) {
            Long projectId = story.getProject().getId();
            messagingTemplate.convertAndSend(
                "/topic/task-management.projects." + projectId + ".discussion",
                new StoryDiscussionRealtimeEvent(
                    eventType,
                    projectId,
                    story.getId(),
                    actorUserId,
                    unreadStoryIds,
                    LocalDateTime.now()));
            }

    private TaskDiscussionMessageResponse toTaskDiscussionMessageResponse(
            TaskDiscussionMessage message,
            Map<Long, List<TaskDiscussionMessage>> childrenByParentId) {
        List<TaskDiscussionMessageResponse> replies = childrenByParentId
                .getOrDefault(message.getId(), List.of())
                .stream()
                .map(child -> toTaskDiscussionMessageResponse(child, childrenByParentId))
                .toList();

        return new TaskDiscussionMessageResponse(
                message.getId(),
                message.getTask().getId(),
                message.getParentMessage() == null ? null : message.getParentMessage().getId(),
                message.getAuthor().getId(),
                message.getAuthor().getDisplayName(),
                message.getContent(),
                message.getCreatedAt(),
                replies);
    }

    private void saveTaskDiscussionReadState(TaskItem task, TaskManagementUser user) {
        TaskDiscussionReadState readState = taskDiscussionReadStateRepository
                .findByTaskIdAndUserId(task.getId(), user.getId())
                .orElseGet(TaskDiscussionReadState::new);

        readState.setTask(task);
        readState.setUser(user);
        readState.setLastReadAt(LocalDateTime.now());
        taskDiscussionReadStateRepository.save(readState);
    }

    private void broadcastTaskDiscussionEvent(TaskItem task, String eventType, String actorUserId, List<Long> unreadTaskIds) {
        Long projectId = task.getStory().getProject().getId();
        messagingTemplate.convertAndSend(
                "/topic/task-management.projects." + projectId + ".task-discussion",
                new TaskDiscussionRealtimeEvent(
                        eventType,
                        projectId,
                        task.getId(),
                        actorUserId,
                        unreadTaskIds,
                        LocalDateTime.now()));
    }

    private record AccessContext(TaskManagementUser user, String username, boolean admin) {
    }
}
