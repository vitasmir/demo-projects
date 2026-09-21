package com.peta.films;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileUserDetailsService implements UserDetailsService {

	private final PasswordEncoder passwordEncoder;

	// Volatile ensures that when we swap the map, all threads see the change
	// immediately
	private volatile Map<String, String> userCache = Collections.emptyMap();

	public FileUserDetailsService(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@PostConstruct
	public void init() {
		reloadUsers();
	}

	// Public method to trigger the reload
	public synchronized void reloadUsers() {
		try {
			System.out.println("Reloading users from file...");

			// 1. Load into a TEMPORARY map first (Thread-safe strategy)
			Map<String, String> tempMap = new HashMap<>();

			ClassPathResource resource = new ClassPathResource("users.txt");
			// Check if file exists to avoid crashing
			if (resource.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						String user = parts[0].trim();
						String rawOrHash = parts[1].trim();
						// If the value looks like an encoded password (bcrypt or has an id prefix),
						// keep it as-is. Otherwise encode it using the configured PasswordEncoder.
						tempMap.put(user, rawOrHash);
					}
				}
				reader.close();
			} else {
				System.out.println("Warning: users.txt not found!");
			}

			// 2. Atomic Swap: Replace the old map with the new one instantly
			this.userCache = tempMap;
			System.out.println("Reload complete. Total users: " + userCache.size());

		} catch (IOException e) {
			throw new RuntimeException("Failed to reload users.txt", e);
		}
	}

	// Added method: load user details from the in-memory cache
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String storedHash = this.userCache.get(username);
		if (storedHash == null) {
			throw new UsernameNotFoundException("User not found: " + username);
		}

		return User.withUsername(username).password(storedHash).roles("USER").build();
	}

}
