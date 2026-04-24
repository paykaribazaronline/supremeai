package com.supremeai.controller;

import com.supremeai.dto.LanguagePreference;
import com.supremeai.dto.TranslationRequest;
import com.supremeai.dto.TranslationResponse;
import com.supremeai.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/translation")
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    /**
     * টেক্সট অনুবাদ করে
     */
    @PostMapping("/translate")
    public Mono<ResponseEntity<TranslationResponse>> translate(@RequestBody TranslationRequest request) {
        return translationService.translate(request.getText(), request.getFromLanguage(), request.getToLanguage())
                .map(translatedText -> ResponseEntity.ok(new TranslationResponse(translatedText, true)))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * টেক্সট ইংরেজি থেকে নির্দিষ্ট ভাষায় অনুবাদ করে
     */
    @PostMapping("/translate-from-english")
    public Mono<ResponseEntity<TranslationResponse>> translateFromEnglish(@RequestBody TranslationRequest request) {
        return translationService.translateFromEnglish(request.getText(), request.getToLanguage())
                .map(translatedText -> ResponseEntity.ok(new TranslationResponse(translatedText, true)))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * টেক্সট নির্দিষ্ট ভাষা থেকে ইংরেজিতে অনুবাদ করে
     */
    @PostMapping("/translate-to-english")
    public Mono<ResponseEntity<TranslationResponse>> translateToEnglish(@RequestBody TranslationRequest request) {
        return translationService.translateToEnglish(request.getText(), request.getFromLanguage())
                .map(translatedText -> ResponseEntity.ok(new TranslationResponse(translatedText, true)))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * সমর্থিত ভাষার তালিকা প্রদান করে
     */
    @GetMapping("/languages")
    public Mono<ResponseEntity<LanguagePreference[]>> getSupportedLanguages() {
        return Mono.just(ResponseEntity.ok(LanguagePreference.values()));
    }
}