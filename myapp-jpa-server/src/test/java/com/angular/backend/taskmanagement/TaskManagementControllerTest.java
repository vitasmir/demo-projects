package com.angular.backend.taskmanagement;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.angular.backend.AbstractIntegrationTest;
import com.angular.backend.taskmanagement.TaskManagementDtos.StoryDiscussionRealtimeEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskManagementControllerTest extends AbstractIntegrationTest {

        @MockitoBean
        private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskCodeReferenceRepository codeReferenceRepository;

        @Autowired
        private StoryDiscussionMessageRepository storyDiscussionMessageRepository;

        @Autowired
        private StoryDiscussionReadStateRepository storyDiscussionReadStateRepository;

    @Autowired
    private TaskItemRepository taskRepository;

    @Autowired
    private UserStoryRepository storyRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TmProjectRepository projectRepository;

    @Autowired
    private TaskManagementUserRepository userRepository;

    @AfterEach
    void tearDown() {
                reset(messagingTemplate);
                storyDiscussionReadStateRepository.deleteAll();
                storyDiscussionMessageRepository.deleteAll();
        codeReferenceRepository.deleteAll();
        taskRepository.deleteAll();
        storyRepository.deleteAll();
        sprintRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

        @Test
        void backlog_shouldRequireAuthentication() throws Exception {
                mockMvc.perform(get("/api/task-management/stories/backlog").param("projectId", "1"))
                                .andExpect(status().isUnauthorized());
        }

            @Test
            void adminOverview_shouldRequireAdminRole() throws Exception {
                userRepository.save(createUser("worker", "Worker User"));

                mockMvc.perform(get("/api/task-management/admin/project-access")
                        .with(bearerToken("worker", "worker123")))
                        .andExpect(status().isForbidden());
            }

            @Test
            void createProject_shouldAllowAuthenticatedUser() throws Exception {
                userRepository.save(createUser("worker", "Worker User"));

                mockMvc.perform(post("/api/task-management/projects")
                        .with(bearerToken("worker", "worker123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Projekt bez admina",
                                "description", "Popis"))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value("Projekt bez admina"));
            }

            @Test
            void deleteProject_shouldRequireAdminRole() throws Exception {
                userRepository.save(createUser("worker", "Worker User"));
                long projectId = createProject("Projekt pouze pro admina");

                mockMvc.perform(delete("/api/task-management/projects/{projectId}", projectId)
                        .with(bearerToken("worker", "worker123")))
                        .andExpect(status().isForbidden());
            }

            @Test
            void projects_shouldBeFilteredByAssignedAccess() throws Exception {
                TaskManagementUser worker = userRepository.save(createUser("worker", "Worker User"));

                createProject("Projekt Alpha");
                long betaProjectId = createProject("Projekt Beta");

                mockMvc.perform(put("/api/task-management/admin/project-access/users/{userId}", worker.getId())
                        .with(bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("projectIds", java.util.List.of(betaProjectId)))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessibleProjectIds", hasSize(1)))
                        .andExpect(jsonPath("$.accessibleProjectIds[0]", is((int) betaProjectId)));

                mockMvc.perform(get("/api/task-management/projects")
                        .with(bearerToken("worker", "worker123")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(1)))
                        .andExpect(jsonPath("$[0].id", is((int) betaProjectId)))
                        .andExpect(jsonPath("$[0].name", is("Projekt Beta")));

                mockMvc.perform(get("/api/task-management/projects")
                        .with(bearerToken()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(2)));
            }

            @Test
                        void projectMembershipSelectionShouldPersistInOverview() throws Exception {
                TaskManagementUser manager = userRepository.save(createUser("manager", "Manager User"));

                long projectId = createProject("Projekt 1");

                mockMvc.perform(put("/api/task-management/admin/project-access/users/{userId}", manager.getId())
                        .with(bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("projectIds", java.util.List.of(projectId)))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessibleProjectIds", hasSize(1)))
                        .andExpect(jsonPath("$.accessibleProjectIds[0]", is((int) projectId)));

                org.junit.jupiter.api.Assertions.assertTrue(
                        projectRepository.existsByIdAndMembersId(projectId, manager.getId()));

                MvcResult overviewResult = mockMvc.perform(get("/api/task-management/admin/project-access")
                        .with(bearerToken()))
                        .andExpect(status().isOk())
                        .andReturn();

                JsonNode overviewUsers = objectMapper.readTree(overviewResult.getResponse().getContentAsString()).get("users");
                JsonNode managerOverview = java.util.stream.StreamSupport.stream(overviewUsers.spliterator(), false)
                        .filter(userNode -> "manager".equals(userNode.path("username").asText()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Manager user missing from project access overview"));

                org.junit.jupiter.api.Assertions.assertEquals(1, managerOverview.path("accessibleProjectIds").size());
                org.junit.jupiter.api.Assertions.assertEquals(projectId, managerOverview.path("accessibleProjectIds").get(0).asLong());
            }

            @Test
            void users_shouldBeScopedToSelectedProject() throws Exception {
                TaskManagementUser worker = userRepository.save(createUser("worker", "Worker User"));
                TaskManagementUser outsider = userRepository.save(createUser("outsider", "Outsider User"));

                long alphaProjectId = createProject("Projekt Alpha");
                long betaProjectId = createProject("Projekt Beta");

                grantProjectAccess(alphaProjectId, worker.getId());
                grantProjectAccess(betaProjectId, outsider.getId());

                mockMvc.perform(get("/api/task-management/users")
                        .with(bearerToken())
                        .param("projectId", String.valueOf(alphaProjectId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].username", containsInAnyOrder("demo", "worker")));

                mockMvc.perform(get("/api/task-management/users")
                        .with(bearerToken("worker", "worker123"))
                        .param("projectId", String.valueOf(alphaProjectId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].username", containsInAnyOrder("demo", "worker")));
            }

            @Test
            void anonymousUser_shouldBeAbleToRegisterAccountInKeycloak() throws Exception {
                                mockMvc.perform(post("/api/task-management/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "self-register-user",
                                "displayName", "Self Register User",
                                "password", "ValidPass12",
                                "admin", false))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username", is("self-register-user")))
                        .andExpect(jsonPath("$.displayName", is("Self Register User")))
                        .andExpect(jsonPath("$.admin", is(false)));

                mockMvc.perform(get("/api/task-management/auth/me")
                        .with(bearerToken("self-register-user", "ValidPass12")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.username", is("self-register-user")))
                        .andExpect(jsonPath("$.admin", is(false)));
            }

            @Test
            void register_shouldAllowShortPassword() throws Exception {
                mockMvc.perform(post("/api/task-management/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "weak-user",
                                "displayName", "Weak User",
                                "password", "weak123",
                                "admin", false))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username", is("weak-user")))
                        .andExpect(jsonPath("$.displayName", is("Weak User")))
                        .andExpect(jsonPath("$.admin", is(false)));
            }

            @Test
            void register_shouldFallbackDisplayNameToUsernameWhenDisplayNameIsBlank() throws Exception {
                mockMvc.perform(post("/api/task-management/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "self-register-no-display",
                                "displayName", "   ",
                                "password", "ValidPass12",
                                "admin", false))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username", is("self-register-no-display")))
                        .andExpect(jsonPath("$.displayName", is("self-register-no-display")))
                        .andExpect(jsonPath("$.admin", is(false)));
            }

            @Test
            void anonymousUser_shouldNotBeAbleToRegisterAdminAccount() throws Exception {
                                mockMvc.perform(post("/api/task-management/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "blocked-admin-user",
                                "displayName", "Blocked Admin User",
                                "password", "blocked123",
                                "admin", true))))
                        .andExpect(status().isForbidden());
            }

            @Test
            void adminShouldBeAbleToCreateAdminUserInKeycloak() throws Exception {
                                TaskManagementUser demoAdmin = createUser("demo", "Demo User");
                                demoAdmin.setAdmin(true);
                                userRepository.save(demoAdmin);

                mockMvc.perform(post("/api/task-management/users")
                        .with(bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "managed-admin-user",
                                "displayName", "Managed Admin User",
                                "password", "Managed1234",
                                "admin", true))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username", is("managed-admin-user")))
                        .andExpect(jsonPath("$.admin", is(true)));

                mockMvc.perform(get("/api/task-management/admin/project-access")
                        .with(bearerToken("managed-admin-user", "Managed1234")))
                        .andExpect(status().isOk());
            }

            @Test
            void backlog_shouldReturnForbiddenForUserWithoutProjectAccess() throws Exception {
                userRepository.save(createUser("worker", "Worker User"));

                long projectId = createProject("Projekt Chraneny");

                mockMvc.perform(get("/api/task-management/stories/backlog")
                        .with(bearerToken("worker", "worker123"))
                        .param("projectId", String.valueOf(projectId)))
                        .andExpect(status().isForbidden());
            }

    @Test
    void stories_shouldBeNumberedFromOneWithinEachProject() throws Exception {
        long projectA = createProject("Projekt A");
        long projectB = createProject("Projekt B");

        createStory(projectA, "Story A1", "Desc A1", 10, "#FFF8DC");
        createStory(projectA, "Story A2", "Desc A2", 20, "#FFF8DC");
        createStory(projectB, "Story B1", "Desc B1", 5, "#FFF8DC");

        mockMvc.perform(get("/api/task-management/stories/backlog")
                .with(bearerToken())
                .param("projectId", String.valueOf(projectA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].storyNumber", is(1)))
                .andExpect(jsonPath("$[0].title", is("Story A1")))
                .andExpect(jsonPath("$[1].storyNumber", is(2)))
                .andExpect(jsonPath("$[1].title", is("Story A2")));

        mockMvc.perform(get("/api/task-management/stories/backlog")
                .with(bearerToken())
                .param("projectId", String.valueOf(projectB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].storyNumber", is(1)))
                .andExpect(jsonPath("$[0].title", is("Story B1")));
    }

    @Test
        void tasks_shouldBeNumberedWithinEachStory() throws Exception {
        JsonNode user = createTaskManagementUser("bob", "Bob");
        String userId = user.get("id").asText();

        long projectA = createProject("Projekt A");
                grantProjectAccess(projectA, userId);
        long sprintA = createSprint(projectA, "Sprint A");
        long storyA1 = createStory(projectA, "Story A1", "Desc A1", 10, "#FFF8DC");
        long storyA2 = createStory(projectA, "Story A2", "Desc A2", 20, "#FFF8DC");
        assignStoryToSprint(storyA1, sprintA);
        assignStoryToSprint(storyA2, sprintA);

        JsonNode taskA1 = createTask(storyA1, userId, "Task A1");
        JsonNode taskA2 = createTask(storyA1, userId, "Task A2");

        long projectB = createProject("Projekt B");
        grantProjectAccess(projectB, userId);
        long sprintB = createSprint(projectB, "Sprint B");
        long storyB1 = createStory(projectB, "Story B1", "Desc B1", 5, "#FFF8DC");
        assignStoryToSprint(storyB1, sprintB);
        JsonNode taskB1 = createTask(storyB1, userId, "Task B1");

        org.junit.jupiter.api.Assertions.assertEquals("task#1", taskA1.get("taskKey").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, taskA1.get("taskNumber").asInt());
        org.junit.jupiter.api.Assertions.assertEquals("task#2", taskA2.get("taskKey").asText());
        org.junit.jupiter.api.Assertions.assertEquals(2, taskA2.get("taskNumber").asInt());
        org.junit.jupiter.api.Assertions.assertEquals("task#1", taskB1.get("taskKey").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, taskB1.get("taskNumber").asInt());
    }

    @Test
    void sprintDashboard_shouldExposeStoryAndTaskNumbers() throws Exception {
        JsonNode user = createTaskManagementUser("carol", "Carol");
        String userId = user.get("id").asText();

        long projectId = createProject("Projekt Dashboard");
                grantProjectAccess(projectId, userId);
        long sprintId = createSprint(projectId, "Sprint Dashboard");
        long storyId = createStory(projectId, "Story Dashboard", "Desc", 10, "#FFF8DC");
        assignStoryToSprint(storyId, sprintId);
        createTask(storyId, userId, "Task Dashboard");

        mockMvc.perform(get("/api/task-management/dashboard/sprints/{sprintId}", sprintId)
                .with(bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userStories", hasSize(1)))
                .andExpect(jsonPath("$.userStories[0].storyNumber", is(1)))
                .andExpect(jsonPath("$.userStories[0].title", is("Story Dashboard")))
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].taskNumber", is(1)))
                .andExpect(jsonPath("$.tasks[0].taskKey", is("task#1")))
                .andExpect(jsonPath("$.tasks[0].title", is("Task Dashboard")));
    }

        @Test
        void backlogStory_shouldKeepTaskCountsAfterMovingOutOfSprint() throws Exception {
                JsonNode user = createTaskManagementUser("erin", "Erin");
                String userId = user.get("id").asText();

                long projectId = createProject("Projekt Preserve Tasks");
                grantProjectAccess(projectId, userId);
                long sprintId = createSprint(projectId, "Sprint Preserve Tasks");
                long storyId = createStory(projectId, "Story Preserve Tasks", "Desc", 10, "#FFF8DC");
                assignStoryToSprint(storyId, sprintId);

                long taskTodoId = createTask(storyId, userId, "Task Preserve TODO").get("id").asLong();
                createTask(storyId, userId, "Task Preserve DONE");
                updateTaskStatus(taskTodoId, "DONE");

                moveStoryToBacklog(storyId);

                mockMvc.perform(get("/api/task-management/stories/backlog")
                                .with(bearerToken())
                                .param("projectId", String.valueOf(projectId)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].storyNumber", is(1)))
                                .andExpect(jsonPath("$[0].taskCount", is(2)))
                                .andExpect(jsonPath("$[0].completedTaskCount", is(1)))
                                .andExpect(jsonPath("$[0].status", is("in progress")));
        }

    @Test
    void createTaskForBacklogStory_shouldReturnBadRequest() throws Exception {
        JsonNode user = createTaskManagementUser("dave", "Dave");
        String userId = user.get("id").asText();

        long projectId = createProject("Projekt Backlog");
                grantProjectAccess(projectId, userId);
        long storyId = createStory(projectId, "Backlog story", "Desc", 10, "#FFF8DC");

        mockMvc.perform(post("/api/task-management/tasks")
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "storyId", storyId,
                        "title", "Task without sprint",
                        "description", "Desc",
                        "definitionOfDone", "Done",
                        "colorHex", "#FFF8DC",
                        "creatorUserId", userId,
                        "assigneeUserId", userId,
                        "reviewerUserId", userId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void storyDiscussion_shouldReturnTreeAndUnreadState() throws Exception {
        JsonNode alice = createTaskManagementUser("alice-discussion", "Alice Discussion");
        JsonNode bob = createTaskManagementUser("bob-discussion", "Bob Discussion");
        String aliceId = alice.get("id").asText();
        String bobId = bob.get("id").asText();

        long projectId = createProject("Projekt Discussion");
                grantProjectAccess(projectId, aliceId, bobId);
        long storyId = createStory(projectId, "Story Discussion", "Desc", 10, "#FFF8DC");

        MvcResult rootMessageResult = mockMvc.perform(post("/api/task-management/stories/{storyId}/discussion/messages", storyId)
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "authorUserId", aliceId,
                        "content", "První zpráva"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content", is("První zpráva")))
                .andReturn();

        long rootMessageId = objectMapper.readTree(rootMessageResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/task-management/stories/{storyId}/discussion/messages", storyId)
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "authorUserId", bobId,
                        "parentMessageId", rootMessageId,
                        "content", "Reakce na zprávu"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentMessageId", is((int) rootMessageId)));

        ArgumentCaptor<StoryDiscussionRealtimeEvent> messageEventCaptor = ArgumentCaptor.forClass(StoryDiscussionRealtimeEvent.class);
        verify(messagingTemplate, times(2)).convertAndSend(
                eq("/topic/task-management.projects." + projectId + ".discussion"),
                messageEventCaptor.capture());

        StoryDiscussionRealtimeEvent firstMessageEvent = messageEventCaptor.getAllValues().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("MESSAGE_CREATED", firstMessageEvent.eventType());
        org.junit.jupiter.api.Assertions.assertEquals(aliceId, firstMessageEvent.actorUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(firstMessageEvent.unreadStoryIds());
        org.junit.jupiter.api.Assertions.assertTrue(firstMessageEvent.unreadStoryIds().isEmpty());

        StoryDiscussionRealtimeEvent secondMessageEvent = messageEventCaptor.getAllValues().get(1);
        org.junit.jupiter.api.Assertions.assertEquals("MESSAGE_CREATED", secondMessageEvent.eventType());
        org.junit.jupiter.api.Assertions.assertEquals(bobId, secondMessageEvent.actorUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(secondMessageEvent.unreadStoryIds());
        org.junit.jupiter.api.Assertions.assertTrue(secondMessageEvent.unreadStoryIds().isEmpty());

        mockMvc.perform(get("/api/task-management/stories/{storyId}/discussion/messages", storyId)
                .with(bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("První zpráva")))
                .andExpect(jsonPath("$[0].replies", hasSize(1)))
                .andExpect(jsonPath("$[0].replies[0].content", is("Reakce na zprávu")));

        mockMvc.perform(get("/api/task-management/projects/{projectId}/story-discussions/unread", projectId)
                .with(bearerToken())
                .param("userId", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is((int) storyId)));

        mockMvc.perform(put("/api/task-management/stories/{storyId}/discussion/read", storyId)
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("userId", aliceId))))
                .andExpect(status().isNoContent());

        ArgumentCaptor<StoryDiscussionRealtimeEvent> eventCaptor = ArgumentCaptor.forClass(StoryDiscussionRealtimeEvent.class);
        verify(messagingTemplate, times(3)).convertAndSend(
                eq("/topic/task-management.projects." + projectId + ".discussion"),
                eventCaptor.capture());

        StoryDiscussionRealtimeEvent readStateEvent = eventCaptor.getAllValues().get(2);
        org.junit.jupiter.api.Assertions.assertEquals("READ_STATE_UPDATED", readStateEvent.eventType());
        org.junit.jupiter.api.Assertions.assertEquals(aliceId, readStateEvent.actorUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(readStateEvent.unreadStoryIds());
        org.junit.jupiter.api.Assertions.assertTrue(readStateEvent.unreadStoryIds().isEmpty());

        mockMvc.perform(get("/api/task-management/projects/{projectId}/story-discussions/unread", projectId)
                .with(bearerToken())
                .param("userId", aliceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private JsonNode createTaskManagementUser(String username, String displayName) {
        TaskManagementUser savedUser = userRepository.save(createUser(username, displayName));
        return objectMapper.valueToTree(savedUser);
    }

    private TaskManagementUser createUser(String username, String displayName) {
        TaskManagementUser user = new TaskManagementUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        return user;
    }

    private long createProject(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/task-management/projects")
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", name,
                        "description", name + " description"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createSprint(long projectId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/task-management/sprints")
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "projectId", projectId,
                        "name", name,
                        "startDate", "2026-03-20",
                        "endDate", "2026-03-28"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createStory(long projectId, String title, String description, int difficulty, String colorHex) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/task-management/stories")
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "projectId", projectId,
                        "title", title,
                        "description", description,
                        "difficulty", difficulty,
                        "colorHex", colorHex))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

        private void grantProjectAccess(long projectId, String... userIds) {
                TmProject project = projectRepository.findWithMembersById(projectId)
                                .orElseThrow(() -> new IllegalStateException("Project not found for test setup"));

                for (String userId : userIds) {
                        TaskManagementUser user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalStateException("User not found for test setup"));
                        project.getMembers().add(user);
                }

                projectRepository.save(project);
        }

    private void assignStoryToSprint(long storyId, long sprintId) throws Exception {
        mockMvc.perform(put("/api/task-management/stories/{storyId}/assign-sprint", storyId)
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sprintId", sprintId))))
                .andExpect(status().isOk());
    }

    private void moveStoryToBacklog(long storyId) throws Exception {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("sprintId", null);

        mockMvc.perform(put("/api/task-management/stories/{storyId}/assign-sprint", storyId)
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private void updateTaskStatus(long taskId, String status) throws Exception {
        mockMvc.perform(put("/api/task-management/tasks/{taskId}/status", taskId)
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "status", status,
                        "reviewComment", ""))))
                .andExpect(status().isOk());
    }

    private JsonNode createTask(long storyId, String userId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/task-management/tasks")
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "storyId", storyId,
                        "title", title,
                        "description", title + " description",
                        "definitionOfDone", title + " done",
                        "colorHex", "#FFF8DC",
                        "creatorUserId", userId,
                        "assigneeUserId", userId,
                        "reviewerUserId", userId))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}