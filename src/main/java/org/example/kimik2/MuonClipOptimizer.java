package org.example.kimik2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * KIMI K2 TECHNIQUE 3: MuonClip Optimizer
 *
 * Kimi K2 key innovation:
 *   - Used the Muon optimizer (Momentum + Newton-Schulz orthogonalization)
 *     at 1-TRILLION-parameter scale — an unprecedented first.
 *   - Added "Clip" to resolve gradient explosions at scale.
 *   - Result: ZERO training instability across 15.5T tokens.
 *
 * Core idea behind MuonClip for SupremeAI's routing weights:
 *   1. MOMENTUM: Don't just use the raw reward signal — smooth it with
 *      a running average (exponential moving average) to reduce noise.
 *   2. CLIP: Cap the effective gradient step so no single bad outcome
 *      can catastrophically collapse an agent's routing weight.
 *   3. ORTHOGONALIZATION: Ensure different task-type weights for the
 *      same agent don't all move in lock-step (diversity preserved).
 *
 * This prevents:
 *   - An agent getting permanently deprioritized from one bad build
 *   - All agents converging to the same weights (specialization dies)
 *   - Oscillation where an agent swings good→bad→good repeatedly
 */
@Service
public class MuonClipOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(MuonClipOptimizer.class);

    // Momentum coefficient β (EMA of past gradients)
    private static final double BETA = 0.9;

    // Gradient clip threshold (no single step larger than this)
    private static final double CLIP_THRESHOLD = 0.05;

    // Minimum effective learning rate (prevent vanishing)
    private static final double MIN_LR = 0.001;

    // Maximum effective learning rate (prevent explosion)
    private static final double MAX_LR = 0.02;

    // Momentum buffer: agent → taskType → momentum value
    private final Map<String, Map<KimiMoERouter.TaskType, Double>> momentumBuffer
        = new ConcurrentHashMap<>();

    // Step counter per agent (for bias correction, like Adam)
    private final Map<String, Integer> stepCount = new ConcurrentHashMap<>();

    /**
     * Compute MuonClip-adjusted learning rate for a given gradient (reward).
     *
     * Algorithm:
     *   1. Update momentum: m = β * m_prev + (1 - β) * gradient
     *   2. Bias correction:  m_hat = m / (1 - β^t)
     *   3. Clip:             effective_lr = base_lr * clip(|m_hat|, 0, CLIP_THRESHOLD)
     *   4. Clamp:            effective_lr = clamp(effective_lr, MIN_LR, MAX_LR)
     *
     * @param gradient    raw reward signal in [-1, +1]
     * @param baseLR      starting learning rate
     * @return            clipped, momentum-smoothed effective learning rate
     */
    public double clip(double gradient, double baseLR) {
        // Simple form without agent context (used by RLVRTrainer)
        double clippedGradient = Math.max(-CLIP_THRESHOLD,
                                 Math.min(+CLIP_THRESHOLD, gradient));
        double effectiveLR = baseLR * Math.abs(clippedGradient) / CLIP_THRESHOLD;
        return Math.max(MIN_LR, Math.min(MAX_LR, effectiveLR));
    }

    /**
     * Full MuonClip step with per-agent momentum tracking.
     *
     * @param agentName   agent being updated
     * @param taskType    which task dimension
     * @param gradient    reward signal
     * @param baseLR      base learning rate
     * @return            effective learning rate to use
     */
    public double step(String agentName, KimiMoERouter.TaskType taskType,
                       double gradient, double baseLR) {
        // Retrieve or init momentum
        momentumBuffer.computeIfAbsent(agentName, k -> new ConcurrentHashMap<>());
        Map<KimiMoERouter.TaskType, Double> agentMomentum = momentumBuffer.get(agentName);

        double mPrev = agentMomentum.getOrDefault(taskType, 0.0);
        int t = stepCount.merge(agentName, 1, Integer::sum);

        // Momentum update (EMA)
        double m = BETA * mPrev + (1 - BETA) * gradient;
        agentMomentum.put(taskType, m);

        // Bias correction
        double mHat = m / (1 - Math.pow(BETA, t));

        // Clip
        double clippedMHat = Math.max(-CLIP_THRESHOLD,
                             Math.min(+CLIP_THRESHOLD, mHat));

        // Scale learning rate
        double effectiveLR = baseLR * (Math.abs(clippedMHat) / CLIP_THRESHOLD + 0.1);
        effectiveLR = Math.max(MIN_LR, Math.min(MAX_LR, effectiveLR));

        logger.debug("🎛️ MuonClip: agent={} task={} grad={:.3f} m={:.3f} mHat={:.3f} lr={:.4f}",
            agentName, taskType, gradient, m, mHat, effectiveLR);

        return effectiveLR;
    }

    /**
     * Orthogonalize weight updates across task types for an agent.
     *
     * Ensures agents maintain SPECIALIZATION — one bad result in CODE_GENERATION
     * doesn't drag down the SECURITY weights (which may still be good).
     *
     * Simplified Newton-Schulz-inspired approach: scale each task's update
     * by the inverse correlation with the current gradient direction.
     *
     * @param agentName  the agent being updated
     * @param primaryTask  the task that just got a reward
     * @param primaryReward the reward value
     * @return per-task-type orthogonalized reward multiplier (≤1.0)
     */
    public Map<KimiMoERouter.TaskType, Double> orthogonalize(
            String agentName,
            KimiMoERouter.TaskType primaryTask,
            double primaryReward) {

        Map<KimiMoERouter.TaskType, Double> multipliers = new java.util.EnumMap<>(KimiMoERouter.TaskType.class);
        Map<KimiMoERouter.TaskType, Double> agentMomentum =
            momentumBuffer.getOrDefault(agentName, java.util.Collections.emptyMap());

        for (KimiMoERouter.TaskType tt : KimiMoERouter.TaskType.values()) {
            if (tt == primaryTask) {
                multipliers.put(tt, 1.0); // full update for primary task
                continue;
            }

            // Reduce cross-task contamination based on momentum alignment
            double m = agentMomentum.getOrDefault(tt, 0.0);
            double alignment = m * primaryReward; // positive if aligned, negative if opposite

            // If momentum is opposite direction → don't push this task too
            double multiplier = alignment > 0 ? 0.3 : 0.1;
            multipliers.put(tt, multiplier);
        }

        return multipliers;
    }

    /**
     * Reset momentum for an agent (e.g., after agent config is evicted/reset).
     */
    public void reset(String agentName) {
        momentumBuffer.remove(agentName);
        stepCount.remove(agentName);
        logger.info("🔄 MuonClip momentum reset for agent: {}", agentName);
    }

    /** Get momentum diagnostics for monitoring. */
    public Map<String, Object> getDiagnostics(String agentName) {
        Map<String, Object> diag = new java.util.LinkedHashMap<>();
        diag.put("agent", agentName);
        diag.put("step_count", stepCount.getOrDefault(agentName, 0));
        diag.put("momentum_snapshot",
            momentumBuffer.getOrDefault(agentName, java.util.Collections.emptyMap()));
        diag.put("beta", BETA);
        diag.put("clip_threshold", CLIP_THRESHOLD);
        diag.put("lr_range", MIN_LR + " – " + MAX_LR);
        return diag;
    }
}
