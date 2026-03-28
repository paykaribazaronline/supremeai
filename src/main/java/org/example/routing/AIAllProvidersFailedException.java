package org.example.routing;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when all providers in the routing sequence have been exhausted without success.
 */
public class AIAllProvidersFailedException extends RuntimeException {

    private final List<String> providerErrors;

    public AIAllProvidersFailedException(List<String> providerErrors) {
        super("All AI providers failed. Errors: " + providerErrors);
        this.providerErrors = Collections.unmodifiableList(providerErrors);
    }

    public List<String> getProviderErrors() {
        return providerErrors;
    }
}
