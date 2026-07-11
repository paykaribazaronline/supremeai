# 📄 ফাইল: apps/web-chat/api.ts

**প্রকার:** .ts  
**সাইজ:** 3,042 বাইট  
**আপডেট:** 2026-07-11T13:49:08.446426

---

## কোড

```ts
import axios, { AxiosError } from 'axios';
import { AppConfig } from './env';
import { errorBus } from './error-bus';

// --- Type Definitions ---
export interface TaskMessage {
  role: "user" | "assistant" | "system";
  content: string;
}

export interface TaskExecutionPayload {
  task: string;
  task_type: string;
  messages: TaskMessage[];
}

export interface TaskExecutionResponse {
  result: unknown | null;
  error?: string;
}

export interface QuotaResponse {
  remaining: number;
}

// --- Strict API Client Setup ---
// এখানে আমরা সরাসরি AppConfig ব্যবহার করছি। Hardcoded fallback নেই।
const apiClient = axios.create({
  baseURL: AppConfig.apiUrl,
  timeout: AppConfig.apiTimeoutMs,
  headers: {
    'Content-Type': 'application/json',
  },
});

// JWT Interceptor
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(AppConfig.jwtStorageKey);
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error: unknown) => {
  // Request configuration phase errors
  errorBus.report(error, {
    sourceModule: "api.ts:apiClient.interceptors.request",
    action: "Configuring request interceptor"
  });
  return Promise.reject(error);
});

// --- API Service Methods ---
export const api = {
  /**
   * Execute a task against the backend.
   */
  async executeTask(
    task: string,
    messages: TaskMessage[],
    taskType: string = 'general'
  ): Promise<TaskExecutionResponse> {
    const payload: TaskExecutionPayload = { task, task_type: taskType, messages };
    
    try {
      const response = await apiClient.post<TaskExecutionResponse>('/task/execute', payload);
      return response.data;
    } catch (error: unknown) {
      // Anti-Silent Error: সেন্ট্রাল ErrorEventBus-এ এরর রিপোর্ট করা হচ্ছে
      const isAxiosError = error instanceof AxiosError;
      const errorMessage = isAxiosError ? error.message : "Unknown task execution error";
      
      errorBus.report(error, {
        sourceModule: "api.ts:executeTask",
        action: `Executing task of type: ${taskType}`,
        taskId: "N/A", // Can be dynamic if we track task IDs
        payload: payload,
      });

      return { result: null, error: errorMessage };
    }
  },

  /**
   * Fetch remaining usage quota.
   * Note: This is now a real API call. The previous dummy implementation has been removed.
   */
  async fetchQuota(): Promise<QuotaResponse> {
    try {
      // Changed from dummy setTimeout to real backend call
      const response = await apiClient.get<QuotaResponse>('/users/me/quota');
      return response.data;
    } catch (error: unknown) {
      errorBus.report(error, {
        sourceModule: "api.ts:fetchQuota",
        action: "Fetching user quota"
      });
      // Failed API call will throw or return a safe zero to prevent bypass
      throw new Error("Failed to fetch quota. Operation aborted.");
    }
  }
};

```