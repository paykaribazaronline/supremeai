package com.supremeai.repository;

import com.supremeai.model.APIProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderRepositoryTest {ProviderRepositorypublic ProviderRepositoryTest(ProviderRepository repository) {
ProviderRepository    this.repository = repository;
ProviderRepository}




    @Test
    void save_shouldPersistProvider() {
        APIProvider provider = new APIProvider("prov-1", "OpenAI", "llm", "active");
        when(repository.save(provider)).thenReturn(Mono.just(provider));

        StepVerifier.create(repository.save(provider))
                .expectNextMatches(p -> "prov-1".equals(p.getId()) && "OpenAI".equals(p.getName()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnProvider_whenExists() {
        APIProvider provider = new APIProvider("prov-2", "Anthropic", "llm", "active");
        when(repository.findById("prov-2")).thenReturn(Mono.just(provider));

        StepVerifier.create(repository.findById("prov-2"))
                .expectNextMatches(p -> "Anthropic".equals(p.getName()))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllProviders() {
        APIProvider p1 = new APIProvider("prov-3", "OpenAI", "llm", "active");
        APIProvider p2 = new APIProvider("prov-4", "Google", "llm", "active");

        when(repository.findAll()).thenReturn(Flux.fromIterable(List.of(p1, p2)));

        StepVerifier.create(repository.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void deleteById_shouldRemoveProvider() {
        when(repository.deleteById("prov-delete")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById("prov-delete"))
                .verifyComplete();
    }

    @Test
    void delete_shouldRemoveProvider() {
        APIProvider provider = new APIProvider("prov-5", "Groq", "llm", "inactive");
        when(repository.delete(provider)).thenReturn(Mono.empty());

        StepVerifier.create(repository.delete(provider))
                .verifyComplete();
    }
}
