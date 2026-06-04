package com.supremeai.repository;

import com.supremeai.model.ChatConfirmation;
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
class ChatConfirmationRepositoryTest {

    @Mock
    private ChatConfirmationRepository repository;

    @Test
    void findByItemId_shouldReturnConfirmationsForItem() {
        ChatConfirmation c1 = new ChatConfirmation("chat-1", "rule", "rule-1", true, "user-1");
        c1.setId("conf-1");
        ChatConfirmation c2 = new ChatConfirmation("chat-2", "rule", "rule-1", false, "user-2");
        c2.setId("conf-2");

        when(repository.findByItemId("rule-1")).thenReturn(Flux.fromIterable(List.of(c1, c2)));

        StepVerifier.create(repository.findByItemId("rule-1"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByItemId_shouldReturnEmpty_whenNotFound() {
        when(repository.findByItemId("no-item")).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByItemId("no-item"))
                .verifyComplete();
    }

    @Test
    void findByChatId_shouldReturnConfirmationsForChat() {
        ChatConfirmation c = new ChatConfirmation("chat-5", "plan", "plan-1", true, "user-3");
        c.setId("conf-3");

        when(repository.findByChatId("chat-5")).thenReturn(Flux.just(c));

        StepVerifier.create(repository.findByChatId("chat-5"))
                .expectNextMatches(conf -> "chat-5".equals(conf.getChatId()))
                .verifyComplete();
    }

    @Test
    void findByChatId_shouldReturnEmpty_whenNotFound() {
        when(repository.findByChatId("no-chat")).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByChatId("no-chat"))
                .verifyComplete();
    }

    @Test
    void findByItemTypeAndItemId_shouldReturnMatchingConfirmations() {
        ChatConfirmation c = new ChatConfirmation("chat-6", "command", "cmd-1", true, "user-4");
        c.setId("conf-4");

        when(repository.findByItemTypeAndItemId("command", "cmd-1")).thenReturn(Flux.just(c));

        StepVerifier.create(repository.findByItemTypeAndItemId("command", "cmd-1"))
                .expectNextMatches(conf -> "command".equals(conf.getItemType()) && "cmd-1".equals(conf.getItemId()))
                .verifyComplete();
    }

    @Test
    void findByItemTypeAndItemId_shouldReturnEmpty_whenNoMatch() {
        when(repository.findByItemTypeAndItemId("rule", "nonexistent")).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByItemTypeAndItemId("rule", "nonexistent"))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistConfirmation() {
        ChatConfirmation c = new ChatConfirmation("chat-new", "plan", "plan-new", true, "user-new");
        when(repository.save(c)).thenReturn(Mono.just(c));

        StepVerifier.create(repository.save(c))
                .expectNextMatches(conf -> "plan".equals(conf.getItemType()))
                .verifyComplete();
    }
}
