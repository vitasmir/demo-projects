package com.angular.backend;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.Map;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

        private static final String REALM = "myrealm";
        private static final String CLIENT_ID = "myapp-client";
        private static final String DEFAULT_USERNAME = "demo";
        private static final String DEFAULT_PASSWORD = "demo123";

        @ServiceConnection
        @Container
        public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                        DockerImageName.parse("postgres:17.7"));

        @ServiceConnection
        @Container
        public static final RabbitMQContainer rabbitmq = new RabbitMQContainer(
                        DockerImageName.parse("rabbitmq:4.1.8"));

        @SuppressWarnings("resource")
        @Container
        static final KeycloakContainer keycloak = new KeycloakContainer("keycloak/keycloak:26.5.7")
                .withRealmImportFile("keycloak/realm-export.json");

        @DynamicPropertySource
        static void configure(DynamicPropertyRegistry registry) {
                registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                                () -> keycloak.getAuthServerUrl() + "/realms/" + REALM);
                registry.add("app.keycloak.admin.server-url", keycloak::getAuthServerUrl);
        }

        protected String getAccessToken() {
                return getAccessToken(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        }

        protected String getAccessToken(String username, String password) {
                RestTemplate restTemplate = new RestTemplate();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("client_id", CLIENT_ID);
                form.add("grant_type", "password");
                form.add("username", username);
                form.add("password", password);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                keycloak.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token",
                                HttpMethod.POST,
                                new HttpEntity<>(form, headers),
                                new ParameterizedTypeReference<>() {
                                });

                Map<String, Object> body = response.getBody();
                if (body == null || body.get("access_token") == null) {
                        throw new IllegalStateException("Keycloak did not return an access token");
                }

                return body.get("access_token").toString();
        }

        protected RequestPostProcessor bearerToken() {
                return request -> {
                        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
                        return request;
                };
        }

        protected RequestPostProcessor bearerToken(String username, String password) {
                return request -> {
                        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken(username, password));
                        return request;
                };
        }

}
