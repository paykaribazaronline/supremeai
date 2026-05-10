"""
Endpoint Discovery - Plan 23 Week 5-6
Static analysis of JS files to find API endpoints
No external dependencies (uses regex + standard library)
"""
import re
import urllib.request
import urllib.parse

class EndpointDiscovery:
    def __init__(self, js_bundles: list, base_url: str = ''):
        self.js_bundles = js_bundles or []
        self.base_url = base_url
        self.endpoints = []
    
    def discover_from_html(self, html_content: str):
        """Extract endpoints from HTML (API calls in script tags)"""
        # Find API-like patterns in HTML
        patterns = [
            r'[\'"]/(api/[^"\']{3,})[\'"]',  # /api/xxx
            r'[\'"]/(v[0-9]+/[^"\']{3,})[\'"]',  # /v1/xxx
            r'fetch\([\'"](https?://[^"\']+)[\'"]',  # fetch('url')
        ]
        for pattern in patterns:
            matches = re.findall(pattern, html_content)
            self.endpoints.extend(matches)
        return self.endpoints
    
    def fetch_js_content(self, js_url: str):
        """Fetch JS file content using standard lib"""
        try:
            # Handle relative URLs
            if not js_url.startswith(('http://', 'https://')):
                js_url = urllib.parse.urljoin(self.base_url, js_url)
            
            req = urllib.request.Request(js_url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as response:
                return response.read().decode('utf-8', errors='ignore')
        except Exception as e:
            return ""  # Fail silently for invalid URLs
    
    def extract_endpoints_from_js(self, js_content: str):
        """Extract API endpoints from JS content using regex"""
        patterns = [
            r'[\'"]/(api/[^"\']+)[\'"]',  # /api/xxx
            r'[\'"]/(graphql)[\'"]',  # /graphql
            r'fetch\([\'"](https?://[^"\']+)[\'"]',  # fetch('url')
            r'axios\.(get|post|put|delete)\([\'"](https?://[^"\']+)[\'"]',  # axios
            r'new WebSocket\([\'"](wss?://[^"\']+)[\'"]'  # WebSocket
        ]
        
        found = []
        for pattern in patterns:
            matches = re.findall(pattern, js_content)
            if matches:
                for match in matches:
                    if isinstance(match, tuple):
                        found.append(match[1] if len(match) > 1 else match[0])
                    else:
                        found.append(match)
        return list(set(found))
    
    def discover(self):
        """Run full endpoint discovery"""
        for bundle_url in self.js_bundles:
            js_content = self.fetch_js_content(bundle_url)
            if js_content:
                endpoints = self.extract_endpoints_from_js(js_content)
                self.endpoints.extend(endpoints)
        
        return list(set(self.endpoints))

# Example usage
if __name__ == "__main__":
    discovery = EndpointDiscovery([
        'https://example.com/static/js/main.js'
    ])
    endpoints = discovery.discover()
    print(f"Discovered endpoints: {endpoints}")
