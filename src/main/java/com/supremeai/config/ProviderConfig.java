package com.supremeai.config;

import com.supremeai.ai.provider.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ProviderConfig {

    @Bean
    @ConfigurationProperties(prefix = "supremeai.providers")
    public ProviderProperties providerProperties() {
        return new ProviderProperties();
    }

    @Bean
    public List<AIProvider> aiProviders(ProviderProperties properties) {
        List<AIProvider> providers = new ArrayList<>();

        if (StringUtils.hasText(properties.getOpenai())) {
            providers.add(new OpenAIProvider(properties.getOpenai()));
            log.info("Registered OpenAI provider");
        }
        if (StringUtils.hasText(properties.getGemini())) {
            providers.add(new GeminiProvider(properties.getGemini()));
            log.info("Registered Gemini provider");
        }
        if (StringUtils.hasText(properties.getAnthropic())) {
            providers.add(new ClaudeProvider(properties.getAnthropic()));
            log.info("Registered Claude provider");
        }
        if (StringUtils.hasText(properties.getCohere())) {
            providers.add(new CohereProvider(properties.getCohere()));
            log.info("Registered Cohere provider");
        }
        if (StringUtils.hasText(properties.getPerplexity())) {
            providers.add(new PerplexityProvider(properties.getPerplexity()));
            log.info("Registered Perplexity provider");
        }
        if (StringUtils.hasText(properties.getMistral())) {
            providers.add(new MistralProvider(properties.getMistral()));
            log.info("Registered Mistral provider");
        }
        if (StringUtils.hasText(properties.getLlama())) {
            providers.add(new LlamaProvider(properties.getLlama()));
            log.info("Registered Llama provider");
        }
        if (StringUtils.hasText(properties.getDeepseek())) {
            providers.add(new DeepSeekProvider(properties.getDeepseek()));
            log.info("Registered DeepSeek provider");
        }
        if (StringUtils.hasText(properties.getGrok())) {
            providers.add(new GrokProvider(properties.getGrok()));
            log.info("Registered Grok provider");
        }
        if (StringUtils.hasText(properties.getCustomSupremeAi())) {
            providers.add(new CustomSupremeAIProvider(properties.getCustomSupremeAi()));
            log.info("Registered Custom Supreme AI provider");
        }

        if (providers.isEmpty()) {
            log.warn("No AI providers configured! Voting system will not function.");
        }

        return providers;
    }

    public static class ProviderProperties {
        private String openai;
        private String gemini;
        private String anthropic;
        private String cohere;
        private String perplexity;
        private String mistral;
        private String llama;
        private String deepseek;
        private String grok;
        private String customSupremeAi;

        public String getOpenai() { return openai; }
        public void setOpenai(String openai) { this.openai = openai; }
        public String getGemini() { return gemini; }
        public void setGemini(String gemini) { this.gemini = gemini; }
        public String getAnthropic() { return anthropic; }
        public void setAnthropic(String anthropic) { this.anthropic = anthropic; }
        public String getCohere() { return cohere; }
        public void setCohere(String cohere) { this.cohere = cohere; }
        public String getPerplexity() { return perplexity; }
        public void setPerplexity(String perplexity) { this.perplexity = perplexity; }
        public String getMistral() { return mistral; }
        public void setMistral(String mistral) { this.mistral = mistral; }
        public String getLlama() { return llama; }
        public void setLlama(String llama) { this.llama = llama; }
        public String getDeepseek() { return deepseek; }
        public void setDeepseek(String deepseek) { this.deepseek = deepseek; }
        public String getGrok() { return grok; }
        public void setGrok(String grok) { this.grok = grok; }
        public String getCustomSupremeAi() { return customSupremeAi; }
        public void setCustomSupremeAi(String customSupremeAi) { this.customSupremeAi = customSupremeAi; }
    }
}
