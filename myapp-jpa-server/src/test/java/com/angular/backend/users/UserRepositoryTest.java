package com.angular.backend.users;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.angular.backend.AbstractIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateAndFindUser() {
        UserJPA user = new UserJPA();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");

        UserJPA saved = userRepository.save(user);
        assertNotNull(saved.getId());

        Optional<UserJPA> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("testuser@example.com", found.get().getEmail());
    }

    @Test
    void testDeleteUser() {
        UserJPA user = new UserJPA();
        user.setUsername("deleteme");
        user.setEmail("deleteme@example.com");

        UserJPA saved = userRepository.save(user);
        String id = saved.getId();
        userRepository.deleteById(id);
        assertFalse(userRepository.findById(id).isPresent());
    }
}
