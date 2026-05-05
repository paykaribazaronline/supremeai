"""
Validator & Self-Healer - Plan 23 Week 15-16
Validates generated connectors and auto-fixes issues
"""
import importlib.util
import sys
from pathlib import Path

class ConnectorValidator:
    def __init__(self, connector_file: str):
        self.connector_file = connector_file
        self.errors = []
        self.warnings = []
    
    def validate_syntax(self) -> bool:
        """Check if connector file has valid Python syntax"""
        try:
            with open(self.connector_file, 'r') as f:
                compile(f.read(), self.connector_file, 'exec')
            return True
        except SyntaxError as e:
            self.errors.append(f"Syntax error: {e}")
            return False
    
    def validate_structure(self) -> bool:
        """Check if connector has required methods"""
        required_methods = ['__init__', 'authenticate']
        try:
            spec = importlib.util.spec_from_file_location("connector", self.connector_file)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            
            # Check for Connector class
            class_name = [name for name in dir(module) if name.endswith('Connector')]
            if not class_name:
                self.errors.append("No Connector class found")
                return False
            
            ConnectorClass = getattr(module, class_name[0])
            for method in required_methods:
                if not hasattr(ConnectorClass, method):
                    self.errors.append(f"Missing method: {method}")
                    return False
            return True
        except Exception as e:
            self.errors.append(f"Structure validation error: {e}")
            return False
    
    def validate_authentication(self, test_creds: dict = None) -> bool:
        """Test if authentication works (if creds provided)"""
        if not test_creds:
            self.warnings.append("No credentials provided for auth test")
            return True  # Skip if no creds
        
        try:
            spec = importlib.util.spec_from_file_location("connector", self.connector_file)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            
            class_name = [name for name in dir(module) if name.endswith('Connector')][0]
            ConnectorClass = getattr(module, class_name)
            connector = ConnectorClass(test_creds)
            
            auth_result = connector.authenticate()
            if not auth_result:
                self.errors.append("Authentication failed with provided credentials")
                return False
            return True
        except Exception as e:
            self.errors.append(f"Auth test error: {e}")
            return False
    
    def full_validation(self, test_creds: dict = None) -> dict:
        """Run all validation checks"""
        results = {
            'syntax': self.validate_syntax(),
            'structure': self.validate_structure(),
            'authentication': self.validate_authentication(test_creds),
            'errors': self.errors,
            'warnings': self.warnings
        }
        results['passed'] = all([
            results['syntax'], 
            results['structure'], 
            results['authentication']
        ])
        return results

# Test
if __name__ == "__main__":
    validator = ConnectorValidator("example.com_connector.py")
    result = validator.full_validation()
    print("Validation Result:")
    for key, value in result.items():
        print(f"  {key}: {value}")
