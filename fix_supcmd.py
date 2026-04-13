import json
import re

with open('command-hub/cli/supcmd.py', 'r', encoding='utf-8') as f:
    content = f.read()

if 'from tenacity import' not in content:
    content = content.replace('from typing import Dict, Optional, Any', 'from typing import Dict, Optional, Any\nfrom tenacity import retry, stop_after_attempt, wait_exponential')

new_func = '''    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    def _post_command(self, url: str, payload: dict, headers: dict):
        import requests
        return requests.post(url, json=payload, headers=headers, timeout=30)'''

if 'def _post_command' not in content:
    content = content.replace('    def execute_command', new_func + '\n\n    def execute_command')
    
    pattern = r'response = requests\.post\([^)]+\)'
    replacement = 'response = self._post_command(\n                f"{self.api_url}/execute",\n                payload={"name": name, "parameters": params},\n                headers=headers\n            )'
    content = re.sub(pattern, replacement, content, count=1)

with open('command-hub/cli/supcmd.py', 'w', encoding='utf-8') as f:
    f.write(content)
