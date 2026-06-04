package com.supremeai.config;

import com.supremeai.service.GitHubAppService;
import com.supremeai.service.GitHubAutomationService;
import com.supremeai.service.MultiAIVotingService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
