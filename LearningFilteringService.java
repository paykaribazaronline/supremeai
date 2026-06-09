package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class LearningFilteringService {

    private final Firestore firestore;

    /**
     * নতুন লার্নিংটি ডুপ্লিকেট কি না তা যাচাই করা।
     * 
     * @param content লার্নিং এর বিষয়বস্তু
     * @return যদি ডুপ্লিকেট হয় তবে true, নাহলে false
     */
    public boolean isDuplicate(String content) throws ExecutionException, InterruptedException {
        // কন্টেন্ট নর্মালাইজ করা (ছোট হাতের অক্ষর এবং বাড়তি স্পেস সরানো)
        String normalizedContent = filterContent(content);
        String contentHash = generateMd5Hash(normalizedContent);

        // ফায়ারবেসে একই কন্টেন্ট আছে কি না চেক করা
        QuerySnapshot query = firestore.collection("system_learning")
                .whereEqualTo("contentHash", contentHash)
                .get().get();

        return !query.isEmpty();
    }

    /**
     * টেক্সট থেকে অপ্রয়োজনীয় অংশ ফিল্টার করা এবং ছোট হাতের অক্ষরে রূপান্তর করা।
     */
    public String filterContent(String rawContent) {
        return rawContent.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    /**
     * কন্টেন্টের MD5 হ্যাশ তৈরি করা।
     * 
     * @param content হ্যাশ করার জন্য কন্টেন্ট
     * @return SHA-256 হ্যাশ স্ট্রিং
     */
    private String generateHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(content.getBytes());
            // বাইট অ্যারে থেকে হেক্স স্ট্রিং-এ রূপান্তর
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 অ্যালগরিদম না পাওয়া গেলে, এটি একটি রানটাইম এক্সেপশন থ্রো করবে
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}