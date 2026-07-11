# 📄 ফাইল: apps/web-chat/env.ts

**প্রকার:** .ts  
**সাইজ:** 4,135 বাইট  
**আপডেট:** 2026-07-11T17:16:16.996302

---

## কোড

```ts
/**
 * env.ts — কেন্দ্রীয় Environment Validation Layer (Fail-Fast Configuration)
 *
 * এই মডিউলটি Pydantic-inspired Fail-Fast প্যাটার্নে কাজ করে।
 * কোনো প্রয়োজনীয় env variable অনুপস্থিত থাকলে অ্যাপ স্টার্টআপেই
 * hard crash করবে — কোনো silent fallback বা ডামি ভ্যালু নেই।
 *
 * আর্কিটেকচারাল নিয়ম:
 * - সমস্ত config এই একটি মাত্র জায়গা থেকে আসবে (Single Source of Truth)
 * - `import.meta.env` সরাসরি অন্য কোথাও ব্যবহার করা নিষিদ্ধ
 * - অনুপস্থিত required variable = Error throw (Fail-Fast)
 */

// --- Required env variables এর নাম লিস্ট ---
const REQUIRED_ENV_KEYS = ["VITE_API_URL", "VITE_WS_URL"] as const;

type RequiredEnvKey = (typeof REQUIRED_ENV_KEYS)[number];

/**
 * Fail-Fast env validator: যদি কোনো required key না থাকে তবে Error throw করে।
 * Empty string-কেও অনুপস্থিত হিসেবে গণ্য করা হয়।
 */
function validateEnv(): Record<RequiredEnvKey, string> {
  // সব required key-এর জন্য validation চলবে, প্রথম error-এই থামবে না
  const missing: string[] = [];
  const validated: Partial<Record<RequiredEnvKey, string>> = {};

  for (const key of REQUIRED_ENV_KEYS) {
    const value = import.meta.env[key] as string | undefined;
    if (!value || value.trim() === "") {
      missing.push(key);
    } else {
      validated[key] = value.trim();
    }
  }

  if (missing.length > 0) {
    // Fail-Fast: অ্যাপ চালু হওয়ার আগেই crash করো।
    // `.env.example` ফাইলে সঠিক ভ্যালু দেখুন।
    throw new Error(
      `[SupremeAI] Startup Failure — নিচের required environment variable(s) অনুপস্থিত:\n` +
        missing.map((k) => `  • ${k}`).join("\n") +
        `\n\nসমাধান: .env.example কপি করে .env তৈরি করুন এবং সব ভ্যালু পূরণ করুন।`
    );
  }

  return validated as Record<RequiredEnvKey, string>;
}

// --- Singleton Config Object ---
// এই অবজেক্টটি মডিউল লোড হওয়ার সময় একবারই ভ্যালিডেট হয়।
// যেকোনো সমস্যা হলে এখানেই crash হবে, বাকি কোড চলবে না।
const _config = validateEnv();

/**
 * AppConfig — সমস্ত environment configuration-এর কেন্দ্রীয় টাইপ-সেফ অবজেক্ট।
 * সব ভ্যালু ইমিউটেবল (readonly)।
 */
export const AppConfig = Object.freeze({
  /** Backend HTTP base URL — task/quota API calls এর জন্য */
  apiUrl: _config.VITE_API_URL,

  /** Backend WebSocket URL — real-time chat stream এর জন্য */
  wsUrl: _config.VITE_WS_URL,

  /** Local storage key যেখানে JWT token সেভ থাকে */
  jwtStorageKey: "supreme_jwt_token",

  /**
   * WebSocket reconnection backoff limits (মিলিসেকেন্ড)।
   * Exponential backoff-এ এই range ব্যবহার হবে।
   */
  ws: {
    /** Initial reconnect delay (ms) */
    initialReconnectDelayMs: 1_000,
    /** Maximum reconnect delay (ms) — এর বেশি যাবে না */
    maxReconnectDelayMs: 30_000,
    /** Maximum consecutive reconnect attempts এর পরে give up */
    maxReconnectAttempts: 8,
  },

  /** API timeout (ms) */
  apiTimeoutMs: 10_000,
} as const);

// টাইপ এক্সপোর্ট — অন্য ফাইলে type-only import এর জন্য
export type AppConfigType = typeof AppConfig;

```