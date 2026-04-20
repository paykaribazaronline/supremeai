package com.supremeai.service.quota;

import com.supremeai.model.UserTier;
import com.supremeai.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Abstract base quota manager with common implementation
 * All quota systems extend this to get unified behavior
 */
public abstract class AbstractQuotaManager<T> implements QuotaManager<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ConfigService configService;

    protected Function<T, Long> currentUsageExtractor;
    protected Function<T, Long> quotaLimitExtractor;
    protected Function<T, Boolean> unlimitedChecker;
    protected BiConsumer<T, Long> quotaLimitSetter;
    protected BiConsumer<T, Long> usageIncrementer;
    protected Runnable onConfigChange;

    @PostConstruct
    public void init() {
        initializeExtractors();
        logger.info("{} initialized successfully", getClass().getSimpleName());
    }

    /**
     * Initialize extractor functions for this quota type
     */
    protected abstract void initializeExtractors();

    @Override
    public boolean hasQuotaRemaining(T entity) {
        if (entity == null) return false;
        if (isUnlimited(entity)) return true;
        return getCurrentUsage(entity) < getQuotaLimit(entity);
    }

    @Override
    public long getCurrentUsage(T entity) {
        return entity != null ? currentUsageExtractor.apply(entity) : 0L;
    }

    @Override
    public long getQuotaLimit(T entity) {
        return entity != null ? quotaLimitExtractor.apply(entity) : 0L;
    }

    @Override
    public boolean isUnlimited(T entity) {
        return entity != null && Boolean.TRUE.equals(unlimitedChecker.apply(entity));
    }

    @Override
    public boolean incrementUsage(T entity) {
        if (entity == null) return false;
        if (!hasQuotaRemaining(entity)) {
            logger.warn("Quota exceeded for entity: {}", entity);
            return false;
        }
        usageIncrementer.accept(entity, 1L);
        return true;
    }

    @Override
    public void resetUsage(T entity) {
        if (entity != null) {
            usageIncrementer.accept(entity, -getCurrentUsage(entity));
            logger.debug("Quota usage reset for entity: {}", entity);
        }
    }

    @Override
    public void syncWithTierConfig(T entity, UserTier tier) {
        if (entity == null || tier == null) return;
        
        long newLimit = getTierQuota(tier);
        long currentLimit = getQuotaLimit(entity);
        
        if (currentLimit != newLimit && !isUnlimited(entity)) {
            quotaLimitSetter.accept(entity, newLimit);
            logger.info("Quota synced for tier {}: {} → {}", tier, currentLimit, newLimit);
        }
    }

    /**
     * Get quota limit for specific tier from config
     */
    protected abstract long getTierQuota(UserTier tier);

    /**
     * Call this method when global config changes to sync all entities
     */
    protected void onConfigUpdated() {
        if (onConfigChange != null) {
            onConfigChange.run();
        }
        logger.info("Global configuration updated - quota limits refreshed");
    }
}
