
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
 * EWebAgent - Web Application Agent
 * এই এজেন্টটি ওয়েব অ্যাপ তৈরির জন্য দায়িত্বপ্রাপ্ত
 */
@Service
public class EWebAgent {

    private static final Logger logger = LoggerFactory.getLogger(EWebAgent.class);

    @Autowired
    private AIProviderFactory providerFactory;

    /**
     * ওয়েব অ্যাপের জন্য প্রয়োজনীয়তা বিশ্লেষণ করে
     */
    public List<Question> analyzeWebRequirements(String requirement) {
        try {
            AIProvider provider = providerFactory.getProvider("groq");
            String prompt = "একটি ওয়েব অ্যাপের জন্য নিম্নলিখিত প্রয়োজনীয়তা বিশ্লেষণ করুন: \"" + requirement + "\"\n" +
                    "ওয়েব অ্যাপ ডেভেলপমেন্ট সম্পর্কিত 5-7টি প্রশ্ন তৈরি করুন।\n" +
                    "প্রতিটি প্রশ্নকে একটি JSON অবজেক্ট হিসেবে ফরম্যাট করুন যেখানে 'key', 'text', এবং 'priority' (CRITICAL, HIGH, MEDIUM, LOW) থাকবে।\n" +
                    "শুধুমাত্র এই অবজেক্টগুলির একটি JSON অ্যারে রিটার্ন করুন।";

            String response = provider.generate(prompt);
            return parseQuestions(response);
        } catch (Exception e) {
            logger.warn("ওয়েব প্রয়োজনীয়তা বিশ্লেষণ ব্যর্থ হয়েছে, ডিফল্ট প্রশ্নগুলি ব্যবহার করা হচ্ছে: {}", e.getMessage());
            return getDefaultWebQuestions();
        }
    }

    /**
     * ওয়েব অ্যাপের জন্য ডিফল্ট প্রশ্নগুলি প্রদান করে
     */
    private List<Question> getDefaultWebQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("webFramework", "কোন ওয়েব ফ্রেমওয়ার্ক ব্যবহার করবেন? (React, Vue, Angular, Next.js)", "HIGH"));
        questions.add(new Question("webLanguage", "কোন প্রোগ্রামিং ভাষা ব্যবহার করবেন? (JavaScript, TypeScript)", "CRITICAL"));
        questions.add(new Question("webStyling", "স্টাইলিংয়ের জন্য কী ব্যবহার করবেন? (CSS, SCSS, Tailwind, Styled Components)", "MEDIUM"));
        questions.add(new Question("webStateManagement", "স্টেট ম্যানেজমেন্টের জন্য কী ব্যবহার করবেন? (Redux, MobX, Context API, Zustand)", "HIGH"));
        questions.add(new Question("webBuildTool", "বিল্ড টুল হিসেবে কী ব্যবহার করবেন? (Vite, Webpack, Parcel)", "MEDIUM"));
        questions.add(new Question("webDeployment", "ওয়েব অ্যাপ ডিপ্লয়মেন্ট কোথায় করবেন? (Vercel, Netlify, AWS, GCP)", "HIGH"));
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
