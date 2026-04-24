package com.supremeai.config;

import com.supremeai.provider.*;
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
            providers.add(new AnthropicProvider(properties.getAnthropic()));
            log.info("Registered Anthropic/Claude provider");
        }
        if (StringUtils.hasText(properties.getMistral())) {
            providers.add(new MistralProvider(properties.getMistral()));
            log.info("Registered Mistral provider");
        }
        if (StringUtils.hasText(properties.getDeepseek())) {
            providers.add(new DeepSeekProvider(properties.getDeepseek()));
            log.info("Registered DeepSeek provider");
        }
        if (StringUtils.hasText(properties.getGroq())) {
            providers.add(new GroqProvider(properties.getGroq()));
            log.info("Registered Groq provider");
        }
        if (StringUtils.hasText(properties.getKimi())) {
            providers.add(new KimiProvider(properties.getKimi()));
            log.info("Registered Kimi provider");
        }
        if (StringUtils.hasText(properties.getAirllm())) {
            providers.add(new AirLLMProvider("https://airllm-endpoint/v1/chat/completions", properties.getAirllm(), "mistralai/Mistral-7B-Instruct-v0.3"));
            log.info("Registered AirLLM provider");
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
        private String groq;
        private String mistral;
        private String deepseek;
        private String kimi;
        private String airllm;

        public String getOpenai() { return openai; }
        public void setOpenai(String openai) { this.openai = openai; }
        public String getGemini() { return gemini; }
        public void setGemini(String gemini) { this.gemini = gemini; }
        public String getAnthropic() { return anthropic; }
        public void setAnthropic(String anthropic) { this.anthropic = anthropic; }
        public String getGroq() { return groq; }
        public void setGroq(String groq) { this.groq = groq; }
        public String getMistral() { return mistral; }
        public void setMistral(String mistral) { this.mistral = mistral; }
        public String getDeepseek() { return deepseek; }
        public void setDeepseek(String deepseek) { this.deepseek = deepseek; }
        public String getKimi() { return kimi; }
        public void setKimi(String kimi) { this.kimi = kimi; }
        public String getAirllm() { return airllm; }
        public void setAirllm(String airllm) { this.airllm = airllm; }
    }
}
