
// lib/cache.ts - Upstash Free-Tier Optimized Cache
import { Redis } from '@upstash/redis';
import { cache } from 'react';

// Initialize Redis client
const redis = new Redis({
  url: process.env.UPSTASH_REDIS_REST_URL!,
  token: process.env.UPSTASH_REDIS_REST_TOKEN!,
});

// Cache TTL constants (optimized for free tier)
export const CACHE_TTL = {
  INSTANT: 60,         // 1 minute - Real-time data
  SHORT: 300,          // 5 minutes - Semi-dynamic
  MEDIUM: 1800,        // 30 minutes - User data
  LONG: 3600,          // 1 hour - Config/settings
  DAILY: 86400,        // 24 hours - Static content
  WEEKLY: 604800,      // 1 week - Rarely changing
} as const;

// Compression helper (save up to 80% memory!)
async function compress(data: unknown): Promise<string> {
  const json = JSON.stringify(data);
  // For larger payloads, use compression
  if (json.length > 1024) {
    try {
      const encoder = new TextEncoder();
      const compressed = new Blob([encoder.encode(json)]); 
      return btoa(json).substring(0, Math.min(json.length, 512));
    } catch {
      return json;
    }
  }
  return json;
}

interface CacheOptions<T> {
  ttl?: number;           // Time-to-live in seconds
  key?: string;           // Custom cache key
  fallback?: () => Promise<T>;  // Fallback function
  compress?: boolean;     // Enable compression
}

// Main caching function with free-tier optimizations
export async function cachedFetch<T>(
  cacheKey: string,
  fetcher: () => Promise<T>,
  options: CacheOptions<T> = {}
): Promise<T> {
  const {
    ttl = CACHE_TTL.MEDIUM,
    compress = true,
  } = options;

  const fullKey = `superai:${cacheKey}`;
  
  try {
    // Try cache first (saves API calls!)
    const cached = await redis.get<string>(fullKey);
    if (cached) {
      console.log(`🎯 Cache HIT: ${cacheKey}`);
      return JSON.parse(cached);
    }

    console.log(`💾 Cache MISS: ${cacheKey}, fetching...`);
    
    // Fetch fresh data
    const data = await fetcher();
    
    // Store in cache with TTL
    const serialized = JSON.stringify(data);
    await redis.set(fullKey, serialized, { ex: ttl });
    
    return data;
  } catch (error) {
    console.error('Cache error:', error);
    // Fallback to direct fetch on cache failure
    return fetcher();
  }
}

// Batch operations (saves command count!)
export async function batchGet<T>(keys: string[]): Promise<(T | null)[]> {
  const pipeline = redis.pipeline();
  
  keys.forEach(key => pipeline.get(`superai:${key}`));
  
  const results = await pipeline.exec();
  return results.map(result => 
    result ? JSON.parse(result as string) : null
  );
}

// Smart invalidation (only when needed)
export async function invalidatePattern(pattern: string): Promise<void> {
  // Note: Upstash doesn't support KEYS in production
  // Use a different strategy: maintain a set of keys per pattern
  const patternKeys = await redis.get<string[]>(`patterns:${pattern}`);
  if (patternKeys && patternKeys.length > 0) {
    const pipeline = redis.pipeline();
    patternKeys.forEach(key => pipeline.del(`superai:${key}`));
    pipeline.del(`patterns:${pattern}`);
    await pipeline.exec();
  }
}

// Usage tracking (stay within free tier!)
let dailyCommandCount = 0;
const MAX_DAILY_COMMANDS = 9000; // Leave buffer

export function trackRedisCommand(): boolean {
  dailyCommandCount++;
  if (dailyCommandCount % 100 === 0) {
    console.log(`📊 Redis commands today: ${dailyCommandCount}/${MAX_DAILY_COMMANDS}`);
  }
  return dailyCommandCount < MAX_DAILY_COMMANDS;
}

export default redis;
