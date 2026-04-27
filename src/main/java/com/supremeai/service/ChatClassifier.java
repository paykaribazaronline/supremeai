package com.supremeai.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class ChatClassifier {

    public enum ChatType {
        RULE,
        PLAN,
        COMMAND,
        NORMAL
    }

    private final List<String> rulePatterns;
    private final List<String> planPatterns;
    private final List<String> commandPatterns;
    private final double confidenceThreshold = 0.6;
    private final Map<ChatType, Double> typeWeights;

    public ChatClassifier() {
        // Rule detection patterns (Bengali + English)
        rulePatterns = Arrays.asList(
            "সবসময়|সবসময়ের জন্য|আমাদের নিয়ম|নিয়ম হল|নিয়মাবলী|প্রতিবার|যখনই",
            "always|must|should|never|rule|policy|guideline|requirement",
            "আমাদের পলিসি|পলিসি অনুযায়ী|নির্দেশনা|নির্দেশিকা|প্রয়োজনীয়তা",
            "policy|guideline|instruction|requirement|mandatory"
        );

        // Plan detection patterns
        planPatterns = Arrays.asList(
            "পরিকল্পনা|পরিকল্পনা অনুযায়ী|পরবর্তী ধাপ|ধাপসমূহ|কর্মপরিকল্পনা|কৌশল",
            "plan|strategy|roadmap|timeline|milestone|action plan|next step|steps",
            "ভবিষ্যতে|আগামী|পরবর্তী|পরবর্তীতে|সময়সূচী|সময়রেখা",
            "future|upcoming|next|schedule|timeline|deadline|target"
        );

        // Command detection patterns
        commandPatterns = Arrays.asList(
            "কর|করো|করুন|করতে হবে|শুরু কর|শুরু করুন|বন্ধ কর|বন্ধ করুন|চালাও|চালু কর",
            "do|execute|run|start|stop|begin|end|terminate|initiate|launch",
            "পাঠাও|পাঠান|দেখাও|দেখান|চেক কর|চেক করুন|যাচাই কর|যাচাই করুন",
            "send|show|display|check|verify|validate|confirm|test|examine"
        );

        // Type weights
        typeWeights = new HashMap<>();
        typeWeights.put(ChatType.RULE, 1.2);
        typeWeights.put(ChatType.PLAN, 1.1);
        typeWeights.put(ChatType.COMMAND, 1.0);
    }

    public ClassificationResult classify(String message) {
        String messageLower = message.toLowerCase();

        double ruleScore = calculateScore(messageLower, rulePatterns, ChatType.RULE);
        double planScore = calculateScore(messageLower, planPatterns, ChatType.PLAN);
        double commandScore = calculateScore(messageLower, commandPatterns, ChatType.COMMAND);

        double maxScore = Math.max(ruleScore, Math.max(planScore, commandScore));

        if (maxScore < confidenceThreshold) {
            return new ClassificationResult(ChatType.NORMAL, 1.0 - maxScore, "এটি একটি সাধারণ চ্যাট মেসেজ");
        }

        if (maxScore == ruleScore) {
            return new ClassificationResult(ChatType.RULE, ruleScore, "এই মেসেজে রুলস বা নিয়মাবলী রয়েছে");
        } else if (maxScore == planScore) {
            return new ClassificationResult(ChatType.PLAN, planScore, "এই মেসেজে পরিকল্পনা বা প্ল্যান রয়েছে");
        } else {
            return new ClassificationResult(ChatType.COMMAND, commandScore, "এই মেসেজে কমান্ড বা নির্দেশ রয়েছে");
        }
    }

    private double calculateScore(String message, List<String> patterns, ChatType type) {
        double score = 0.0;
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(message);
            while (m.find()) {
                score += 0.1;
            }
        }
        return score * typeWeights.get(type);
    }

    public String extractContent(String message, ChatType chatType) {
        String pattern;
        switch (chatType) {
            case RULE:
                pattern = "[^।.!?\n]*?(?:সবসময়|always|must|should|never|rule|policy|guideline|প্রতিবার|নিয়ম)[^।.!?\n]*";
                break;
            case PLAN:
                pattern = "[^।.!?\n]*?(?:পরিকল্পনা|plan|strategy|roadmap|timeline|future|upcoming|পরবর্তী)[^।.!?\n]*";
                break;
            case COMMAND:
                pattern = "[^।.!?\n]*?(?:কর|do|execute|run|start|stop|পাঠাও|send|show)[^।.!?\n]*";
                break;
            default:
                return message;
        }
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(message);
        StringBuilder extracted = new StringBuilder();
        while (m.find()) {
            if (extracted.length() > 0) {
                extracted.append("। ");
            }
            extracted.append(m.group().trim());
        }
        String result = extracted.toString();
        return result.isEmpty() ? message : result;
    }

    public static class ClassificationResult {
        private ChatType chatType;
        private double confidence;
        private String reason;

        public ClassificationResult(ChatType chatType, double confidence, String reason) {
            this.chatType = chatType;
            this.confidence = confidence;
            this.reason = reason;
        }

        public ChatType getChatType() { return chatType; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
    }
}
