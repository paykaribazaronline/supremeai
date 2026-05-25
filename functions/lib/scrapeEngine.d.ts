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
export declare const scrapeAndRespondFn: any;
/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
export declare const classifyIntentFn: any;
/**
 * GET /health
 */
export declare const scrapeHealthFn: any;
//# sourceMappingURL=scrapeEngine.d.ts.map