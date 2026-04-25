
package com.supremeai.agent;

import com.supremeai.agentorchestration.Question;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FDesktopAgent - Desktop Application Agent
 * এই এজেন্টটি ডেস্কটপ অ্যাপ তৈরির জন্য দায়িত্বপ্রাপ্ত
 */
@Service
public class FDesktopAgent {

    private static final Logger logger = LoggerFactory.getLogger(FDesktopAgent.class);

    @Autowired
    private AIProviderFactory providerFactory;

    /**
     * ডেস্কটপ অ্যাপের জন্য প্রয়োজনীয়তা বিশ্লেষণ করে
     */
    public List<Question> analyzeDesktopRequirements(String requirement) {
        try {
            AIProvider provider = providerFactory.getProvider("groq");
            String prompt = "একটি ডেস্কটপ অ্যাপের জন্য নিম্নলিখিত প্রয়োজনীয়তা বিশ্লেষণ করুন: \"" + requirement + "\"\n" +
                    "ডেস্কটপ অ্যাপ ডেভেলপমেন্ট সম্পর্কিত 5-7টি প্রশ্ন তৈরি করুন।\n" +
                    "প্রতিটি প্রশ্নকে একটি JSON অবজেক্ট হিসেবে ফরম্যাট করুন যেখানে 'key', 'text', এবং 'priority' (CRITICAL, HIGH, MEDIUM, LOW) থাকবে।\n" +
                    "শুধুমাত্র এই অবজেক্টগুলির একটি JSON অ্যারে রিটার্ন করুন।";

            String response = provider.generate(prompt).block();
            return parseQuestions(response);
        } catch (Exception e) {
            logger.warn("ডেস্কটপ প্রয়োজনীয়তা বিশ্লেষণ ব্যর্থ হয়েছে, ডিফল্ট প্রশ্নগুলি ব্যবহার করা হচ্ছে: {}", e.getMessage());
            return getDefaultDesktopQuestions();
        }
    }

    /**
     * ডেস্কটপ অ্যাপের জন্য ডিফল্ট প্রশ্নগুলি প্রদান করে
     */
    private List<Question> getDefaultDesktopQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("desktopFramework", "কোন ডেস্কটপ ফ্রেমওয়ার্ক ব্যবহার করবেন? (Electron, Tauri, JavaFX)", "HIGH"));
        questions.add(new Question("desktopLanguage", "কোন প্রোগ্রামিং ভাষা ব্যবহার করবেন? (JavaScript, Rust, Java)", "CRITICAL"));
        questions.add(new Question("desktopArchitecture", "ডেস্কটপ অ্যাপের আর্কিটেকচার কেমন হবে? (MVC, MVVM, Clean Architecture)", "HIGH"));
        questions.add(new Question("desktopDataStorage", "ডেটা সংরক্ষণের জন্য কী ব্যবহার করবেন? (SQLite, IndexedDB, File System)", "MEDIUM"));
        questions.add(new Question("desktopPackaging", "অ্যাপ প্যাকেজিং কীভাবে করবেন? (MSI, DMG, AppImage)", "MEDIUM"));
        questions.add(new Question("desktopUpdate", "অ্যাপ আপডেট সিস্টেম কীভাবে হবে? (Auto-updater, Manual)", "LOW"));
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
