package com.supremeai.service;

import com.supremeai.model.UserLanguagePreference;
import com.supremeai.repository.UserLanguagePreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserLanguagePreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserLanguagePreferenceService.class);

    @Autowired
    private UserLanguagePreferenceRepository repository;

    /**
     * ব্যবহারকারীর ভাষা পছন্দ সংরক্ষণ করে
     */
    public Mono<UserLanguagePreference> saveUserLanguagePreference(UserLanguagePreference preference) {
        preference.updateTimestamp();
        return repository.save(preference)
                .doOnSuccess(saved -> logger.info("ভাষা পছন্দ সংরক্ষিত: {} for user: {}",
                        saved.getLanguageName(), saved.getUserId()))
                .doOnError(error -> logger.error("ভাষা পছন্দ সংরক্ষণে ব্যর্থ: {}", error.getMessage()));
    }

    /**
     * ব্যবহারকারী আইডি দিয়ে ভাষা পছন্দ খুঁজে বের করে
     */
    public Mono<UserLanguagePreference> getUserLanguagePreference(String userId) {
        return repository.findByUserId(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    // ডিফল্ট ভাষা পছন্দ তৈরি করা
                    UserLanguagePreference defaultPreference = new UserLanguagePreference(userId, "en", "English");
                    return repository.save(defaultPreference);
                }))
                .doOnError(error -> logger.error("ভাষা পছন্দ পুনরুদ্ধারে ব্যর্থ: {}", error.getMessage()));
    }

    /**
     * ব্যবহারকারীর ভাষা পছন্দ আপডেট করে
     */
    public Mono<UserLanguagePreference> updateUserLanguagePreference(String userId, String languageCode, String languageName) {
        return getUserLanguagePreference(userId)
                .flatMap(preference -> {
                    preference.setLanguageCode(languageCode);
                    preference.setLanguageName(languageName);
                    preference.updateTimestamp();
                    return repository.save(preference);
                })
                .doOnSuccess(updated -> logger.info("ভাষা পছন্দ আপডেট করা হয়েছে: {} for user: {}",
                        updated.getLanguageName(), updated.getUserId()))
                .doOnError(error -> logger.error("ভাষা পছন্দ আপডেটে ব্যর্থ: {}", error.getMessage()));
    }

    /**
     * সব ব্যবহারকারীর ভাষা পছন্দ পায়
     */
    public Flux<UserLanguagePreference> getAllUserLanguagePreferences() {
        return repository.findAll()
                .doOnError(error -> logger.error("সব ভাষা পছন্দ পুনরুদ্ধারে ব্যর্থ: {}", error.getMessage()));
    }
}