import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class UpdateAIService {
    public static void main(String[] args) throws Exception {
        String path = "src/main/java/org/example/service/AIAPIService.java";
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        
        content = content.replace("\"GPT4\", \"CLAUDE\", \"GROQ\", \"DEEPSEEK\", \"GEMINI\", \"COHERE\", \"PERPLEXITY\", \"LLAMA\", \"HUGGINGFACE\", \"XAI\", \"AIRLLM\"", "\"GPT4\", \"CLAUDE\", \"GROQ\", \"DEEPSEEK\", \"GEMINI\", \"COHERE\", \"PERPLEXITY\", \"LLAMA\", \"HUGGINGFACE\", \"XAI\", \"AIRLLM\", \"OLLAMA\"");
        
        content = content.replace("Map.entry(\"AIRLLM\", \"https://unsymmetrical-unrepugnant-lilah.ngrok-free.dev/v1/chat/completions\")", "Map.entry(\"AIRLLM\", \"https://unsymmetrical-unrepugnant-lilah.ngrok-free.dev/v1/chat/completions\"),\n        Map.entry(\"OLLAMA\", \"http://localhost:11434/api/chat\")");

        content = content.replace("Map.entry(\"AIRLLM\", \"mistralai/Mistral-7B-Instruct-v0.3\")", "Map.entry(\"AIRLLM\", \"mistralai/Mistral-7B-Instruct-v0.3\"),\n        Map.entry(\"OLLAMA\", \"llama3.2:70b\")");

        content = content.replace("case \"AIRLLM\":\n                return callOpenAICompatible(endpoint, apiKey, defaultModels.get(\"AIRLLM\"), prompt, Collections.emptyMap());", "case \"AIRLLM\":\n                return callOpenAICompatible(endpoint, apiKey, defaultModels.get(\"AIRLLM\"), prompt, Collections.emptyMap());\n            case \"OLLAMA\":\n                return callOllama(endpoint, defaultModels.get(\"OLLAMA\"), prompt);");

        if (!content.contains("callOllama(")) {
            String ollamaMethod = "\n    private String callOllama(String endpoint, String model, String prompt) throws IOException {\n        var root = mapper.createObjectNode();\n        root.put(\"model\", model);\n        var messages = root.putArray(\"messages\");\n        var msg = mapper.createObjectNode();\n        msg.put(\"role\", \"user\");\n        msg.put(\"content\", prompt);\n        messages.add(msg);\n        root.put(\"stream\", false);\n        \n        Request request = new Request.Builder()\n            .url(endpoint)\n            .post(RequestBody.create(mapper.writeValueAsString(root), MediaType.parse(\"application/json\")))\n            .build();\n            \n        try (Response response = client.newCall(request).execute()) {\n            if (!response.isSuccessful()) throw new IOException(\"Unexpected code \" + response);\n            JsonNode jsonResponse = mapper.readTree(response.body().string());\n            return jsonResponse.at(\"/message/content\").asText();\n        }\n    }\n";
            content = content.replace("private String callGemini(", ollamaMethod + "\n    private String callGemini(");
        }
        
        content = content.replace("if (normalized.contains(\"airllm\")) {\n            return \"airllm-local\";\n        }", "if (normalized.contains(\"airllm\")) {\n            return \"airllm-local\";\n        }\n        if (normalized.contains(\"ollama\")) {\n            return \"ollama-local\";\n        }");
        
        content = content.replace("case \"airllm-local\":\n                return \"AirLLM Local\";", "case \"airllm-local\":\n                return \"AirLLM Local\";\n            case \"ollama-local\":\n                return \"Ollama Local\";");
        
        content = content.replace("aliases.put(\"local-airllm\", \"AIRLLM\");", "aliases.put(\"local-airllm\", \"AIRLLM\");\n        aliases.put(\"ollama\", \"OLLAMA\");\n        aliases.put(\"ollama-local\", \"OLLAMA\");\n        aliases.put(\"local-ollama\", \"OLLAMA\");");
        
        content = content.replace("fallbacks.put(\"airllm-local\", buildChain(\"AIRLLM\", Arrays.asList(\"DEEPSEEK\", \"GROQ\", \"CLAUDE\", \"GPT4\")));", "fallbacks.put(\"airllm-local\", buildChain(\"AIRLLM\", Arrays.asList(\"DEEPSEEK\", \"GROQ\", \"CLAUDE\", \"GPT4\")));\n        fallbacks.put(\"ollama-local\", buildChain(\"OLLAMA\", Arrays.asList(\"DEEPSEEK\", \"GROQ\", \"CLAUDE\", \"GPT4\")));");
        
        content = content.replace("if (lower.contains(\"mistral\"))  return \"mistral-small-latest\";", "if (lower.contains(\"mistral\"))  return \"mistral-small-latest\";\n        if (lower.contains(\"ollama\"))   return \"llama3.2:70b\";");
        
        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
    }
}
