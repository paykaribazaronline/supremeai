
package com.supremeai.integration;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Integration tests require proper infrastructure setup")
public class DatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll().block();

        testUser = new User("test-uid", "test@example.com", "Test User");
        testUser.setTier(com.supremeai.model.UserTier.FREE);
    }

    @Test
    public void testCreateAndRetrieveUser() {
        User savedUser = userRepository.save(testUser).block();

        assertNotNull(savedUser);
        assertEquals("test-uid", savedUser.getFirebaseUid());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getDisplayName());
        assertEquals(com.supremeai.model.UserTier.FREE, savedUser.getTier());
    }

    @Test
    public void testFindUserByFirebaseUid() {
        userRepository.save(testUser).block();

        User foundUser = userRepository.findByFirebaseUid("test-uid").block();

        assertNotNull(foundUser);
        assertEquals("test-uid", foundUser.getFirebaseUid());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    public void testUpdateUser() {
        User savedUser = userRepository.save(testUser).block();
        savedUser.setDisplayName("Updated User");
        savedUser.setTier(com.supremeai.model.UserTier.PRO);

        User updatedUser = userRepository.save(savedUser).block();

        assertNotNull(updatedUser);
        assertEquals("Updated User", updatedUser.getDisplayName());
        assertEquals(com.supremeai.model.UserTier.PRO, updatedUser.getTier());
    }

    @Test
    public void testDeleteUser() {
        User savedUser = userRepository.save(testUser).block();

        userRepository.deleteById(savedUser.getFirebaseUid()).block();

        Mono<User> deletedUser = userRepository.findById(savedUser.getFirebaseUid());
        assertNull(deletedUser.block());
    }

    @Test
    public void testFindAllUsers() {
        userRepository.save(testUser).block();

        User user2 = new User("test-uid-2", "test2@example.com", "Test User 2");
        user2.setTier(com.supremeai.model.UserTier.PRO);
        userRepository.save(user2).block();

        var users = userRepository.findAll().collectList().block();

        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public void testUserExistsByFirebaseUid() {
        userRepository.save(testUser).block();

        User exists = userRepository.findByFirebaseUid("test-uid").block();
        User notExists = userRepository.findByFirebaseUid("nonexistent-uid").block();

        assertNotNull(exists);
        assertNull(notExists);
    }
}
