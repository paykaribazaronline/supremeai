
// lib/supabase.ts - Free-Tier Optimized Client
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!;
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!;

// Options optimized for free tier usage
export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,  // Reduce re-auth requests
    autoRefreshToken: true,
    detectSessionInUrl: true,
    storage: typeof window !== 'undefined' ? window.localStorage : undefined,
    storageKey: 'supremai-auth-token',
  },
  global: {
    headers: {
      'x-application-name': 'superai-free-tier',
    },
  },
  db: {
    schema: 'public',
  },
  // Realtime (free tier has limits!)
  realtime: {
    params: {
      eventsPerSecond: 10,  // Reduce events to stay within limits
    },
  },
});

// Server-side client with connection pooling awareness
export const supabaseAdmin = createClient(
  supabaseUrl,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
  {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
    global: {
      headers: {
        'x-role': 'service_role',
      },
    },
  }
);

export default supabase;
