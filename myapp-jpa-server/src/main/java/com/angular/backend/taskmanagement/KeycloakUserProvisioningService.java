package com.angular.backend.taskmanagement;

import java.util.List;
import java.util.Locale;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Service
public class KeycloakUserProvisioningService {

    private static final String DEFAULT_USER_ROLE = "api.read";
    private static final String ADMIN_ROLE = "task-management-admin";

    private final KeycloakAdminProperties properties;

    public KeycloakUserProvisioningService(KeycloakAdminProperties properties) {
        this.properties = properties;
    }

    public String createUser(String username, String firstName, String lastName, String email, String password, boolean admin) {
        try (Keycloak keycloak = newKeycloakClient()) {
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(username);
            user.setFirstName(firstName.trim());
            user.setLastName(lastName.trim());
            user.setEmail(email.trim());
            user.setEmailVerified(true);
            user.setRequiredActions(List.of());

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));

            Response response = keycloak.realm(properties.getRealm()).users().create(user);
            try {
                if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists in Keycloak");
                }
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    throw mapKeycloakResponse(response, "Keycloak user creation failed");
                }

                String keycloakUserId = CreatedResponseUtil.getCreatedId(response);
                assignRealmRoles(keycloak, keycloakUserId, admin);
                return keycloakUserId;
            } finally {
                response.close();
            }
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null) {
                throw mapKeycloakResponse(exception.getResponse(), "Keycloak provisioning failed");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak provisioning failed", exception);
        }
    }

    public void deleteUser(String keycloakUserId) {
        try (Keycloak keycloak = newKeycloakClient()) {
            keycloak.realm(properties.getRealm()).users().delete(keycloakUserId);
        } catch (RuntimeException ignored) {
            // Best-effort rollback for a previously created Keycloak user.
        }
    }

    public void deleteUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        try (Keycloak keycloak = newKeycloakClient()) {
            List<UserRepresentation> users = keycloak.realm(properties.getRealm()).users().searchByUsername(username.trim(), true);
            if (users == null || users.isEmpty()) {
                return;
            }

            keycloak.realm(properties.getRealm()).users().delete(users.get(0).getId());
        } catch (RuntimeException ignored) {
            // Best-effort cleanup for a previously removed local user.
        }
    }

    public boolean userHasAdminRole(String username) {
        try (Keycloak keycloak = newKeycloakClient()) {
            List<UserRepresentation> users = keycloak.realm(properties.getRealm()).users().searchByUsername(username, true);
            if (users == null || users.isEmpty()) {
                return false;
            }

            String userId = users.get(0).getId();
            return keycloak.realm(properties.getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .anyMatch(ADMIN_ROLE::equals);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to verify Keycloak admin role for current user",
                    exception);
        }
    }

    public boolean isBootstrapAdminUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        return properties.getBootstrapAdminUsernames().stream()
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .anyMatch(candidate -> candidate.trim().equalsIgnoreCase(username));
    }

    private void assignRealmRoles(Keycloak keycloak, String keycloakUserId, boolean admin) {
        RoleRepresentation defaultRole = getOrCreateRealmRole(keycloak, DEFAULT_USER_ROLE);
        keycloak.realm(properties.getRealm()).users().get(keycloakUserId).roles().realmLevel().add(List.of(defaultRole));

        if (!admin) {
            return;
        }

        RoleRepresentation adminRole = getOrCreateRealmRole(keycloak, ADMIN_ROLE);
        keycloak.realm(properties.getRealm()).users().get(keycloakUserId).roles().realmLevel().add(List.of(adminRole));
    }

    private RoleRepresentation getOrCreateRealmRole(Keycloak keycloak, String roleName) {
        try {
            return keycloak.realm(properties.getRealm()).roles().get(roleName).toRepresentation();
        } catch (NotFoundException ignored) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            keycloak.realm(properties.getRealm()).roles().create(role);
            return keycloak.realm(properties.getRealm()).roles().get(roleName).toRepresentation();
        }
    }

    private Keycloak newKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(requireConfigured(properties.getServerUrl(), "app.keycloak.admin.server-url"))
                .realm(requireConfigured(properties.getAdminRealm(), "app.keycloak.admin.admin-realm"))
                .clientId(requireConfigured(properties.getClientId(), "app.keycloak.admin.client-id"))
                .username(requireConfigured(properties.getUsername(), "app.keycloak.admin.username"))
                .password(requireConfigured(properties.getPassword(), "app.keycloak.admin.password"))
                .build();
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Missing Keycloak admin configuration: " + propertyName);
        }
        return value;
    }

    private ResponseStatusException mapKeycloakResponse(Response response, String fallbackMessage) {
        int statusCode = response.getStatus();
        String keycloakMessage = extractResponseBody(response);
        String normalizedMessage = keycloakMessage == null ? "" : keycloakMessage.trim();

        if (statusCode == Response.Status.CONFLICT.getStatusCode()) {
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    normalizedMessage.isBlank() ? "User already exists in Keycloak" : normalizedMessage);
        }

        if (statusCode >= 400 && statusCode < 500) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    normalizedMessage.isBlank() ? fallbackMessage : normalizeKeycloakMessage(normalizedMessage));
        }

        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                normalizedMessage.isBlank() ? fallbackMessage : normalizeKeycloakMessage(normalizedMessage));
    }

    private String extractResponseBody(Response response) {
        try {
            Object entity = response.getEntity();
            if (entity instanceof String body && !body.isBlank()) {
                return body;
            }
            if (response.hasEntity()) {
                String body = response.readEntity(String.class);
                return body == null ? "" : body;
            }
        } catch (RuntimeException ignored) {
            return "";
        }
        return "";
    }

    private String normalizeKeycloakMessage(String message) {
        String compact = message.replace('"', ' ').replace('{', ' ').replace('}', ' ').trim();
        String lower = compact.toLowerCase(Locale.ROOT);

        if (lower.contains("password") && lower.contains("policy")) {
            return "Heslo nevyhovuje pravidlům Keycloaku.";
        }
        if (lower.contains("password") && lower.contains("invalid")) {
            return "Heslo nevyhovuje pravidlům Keycloaku.";
        }
        if (lower.contains("username") && lower.contains("exists")) {
            return "User already exists in Keycloak";
        }

        return compact;
    }
}