package com.supremeai.service;

import com.google.firebase.auth.FirebaseAuth;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseAuth firebaseAuth;

    @InjectMocks
    private UserAccountService userAccountService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("uid-123", "test@example.com", "Test User");
        testUser.setTier(UserTier.FREE);
        testUser.setIsActive(true);
    }

    // ==================== getUser Tests ====================

    @Test
    void getUser_ExistingUser_ReturnsUser() {
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Mono.just(testUser));

        Mono<User> result = userAccountService.getUser("uid-123");

        StepVerifier.create(result)
                .expectNextMatches(u -> u.getFirebaseUid().equals("uid-123")
                        && u.getEmail().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    void getUser_NonExistentUser_ReturnsEmpty() {
        when(userRepository.findByFirebaseUid("nonexistent")).thenReturn(Mono.empty());

        Mono<User> result = userAccountService.getUser("nonexistent");

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getUser_NullUid_ThrowsException() {
        StepVerifier.create(userAccountService.getUser(null))
                .expectError(NullPointerException.class)
                .verify();
    }

    // ==================== listAllUsers Tests ====================

    @Test
    void listAllUsers_MultipleUsers_ReturnsFlux() {
        User user2 = new User("uid-456", "other@example.com", "Other User");
        when(userRepository.findAll()).thenReturn(Flux.just(testUser, user2));

        Flux<User> result = userAccountService.listAllUsers();

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void listAllUsers_Empty_ReturnsEmptyFlux() {
        when(userRepository.findAll()).thenReturn(Flux.empty());

        Flux<User> result = userAccountService.listAllUsers();

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== updateUserTier Tests ====================

    @Test
    void updateUserTier_ValidUserAndTier_ReturnsUpdatedUser() {
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        Mono<User> result = userAccountService.updateUserTier("uid-123", UserTier.PRO);

        StepVerifier.create(result)
                .expectNextMatches(u -> u.getTier() == UserTier.PRO)
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserTier_NonExistentUser_ThrowsIllegalArgumentException() {
        when(userRepository.findByFirebaseUid("nonexistent")).thenReturn(Mono.empty());

        Mono<User> result = userAccountService.updateUserTier("nonexistent", UserTier.PRO);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void updateUserTier_NullTier_ThrowsIllegalArgumentException() {
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Mono.just(testUser));

        Mono<User> result = userAccountService.updateUserTier("uid-123", null);

        StepVerifier.create(result)
                .expectError(NullPointerException.class)
                .verify();
    }

    // ==================== deactivateUser Tests ====================

    @Test
    void deactivateUser_ValidUser_ReturnsDeactivatedUser() {
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        Mono<User> result = userAccountService.deactivateUser("uid-123");

        StepVerifier.create(result)
                .expectNextMatches(u -> !u.getIsActive())
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivateUser_AlreadyDeactivatedUser_StillDeactivates() {
        testUser.setIsActive(false);
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        Mono<User> result = userAccountService.deactivateUser("uid-123");

        StepVerifier.create(result)
                .expectNextMatches(u -> !u.getIsActive())
                .verifyComplete();
    }

    @Test
    void deactivateUser_NonExistentUser_ThrowsIllegalArgumentException() {
        when(userRepository.findByFirebaseUid("nonexistent")).thenReturn(Mono.empty());

        Mono<User> result = userAccountService.deactivateUser("nonexistent");

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== Edge Cases ====================

    @Test
    void getUser_EmptyStringUid_ReturnsEmpty() {
        when(userRepository.findByFirebaseUid("")).thenReturn(Mono.empty());

        Mono<User> result = userAccountService.getUser("");

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void deactivateUser_EmptyStringUid_ThrowsException() {
        when(userRepository.findByFirebaseUid("")).thenReturn(Mono.empty());

        Mono<User> result = userAccountService.deactivateUser("");

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}