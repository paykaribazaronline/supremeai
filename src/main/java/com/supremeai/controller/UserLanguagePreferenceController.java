package com.supremeai.controller;

import com.supremeai.model.UserLanguagePreference;
import com.supremeai.service.UserLanguagePreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/user/language-preference")
public class UserLanguagePreferenceController {

    @Autowired
    private UserLanguagePreferenceService languagePreferenceService;

    /**
     * ব্যবহারকারীর ভাষা পছন্দ সংরক্ষণ করে
     */
    @PostMapping
    public Mono<ResponseEntity<UserLanguagePreference>> saveLanguagePreference(@RequestBody UserLanguagePreference preference) {
        return languagePreferenceService.saveUserLanguagePreference(preference)
                .map(saved -> ResponseEntity.ok(saved))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * ব্যবহারকারী আইডি দিয়ে ভাষা পছন্দ খুঁজে বের করে
     */
    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserLanguagePreference>> getLanguagePreference(@PathVariable String userId) {
        return languagePreferenceService.getUserLanguagePreference(userId)
                .map(preference -> ResponseEntity.ok(preference))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * ব্যবহারকারীর ভাষা পছন্দ আপডেট করে
     */
    @PutMapping("/{userId}")
    public Mono<ResponseEntity<UserLanguagePreference>> updateLanguagePreference(
            @PathVariable String userId,
            @RequestParam String languageCode,
            @RequestParam String languageName) {
        return languagePreferenceService.updateUserLanguagePreference(userId, languageCode, languageName)
                .map(updated -> ResponseEntity.ok(updated))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * সব ব্যবহারকারীর ভাষা পছন্দ পায়
     */
    @GetMapping
    public Flux<UserLanguagePreference> getAllLanguagePreferences() {
        return languagePreferenceService.getAllUserLanguagePreferences();
    }
}