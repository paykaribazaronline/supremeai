package com.supremeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.service.GitHubAppService;
import com.supremeai.service.GitHubAutomationService;

@Configuration
public class GlobalMockConfig {

    @Bean
    @Primary
    public MultiAIVotingService multiAIVotingService() {
        return Mockito.mock(MultiAIVotingService.class);
    }

    @Bean
    @Primary
    public GitHubAppService gitHubAppService() {
        return Mockito.mock(GitHubAppService.class);
    }

    @Bean
    @Primary
    public GitHubAutomationService gitHubAutomationService() {
        return Mockito.mock(GitHubAutomationService.class);
    }
}