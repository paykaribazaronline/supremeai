package com.supremeai.controller;

import com.supremeai.model.ChatSession;
import com.supremeai.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat/sessions")
public class ChatSessionController {

  @Autowired private ChatSessionService chatSessionService;

  @PostMapping
  public Mono<ResponseEntity<ChatSession>> saveSession(@RequestBody ChatSession session) {
    return chatSessionService
        .saveSession(session)
        .map(ResponseEntity::ok)
        .onErrorReturn(ResponseEntity.badRequest().build());
  }

  @GetMapping("/user/{userId}")
  public Flux<ChatSession> getUserSessions(@PathVariable String userId) {
    return chatSessionService.getSessionsByUserId(userId);
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<ChatSession>> getSession(@PathVariable String id) {
    return chatSessionService
        .getSession(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteSession(@PathVariable String id) {
    return chatSessionService.deleteSession(id).then(Mono.just(ResponseEntity.ok().<Void>build()));
  }

  @DeleteMapping("/user/{userId}")
  public Mono<ResponseEntity<Void>> clearUserSessions(@PathVariable String userId) {
    return chatSessionService
        .deleteAllSessionsByUserId(userId)
        .then(Mono.just(ResponseEntity.ok().<Void>build()));
  }
}
