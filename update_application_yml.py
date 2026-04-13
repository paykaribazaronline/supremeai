with open('src/main/resources/application.properties', 'r', encoding='utf-8') as f:
    content = f.read()

# Add Ollama settings at the end
if 'ai.providers.ollama' not in content:
    content += '''
# ========== OLLAMA CONFIGURATION ==========
ai.providers.ollama.endpoint=\
ai.providers.ollama.health-check-url=\
ai.providers.ollama.api-key=
ai.providers.ollama.model=\
ai.providers.ollama.rate-limit-per-minute=\
'''

with open('src/main/resources/application.properties', 'w', encoding='utf-8') as f:
    f.write(content)
