package com.supremeai.mcp;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** MCP Server Implementation - Plan 24 Phase 1 Implements actual MCP protocol endpoints */
@RestController
@RequestMapping("/mcp")
public class MCPServerController {

  /** MCP tools/list - List all available tools */
  @PostMapping(value = "/tools/list", consumes = "application/json", produces = "application/json")
  public Map<String, Object> listTools() {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> tools = new ArrayList<>();

    // Register Reverse Engineer tool (Plan 23)
    tools.add(
        createTool(
            "reverse_engineer",
            "Reverse engineer any website and generate API connector",
            Map.of(
                "url", Map.of("type", "string", "description", "Target website URL"),
                "credentials",
                    Map.of(
                        "type",
                        "object",
                        "description",
                        "Login credentials",
                        "properties",
                        Map.of(
                            "email", Map.of("type", "string"),
                            "password", Map.of("type", "string")))),
            List.of("url")));

    // Register Dynamic Agent tool (Plan 1)
    tools.add(
        createTool(
            "dynamic_agent",
            "Execute task with dynamic AI agent selection",
            Map.of(
                "task", Map.of("type", "string", "description", "Task to execute"),
                "language", Map.of("type", "string", "description", "Language code (en/bn)")),
            List.of("task")));

    response.put("tools", tools);
    return response;
  }

  /** MCP tools/call - Execute a tool */
  @SuppressWarnings("unchecked")
  @PostMapping(value = "/tools/call", consumes = "application/json", produces = "application/json")
  public Map<String, Object> callTool(@RequestBody Map<String, Object> request) {
    String toolName = (String) request.get("name");
    Map<String, Object> args = (Map<String, Object>) request.get("arguments");

    Map<String, Object> response = new HashMap<>();

    if ("reverse_engineer".equals(toolName)) {
      return executeReverseEngineer(args);
    } else if ("dynamic_agent".equals(toolName)) {
      return executeDynamicAgent(args);
    } else {
      response.put("error", "Tool not found: " + toolName);
      return response;
    }
  }

  @Autowired private PythonBridge pythonBridge;

  @SuppressWarnings("unchecked")
  private Map<String, Object> executeReverseEngineer(Map<String, Object> args) {
    String url = (String) args.get("url");
    Map<String, Object> credentials = (Map<String, Object>) args.get("credentials");

    // Call Python reverse_engineer
    return pythonBridge.callReverseEngineer(url, credentials);
  }

  @Autowired private com.supremeai.agentorchestration.AdaptiveAgentOrchestrator orchestrator;

  private Map<String, Object> executeDynamicAgent(Map<String, Object> args) {
    Map<String, Object> response = new HashMap<>();
    String task = (String) args.get("task");

    com.supremeai.agentorchestration.OrchesResultContext result = orchestrator.orchestrate(task);
    response.put("status", result.getStatus());
    response.put("result", "Executed: " + task);
    response.put("details", result.getContext());
    return response;
  }

  private Map<String, Object> createTool(
      String name, String desc, Map<String, Object> props, List<String> required) {
    Map<String, Object> tool = new HashMap<>();
    tool.put("name", name);
    tool.put("description", desc);
    tool.put(
        "inputSchema",
        Map.of(
            "type", "object",
            "properties", props,
            "required", required));
    return tool;
  }

  /** MCP resources/list - List available resources */
  @PostMapping(
      value = "/resources/list",
      consumes = "application/json",
      produces = "application/json")
  public Map<String, Object> listResources() {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> resources = new ArrayList<>();

    resources.add(
        Map.of(
            "uri", "supremeai://plans/23",
            "name", "Plan 23 - Reverse Engineering Guide",
            "mimeType", "text/markdown"));

    resources.add(
        Map.of(
            "uri", "supremeai://plans/24",
            "name", "Plan 24 - AI Agent Ecosystem",
            "mimeType", "text/markdown"));

    response.put("resources", resources);
    return response;
  }

  /** MCP resources/read - Read resource content */
  @PostMapping(
      value = "/resources/read",
      consumes = "application/json",
      produces = "application/json")
  public Map<String, Object> readResource(@RequestBody Map<String, Object> request) {
    Map<String, Object> response = new HashMap<>();
    String uri = (String) request.get("uri");

    List<Map<String, Object>> contents = new ArrayList<>();

    try {
      if ("supremeai://plans/23".equals(uri)) {
        String path =
            "/home/nazifarabbu/supremeai/final_document/main plan/Plan_23_Website_Reverse_Engineering_Master_Guide.md";
        String content = Files.readString(Paths.get(path));
        contents.add(
            Map.of(
                "uri", uri,
                "mimeType", "text/markdown",
                "text", content));
      } else if ("supremeai://plans/24".equals(uri)) {
        String path =
            "/home/nazifarabbu/supremeai/final_document/main plan/Plan_24_AI_Agent_Ecosystem_Integration.md";
        String content = Files.readString(Paths.get(path));
        contents.add(
            Map.of(
                "uri", uri,
                "mimeType", "text/markdown",
                "text", content));
      } else {
        response.put("error", "Resource not found: " + uri);
        return response;
      }
    } catch (Exception e) {
      response.put("error", "Error reading resource: " + e.getMessage());
      return response;
    }

    response.put("contents", contents);
    return response;
  }

  /** MCP prompts/list - List available prompt templates */
  @PostMapping(
      value = "/prompts/list",
      consumes = "application/json",
      produces = "application/json")
  public Map<String, Object> listPrompts() {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> prompts = new ArrayList<>();

    prompts.add(
        Map.of(
            "name", "reverse_engineer_prompt",
            "description", "Template for reverse engineering request",
            "arguments",
                List.of(
                    Map.of(
                        "name", "url",
                        "description", "Website URL to analyze",
                        "required", true))));

    response.put("prompts", prompts);
    return response;
  }

  /** MCP prompts/get - Get prompt content */
  @PostMapping(value = "/prompts/get", consumes = "application/json", produces = "application/json")
  public Map<String, Object> getPrompt(@RequestBody Map<String, Object> request) {
    Map<String, Object> response = new HashMap<>();
    String name = (String) request.get("name");
    Map<String, Object> args = (Map<String, Object>) request.get("arguments");

    if ("reverse_engineer_prompt".equals(name)) {
      String url = args != null ? (String) args.get("url") : "";

      response.put("description", "Template for reverse engineering request");
      response.put(
          "messages",
          List.of(
              Map.of(
                  "role",
                  "user",
                  "content",
                  Map.of(
                      "type",
                      "text",
                      "text",
                      "Please reverse engineer the following website and generate a connector: "
                          + url))));
    } else {
      response.put("error", "Prompt not found: " + name);
    }

    return response;
  }
}
