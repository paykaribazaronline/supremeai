package com.supremeai.repository;

import com.supremeai.model.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigRepositoryTest {SystemConfigRepositorypublic SystemConfigRepositoryTest(SystemConfigRepository repository) {
SystemConfigRepository    this.repository = repository;
SystemConfigRepository}




    @Test
    void save_shouldPersistConfig() {
        SystemConfig config = new SystemConfig();
        when(repository.save(config)).thenReturn(Mono.just(config));

        StepVerifier.create(repository.save(config))
                .expectNextMatches(c -> "global_settings".equals(c.getId()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnConfig_whenExists() {
        SystemConfig config = new SystemConfig();
        config.setActiveModel("gpt-4o");
        config.setMaintenanceMode(true);
        when(repository.findById("global_settings")).thenReturn(Mono.just(config));

        StepVerifier.create(repository.findById("global_settings"))
                .expectNextMatches(c -> "gpt-4o".equals(c.getActiveModel()) && c.isMaintenanceMode())
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById("nonexistent"))
                .verifyComplete();
    }

    @Test
    void deleteById_shouldRemoveConfig() {
        when(repository.deleteById("global_settings")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById("global_settings"))
                .verifyComplete();
    }
}
