package com.angular.backend.users;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserJPAController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new user",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User object to be created.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserJPA.class))
            ))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UserJPA.class))),
        @ApiResponse(responseCode = "400", description = "Invalid user data supplied", content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserJPA> createUser(@RequestBody UserJPA user) {
        UserJPA createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = UserJPA.class))))
    })
    @GetMapping
    public ResponseEntity<Iterable<UserJPA>> getAllUsers() {
        List<UserJPA> allUsers = userService.getAllUsers();
        return new ResponseEntity<>(allUsers, HttpStatus.OK);
    }

    @Operation(summary = "Get a user by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found the user",
                content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserJPA.class))}),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserJPA> getUserById(@Parameter(description = "ID of the user to be fetched") @PathVariable String id) {
        UserJPA user = userService.getUserById(id);
        return ResponseEntity.of(Optional.ofNullable(user));
    }

    @Operation(summary = "Update an existing user",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User object with updated details. The ID in the body is ignored.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserJPA.class))
            ))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully",
                content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserJPA.class))}),
        @ApiResponse(responseCode = "400", description = "Invalid ID or data supplied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserJPA> updateUser(
            @Parameter(description = "ID of the user to update") @PathVariable String id,
            @RequestBody UserJPA user) {
        // Ensure the ID from the path is used, not from the body
        user.setId(id);
        UserJPA updatedUser = userService.updateUser(id, user);
        return ResponseEntity.of(Optional.ofNullable(updatedUser));
    }

    @Operation(summary = "Delete a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@Parameter(description = "ID of the user to delete") @PathVariable String id) {
        boolean deleted = userService.deleteUser(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
