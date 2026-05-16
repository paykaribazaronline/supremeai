"""
Test Suite - Plan 23 Comprehensive Testing
Run all reverse_engineer modules with examples
"""
import json
from observer import KimiObserver
from auth_analyzer import AuthAnalyzer
from endpoint_discovery import EndpointDiscovery
from code_generator import ConnectorGenerator
from payload_analyzer import PayloadAnalyzer
from validator import ConnectorValidator

def test_all():
    print("=" * 60)
    print("SupremeAI Reverse Engineering - Test Suite")
    print("=" * 60)
    
    # Test 1: Observer
    print("\n[1] Testing KimiObserver...")
    observer = KimiObserver("https://example.com")
    obs_result = observer.analyze()
    assert obs_result['url'] == "https://example.com"
    assert obs_result['framework'] in ['React', 'Vue', 'Angular', 'Plain JS']
    print(f"   ✓ Framework detected: {obs_result['framework']}")
    print(f"   ✓ Page length: {obs_result['page_length']} bytes")
    
    # Test 2: Auth Analyzer
    print("\n[2] Testing AuthAnalyzer...")
    auth = AuthAnalyzer(observer.page_source, "https://example.com")
    auth_result = auth.analyze()
    print(f"   ✓ Auth type: {auth_result['auth_type']}")
    print(f"   ✓ Login forms: {len(auth_result['login_forms'])}")
    
    # Test 3: Endpoint Discovery
    print("\n[3] Testing EndpointDiscovery...")
    discovery = EndpointDiscovery(obs_result['js_bundles'])
    endpoints = discovery.discover()
    print(f"   ✓ Endpoints found: {len(endpoints)}")
    
    # Test 4: Payload Analyzer
    print("\n[4] Testing PayloadAnalyzer...")
    analyzer = PayloadAnalyzer('/api/generate')
    schema = analyzer.analyze_request({
        'prompt': 'Test',
        'language': 'bn',
        'max_tokens': 1000
    })
    assert 'prompt' in schema['required_fields']
    print(f"   ✓ Request schema analyzed")
    print(f"   ✓ Required fields: {schema['required_fields']}")
    
    # Test 5: Code Generator
    print("\n[5] Testing ConnectorGenerator...")
    gen = ConnectorGenerator(
        platform_name="test_platform",
        base_url="https://test.com",
        auth_type="Session-based",
        endpoints=["/api/gen"]
    )
    code = gen.generate()
    assert "class TestPlatformConnector" in code
    assert "def authenticate" in code
    print(f"   ✓ Connector code generated ({len(code)} bytes)")
    
    # Test 6: Validator
    print("\n[6] Testing ConnectorValidator...")
    with open("test_connector.py", "w") as f:
        f.write(code)
    
    validator = ConnectorValidator("test_connector.py")
    val_result = validator.full_validation()
    assert val_result['passed'] == True
    print(f"   ✓ Validation passed: {val_result['passed']}")
    
    # Cleanup
    import os
    if os.path.exists("test_connector.py"):
        os.remove("test_connector.py")
    
    print("\n" + "=" * 60)
    print(" ALL TESTS PASSED!")
    print("=" * 60)
    return True

if __name__ == "__main__":
    try:
        test_all()
    except AssertionError as e:
        print(f"\n TEST FAILED: {e}")
    except Exception as e:
        print(f"\n ERROR: {e}")
        import traceback
        traceback.print_exc()
