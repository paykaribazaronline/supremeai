"""
Payload Analyzer - Plan 23 Week 7-8
Analyzes API request/response schemas
"""

class PayloadAnalyzer:
    def __init__(self, endpoint: str, sample_response: dict = None):
        self.endpoint = endpoint
        self.sample_response = sample_response or {}
        self.schema = {
            'endpoint': endpoint,
            'method': 'POST',  # Default, can be detected
            'required_fields': [],
            'optional_fields': [],
            'response_schema': {}
        }
    
    def analyze_request(self, request_body: dict):
        """Analyze request body schema"""
        if not request_body:
            return self.schema
        
        # Separate required vs optional (simple heuristic)
        for key, value in request_body.items():
            if value is not None and value != '':
                self.schema['required_fields'].append(key)
            else:
                self.schema['optional_fields'].append(key)
        
        # Detect common fields
        if 'prompt' in request_body:
            self.schema['type'] = 'text_generation'
        elif 'image' in request_body:
            self.schema['type'] = 'image_generation'
        
        return self.schema
    
    def analyze_response(self):
        """Analyze response schema from sample"""
        if not self.sample_response:
            return self.schema
        
        def extract_schema(data, prefix=''):
            schema = {}
            if isinstance(data, dict):
                for key, value in data.items():
                    full_key = f"{prefix}.{key}" if prefix else key
                    schema[full_key] = type(value).__name__
                    if isinstance(value, (dict, list)):
                        schema.update(extract_schema(value, full_key))
            elif isinstance(data, list) and data:
                schema[prefix + '[]'] = type(data[0]).__name__
                if isinstance(data[0], dict):
                    schema.update(extract_schema(data[0], prefix + '[0]'))
            return schema
        
        self.schema['response_schema'] = extract_schema(self.sample_response)
        return self.schema
    
    def generate_payload_template(self) -> dict:
        """Generate empty payload template"""
        template = {}
        for field in self.schema['required_fields']:
            template[field] = None
        for field in self.schema['optional_fields']:
            template[field] = None
        return template

# Test
if __name__ == "__main__":
    analyzer = PayloadAnalyzer('/api/generate')
    req_schema = analyzer.analyze_request({
        'prompt': 'Test prompt',
        'language': 'bn',
        'max_tokens': 1000
    })
    print("Request Schema:", req_schema)
    
    resp_schema = analyzer.analyze_response({
        'success': True,
        'text': 'Generated text',
        'platform': 'test'
    })
    print("Response Schema:", resp_schema)
