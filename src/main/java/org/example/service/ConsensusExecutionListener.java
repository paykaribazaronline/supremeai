package org.example.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConsensusExecutionListener {

    // Command hub or deployment service injection would go here

    @EventListener
    public void onConsensusReached(ConsensusEngine.ConsensusReachedEvent event) {
        if (event.getApprovalRate() >= 0.70) {
            // Auto-trigger build & deploy
            System.out.println("Consensus reached! Auto-deploying app: " + event.getAppId());
            // commandHub.execute("auto-deploy", event.getAppId());
        }
    }
}
