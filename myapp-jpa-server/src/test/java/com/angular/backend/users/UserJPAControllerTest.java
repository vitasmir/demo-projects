package com.angular.backend.users;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.angular.backend.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserJPAControllerTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void getAllUsers_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_shouldReturnCreated() throws Exception {
        UserJPA user = new UserJPA();
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        mockMvc.perform(post("/api/users")
                .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    void getAllUsers_shouldReturnOk() throws Exception {
        UserJPA user1 = new UserJPA();
        user1.setUsername("userone");
        UserJPA user2 = new UserJPA();
        user2.setUsername("usertwo");
        userRepository.saveAll(Arrays.asList(user1, user2));

        mockMvc.perform(get("/api/users").with(bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("userone")))
                .andExpect(jsonPath("$[1].username", is("usertwo")));
    }

    @Test
    void getUserById_whenExists_shouldReturnOk() throws Exception {
        UserJPA user = new UserJPA();
        user.setUsername("findme");
        UserJPA savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/users/{id}", savedUser.getId()).with(bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedUser.getId())))
                .andExpect(jsonPath("$.username", is("findme")));
    }

    @Test
    void getUserById_whenNotExists_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", "non-existent-id").with(bearerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_whenExists_shouldReturnOk() throws Exception {
        UserJPA existingUser = new UserJPA();
        existingUser.setUsername("old.username");
        UserJPA savedUser = userRepository.save(existingUser);

        UserJPA updatedDetails = new UserJPA();
        updatedDetails.setUsername("new.username");
        updatedDetails.setFirstName("New");

        mockMvc.perform(put("/api/users/{id}", savedUser.getId())
            .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("new.username")))
                .andExpect(jsonPath("$.firstName", is("New")));
    }

    @Test
    void updateUser_whenNotExists_shouldReturnNotFound() throws Exception {
        UserJPA updatedDetails = new UserJPA();
        updatedDetails.setUsername("new.username");

        mockMvc.perform(put("/api/users/{id}", "non-existent-id")
            .with(bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_whenExists_shouldReturnNoContent() throws Exception {
        UserJPA user = new UserJPA();
        user.setUsername("deleteme");
        UserJPA savedUser = userRepository.save(user);

        mockMvc.perform(delete("/api/users/{id}", savedUser.getId()).with(bearerToken()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(savedUser.getId()));
    }

    @Test
    void deleteUser_whenNotExists_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", "non-existent-id").with(bearerToken()))
                .andExpect(status().isNotFound());
    }
}
