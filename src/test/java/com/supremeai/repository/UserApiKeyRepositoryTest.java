package com.supremeai.repository;

import com.supremeai.model.UserApiKey;
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
class UserApiKeyRepositoryTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Test
    void findByApiKey_shouldReturnKey_whenExists() {
        UserApiKey key = new UserApiKey("user-1", "OpenAI", "Prod Key", "sk-abc123");
        key.setId("key-1");
        when(userApiKeyRepository.findByApiKey("sk-abc123")).thenReturn(Mono.just(key));

        StepVerifier.create(userApiKeyRepository.findByApiKey("sk-abc123"))
                .expectNextMatches(k -> "key-1".equals(k.getId()) && "OpenAI".equals(k.getProvider()))
                .verifyComplete();
    }

    @Test
    void findByApiKey_shouldReturnEmpty_whenNotFound() {
        when(userApiKeyRepository.findByApiKey("sk-nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(userApiKeyRepository.findByApiKey("sk-nonexistent"))
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnAllKeysForUser() {
        UserApiKey k1 = new UserApiKey("user-2", "OpenAI", "Key 1", "sk-111");
        k1.setId("key-2");
        UserApiKey k2 = new UserApiKey("user-2", "Anthropic", "Key 2", "sk-222");
        k2.setId("key-3");

        when(userApiKeyRepository.findByUserId("user-2")).thenReturn(Flux.fromIterable(List.of(k1, k2)));

        StepVerifier.create(userApiKeyRepository.findByUserId("user-2"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnEmpty_whenNoKeys() {
        when(userApiKeyRepository.findByUserId("user-no-keys")).thenReturn(Flux.empty());

        StepVerifier.create(userApiKeyRepository.findByUserId("user-no-keys"))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndProvider_shouldReturnMatchingKeys() {
        UserApiKey key = new UserApiKey("user-3", "OpenAI", "Dev Key", "sk-dev");
        key.setId("key-4");

        when(userApiKeyRepository.findByUserIdAndProvider("user-3", "OpenAI"))
                .thenReturn(Flux.just(key));

        StepVerifier.create(userApiKeyRepository.findByUserIdAndProvider("user-3", "OpenAI"))
                .expectNextMatches(k -> "OpenAI".equals(k.getProvider()) && "user-3".equals(k.getUserId()))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndProvider_shouldReturnEmpty_whenNoMatch() {
        when(userApiKeyRepository.findByUserIdAndProvider("user-3", "Groq"))
                .thenReturn(Flux.empty());

        StepVerifier.create(userApiKeyRepository.findByUserIdAndProvider("user-3", "Groq"))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndStatus_shouldReturnKeysWithStatus() {
        UserApiKey k1 = new UserApiKey("user-4", "OpenAI", "Active", "sk-a1");
        k1.setId("key-5");
        k1.setStatus("active");
        UserApiKey k2 = new UserApiKey("user-4", "Google AI", "Also Active", "sk-a2");
        k2.setId("key-6");
        k2.setStatus("active");

        when(userApiKeyRepository.findByUserIdAndStatus("user-4", "active"))
                .thenReturn(Flux.fromIterable(List.of(k1, k2)));

        StepVerifier.create(userApiKeyRepository.findByUserIdAndStatus("user-4", "active"))
                .expectNextMatches(k -> "active".equals(k.getStatus()))
                .expectNextMatches(k -> "active".equals(k.getStatus()))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndStatus_shouldReturnEmpty_whenNoMatchingStatus() {
        when(userApiKeyRepository.findByUserIdAndStatus("user-4", "error"))
                .thenReturn(Flux.empty());

        StepVerifier.create(userApiKeyRepository.findByUserIdAndStatus("user-4", "error"))
                .verifyComplete();
    }

    @Test
    void countByUserId_shouldReturnCount() {
        when(userApiKeyRepository.countByUserId("user-5")).thenReturn(Mono.just(3L));

        StepVerifier.create(userApiKeyRepository.countByUserId("user-5"))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void countByUserId_shouldReturnZero_whenNoKeys() {
        when(userApiKeyRepository.countByUserId("user-empty")).thenReturn(Mono.just(0L));

        StepVerifier.create(userApiKeyRepository.countByUserId("user-empty"))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void countByUserIdAndStatus_shouldReturnCount() {
        when(userApiKeyRepository.countByUserIdAndStatus("user-6", "active")).thenReturn(Mono.just(2L));

        StepVerifier.create(userApiKeyRepository.countByUserIdAndStatus("user-6", "active"))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void countByUserIdAndStatus_shouldReturnZero_whenNoneMatch() {
        when(userApiKeyRepository.countByUserIdAndStatus("user-6", "inactive")).thenReturn(Mono.just(0L));

        StepVerifier.create(userApiKeyRepository.countByUserIdAndStatus("user-6", "inactive"))
                .expectNext(0L)
                .verifyComplete();
    }
}
