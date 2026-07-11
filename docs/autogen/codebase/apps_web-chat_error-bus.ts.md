# 📄 ফাইল: apps/web-chat/error-bus.ts

**প্রকার:** .ts  
**সাইজ:** 4,918 বাইট  
**আপডেট:** 2026-07-11T16:17:51.650546

---

## কোড

```ts
/**
 * error-bus.ts — কেন্দ্রীয় Observable Error Event Bus (Anti-Silent Error)
 *
 * এই মডিউলটি অ্যাপ্লিকেশনের যেকোনো জায়গায় ঘটা এররগুলোকে 
 * সাইলেন্টলি সাপ্রেস না করে সেন্ট্রাল লগিং এবং মনিটরিং-এর আওতায় নিয়ে আসে।
 * 
 * আর্কিটেকচারাল নিয়ম:
 * - কোডের কোথাও generic `catch (e) { console.error(e) }` বা `try {} catch(e) {}` 
 *   দিয়ে এরর সাপ্রেস করা কঠোরভাবে নিষিদ্ধ।
 * - যেকোনো exception এই বাস-এর মাধ্যমে এমিট করতে হবে।
 */

// Error severity levels
export type ErrorSeverity = "info" | "warning" | "error" | "critical";

// Error event context (user_id, task_id etc. যোগ করা যায়)
export interface ErrorContext {
  userId?: string;
  taskId?: string;
  tenantId?: string;
  sourceModule: string; // কোন ফাইল বা কম্পোনেন্ট থেকে এরর এসেছে
  action?: string;      // এরর ঘটার সময় কী কাজ হচ্ছিল
  [key: string]: unknown; // অন্যান্য কাস্টম ডেটা
}

// Event Bus Payload
export interface ErrorEventPayload {
  error: Error;
  severity: ErrorSeverity;
  context: ErrorContext;
  timestamp: string;
}

// Error Listener Type
type ErrorListener = (payload: ErrorEventPayload) => void;

class ErrorEventBus {
  private listeners: Set<ErrorListener> = new Set();
  
  /**
   * এরর রিপোর্ট করার মেইন এন্ট্রি পয়েন্ট।
   * 
   * @param error অরিজিনাল Error অবজেক্ট
   * @param context কোন কনটেক্সট থেকে এররটি এল তার বিস্তারিত
   * @param severity এররটির গুরুত্ব (ডিফল্ট: error)
   */
  public report(
    error: Error | unknown,
    context: ErrorContext,
    severity: ErrorSeverity = "error"
  ): void {
    // Ensure we have a valid Error object
    const normalizedError = error instanceof Error 
      ? error 
      : new Error(typeof error === "string" ? error : JSON.stringify(error));

    const payload: ErrorEventPayload = {
      error: normalizedError,
      severity,
      context,
      timestamp: new Date().toISOString(),
    };

    // 1. Console Logging (with strict context)
    this.logToConsole(payload);

    // 2. Notify all registered listeners (e.g., UI toast managers, external loggers like Sentry)
    this.notifyListeners(payload);

    // 3. (Optional) Fail-Fast on Critical Errors
    if (severity === "critical") {
      this.handleCriticalError(payload);
    }
  }

  /**
   * UI বা অন্য সাবসিস্টেম এই মেথড দিয়ে এরর ইভেন্ট শুনতে পারে।
   */
  public subscribe(listener: ErrorListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener); // Unsubscribe cleanup
    };
  }

  private notifyListeners(payload: ErrorEventPayload): void {
    this.listeners.forEach((listener) => {
      try {
        listener(payload);
      } catch (listenerError) {
        // Fallback: If a listener fails, log it directly (don't re-enter the bus)
        console.error("ErrorEventBus: Listener failed to process error.", listenerError);
      }
    });
  }

  private logToConsole(payload: ErrorEventPayload): void {
    const { error, severity, context, timestamp } = payload;
    const logPrefix = `[${timestamp}] [${severity.toUpperCase()}] [${context.sourceModule}]`;
    
    const logData = {
      message: error.message,
      context,
      stack: error.stack,
    };

    switch (severity) {
      case "info":
        console.info(logPrefix, logData);
        break;
      case "warning":
        console.warn(logPrefix, logData);
        break;
      case "critical":
      case "error":
        console.error(logPrefix, logData);
        break;
    }
  }

  private handleCriticalError(payload: ErrorEventPayload): void {
    // Critical এরর হলে আমরা অ্যাপের স্টেট করাপ্ট হওয়া ঠেকাতে
    // অ্যাপ রিলোড করতে পারি বা UI-তে একটি ব্লকিং ডায়ালগ দেখাতে পারি।
    console.error("CRITICAL ERROR: System halting to prevent corruption.", payload);
    // Future: Display catastrophic failure UI
  }
}

// Singleton instance
export const errorBus = new ErrorEventBus();

```