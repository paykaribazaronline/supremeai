package com.supremeai.service;

import org.springframework.stereotype.Service;

/** Bridges the gap between complex System Instructions and Human Language. */
@Service
public class CommunicationBridgeService {

  // অ্যাডমিনের সিম্পল কথাকে এআই প্রম্পটে রূপান্তর করে
  public String translateToSystemPrompt(String humanInput) {
    return "SYSTEM_INSTRUCTION: "
        + humanInput
        + ". Execute with high security, provide audit trail, and optimize for cost.";
  }

  // Translates complex system results into human-readable language
  public String explainToHuman(String systemResult) {
    if (systemResult.contains("optimized")) {
      return "System performance has been optimized and will now operate significantly faster.";
    }
    return "Your instruction has been successfully executed: " + systemResult;
  }
}
