"""
Authentication Analyzer - Plan 23 Week 3
Detects login forms, extracts cookies, JWT, OAuth patterns
Uses standard library only (no external deps)
"""
import re
from html.parser import HTMLParser

class FormParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.forms = []
        self.current_form = None
        self.current_inputs = []
    
    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        if tag == 'form':
            self.current_form = {
                'action': attrs_dict.get('action'),
                'method': attrs_dict.get('method', 'get'),
                'inputs': []
            }
        elif tag == 'input' and self.current_form is not None:
            input_type = attrs_dict.get('type')
            input_name = attrs_dict.get('name')
            self.current_form['inputs'].append(input_name)
            if input_type == 'password':
                self.current_form['has_password'] = True
    
    def handle_endtag(self, tag):
        if tag == 'form' and self.current_form is not None:
            if self.current_form.get('has_password'):
                self.forms.append(self.current_form)
            self.current_form = None

class AuthAnalyzer:
    def __init__(self, page_source: str, url: str):
        self.page_source = page_source
        self.url = url
        self.auth_type = "unknown"
        self.cookies = []
        self.jwt_tokens = []
    
    def detect_login_form(self):
        """Find login forms in page source using standard HTMLParser"""
        parser = FormParser()
        parser.feed(self.page_source)
        return parser.forms
    
    def extract_cookies(self, cookies_str: str):
        """Parse cookie string (from Playwright)"""
        cookies = []
        for cookie in cookies_str.split(';'):
            if '=' in cookie:
                name, value = cookie.strip().split('=', 1)
                cookies.append({'name': name, 'value': value})
        self.cookies = cookies
        return cookies
    
    def find_jwt_patterns(self):
        """Search for JWT in page source/localStorage/API responses"""
        jwt_pattern = r'eyJ[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+/=]*'
        matches = re.findall(jwt_pattern, self.page_source)
        self.jwt_tokens = matches
        return matches
    
    def detect_oauth(self):
        """Check for OAuth flow patterns"""
        oauth_patterns = [
            r'accounts\.google\.com',
            r'facebook\.com/login',
            r'oauth',
            r'authorize'
        ]
        for pattern in oauth_patterns:
            if re.search(pattern, self.page_source, re.IGNORECASE):
                return True
        return False
    
    def analyze(self):
        """Run full auth analysis"""
        forms = self.detect_login_form()
        jwt = self.find_jwt_patterns()
        oauth = self.detect_oauth()
        
        # Determine auth type
        if forms and jwt:
            self.auth_type = "JWT + Session"
        elif forms:
            self.auth_type = "Session-based"
        elif oauth:
            self.auth_type = "OAuth"
        elif jwt:
            self.auth_type = "JWT Bearer"
        
        return {
            "url": self.url,
            "auth_type": self.auth_type,
            "login_forms": forms,
            "jwt_tokens_found": len(jwt),
            "oauth_detected": oauth,
            "cookies": self.cookies
        }
