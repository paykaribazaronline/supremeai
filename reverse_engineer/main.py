"""
Main Entry Point - Plan 23 Complete Pipeline
Integrates all modules with CLI interface
"""
import sys
import json
from observer import KimiObserver
from auth_analyzer import AuthAnalyzer
from endpoint_discovery import EndpointDiscovery
from payload_analyzer import PayloadAnalyzer
from code_generator import ConnectorGenerator
from validator import ConnectorValidator
from self_healer import SelfHealer

class ReverseEngineer:
    def __init__(self, url: str, credentials: dict = None):
        self.url = url
        self.credentials = credentials or {}
        self.results = {}
    
    def run_full_pipeline(self) -> dict:
        """Execute complete reverse engineering pipeline"""
        print(f"🚀 Starting reverse engineering of {self.url}...")
        
        # Step 1: Observe
        print("\n[1/6] Observing page...")
        observer = KimiObserver(self.url)
        obs_result = observer.analyze()
        print(f"   ✓ Framework: {obs_result['framework']}")
        print(f"   ✓ JS bundles: {len(obs_result['js_bundles'])} found")
        self.results['observation'] = obs_result
        
        # Step 2: Auth Analysis
        print("\n[2/6] Analyzing authentication...")
        auth = AuthAnalyzer(observer.page_source, self.url)
        auth_result = auth.analyze()
        print(f"   ✓ Auth type: {auth_result['auth_type']}")
        print(f"   ✓ Login forms: {len(auth_result['login_forms'])} found")
        self.results['auth'] = auth_result
        
        # Step 3: Endpoint Discovery
        print("\n[3/6] Discovering endpoints...")
        discovery = EndpointDiscovery(obs_result['js_bundles'], self.url)
        # Also search in HTML for API patterns
        html_endpoints = discovery.discover_from_html(observer.page_source)
        js_endpoints = discovery.discover()
        endpoints = list(set(html_endpoints + js_endpoints))
        print(f"   ✓ Endpoints found: {len(endpoints)}")
        for ep in endpoints[:5]:
            print(f"     - {ep}")
        self.results['endpoints'] = endpoints
        
        # Step 4: Payload Analysis
        print("\n[4/6] Analyzing payload schemas...")
        analyzer = PayloadAnalyzer(endpoints[0] if endpoints else "/api/generate")
        schema = analyzer.analyze_request({
            'prompt': 'test',
            'language': 'bn',
            'max_tokens': 1000
        })
        print(f"   ✓ Schema analyzed: {len(schema.get('required_fields', []))} required fields")
        self.results['payload_schema'] = schema
        
        # Step 5: Code Generation
        print("\n[5/6] Generating connector code...")
        platform_name = self.url.replace("https://", "").replace("http://", "").replace("/", "_")
        generator = ConnectorGenerator(
            platform_name=platform_name,
            base_url=self.url,
            auth_type=auth_result['auth_type'],
            endpoints=endpoints
        )
        code = generator.generate()
        
        # Save connector
        filename = f"{platform_name}_connector.py"
        with open(filename, "w") as f:
            f.write(code)
        print(f"   ✓ Saved to {filename} ({len(code)} bytes)")
        self.results['connector_file'] = filename
        
        # Step 6: Validation
        print("\n[6/6] Validating connector...")
        validator = ConnectorValidator(filename)
        val_result = validator.full_validation(self.credentials if self.credentials else None)
        print(f"   ✓ Syntax: {'PASS' if val_result['syntax'] else 'FAIL'}")
        print(f"   ✓ Structure: {'PASS' if val_result['structure'] else 'FAIL'}")
        print(f"   ✓ Authentication: {'PASS' if val_result['authentication'] else 'FAIL'}")
        self.results['validation'] = val_result
        
        print(f"\n✅ Pipeline completed! Connector: {filename}")
        return self.results
    
    def export_report(self, filename: str = "report.json"):
        """Export full results to JSON"""
        with open(filename, "w") as f:
            json.dump(self.results, f, indent=2)
        print(f"📄 Report saved to {filename}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 main.py <url> [--creds email:pass]")
        sys.exit(1)
    
    url = sys.argv[1]
    creds = {}
    if "--creds" in sys.argv:
        idx = sys.argv.index("--creds")
        if idx + 1 < len(sys.argv):
            email, pwd = sys.argv[idx + 1].split(":")
            creds = {"email": email, "password": pwd}
    
    engine = ReverseEngineer(url, creds)
    results = engine.run_full_pipeline()
    engine.export_report()
