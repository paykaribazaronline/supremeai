package com.supremeai.config;

import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class BeanConfiguration {

  @Bean(name = "votingTaskExecutor")
  public java.util.concurrent.ExecutorService votingTaskExecutor() {
    // Use virtual threads for highly concurrent voting tasks
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean(name = "batchTaskExecutor")
  public java.util.concurrent.ExecutorService batchTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean(name = "analysisTaskExecutor")
  public java.util.concurrent.ExecutorService analysisTaskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
