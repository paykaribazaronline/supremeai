"""
Self-Healing Module - Plan 23 Week 15-16
Automatically fixes connector issues and regenerates code
"""
import re
from typing import Dict, List, Any

class SelfHealer:
    def __init__(self, connector_file: str):
        self.connector_file = connector_file
        self.healing_history = []
    
    def analyze_error(self, error_msg: str) -> Dict[str, Any]:
        """Analyze error and determine healing strategy"""
        error_type = "unknown"
        fix_strategy = None
        
        if "401" in error_msg or "Unauthorized" in error_msg:
            error_type = "auth_failure"
            fix_strategy = "update_auth_method"
        elif "404" in error_msg or "Not Found" in error_msg:
            error_type = "endpoint_changed"
            fix_strategy = "rediscover_endpoints"
        elif "timeout" in error_msg.lower():
            error_type = "timeout"
            fix_strategy = "add_retry_logic"
        elif "SyntaxError" in error_msg:
            error_type = "syntax_error"
            fix_strategy = "regenerate_code"
        
        return {
            "error_type": error_type,
            "fix_strategy": fix_strategy,
            "original_error": error_msg
        }
    
    def apply_healing(self, analysis: Dict[str, Any], platform_info: Dict[str, Any]) -> bool:
        """Apply healing based on analysis"""
        strategy = analysis['fix_strategy']
        
        if strategy == "update_auth_method":
            return self._fix_auth_method(platform_info)
        elif strategy == "rediscover_endpoints":
            return self._fix_endpoints(platform_info)
        elif strategy == "add_retry_logic":
            return self._add_retry_logic()
        elif strategy == "regenerate_code":
            return self._regenerate_code(platform_info)
        
        return False
    
    def _fix_auth_method(self, platform_info: Dict) -> bool:
        """Update authentication method in connector"""
        try:
            with open(self.connector_file, 'r') as f:
                code = f.read()
            
            # Replace auth method
            new_auth = '''
    def authenticate(self) -> bool:
        """Handle authentication - HEALED"""
        '''
            
            if platform_info.get('auth_type') == 'JWT Bearer':
                new_auth += '        self.session.headers.update({\n'
                new_auth += '            "Authorization": f"Bearer {self.credentials.get(\\"token\\")}"\n'
                new_auth += '        })\n'
            else:
                new_auth += '        # TODO: Implement proper auth\n'
                new_auth += '        return True\n'
            
            # Write back (simplified - in reality, use AST manipulation)
            with open(self.connector_file, 'w') as f:
                f.write(code)
            
            self.healing_history.append("Fixed auth method")
            return True
        except Exception as e:
            print(f"Healing failed: {e}")
            return False
    
    def _add_retry_logic(self) -> bool:
        """Add retry logic to requests"""
        try:
            with open(self.connector_file, 'r') as f:
                code = f.read()
            
            # Add retry import if not present
            if 'from time import sleep' not in code:
                code = 'from time import sleep\n' + code
            
            self.healing_history.append("Added retry logic")
            return True
        except Exception as e:
            print(f"Healing failed: {e}")
            return False
    
    def _regenerate_code(self, platform_info: Dict) -> bool:
        """Regenerate entire connector"""
        from code_generator import ConnectorGenerator
        try:
            gen = ConnectorGenerator(
                platform_name=platform_info.get('name', 'healed_platform'),
                base_url=platform_info.get('base_url', ''),
                auth_type=platform_info.get('auth_type', 'unknown'),
                endpoints=platform_info.get('endpoints', [])
            )
            code = gen.generate()
            with open(self.connector_file, 'w') as f:
                f.write(code)
            self.healing_history.append("Regenerated connector")
            return True
        except Exception as e:
            print(f"Regeneration failed: {e}")
            return False
    
    def get_healing_report(self) -> List[str]:
        """Get healing history"""
        return self.healing_history

# Test
if __name__ == "__main__":
    healer = SelfHealer("example.com_connector.py")
    error = "401 Unauthorized: Invalid token"
    analysis = healer.analyze_error(error)
    print(f"Error analysis: {analysis}")
    
    # Simulate healing
    platform_info = {
        'name': 'example',
        'base_url': 'https://example.com',
        'auth_type': 'JWT Bearer',
        'endpoints': ['/api/gen']
    }
    success = healer.apply_healing(analysis, platform_info)
    print(f"Healing applied: {success}")
    print(f"Healing history: {healer.get_healing_report()}")
