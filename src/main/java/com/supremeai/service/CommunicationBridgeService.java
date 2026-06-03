package com.supremeai.service;

import org.springframework.stereotype.Service;

/**
 * Bridges the gap between complex System Instructions and Human Language.
 */
@Service
public class CommunicationBridgeService {

    // অ্যাডমিনের সিম্পল কথাকে এআই প্রম্পটে রূপান্তর করে
    public String translateToSystemPrompt(String humanInput) {
        return "SYSTEM_INSTRUCTION: " + humanInput + 
               ". Execute with high security, provide audit trail, and optimize for cost.";
    }

    // সিস্টেমের জটিল রেজাল্টকে মানুষের জন্য সহজ ভাষায় রূপান্তর করে
    public String explainToHuman(String systemResult) {
        if (systemResult.contains("optimized")) {
            return "সিস্টেমের পারফরম্যান্স অপ্টিমাইজ করা হয়েছে এবং এখন আগের চেয়ে অনেক দ্রুত কাজ করবে।";
        }
        return "আপনার নির্দেশ সফলভাবে কার্যকর করা হয়েছে: " + systemResult;
    }
}
