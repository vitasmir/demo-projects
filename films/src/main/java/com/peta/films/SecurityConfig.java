package com.peta.films;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
					http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated() // Protect all endpoints
		).httpBasic(withDefaults()) // Use Basic Auth (Pop-up in browser)
				.formLogin(form -> form.defaultSuccessUrl("/", true));
				
				// Or use standard Form Login


		/*
				http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
			.anyRequest().authenticated() // Protect all other endpoints
		)
		.httpBasic(withDefaults()) // Use Basic Auth (Pop-up in browser)
		.formLogin(form -> form
			.defaultSuccessUrl("/index.html", true) // Always redirect to root after successful login
			.permitAll()
		); // Use standard Form Login
*/
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Strength 12 is the recommended minimum for 2026 hardware
		return new BCryptPasswordEncoder(12);
	}
}