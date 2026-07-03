# 📄 ফাইল: infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.d.ts

**প্রকার:** .ts  
**সাইজ:** 849 বাইট  
**আপডেট:** 2026-07-03T15:08:06.550852

---

## কোড

```ts
import * as https from "firebase-functions/v2/https";
/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
export declare function scrapeAndRespond(message: string, userId: string): Promise<Record<string, unknown>>;
/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
export declare const scrapeAndRespondFn: https.HttpsFunction;
/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
export declare const classifyIntentFn: https.HttpsFunction;
/**
 * GET /health
 */
export declare const scrapeHealthFn: https.HttpsFunction;
//# sourceMappingURL=scrapeEngine.d.ts.map
```