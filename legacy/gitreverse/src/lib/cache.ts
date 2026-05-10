interface CacheEntry {
  promise: Promise<any>;
  timestamp: number;
}

export class InFlightCache {
  private cache = new Map<string, CacheEntry>();
  private ttl: number;

  constructor(ttl: number = 60000) {
    this.ttl = ttl; // Default 1 minute TTL
  }

  get(key: string): Promise<any> | undefined {
    const entry = this.cache.get(key);
    if (!entry) return undefined;

    // Check if expired
    if (Date.now() - entry.timestamp > this.ttl) {
      this.cache.delete(key);
      return undefined;
    }

    return entry.promise;
  }

  set(key: string, promise: Promise<any>): void {
    this.cache.set(key, {
      promise,
      timestamp: Date.now(),
    });

    // Auto-cleanup after TTL
    setTimeout(() => {
      this.cache.delete(key);
    }, this.ttl);
  }

  delete(key: string): void {
    this.cache.delete(key);
  }

  clear(): void {
    this.cache.clear();
  }

  size(): number {
    return this.cache.size;
  }
}
