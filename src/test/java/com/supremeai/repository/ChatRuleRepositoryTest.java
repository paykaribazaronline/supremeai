package com.supremeai.repository;

import com.supremeai.model.ChatRule;
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
class ChatRuleRepositoryTest {

    @Mock
    private ChatRuleRepository repository;

    @Test
    void findByActive_shouldReturnActiveRules() {
        ChatRule r1 = new ChatRule("chat-1", "Use TypeScript", 0.99, "ai");
        r1.setId("rule-1");
        ChatRule r2 = new ChatRule("chat-2", "Write tests", 0.95, "ai");
        r2.setId("rule-2");

        when(repository.findByActive(true)).thenReturn(Flux.fromIterable(List.of(r1, r2)));

        StepVerifier.create(repository.findByActive(true))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByActive_shouldReturnEmpty_whenNoneActive() {
        when(repository.findByActive(true)).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByActive(true))
                .verifyComplete();
    }

    @Test
    void findByIdAndActive_shouldReturnRule_whenActive() {
        ChatRule r = new ChatRule("chat-3", "Rule content", 0.85, "human");
        r.setId("rule-3");
        r.setActive(true);

        when(repository.findByIdAndActive("rule-3", true)).thenReturn(Mono.just(r));

        StepVerifier.create(repository.findByIdAndActive("rule-3", true))
                .expectNextMatches(rule -> "rule-3".equals(rule.getId()) && rule.isActive())
                .verifyComplete();
    }

    @Test
    void findByIdAndActive_shouldReturnEmpty_whenNotActive() {
        when(repository.findByIdAndActive("rule-4", true)).thenReturn(Mono.empty());

        StepVerifier.create(repository.findByIdAndActive("rule-4", true))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistRule() {
        ChatRule r = new ChatRule("chat-new", "New rule", 0.9, "ai");
        when(repository.save(r)).thenReturn(Mono.just(r));

        StepVerifier.create(repository.save(r))
                .expectNextMatches(rule -> "New rule".equals(rule.getContent()))
                .verifyComplete();
    }
}
