package com.supremeai.repository;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        User user = new User("uid-123", "test@example.com", "Test User");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(user));

        StepVerifier.create(userRepository.findByEmail("test@example.com"))
                .expectNextMatches(u -> "test@example.com".equals(u.getEmail())
                        && "uid-123".equals(u.getFirebaseUid())
                        && "Test User".equals(u.getDisplayName()))
                .verifyComplete();

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(userRepository.findByEmail("nonexistent@example.com"))
                .verifyComplete();
    }

    @Test
    void findByFirebaseUid_shouldReturnUser_whenUidExists() {
        User user = new User("uid-456", "user@domain.com", "Another User");
        user.setTier(UserTier.PRO);
        when(userRepository.findByFirebaseUid("uid-456")).thenReturn(Mono.just(user));

        StepVerifier.create(userRepository.findByFirebaseUid("uid-456"))
                .expectNextMatches(u -> "uid-456".equals(u.getFirebaseUid())
                        && UserTier.PRO.equals(u.getTier()))
                .verifyComplete();
    }

    @Test
    void findByFirebaseUid_shouldReturnEmpty_whenUidNotFound() {
        when(userRepository.findByFirebaseUid("unknown-uid")).thenReturn(Mono.empty());

        StepVerifier.create(userRepository.findByFirebaseUid("unknown-uid"))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistUser() {
        User user = new User("uid-789", "save@test.com", "Save Test");
        when(userRepository.save(user)).thenReturn(Mono.just(user));

        StepVerifier.create(userRepository.save(user))
                .expectNextMatches(u -> "uid-789".equals(u.getFirebaseUid()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnUser_whenExists() {
        User user = new User("uid-000", "find@test.com", "Find Test");
        when(userRepository.findById("uid-000")).thenReturn(Mono.just(user));

        StepVerifier.create(userRepository.findById("uid-000"))
                .expectNextMatches(u -> "uid-000".equals(u.getFirebaseUid()))
                .verifyComplete();
    }

    @Test
    void deleteById_shouldRemoveUser() {
        when(userRepository.deleteById("uid-delete")).thenReturn(Mono.empty());

        StepVerifier.create(userRepository.deleteById("uid-delete"))
                .verifyComplete();
    }
}
