
package com.supremeai.integration;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    public void setUp() {
        // Clean up before each test
        userRepository.deleteAll().block();

        // Create a test user
        testUser = new User("test-uid", "test@example.com", "Test User");
        testUser.setTier(com.supremeai.model.UserTier.FREE);
    }

    @Test
    public void testCreateAndRetrieveUser() {
        // Act
        User savedUser = userRepository.save(testUser).block();

        // Assert
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("test-uid", savedUser.getFirebaseUid());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getUsername());
        assertEquals(com.supremeai.model.UserTier.FREE, savedUser.getTier());
    }

    @Test
    public void testFindUserByFirebaseUid() {
        // Arrange
        userRepository.save(testUser).block();

        // Act
        User foundUser = userRepository.findByFirebaseUid("test-uid").block();

        // Assert
        assertNotNull(foundUser);
        assertEquals("test-uid", foundUser.getFirebaseUid());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    public void testUpdateUser() {
        // Arrange
        User savedUser = userRepository.save(testUser).block();
        savedUser.setUsername("Updated User");
        savedUser.setTier(com.supremeai.model.UserTier.PREMIUM);

        // Act
        User updatedUser = userRepository.save(savedUser).block();

        // Assert
        assertNotNull(updatedUser);
        assertEquals("Updated User", updatedUser.getUsername());
        assertEquals(com.supremeai.model.UserTier.PREMIUM, updatedUser.getTier());
    }

    @Test
    public void testDeleteUser() {
        // Arrange
        User savedUser = userRepository.save(testUser).block();

        // Act
        userRepository.deleteById(savedUser.getId()).block();

        // Assert
        Mono<User> deletedUser = userRepository.findById(savedUser.getId());
        assertNull(deletedUser.block());
    }

    @Test
    public void testFindAllUsers() {
        // Arrange
        userRepository.save(testUser).block();

        User user2 = new User("test-uid-2", "test2@example.com", "Test User 2");
        user2.setTier(com.supremeai.model.UserTier.PREMIUM);
        userRepository.save(user2).block();

        // Act
        var users = userRepository.findAll().collectList().block();

        // Assert
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public void testUserExistsByFirebaseUid() {
        // Arrange
        userRepository.save(testUser).block();

        // Act
        boolean exists = userRepository.existsByFirebaseUid("test-uid").block();
        boolean notExists = userRepository.existsByFirebaseUid("nonexistent-uid").block();

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }
}
