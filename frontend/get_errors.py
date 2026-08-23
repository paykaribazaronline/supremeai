import json
import sys
import re

with open('lint-results.json', 'r', encoding='utf-8') as f:
    text = f.read()

start = text.find('[')
end = text.rfind(']') + 1
json_str = text[start:end]

try:
    data = json.loads(json_str)
    errors = []
    for file_info in data:
        for msg in file_info.get('messages', []):
            if msg.get('severity') == 2:  # 2 is error
                errors.append({
                    'file': file_info['filePath'],
                    'line': msg['line'],
                    'msg': msg['message'],
                    'rule': msg.get('ruleId')
                })
    
    print(f"Total Errors: {len(errors)}")
    print(json.dumps(errors, indent=2))
except Exception as e:
    print(f"Failed to parse JSON: {e}")
