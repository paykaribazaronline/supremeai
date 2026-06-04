package com.supremeai.repository;

import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.model.UserTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSimulatorProfileRepositoryTest {

    @Mock
    private UserSimulatorProfileRepository repository;

    @Test
    void findByUserId_shouldReturnProfileForUser() {
        UserSimulatorProfile profile = new UserSimulatorProfile("user-1");
        when(repository.findByUserId("user-1")).thenReturn(Mono.just(profile));

        StepVerifier.create(repository.findByUserId("user-1"))
                .expectNextMatches(p -> "user-1".equals(p.getUserId()))
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNotFound() {
        when(repository.findByUserId("no-user")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findByUserId("no-user"))
                .verifyComplete();
    }

    @Test
    void findByActiveInstallsGreaterThan_shouldReturnProfilesWithMinInstalls() {
        UserSimulatorProfile p1 = new UserSimulatorProfile("user-2");
        p1.setActiveInstalls(10);
        UserSimulatorProfile p2 = new UserSimulatorProfile("user-3");
        p2.setActiveInstalls(15);

        when(repository.findByActiveInstallsGreaterThan(5)).thenReturn(Flux.fromIterable(List.of(p1, p2)));

        StepVerifier.create(repository.findByActiveInstallsGreaterThan(5))
                .expectNextMatches(p -> p.getActiveInstalls() > 5)
                .expectNextMatches(p -> p.getActiveInstalls() > 5)
                .verifyComplete();
    }

    @Test
    void findByActiveInstallsGreaterThan_shouldReturnEmpty_whenNoneMatch() {
        when(repository.findByActiveInstallsGreaterThan(100)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByActiveInstallsGreaterThan(100))
                .verifyComplete();
    }

    @Test
    void findByUserTier_shouldReturnProfilesForTier() {
        UserSimulatorProfile p1 = new UserSimulatorProfile("user-4");
        p1.setUserTier(UserTier.PRO);
        UserSimulatorProfile p2 = new UserSimulatorProfile("user-5");
        p2.setUserTier(UserTier.PRO);

        when(repository.findByUserTier(UserTier.PRO)).thenReturn(Flux.fromIterable(List.of(p1, p2)));

        StepVerifier.create(repository.findByUserTier(UserTier.PRO))
                .expectNextMatches(p -> p.getUserTier() == UserTier.PRO)
                .expectNextMatches(p -> p.getUserTier() == UserTier.PRO)
                .verifyComplete();
    }

    @Test
    void findByUserTier_shouldReturnEmpty_whenNoMatch() {
        when(repository.findByUserTier(UserTier.ENTERPRISE)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByUserTier(UserTier.ENTERPRISE))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistProfile() {
        UserSimulatorProfile profile = new UserSimulatorProfile("user-new");
        profile.setUserTier(UserTier.BASIC);
        when(repository.save(profile)).thenReturn(Mono.just(profile));

        StepVerifier.create(repository.save(profile))
                .expectNextMatches(p -> p.getUserTier() == UserTier.BASIC)
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnProfile_whenExists() {
        UserSimulatorProfile profile = new UserSimulatorProfile("user-find");
        when(repository.findById("user-find")).thenReturn(Mono.just(profile));

        StepVerifier.create(repository.findById("user-find"))
                .expectNextMatches(p -> "user-find".equals(p.getUserId()))
                .verifyComplete();
    }
}
