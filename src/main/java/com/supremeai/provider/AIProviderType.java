package com.supremeai.provider;

/**
 * Provider role identifiers used as category keys across the codebase.
 *
 * All provider type, model name, and brand detection must be resolved dynamically
 * from the Firestore-backed ProviderTypeRegistry (provider_types collection).
 *
 * NO provider brand name or model ID is stored here. The static strings in this
 * file are neutral category identifiers (e.g. "local"), not brand names.
 * They are not a substitute for the dynamic registry lookup.
 */
public final class AIProviderType {

    /**
     * Neutral category identifiers — not brand names.
     * Pass the active provider's typeId (resolved from Firestore at runtime) to
     * {@link com.supremeai.service.ProviderTypeRegistry} instead of embedding
     * any provider brand here.
     */
    public static final String CATEGORY_LOCAL   = "ollama";   // Firestore typeId for self-hosted/local
    public static final String CATEGORY_GENERIC = "generic";

    /**
     * Resolve the best available provider typeId from the registry at runtime.
     * Returns "generic" as a last resort — never embeds a brand in a compile-time constant.
     */
    public static String resolveDefaultProviderType(com.supremeai.service.ProviderTypeRegistry registry) {
        if (registry == null) return CATEGORY_GENERIC;
        return registry.getAllTypes().values().stream()
                .filter(com.supremeai.model.ProviderTypeConfig::isEnabled)
                .map(com.supremeai.model.ProviderTypeConfig::getTypeId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(CATEGORY_GENERIC);
    }

    private AIProviderType() {
        // static constants only; not instantiable
    }
}
