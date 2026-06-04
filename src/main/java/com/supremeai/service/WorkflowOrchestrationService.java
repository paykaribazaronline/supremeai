package com.supremeai.service;

import com.supremeai.model.WorkflowDefinition;
import com.supremeai.model.WorkflowExecution;
import com.supremeai.model.WorkflowStep;
import com.supremeai.repository.WorkflowDefinitionRepository;
import com.supremeai.repository.WorkflowExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkflowOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrationService.class);

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private AgentOrchestrationHub agentHub;

    /**
     * Starts a workflow execution.
     */
    public Mono<WorkflowExecution> startWorkflow(String definitionId, Map<String, Object> initialInputs) {
        return definitionRepository.findById(definitionId)
                .flatMap(definition -> {
                    String executionId = UUID.randomUUID().toString();
                    WorkflowExecution execution = new WorkflowExecution(executionId, definitionId, "RUNNING");
                    execution.setStepResults(new HashMap<>(initialInputs));
                    execution.setCurrentStepIndex(0);
                    
                    return executionRepository.save(execution)
                            .flatMap(savedExecution -> executeNextStep(savedExecution, definition));
                });
    }

    /**
     * Executes the next step in the workflow.
     */
    private Mono<WorkflowExecution> executeNextStep(WorkflowExecution execution, WorkflowDefinition definition) {
        if (execution.getCurrentStepIndex() >= definition.getSteps().size()) {
            execution.setStatus("COMPLETED");
            execution.setCompletedAt(new Date());
            return executionRepository.save(execution);
        }

        WorkflowStep step = definition.getSteps().get(execution.getCurrentStepIndex());
        logger.info("[Workflow] Executing step {}: {}", execution.getCurrentStepIndex(), step.getAgent());

        // Resolve parameters from previous steps
        Map<String, Object> resolvedInputs = resolveParams(step.getInput(), execution.getStepResults());

        return agentHub.executeAgent(step.getAgent(), resolvedInputs)
                .flatMap(result -> {
                    // Store the result under the specified output key
                    if (step.getOutput() != null) {
                        execution.getStepResults().put(step.getOutput(), result);
                    }
                    
                    execution.setCurrentStepIndex(execution.getCurrentStepIndex() + 1);
                    return executionRepository.save(execution)
                            .flatMap(updatedExecution -> executeNextStep(updatedExecution, definition));
                })
                .onErrorResume(e -> {
                    logger.error("[Workflow] Step failed: {}", e.getMessage());
                    execution.setStatus("FAILED");
                    execution.setErrorMessage(e.getMessage());
                    execution.setCompletedAt(new Date());
                    return executionRepository.save(execution);
                });
    }

    /**
     * Resolves Jinja-like parameters: {{ step_name.key }}
     */
    private Map<String, Object> resolveParams(Map<String, Object> inputs, Map<String, Object> results) {
        Map<String, Object> resolved = new HashMap<>();
        Pattern pattern = Pattern.compile("\\{\\{\\s*([^\\s}]+)\\s*\\}\\}");

        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strValue) {
                Matcher matcher = pattern.matcher(strValue);
                if (matcher.find()) {
                    String path = matcher.group(1);
                    Object resolvedValue = getValueFromResults(path, results);
                    resolved.put(entry.getKey(), resolvedValue != null ? resolvedValue : strValue);
                } else {
                    resolved.put(entry.getKey(), value);
                }
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private Object getValueFromResults(String path, Map<String, Object> results) {
        String[] parts = path.split("\\.");
        Object current = results;
        
        for (String part : parts) {
            if (current instanceof Map map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
