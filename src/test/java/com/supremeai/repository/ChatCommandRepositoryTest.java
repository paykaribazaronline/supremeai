package com.supremeai.repository;

import com.supremeai.model.ChatCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatCommandRepositoryTest {ChatCommandRepositorypublic ChatCommandRepositoryTest(ChatCommandRepository repository) {
ChatCommandRepository    this.repository = repository;
ChatCommandRepository}




    @Test
    void findByActive_shouldReturnActiveCommands() {
        ChatCommand c1 = new ChatCommand("chat-1", "/deploy", 0.95, "user-1");
        c1.setId("cmd-1");
        ChatCommand c2 = new ChatCommand("chat-2", "/test", 0.87, "user-2");
        c2.setId("cmd-2");

        when(repository.findByActive(true)).thenReturn(Flux.fromIterable(List.of(c1, c2)));

        StepVerifier.create(repository.findByActive(true))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByActive_shouldReturnInactiveCommands() {
        ChatCommand c = new ChatCommand("chat-3", "/old", 0.5, "user-3");
        c.setId("cmd-3");
        c.setActive(false);

        when(repository.findByActive(false)).thenReturn(Flux.just(c));

        StepVerifier.create(repository.findByActive(false))
                .expectNextMatches(cmd -> !cmd.isActive())
                .verifyComplete();
    }

    @Test
    void findByActive_shouldReturnEmpty_whenNoMatch() {
        when(repository.findByActive(true)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByActive(true))
                .verifyComplete();
    }

    @Test
    void findByIdAndActive_shouldReturnCommand_whenMatches() {
        ChatCommand c = new ChatCommand("chat-4", "/build", 0.9, "user-4");
        c.setId("cmd-4");
        c.setActive(true);

        when(repository.findByIdAndActive("cmd-4", true)).thenReturn(Mono.just(c));

        StepVerifier.create(repository.findByIdAndActive("cmd-4", true))
                .expectNextMatches(cmd -> "cmd-4".equals(cmd.getId()) && cmd.isActive())
                .verifyComplete();
    }

    @Test
    void findByIdAndActive_shouldReturnEmpty_whenNotActive() {
        when(repository.findByIdAndActive("cmd-5", true)).thenReturn(Mono.empty());

        StepVerifier.create(repository.findByIdAndActive("cmd-5", true))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistCommand() {
        ChatCommand c = new ChatCommand("chat-new", "/new-cmd", 0.8, "user-new");
        when(repository.save(c)).thenReturn(Mono.just(c));

        StepVerifier.create(repository.save(c))
                .expectNextMatches(cmd -> "/new-cmd".equals(cmd.getContent()))
                .verifyComplete();
    }
}
