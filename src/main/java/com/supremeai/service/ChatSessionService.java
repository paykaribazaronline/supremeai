package com.supremeai.service;

import com.supremeai.model.ChatSession;
import com.supremeai.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    public Mono<ChatSession> saveSession(ChatSession session) {
        return chatSessionRepository.save(session);
    }

    public Mono<ChatSession> getSession(String id) {
        return chatSessionRepository.findById(id);
    }

    public Flux<ChatSession> getSessionsByUserId(String userId) {
        return chatSessionRepository.findAllByUserId(userId);
    }

    public Mono<Void> deleteSession(String id) {
        return chatSessionRepository.deleteById(id);
    }

    public Mono<Void> deleteAllSessionsByUserId(String userId) {
        return chatSessionRepository.deleteAllByUserId(userId);
    }
}
