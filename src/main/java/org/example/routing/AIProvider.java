package org.example.routing;

/**
 * Abstraction for an AI provider.
 *
 * <p>Implement this interface to add a new provider without changing routing logic.
 * Provider name, base URL, model, and credentials are all config-driven so nothing
 * is hardcoded in the router itself.
 */
public interface AIProvider {

    /** Stable lower-case identifier, e.g. "kimi", "deepseek", "gemini". */
    String getName();

    /**
     * Send a prompt to the provider and return the generated text.
     *
     * @param prompt   the user prompt
     * @param taskType optional hint about the task category
     * @return provider response (never {@code null})
     * @throws AIProviderException if the call fails or the response is invalid
     */
    AIRouter.AIResponse generate(String prompt, String taskType);
}
