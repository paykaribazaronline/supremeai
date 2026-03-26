package org.example.service;

import org.example.model.Requirement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApprovalManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void processRequirement(Requirement req) {
        switch (req.getSize()) {
            case SMALL:
                System.out.println("✅ [AUTO-APPROVE] Small task: " + req.getDescription());
                req.setStatus(Requirement.Status.APPROVED);
                break;
            case MEDIUM:
                System.out.println("⏳ [NOTIFY] Medium task: " + req.getDescription() + " (Auto-approving in 10 mins)");
                scheduler.schedule(() -> {
                    if (req.getStatus() == Requirement.Status.PENDING) {
                        req.setStatus(Requirement.Status.APPROVED);
                        System.out.println("✅ [AUTO-APPROVE] Medium task timeout reached: " + req.getDescription());
                    }
                }, 10, TimeUnit.MINUTES);
                break;
            case BIG:
                System.out.println("⚠️  [AUTO-APPROVE] Big task (auto-approval enabled): " + req.getDescription());
                req.setStatus(Requirement.Status.APPROVED);
                // In a real app, this sends a push notification to the Flutter mobile app
                break;
        }
    }

    public void manualAction(Requirement req, boolean approve) {
        req.setStatus(approve ? Requirement.Status.APPROVED : Requirement.Status.REJECTED);
        System.out.println("👤 [ADMIN] Manual " + (approve ? "Approval" : "Rejection") + " for: " + req.getDescription());
    }
}
