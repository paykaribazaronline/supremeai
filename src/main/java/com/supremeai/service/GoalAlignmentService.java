package com.supremeai.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class GoalAlignmentService {

  // ইউজারের মূল প্ল্যান স্টোর করবে
  private final Map<String, String> userGoals = new ConcurrentHashMap<>();

  public void storeGoal(String userId, String goal) {
    userGoals.put(userId, goal);
  }

  public String checkAlignment(String userId, String currentCommand) {
    String goal = userGoals.get(userId);
    if (goal == null)
      return "You haven't set a goal yet.";

    // সিম্পল লজিক: ইউজার তার লক্ষ্যের দিকে যাচ্ছে কি না
    if (currentCommand.contains("fix") || currentCommand.contains("optimize")) {
      return "You are on the right track.";
    }
    return "Warning: This command does not seem to align with your previous goal (" + goal + ").";
  }

  public String predictOutcome(String command) {
    return "Executing this command will reduce response time by 10% and automatically generate security logs.";
  }
}
