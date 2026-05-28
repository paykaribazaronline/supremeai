package com.supremeai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub provider for local-first operation.
 * Provides offline responses without requiring external AI API keys.
 * All responses are generated locally using rule-based patterns.
 */
@Component
public class StubLocalProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(StubLocalProvider.class);

    @Override
    public String getName() {
        return "stub-local";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        Map<String, Object> caps = new HashMap<>();
        caps.put("offline", true);
        caps.put("localModel", true);
        caps.put("supportsCode", true);
        caps.put("supportsChat", true);
        caps.put("bengaliLanguage", true);
        caps.put("ruleBasedResponses", true);
        return caps;
    }

    @Override
    public Mono<String> generate(String prompt) {
        log.info("[StubLocalProvider] Generating offline response for prompt: {}", prompt);
        return Mono.just(generateOfflineResponse(prompt));
    }

    private String generateOfflineResponse(String prompt) {
        String p = prompt.toLowerCase();
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return "আমি লোকাল মডেল ব্যবহার করছি। কোনো বাইরের API কী দরকার পড়ে না।";
        }
        
        if (p.contains("hello") || p.contains("hi") || p.contains("নমস্কার") || p.contains("হ্যালো")) {
            return "হ্যালো! আমি সুপ্রিমএআই। কোনো বাইরের API কী ছাড়াই আমি আপনার সাহায্য করছি।";
        } else if (p.contains("help") || p.contains("সাহায্য") || p.contains("কীভাবে") || p.contains("কীভাবে করব")) {
            return "আমি কীভাবে সাহায্য করতে পারি:\n• কোড লিখতে ও বিশ্লেষণ করতে\n• বাগ ফিক্স করতে\n• প্রজেক্ট গঠন বল্ড করতে\n• বিজ্ঞান ও গণিত সম্পর্কে প্রশ্নের উত্তর দিতে";
        } else if (p.contains("react") || p.contains("javascript") || p.contains("jsx") || p.contains("tsx")) {
            return "১. **প্রোজেক্ট সেটআপ**: `npx create-react-app my-app`\n২. **কম্পোনেন্ট**: ফাংশনাল কম্পোনেন্ট ব্যবহার করুন\n৩. **স্টেট**: useState এবং useEffect হুক ব্যবহার করুন\n৪. **স্টাইলিং**: CSS মডিউল বা Tailwind CSS যোগ করুন";
        } else if ((p.contains("java") || p.contains("spring")) && !p.contains("javascript")) {
            return "১. **স্প্রিং বুট**: spring initializr (https://start.spring.io) ব্যবহার করুন\n২. **REST API**: @RestController এবং @RequestMapping যোগ করুন\n৩. **ডাটাবেস**: JPA এবং PostgreSQL/MySQL কনফিগার করুন\n৪. **সুরক্ষা**: Spring Security দিয়ে JWT অথেন্টিকেশন যোগ করুন";
        } else if (p.contains("python")) {
            return "১. **ভার্চুয়াল এনভায়রনমেন্ট**: `python -m venv venv`\n২. **ডিপেন্ডেন্সি**: `pip install -r requirements.txt`\n৩. **ফ্রেমওয়ার্ক**: Flask বা FastAPI ব্যবহার করুন\n৪. **কোড স্টাইল**: PEP-8 গাইডলাইন মেনে কোড লিখুন";
        } else if (p.contains("code") || p.contains("কোড") || p.contains("প্রোগ্রাম")) {
            return "🤖 **SupremeAI লোকাল-ফার্স্ট মোড সক্রিয়**\n\n" +
                   "কোনো বাইরের AI API ছাড়াই আমি আপনার প্রশ্নের উত্তর দিচ্ছি।\n\n" +
                   "প্রশ্ন: \"" + prompt + "\"\n\n" +
                   "এই মুহূর্তে আমি লোকাল কোড-বেসড রুলস ব্যবহার করছি। আরও নির্দিষ্ট কোনো কিছু জানালে আমি আরও ভালো সাহায্য করতে পারব।";
        } else if (p.contains("bug") || p.contains("fix") || p.contains("ডবেগ") || p.contains("ঠিক কর")) {
            return "🐛 **বাগ ফিক্স গাইড**\n\n" +
                   "১. **ডবেগ কনসোল**: কনসোলের ত্রুটি বার্তা লক্ষ্য করুন\n২. **লগ যাচাই**: লগ ফাইল থেকে সমস্যার ক্ষেত্র খুঁজুন\n৩. **এক ধাপ এক করে**: সমস্যার ক্ষেত্রটি এক ধাপে এক ধাপে সরাসরি করুন\n৪. **টেস্ট করুন**: ফিক্স করার পর ইউনিট টেস্ট চালান";
        } else {
            return "🤖 **SupremeAI লোকাল-ফার্স্ট মোড সক্রিয়**\n\n" +
                   "আমি লোকাল মডেল ব্যবহার করছি। কোনো বাইরের API কী দরকার পড়ে না।\n\n" +
                   "আপনার প্রশ্ন: \"" + prompt + "\"\n\n" +
                   "এই মুহূর্তে আমি লোকাল কোড-বেসড রুলস ও কনোথিউম ব্যবহার করছি। আরও নির্দিষ্ট কোনো কিছু জানালে আমি আরও ভালো সাহায্য করতে পারব।";
        }
    }
}