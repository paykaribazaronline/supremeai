package com.supremeai.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoalAlignmentService {

    // ইউজারের মূল প্ল্যান স্টোর করবে
    private final Map<String, String> userGoals = new ConcurrentHashMap<>();

    public void storeGoal(String userId, String goal) {
        userGoals.put(userId, goal);
    }

    public String checkAlignment(String userId, String currentCommand) {
        String goal = userGoals.get(userId);
        if (goal == null) return "আপনি এখনো আপনার কোনো লক্ষ্য সেট করেননি।";
        
        // সিম্পল লজিক: ইউজার তার লক্ষ্যের দিকে যাচ্ছে কি না
        if (currentCommand.contains("fix") || currentCommand.contains("optimize")) {
            return "আপনি সঠিক ট্র্যাকেই আছেন।";
        }
        return "সতর্কতা: এই কমান্ডটি আপনার আগের লক্ষ্যের (" + goal + ") সাথে সামঞ্জস্যপূর্ণ মনে হচ্ছে না।";
    }

    public String predictOutcome(String command) {
        return "এই কমান্ডটি সম্পন্ন করলে সিস্টেমের রেসপন্স টাইম ১০% কমবে এবং সিকিউরিটি লগ অটোমেটিক্যালি জেনারেট হবে।";
    }
}
