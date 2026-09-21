package com.angular.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!test")
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
                return web -> web.ignoring().requestMatchers(
                                "/public/**",
                                "/login",
                                "/ws",
                                "/ws/**",
                                "/ws2",
                                "/ws2/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html");
        }

        @Bean
        @Order(1)
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(HttpMethod.GET, "/api/task-management/auth/me").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/task-management/users").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/boat-rental/reservations").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/boat-rental/reservations/*").authenticated()
                                                .anyRequest().permitAll())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

                return http.build();
        }
}
