package com.supremeai.admin;

import com.supremeai.model.ImprovementProposal;
import com.supremeai.repository.ImprovementProposalRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AdminDashboardService {

  private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);

  private final ImprovementProposalRepository proposalRepository;

  // AutoPilot notifications can remain in memory or be moved to ActivityLog if needed
  private final List<String> autoPilotNotifications = new CopyOnWriteArrayList<>();

  // Auto Pilot Toggle
  private boolean isAutoPilotEnabled = false;

  @Autowired
  public AdminDashboardService(ImprovementProposalRepository proposalRepository) {
    this.proposalRepository = proposalRepository;
  }

  public void setAutoPilot(boolean enabled) {
    this.isAutoPilotEnabled = enabled;
    log.info("[Admin] Auto Pilot mode set to: {}", (enabled ? "ON" : "OFF"));
  }

  public boolean isAutoPilotEnabled() {
    return isAutoPilotEnabled;
  }

  /**
   * Called by learning modules (KnowledgeBase, ImmunitySystem, etc.) when they want to update
   * themselves.
   */
  public Mono<Boolean> submitImprovement(ImprovementProposal proposal) {
    if (isAutoPilotEnabled) {
      // Auto Pilot is ON! Approve immediately and save.
      proposal.approve();
      return proposalRepository
          .save(proposal)
          .map(
              saved -> {
                String notification =
                    String.format(
                        "Auto-Pilot Action: Learned and applied [%s] - %s",
                        saved.getCategory(), saved.getTitle());
                autoPilotNotifications.add(notification);
                log.info("[Admin Dashboard] {}", notification);
                return true;
              });
    } else {
      // Auto Pilot is OFF. Hold for admin permission.
      return proposalRepository
          .save(proposal)
          .map(
              saved -> {
                log.info(
                    "[Admin Dashboard] New Permission Request: {}. Waiting for Admin approval.",
                    saved.getTitle());
                return false; // Tells the caller to WAIT.
              });
    }
  }

  /** Admin clicks "Approve" on the dashboard. */
  public Mono<Boolean> approveProposal(String proposalId) {
    return proposalRepository
        .findById(proposalId)
        .flatMap(
            proposal -> {
              proposal.approve();
              return proposalRepository
                  .save(proposal)
                  .map(
                      saved -> {
                        log.info("[Admin Dashboard] Admin manually APPROVED: {}", saved.getTitle());
                        return true;
                      });
            })
        .defaultIfEmpty(false);
  }

  /** Admin clicks "Reject" on the dashboard. */
  public Mono<Boolean> rejectProposal(String proposalId) {
    return proposalRepository.deleteById(proposalId).then(Mono.just(true)).onErrorReturn(false);
  }

  public Flux<ImprovementProposal> getPendingApprovals() {
    return proposalRepository.findByApproved(false);
  }

  public List<String> getAutoPilotNotifications() {
    return new ArrayList<>(autoPilotNotifications);
  }
}
