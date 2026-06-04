package com.supremeai.util;

/** Centralized utility class for third-opinion constants. */
public final class ThirdOpinionConstants {

  private ThirdOpinionConstants() {
    /* static only */
  }

  public static final String NO_PROVIDER_RESPONSE =
      "দুঃখিত, বর্তমানে কোনো তৃতীয় পক্ষ এআই (third-party) প্রোভাইডার সাড়া দিচ্ছে না। অনুগ্রহ করে ড্যাশবোর্ড থেকে এপিআই কী এবং প্রোভাইডার স্ট্যাটাস চেক করুন। (CoreKnowledge+Browser active)";

  public static final String VOTING_TIMEOUT =
      "দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে। (Voting Timeout)";

  public static final String CONSENSUS_TIMEOUT =
      "দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে।";

  public static final String VOTING_FAILURE =
      "দুঃখিত, সকল তৃতীয় পক্ষ এআই প্রোভাইডার সাড়া দিতে ব্যর্থ হয়েছে।";
}
