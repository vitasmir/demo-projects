package com.angular.backend.users;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserJPA createUser(UserJPA user) {
        log.info("Attempting to create a new user with username: {}", user.getUsername());
        user.setId(null);
        UserJPA savedUser = userRepository.save(user);
        log.info("Successfully created user with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public List<UserJPA> getAllUsers() {
        log.info("Fetching all users");
        List<UserJPA> allUsers = userRepository.findAll();
        log.info("Found {} users", allUsers.size());
        return allUsers;
    }

    @Transactional
    public UserJPA getUserById(String id) {
        log.info("Fetching user by ID: {}", id);
        Optional<UserJPA> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            log.info("Found user: {}", userOptional.get().getUsername());
        } else {
            log.warn("User with ID {} not found", id);
        }
        return userOptional.orElse(null);
    }

    @Transactional
    public UserJPA updateUser(String id, UserJPA user) {
        log.info("Attempting to update user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("User with ID {} not found for update.", id);
            return null;
        }
        user.setId(id);
        UserJPA updatedUser = userRepository.save(user);
        log.info("Successfully updated user with ID: {}", updatedUser.getId());
        return updatedUser;
    }

    @Transactional
    public boolean deleteUser(String id) {
        log.info("Attempting to delete user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("User with ID {} not found for deletion.", id);
            return false;
        }
        userRepository.deleteById(id);
        log.info("Successfully deleted user with ID: {}", id);
        return true;
    }
}
