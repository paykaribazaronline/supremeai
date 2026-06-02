package com.supremeai.config;

import com.supremeai.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedDataValidatorTest {SeedDataValidatorpublic SeedDataValidatorTest(SeedDataValidator validator) {
SeedDataValidator    this.validator = validator;
SeedDataValidator}


    @Mock private UserRepository userRepository;
    @Mock private ProviderRepository apiProviderRepository;
    @Mock private UserTierRepository userTierRepository;
    @Mock private KnowledgeDomainRepository knowledgeDomainRepository;
    @Mock private SystemLearningRepository systemLearningRepository;
    @Mock private KnowledgeEntryRepository knowledgeEntryRepository;
    @Mock private ProviderTaskPerformanceRepository perfRepository;
    @Mock private WorkflowDefinitionRepository workflowRepository;

    @InjectMocks


    @BeforeEach
    void setUp() {
        when(userRepository.count()).thenReturn(Mono.just(5L));
        when(apiProviderRepository.count()).thenReturn(Mono.just(5L));
        when(userTierRepository.count()).thenReturn(Mono.just(3L));
        when(knowledgeDomainRepository.count()).thenReturn(Mono.just(5L));
        when(systemLearningRepository.count()).thenReturn(Mono.just(5L));
        when(knowledgeEntryRepository.count()).thenReturn(Mono.just(5L));
        when(perfRepository.count()).thenReturn(Mono.just(4L));
        when(workflowRepository.count()).thenReturn(Mono.just(1L));
    }

    @Test
    void run_allValid_shouldNotThrowException() throws Exception {
        validator.run();
        verify(userRepository).count();
        verify(apiProviderRepository).count();
        verify(workflowRepository).count();
    }

    @Test
    void run_missingData_shouldLogWarningsButNotCrash() throws Exception {
        when(userRepository.count()).thenReturn(Mono.just(0L)); 
        when(apiProviderRepository.count()).thenReturn(Mono.just(0L)); 

        validator.run();
        verify(systemLearningRepository).count();
    }
}