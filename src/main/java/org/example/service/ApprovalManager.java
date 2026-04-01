package org.example.service;

import org.example.model.Requirement;
import org.springframework.stereotype.Service;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
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
            case HUMAN_REQUIRED:
                System.out.println("🔴 [HUMAN REVIEW] This task requires human review: " + req.getDescription());
                // Notify admin/human reviewer to manually approve or reject
                break;
        }
    }

    public void manualAction(Requirement req, boolean approve) {
        req.setStatus(approve ? Requirement.Status.APPROVED : Requirement.Status.REJECTED);
        System.out.println("👤 [ADMIN] Manual " + (approve ? "Approval" : "Rejection") + " for: " + req.getDescription());
    }
}
