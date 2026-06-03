package com.supremeai.util;

/**
 * Centralized utility class for fallback constants.
 * Avoids duplicated Bangla response strings across different services.
 */
public final class FallbackConstants {

    private FallbackConstants() { /* static only */ }

    /**
     * Default Bangla message when all AI providers are unavailable/fail to respond,
     * triggering system solo mode active notification.
     */
    public static final String NO_PROVIDER_RESPONSE = 
        "দুঃখিত, বর্তমানে কোনো এআই প্রোভাইডার সাড়া দিচ্ছে না। অনুগ্রহ করে ড্যাশবোর্ড থেকে এপিআই কী এবং প্রোভাইডার স্ট্যাটাস চেক করুন। (System Solo-Mode Active)";

    /**
     * Fallback message when ensemble voting times out.
     */
    public static final String VOTING_TIMEOUT = 
        "দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে। (Voting Timeout)";

    /**
     * Fallback message when consensus calculation times out.
     */
    public static final String CONSENSUS_TIMEOUT = 
        "দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে।";

    /**
     * Fallback message when all AI providers fail to respond to decision voting.
     */
    public static final String VOTING_FAILURE = 
        "দুঃখিত, সকল এআই প্রোভাইডার সাড়া দিতে ব্যর্থ হয়েছে।";
}
