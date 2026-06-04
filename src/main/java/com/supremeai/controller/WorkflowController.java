package com.supremeai.controller;

import com.supremeai.model.WorkflowDefinition;
import com.supremeai.model.WorkflowExecution;
import com.supremeai.repository.WorkflowDefinitionRepository;
import com.supremeai.repository.WorkflowExecutionRepository;
import com.supremeai.service.WorkflowOrchestrationService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

  @Autowired private WorkflowOrchestrationService workflowService;

  @Autowired private WorkflowDefinitionRepository definitionRepository;

  @Autowired private WorkflowExecutionRepository executionRepository;

  @PostMapping("/definitions")
  public Mono<WorkflowDefinition> createDefinition(@RequestBody WorkflowDefinition definition) {
    return definitionRepository.save(definition);
  }

  @GetMapping("/definitions")
  public Flux<WorkflowDefinition> getAllDefinitions() {
    return definitionRepository.findAll();
  }

  @PostMapping("/execute")
  public Mono<WorkflowExecution> executeWorkflow(@RequestBody Map<String, Object> request) {
    String definitionId = (String) request.get("definitionId");
    Map<String, Object> inputs = (Map<String, Object>) request.getOrDefault("inputs", Map.of());
    return workflowService.startWorkflow(definitionId, inputs);
  }

  @GetMapping("/execution/{id}")
  public Mono<WorkflowExecution> getExecutionStatus(@PathVariable String id) {
    return executionRepository.findById(id);
  }
}
