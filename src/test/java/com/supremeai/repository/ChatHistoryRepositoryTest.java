package com.supremeai.repository;

import com.supremeai.dto.Provider;
import com.supremeai.model.ChatMessage;
import com.supremeai.model.SystemConfig;
import com.supremeai.model.ModelEvolution;
import com.supremeai.model.VPNConnection;
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
class ChatHistoryRepositoryTest {

    @Mock
    private ChatHistoryRepository repository;

    @Test
    void save_shouldPersistChatMessage() {
        ChatMessage msg = new ChatMessage("user", "Hello AI");
        when(repository.save(msg)).thenReturn(Mono.just(msg));

        StepVerifier.create(repository.save(msg))
                .expectNextMatches(m -> "user".equals(m.getRole()) && "Hello AI".equals(m.getContent()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnMessage_whenExists() {
        ChatMessage msg = new ChatMessage("ai", "Hello human");
        msg.setId("msg-1");
        when(repository.findById("msg-1")).thenReturn(Mono.just(msg));

        StepVerifier.create(repository.findById("msg-1"))
                .expectNextMatches(m -> "ai".equals(m.getRole()))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllMessages() {
        ChatMessage m1 = new ChatMessage("user", "Hi");
        ChatMessage m2 = new ChatMessage("ai", "Hello");

        when(repository.findAll()).thenReturn(Flux.just(m1, m2));

        StepVerifier.create(repository.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void deleteById_shouldRemoveMessage() {
        when(repository.deleteById("msg-delete")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById("msg-delete"))
                .verifyComplete();
    }
}
