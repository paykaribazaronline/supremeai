"""
Full Reverse Engineering Pipeline - Plan 23
Ties together all components: Observer -> Auth -> Discovery -> Code Gen
"""
from observer import KimiObserver
from auth_analyzer import AuthAnalyzer
from endpoint_discovery import EndpointDiscovery
from code_generator import ConnectorGenerator
import json

def reverse_engineer(url: str, credentials: dict = None):
    print(f" Starting reverse engineering of {url}...")
    
    # Step 1: Observe page
    print("\n1. Observing page...")
    observer = KimiObserver(url)
    obs_result = observer.analyze()
    print(f"   Framework: {obs_result['framework']}")
    print(f"   JS Bundles: {len(obs_result['js_bundles'])} found")
    
    # Step 2: Analyze auth
    print("\n2. Analyzing authentication...")
    auth_analyzer = AuthAnalyzer(observer.page_source, url)
    auth_result = auth_analyzer.analyze()
    print(f"   Auth type: {auth_result['auth_type']}")
    print(f"   Login forms: {len(auth_result['login_forms'])} found")
    
    # Step 3: Discover endpoints
    print("\n3. Discovering endpoints...")
    discovery = EndpointDiscovery(obs_result['js_bundles'])
    endpoints = discovery.discover()
    print(f"   Endpoints found: {len(endpoints)}")
    for ep in endpoints[:5]:  # Show first 5
        print(f"   - {ep}")
    
    # Step 4: Generate connector
    print("\n4. Generating connector code...")
    platform_name = url.replace("https://", "").replace("http://", "").replace("/", "_")
    generator = ConnectorGenerator(
        platform_name=platform_name,
        base_url=url,
        auth_type=auth_result['auth_type'],
        endpoints=endpoints
    )
    code = generator.generate()
    
    # Save connector
    filename = f"{platform_name}_connector.py"
    with open(filename, "w") as f:
        f.write(code)
    print(f"   Saved to {filename}")
    
    # Return full result
    return {
        "observation": obs_result,
        "auth": auth_result,
        "endpoints": endpoints,
        "connector_file": filename
    }

if __name__ == "__main__":
    # Test with example.com
    result = reverse_engineer("https://example.com")
    print("\n Full pipeline result:")
    print(json.dumps(result, indent=2))
