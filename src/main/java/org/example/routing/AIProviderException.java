package org.example.routing;

/**
 * Thrown when an AI provider call fails.
 *
 * <p>The message intentionally does NOT include API keys or other secrets.
 */
public class AIProviderException extends RuntimeException {

    private final String providerName;

    public AIProviderException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
    }

    public AIProviderException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
