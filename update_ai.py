import os

with open('src/main/java/org/example/service/AIAPIService.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"GPT4", "CLAUDE", "GROQ", "DEEPSEEK", "GEMINI", "COHERE", "PERPLEXITY", "LLAMA", "HUGGINGFACE", "XAI", "AIRLLM"', '"GPT4", "CLAUDE", "GROQ", "DEEPSEEK", "GEMINI", "COHERE", "PERPLEXITY", "LLAMA", "HUGGINGFACE", "XAI", "AIRLLM", "OLLAMA"')

content = content.replace('Map.entry("AIRLLM", "https://unsymmetrical-unrepugnant-lilah.ngrok-free.dev/v1/chat/completions")', 'Map.entry("AIRLLM", "https://unsymmetrical-unrepugnant-lilah.ngrok-free.dev/v1/chat/completions"),\n        Map.entry("OLLAMA", "http://localhost:11434/api/chat")')

content = content.replace('Map.entry("AIRLLM", "mistralai/Mistral-7B-Instruct-v0.3")', 'Map.entry("AIRLLM", "mistralai/Mistral-7B-Instruct-v0.3"),\n        Map.entry("OLLAMA", "llama3.2:70b")')

content = content.replace('case "AIRLLM":\n                return callOpenAICompatible(endpoint, apiKey, defaultModels.get("AIRLLM"), prompt, Collections.emptyMap());', 'case "AIRLLM":\n                return callOpenAICompatible(endpoint, apiKey, defaultModels.get("AIRLLM"), prompt, Collections.emptyMap());\n            case "OLLAMA":\n                return callOllama(endpoint, defaultModels.get("OLLAMA"), prompt);')

if 'callOllama(' not in content:
    ollama_method = '''
    private String callOllama(String endpoint, String model, String prompt) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", model);
        var messages = root.putArray("messages");
        var msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        root.put("stream", false);
        
        Request request = new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(mapper.writeValueAsString(root), MediaType.parse("application/json")))
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            JsonNode jsonResponse = mapper.readTree(response.body().string());
            return jsonResponse.at("/message/content").asText();
        }
    }
'''
    content = content.replace('private String callGemini(', ollama_method + '\n    private String callGemini(')

content = content.replace('if (normalized.contains("airllm")) {\n            return "airllm-local";\n        }', 'if (normalized.contains("airllm")) {\n            return "airllm-local";\n        }\n        if (normalized.contains("ollama")) {\n            return "ollama-local";\n        }')

content = content.replace('case "airllm-local":\n                return "AirLLM Local";', 'case "airllm-local":\n                return "AirLLM Local";\n            case "ollama-local":\n                return "Ollama Local";')

content = content.replace('aliases.put("local-airllm", "AIRLLM");', 'aliases.put("local-airllm", "AIRLLM");\n        aliases.put("ollama", "OLLAMA");\n        aliases.put("ollama-local", "OLLAMA");\n        aliases.put("local-ollama", "OLLAMA");')

content = content.replace('fallbacks.put("airllm-local", buildChain("AIRLLM", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));', 'fallbacks.put("airllm-local", buildChain("AIRLLM", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));\n        fallbacks.put("ollama-local", buildChain("OLLAMA", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));')

content = content.replace('if (lower.contains("mistral"))  return "mistral-small-latest";', 'if (lower.contains("mistral"))  return "mistral-small-latest";\n        if (lower.contains("ollama"))   return "llama3.2:70b";')

with open('src/main/java/org/example/service/AIAPIService.java', 'w', encoding='utf-8') as f:
    f.write(content)
