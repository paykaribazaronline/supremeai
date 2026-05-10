import { createClient, SupabaseClient } from "@supabase/supabase-js";

let _client: SupabaseClient | null = null;

export function getSupabase(): SupabaseClient | null {
  if (!process.env.SUPABASE_URL || !process.env.SUPABASE_PUBLISHABLE_KEY) {
    return null; // Supabase not configured
  }

  if (!_client) {
    _client = createClient(
      process.env.SUPABASE_URL,
      process.env.SUPABASE_PUBLISHABLE_KEY
    );
  }

  return _client;
}

// Stub/mock Supabase for development without credentials
export function getMockSupabase() {
  return {
    from: () => ({
      select: () => ({
        order: () => ({
          limit: () => ({
            data: [],
            error: null,
          }),
          range: () => ({
            data: [],
            error: null,
          }),
        }),
        ilike: () => ({
          order: () => ({
            range: () => ({
              data: [],
              error: null,
            }),
          }),
        }),
      }),
      upsert: () => ({ error: null }),
      insert: () => ({ error: null }),
    }),
    storage: {
      from: () => ({
        upload: () => ({ error: null }),
        getPublicUrl: () => ({ data: { publicUrl: "" } }),
      }),
    },
  };
}

/*
Supabase Schema for GitReverse:

-- Quick reverse cache (prompts generated from repos)
CREATE TABLE quick_reverse_cache (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  repo_full_name TEXT NOT NULL,
  prompt TEXT NOT NULL,
  metadata JSONB,
  view_count INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Custom reverse cache (focus-based prompts)
CREATE TABLE custom_reverse_cache (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  focus_hash TEXT NOT NULL UNIQUE,
  focus_text TEXT,
  result TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- View counter (for tracking unique views)
CREATE TABLE view_counter (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  ip_hash TEXT NOT NULL UNIQUE,
  count INTEGER DEFAULT 1,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_quick_reverse_repo ON quick_reverse_cache(repo_full_name);
CREATE INDEX idx_quick_reverse_created ON quick_reverse_cache(created_at DESC);
CREATE INDEX idx_custom_reverse_hash ON custom_reverse_cache(focus_hash);
*/
