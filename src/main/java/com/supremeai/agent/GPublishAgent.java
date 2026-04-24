
package com.supremeai.agent;

import com.supremeai.agentorchestration.Question;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPublishAgent - Publish and Deploy Agent
 * এই এজেন্টটি অ্যাপ পাবলিশিং এবং ডিপ্লয়মেন্টের জন্য দায়িত্বপ্রাপ্ত
 */
@Service
public class GPublishAgent {

    private static final Logger logger = LoggerFactory.getLogger(GPublishAgent.class);

    @Autowired
    private AIProviderFactory providerFactory;

    /**
     * পাবলিশিং এবং ডিপ্লয়মেন্টের জন্য প্রয়োজনীয়তা বিশ্লেষণ করে
     */
    public List<Question> analyzePublishingRequirements(String requirement) {
        try {
            AIProvider provider = providerFactory.getProvider("groq");
            String prompt = "একটি অ্যাপ পাবলিশিং এবং ডিপ্লয়মেন্টের জন্য নিম্নলিখিত প্রয়োজনীয়তা বিশ্লেষণ করুন: \"" + requirement + "\"\n" +
                    "পাবলিশিং এবং ডিপ্লয়মেন্ট সম্পর্কিত 5-7টি প্রশ্ন তৈরি করুন।\n" +
                    "প্রতিটি প্রশ্নকে একটি JSON অবজেক্ট হিসেবে ফরম্যাট করুন যেখানে 'key', 'text', এবং 'priority' (CRITICAL, HIGH, MEDIUM, LOW) থাকবে।\n" +
                    "শুধুমাত্র এই অবজেক্টগুলির একটি JSON অ্যারে রিটার্ন করুন।";

            String response = provider.generate(prompt);
            return parseQuestions(response);
        } catch (Exception e) {
            logger.warn("পাবলিশিং প্রয়োজনীয়তা বিশ্লেষণ ব্যর্থ হয়েছে, ডিফল্ট প্রশ্নগুলি ব্যবহার করা হচ্ছে: {}", e.getMessage());
            return getDefaultPublishingQuestions();
        }
    }

    /**
     * প্ল্যাটফর্ম অনুযায়ী পাবলিশিং প্ল্যান তৈরি করে
     */
    public Map<String, String> createPublishingPlan(String platform, Map<String, String> config) {
        Map<String, String> plan = new HashMap<>();

        switch (platform.toLowerCase()) {
            case "ios":
                plan.put("platform", "iOS");
                plan.put("store", "App Store");
                plan.put("buildTool", "Xcode");
                plan.put("distributionMethod", config.getOrDefault("iosDistribution", "App Store Connect"));
                plan.put("codeSigning", config.getOrDefault("iosCodeSigning", "Automatic"));
                plan.put("testFlight", config.getOrDefault("iosTestFlight", "enabled"));
                break;

            case "android":
                plan.put("platform", "Android");
                plan.put("store", "Google Play Store");
                plan.put("buildTool", "Gradle");
                plan.put("distributionMethod", config.getOrDefault("androidDistribution", "Google Play Console"));
                plan.put("signing", config.getOrDefault("androidSigning", "App Signing"));
                plan.put("testing", config.getOrDefault("androidTesting", "Internal Test Track"));
                break;

            case "web":
                plan.put("platform", "Web");
                plan.put("hosting", config.getOrDefault("webHosting", "Vercel"));
                plan.put("buildTool", config.getOrDefault("webBuildTool", "Vite"));
                plan.put("domain", config.getOrDefault("webDomain", "custom-domain.com"));
                plan.put("ssl", config.getOrDefault("webSSL", "automatic"));
                plan.put("cdn", config.getOrDefault("webCDN", "enabled"));
                break;

            case "desktop":
                plan.put("platform", "Desktop");
                plan.put("packaging", config.getOrDefault("desktopPackaging", "Electron Builder"));
                plan.put("distribution", config.getOrDefault("desktopDistribution", "GitHub Releases"));
                plan.put("updates", config.getOrDefault("desktopUpdates", "Auto-updater"));
                plan.put("codeSigning", config.getOrDefault("desktopCodeSigning", "enabled"));
                break;

            default:
                plan.put("error", "Unsupported platform: " + platform);
        }

        return plan;
    }

    /**
     * পাবলিশিং এবং ডিপ্লয়মেন্টের জন্য ডিফল্ট প্রশ্নগুলি প্রদান করে
     */
    private List<Question> getDefaultPublishingQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("publishPlatform", "কোন প্ল্যাটফর্মে পাবলিশ করবেন? (iOS, Android, Web, Desktop)", "CRITICAL"));
        questions.add(new Question("publishStore", "কোন স্টোরে পাবলিশ করবেন? (App Store, Play Store, Vercel, GitHub)", "HIGH"));
        questions.add(new Question("publishAutomation", "ডিপ্লয়মেন্ট অটোমেশন চান? (CI/CD, Manual, Semi-automated)", "HIGH"));
        questions.add(new Question("publishTesting", "টেস্টিং কীভাবে করবেন? (Beta, Alpha, Internal)", "MEDIUM"));
        questions.add(new Question("publishUpdates", "আপডেট সিস্টেম কীভাবে হবে? (Auto, Manual, Scheduled)", "MEDIUM"));
        questions.add(new Question("publishAnalytics", "অ্যানালিটিক্স ট্র্যাকিং চান? (Yes, No)", "LOW"));
        return questions;
    }

    /**
     * AI থেকে প্রাপ্ত প্রশ্নগুলি পার্স করে
     */
    private List<Question> parseQuestions(String response) {
        try {
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                return new ArrayList<>();
            }
            String jsonArray = response.substring(startIndex, endIndex + 1);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(jsonArray, new com.fasterxml.jackson.core.type.TypeReference<List<Question>>() {});
        } catch (Exception e) {
            logger.error("প্রশ্ন পার্স করতে ব্যর্থ: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
