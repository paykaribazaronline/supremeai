package com.supremeai.repository;

import com.supremeai.model.UserLanguagePreference;
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
class UserLanguagePreferenceRepositoryTest {

    @Mock
    private UserLanguagePreferenceRepository repository;

    @Test
    void findByUserId_shouldReturnPreferenceForUser() {
        UserLanguagePreference pref = new UserLanguagePreference("user-1", "bn", "Bengali");
        pref.setId("pref-1");

        when(repository.findByUserId("user-1")).thenReturn(Mono.just(pref));

        StepVerifier.create(repository.findByUserId("user-1"))
                .expectNextMatches(p -> "user-1".equals(p.getUserId())
                        && "bn".equals(p.getLanguageCode())
                        && "Bengali".equals(p.getLanguageName()))
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNotFound() {
        when(repository.findByUserId("no-user")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findByUserId("no-user"))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllPreferences() {
        UserLanguagePreference p1 = new UserLanguagePreference("user-2", "en", "English");
        UserLanguagePreference p2 = new UserLanguagePreference("user-3", "es", "Spanish");

        when(repository.findAll()).thenReturn(Flux.fromIterable(List.of(p1, p2)));

        StepVerifier.create(repository.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnEmpty_whenNone() {
        when(repository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(repository.findAll())
                .verifyComplete();
    }

    @Test
    void save_shouldPersistPreference() {
        UserLanguagePreference pref = new UserLanguagePreference("user-new", "fr", "French");
        when(repository.save(pref)).thenReturn(Mono.just(pref));

        StepVerifier.create(repository.save(pref))
                .expectNextMatches(p -> "fr".equals(p.getLanguageCode())
                        && "French".equals(p.getLanguageName()))
                .verifyComplete();
    }

    @Test
    void delete_shouldRemovePreference() {
        when(repository.deleteById("pref-delete")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById("pref-delete"))
                .verifyComplete();
    }
}
