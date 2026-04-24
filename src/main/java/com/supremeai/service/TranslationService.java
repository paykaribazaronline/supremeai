package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    /**
     * টেক্সট এক ভাষা থেকে অন্য ভাষায় অনুবাদ করে
     */
    public Mono<String> translate(String text, String fromLanguage, String toLanguage) {
        try {
            AIProvider provider = providerFactory.getProvider("groq");
            String prompt = String.format(
                    "Translate the following text from %s to %s. Return only the translated text without any explanation:\n\n%s",
                    fromLanguage, toLanguage, text
            );

            return Mono.fromCallable(() -> provider.generate(prompt))
                    .doOnSuccess(translated -> logger.debug("অনুবাদ সফল: {} -> {}", text, translated))
                    .doOnError(error -> logger.error("অনুবাদ ব্যর্থ: {}", error.getMessage()));
        } catch (Exception e) {
            logger.error("অনুবাদ সার্ভিস ত্রু라: {}", e.getMessage());
            return Mono.just(text); // অনুবাদ ব্যর্থ হলে মূল টেক্সট ফেরত দেয়
        }
    }

    /**
     * টেক্সট ইংরেজি থেকে নির্দিষ্ট ভাষায় অনুবাদ করে
     */
    public Mono<String> translateFromEnglish(String text, String toLanguage) {
        return translate(text, "English", toLanguage);
    }

    /**
     * টেক্সট নির্দিষ্ট ভাষা থেকে ইংরেজিতে অনুবাদ করে
     */
    public Mono<String> translateToEnglish(String text, String fromLanguage) {
        return translate(text, fromLanguage, "English");
    }
}