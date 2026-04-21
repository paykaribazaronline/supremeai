package com.supremeai.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);
    // Simulating database storage for admin notifications
    private final List<String> autoPilotNotifications = new ArrayList<>();
    private final Map<String, ImprovementProposal> pendingApprovals = new ConcurrentHashMap<>();

    // Auto Pilot Toggle
    private boolean isAutoPilotEnabled = false;

    public void setAutoPilot(boolean enabled) {
        this.isAutoPilotEnabled = enabled;
        log.info("[Admin] Auto Pilot mode set to: {}", (enabled ? "ON" : "OFF"));
    }

    public boolean isAutoPilotEnabled() {
        return isAutoPilotEnabled;
    }

    /**
     * Called by learning modules (KnowledgeBase, ImmunitySystem, etc.)
     * when they want to update themselves.
     */
    public boolean submitImprovement(ImprovementProposal proposal) {
        if (isAutoPilotEnabled) {
            // Auto Pilot is ON! Approve immediately and just send a notification.
            proposal.approve();
            String notification = String.format("Auto-Pilot Action: Learned and applied [%s] - %s",
                                                proposal.getCategory(), proposal.getTitle());
            autoPilotNotifications.add(notification);
            log.info("[Admin Dashboard] {}", notification);

            return true; // Tells the caller it's safe to apply the learning
        } else {
            // Auto Pilot is OFF. Hold for admin permission.
            pendingApprovals.put(proposal.getProposalId(), proposal);
            log.info("[Admin Dashboard] New Permission Request: {}. Waiting for Admin approval.", proposal.getTitle());

            return false; // Tells the caller to WAIT. Do not apply yet.
        }
    }

    /**
     * Admin clicks "Approve" on the dashboard.
     */
    public boolean approveProposal(String proposalId) {
        ImprovementProposal proposal = pendingApprovals.remove(proposalId);
        if (proposal != null) {
            proposal.approve();
            log.info("[Admin Dashboard] Admin manually APPROVED: {}", proposal.getTitle());
            // In a real system, you would trigger an Event here to notify the specific module to apply the payload
            return true;
        }
        return false;
    }

    /**
     * Admin clicks "Reject" on the dashboard.
     */
    public boolean rejectProposal(String proposalId) {
        ImprovementProposal proposal = pendingApprovals.remove(proposalId);
        if (proposal != null) {
            log.info("[Admin Dashboard] Admin REJECTED: {}", proposal.getTitle());
            return true;
        }
        return false;
    }

    public List<ImprovementProposal> getPendingApprovals() {
        return new ArrayList<>(pendingApprovals.values());
    }

    public List<String> getAutoPilotNotifications() {
        return new ArrayList<>(autoPilotNotifications);
    }
}