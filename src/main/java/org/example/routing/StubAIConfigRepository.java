package org.example.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory stub for {@link AIConfigRepository}.
 *
 * <p>Provides a safe, no-Firebase implementation suitable for local development
 * and early Cloud Run deployments.  Replace (or supplement) this with a
 * {@code FirebaseAIConfigRepository} once Firebase credentials are available.
 *
 * <p>State is not persisted across restarts — the router falls back to the
 * properties-file default on each startup.
 */
@Component
public class StubAIConfigRepository implements AIConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(StubAIConfigRepository.class);

    private final AtomicReference<String> stored = new AtomicReference<>();

    @Override
    public Optional<String> loadPriorityOrder() {
        return Optional.ofNullable(stored.get());
    }

    @Override
    public void savePriorityOrder(String priorityOrder) {
        log.info("AI priority order updated (in-memory stub): {}", priorityOrder);
        stored.set(priorityOrder);
    }
}
