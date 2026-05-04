"""
Performance Optimizer - Plan 23/24 Efficiency
Caches results, parallel execution, smart retries
"""
import time
import json
from functools import lru_cache
from concurrent.futures import ThreadPoolExecutor, as_completed

class OptimizedEngine:
    def __init__(self, cache_dir: str = ".cache"):
        self.cache_dir = cache_dir
        self.results_cache = {}
    
    @lru_cache(maxsize=128)
    def fetch_with_cache(self, url: str, ttl: int = 3600):
        """Fetch URL with caching (TTL in seconds)"""
        import hashlib
        url_hash = hashlib.md5(url.encode()).hexdigest()
        cache_file = f"{self.cache_dir}/{url_hash}.json"
        
        # Check cache
        try:
            import os
            if os.path.exists(cache_file):
                mtime = os.path.getmtime(cache_file)
                if time.time() - mtime < ttl:
                    with open(cache_file) as f:
                        return json.load(f)
        except:
            pass
        
        # Fetch fresh
        from observer import KimiObserver
        observer = KimiObserver(url)
        result = observer.analyze()
        
        # Save to cache
        try:
            import os
            os.makedirs(self.cache_dir, exist_ok=True)
            with open(cache_file, 'w') as f:
                json.dump(result, f)
        except:
            pass
        
        return result
    
    def batch_analyze_parallel(self, urls: list, max_workers: int = 5):
        """Process multiple URLs in parallel"""
        results = {}
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            future_to_url = {
                executor.submit(self.fetch_with_cache, url): url 
                for url in urls
            }
            
            for future in as_completed(future_to_url):
                url = future_to_url[future]
                try:
                    results[url] = future.result()
                except Exception as e:
                    results[url] = {'error': str(e)}
        
        return results
    
    def smart_retry(self, func, *args, max_retries: int = 3, **kwargs):
        """Retry with exponential backoff"""
        for attempt in range(max_retries):
            try:
                return func(*args, **kwargs)
            except Exception as e:
                if attempt == max_retries - 1:
                    raise
                wait_time = 2 ** attempt
                print(f"Retry {attempt + 1}/{max_retries} after {wait_time}s...")
                time.sleep(wait_time)

# Test
if __name__ == "__main__":
    engine = OptimizedEngine()
    
    # Test parallel processing
    urls = [
        "https://example.com",
        "https://www.wikipedia.org",
        "https://www.google.com"
    ]
    
    print("Processing in parallel...")
    start = time.time()
    results = engine.batch_analyze_parallel(urls, max_workers=3)
    elapsed = time.time() - start
    
    print(f"\nProcessed {len(urls)} URLs in {elapsed:.2f}s")
    for url, result in results.items():
        status = "✓" if 'error' not in result else "✗"
        print(f"  {status} {url}")
