"""
Kimi Observer Engine - Plan 23 Phase1
Fetches page source, detects JS frameworks, identifies API patterns
Uses standard library (no external deps)
"""
import urllib.request
import re

class KimiObserver:
    def __init__(self, target_url: str):
        self.target_url = target_url
        self.page_source = ""
        self.framework = "unknown"
        self.api_endpoints = []
    
    def fetch_page(self):
        """Fetch page source using standard urllib"""
        try:
            req = urllib.request.Request(
                self.target_url,
                headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
            )
            with urllib.request.urlopen(req, timeout=10) as response:
                self.page_source = response.read().decode('utf-8', errors='ignore')
            return self.page_source
        except Exception as e:
            print(f"Fetch error: {e}")
            return ""
    
    def detect_framework(self):
        """Detect JS framework (React/Vue/Angular/Plain)"""
        lower_source = self.page_source.lower()
        if "react" in lower_source:
            self.framework = "React"
        elif "vue" in lower_source:
            self.framework = "Vue"
        elif "angular" in lower_source:
            self.framework = "Angular"
        else:
            self.framework = "Plain JS"
        return self.framework
    
    def find_js_bundles(self):
        """Extract JS bundle URLs from page source"""
        bundle_patterns = [
            r'src="(.*?\.js)"',
            r'href="(.*?\.js)"',
            r'import\(["\'](.*?\.js)["\']'
        ]
        bundles = []
        for pattern in bundle_patterns:
            matches = re.findall(pattern, self.page_source)
            bundles.extend(matches)
        return list(set(bundles))
    
    def analyze(self):
        """Run full observation pipeline"""
        self.fetch_page()
        framework = self.detect_framework()
        bundles = self.find_js_bundles()
        return {
            "url": self.target_url,
            "framework": framework,
            "js_bundles": bundles,
            "page_length": len(self.page_source)
        }

# Test
if __name__ == "__main__":
    observer = KimiObserver("https://example.com")
    result = observer.analyze()
    print(result)
