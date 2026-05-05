"""
Code Generator - Plan 23 Week 9-10
Generates Python connector classes from reverse-engineered data
"""
import re
from datetime import datetime

class ConnectorGenerator:
    def __init__(self, platform_name: str, base_url: str, auth_type: str, endpoints: list):
        self.platform_name = platform_name
        self.base_url = base_url
        self.auth_type = auth_type
        self.endpoints = endpoints
        self.class_name = self._to_pascal_case(platform_name)
    
    def _to_pascal_case(self, s: str) -> str:
        # Remove invalid characters (dots, slashes, etc.)
        s = re.sub(r'[^a-zA-Z0-9_]', '_', s)
        return ''.join(word.capitalize() for word in s.split('_') if word)
    
    def generate(self) -> str:
        """Generate full Python connector class"""
        lines = []
        # Header
        lines.append(f"# Auto-generated connector for {self.platform_name}")
        lines.append(f"# Generated: {datetime.now().isoformat()}")
        lines.append(f"# Auth type: {self.auth_type}")
        lines.append("")
        
        # Imports
        lines.append("import requests")
        lines.append("from typing import Dict, Any, Optional")
        lines.append("")
        
        # Class definition
        lines.append(f"class {self.class_name}Connector:")
        lines.append(f'    """Auto-generated connector for {self.platform_name}"""')
        lines.append("")
        
        # __init__
        lines.append("    def __init__(self, credentials: Optional[Dict[str, str]] = None):")
        lines.append(f'        self.base_url = "{self.base_url}"')
        lines.append("        self.session = requests.Session()")
        lines.append("        self.auth_data = None")
        lines.append("        self.credentials = credentials or {}")
        lines.append("")
        
        # Authenticate method
        lines.append("    def authenticate(self) -> bool:")
        lines.append('        """Handle authentication"""')
        if self.auth_type == "Session-based":
            lines.append('        login_data = {')
            lines.append('            "email": self.credentials.get("email"),')
            lines.append('            "password": self.credentials.get("password")')
            lines.append('        }')
            lines.append('        resp = self.session.post(f"{self.base_url}/api/login", json=login_data)')
            lines.append("        return resp.status_code == 200")
        elif self.auth_type == "JWT Bearer":
            lines.append('        # JWT auth: token in Authorization header')
            lines.append('        self.session.headers.update({')
            lines.append('            "Authorization": f"Bearer {self.credentials.get(\\"token\\")}"')
            lines.append('        })')
            lines.append("        return True")
        else:
            lines.append("        # OAuth or other auth - implement per platform")
            lines.append("        return True")
        lines.append("")
        
        # Example API method
        if self.endpoints:
            endpoint = self.endpoints[0]
            lines.append(f"    def call_api(self, prompt: str) -> Dict[str, Any]:")
            lines.append(f'        """Call {endpoint} endpoint"""')
            lines.append(f'        url = f"{{self.base_url}}{endpoint}"')
            lines.append('        payload = {"prompt": prompt}')
            lines.append('        resp = self.session.post(url, json=payload)')
            lines.append("        return resp.json()")
            lines.append("")
        
        # Return success format
        lines.append("    def _return_success(self, data: Any) -> Dict[str, Any]:")
        lines.append("        return {")
        lines.append('            "success": True,')
        lines.append(f'            "platform": "{self.platform_name}",')
        lines.append('            "data": data,')
        lines.append('            "auto_generated": True')
        lines.append("        }")
        
        return "\n".join(lines)

# Example usage
if __name__ == "__main__":
    generator = ConnectorGenerator(
        platform_name="bangla_ai",
        base_url="https://banglaai.example.com",
        auth_type="Session-based",
        endpoints=["/api/generate", "/api/chat"]
    )
    code = generator.generate()
    print(code)
    # Save to file
    with open("bangla_ai_connector.py", "w") as f:
        f.write(code)
