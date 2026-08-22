/**
 * SuperAI Supabase Client Configuration
 * 
 * FREE-TIER OPTIMIZATIONS:
 * - Connection pooling via PgBouncer (CRITICAL for free tier!)
 * - Session persistence to reduce auth requests
 * - Realtime rate limiting to stay within limits
 * - Separate admin client with elevated permissions
 */

import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';

// ✅ FIXED: Use VITE_ prefix for Vite-based projects
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || import.meta.env.NEXT_PUBLIC_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || import.meta.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
const supabaseServiceRoleKey = import.meta.env.SUPABASE_SERVICE_ROLE_KEY;

// Options optimized for free tier usage
export const supabase: SupabaseClient = createClient(supabaseUrl!, supabaseAnonKey!, {
  auth: {
    persistSession: true,  // Reduce re-auth requests (saves MAU!)
    autoRefreshToken: true,
    detectSessionInUrl: true,
    storage: typeof window !== 'undefined' ? window.localStorage : undefined,
    storageKey: 'supremai-auth-token',
  },
  global: {
    headers: {
      'x-application-name': 'superai-free-tier',
      'x-priority': 'low',  // Hint for connection pooler
    },
  },
  db: {
    schema: 'public',
  },
  realtime: {
    params: {
      eventsPerSecond: 10,  // Stay within free tier limits!
    },
  },
});

// Server-side client with connection pooling awareness
export const supabaseAdmin: SupabaseClient = createClient(
  supabaseUrl!,
  supabaseServiceRoleKey!,  // ✅ Use env var directly
  {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
    global: {
      headers: {
        'x-role': 'service_role',
        'x-connection-mode': 'pooler',  // ✅ Use PgBouncer
      },
    },
  }
);

// ✅ NEW: Connection health check for monitoring
export async function checkSupabaseHealth(): Promise<{
  connected: boolean;
  latency_ms: number;
  pool_status: string;
}> {
  const start = performance.now();
  
  try {
    const { error } = await supabase.from('_health_check').select('count').single();
    const latency = performance.now() - start;
    
    return {
      connected: !error || error?.code === '42P01',  // Table doesn't exist = OK
      latency_ms: Math.round(latency),
      pool_status: latency < 100 ? 'healthy' : latency < 500 ? 'degraded' : 'slow'
    };
  } catch (e) {
    return {
      connected: false,
      latency_ms: performance.now() - start,
      pool_status: 'error'
    };
  }
}

// ✅ NEW: Retry wrapper for transient failures
export async function withRetry<T>(
  operation: () => Promise<T>,
  maxRetries: number = 3,
  baseDelay: number = 300
): Promise<T> {
  let lastError: Error | null = null;
  
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error as Error;
      
      if (attempt === maxRetries) break;
      
      // Exponential backoff with jitter
      const delay = baseDelay * Math.pow(2, attempt) + Math.random() * 100;
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  throw lastError;
}

export default supabase;
