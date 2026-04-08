#!/usr/bin/env python3
"""
Test Dual Model AirLLM Setup
Tests connectivity and basic functionality
"""

import json
import sys
import urllib.error
import urllib.request
import argparse


def test_endpoint(url: str, model: str = "gemma") -> dict:
    """Test the AirLLM endpoint"""
    print(f"\n🧪 Testing AirLLM Endpoint")
    print(f"   URL: {url}")
    print(f"   Model: {model}")

    results = {
        "endpoint": url,
        "model": model,
        "tests": {}
    }

    # Test 1: Health Check
    print("\n   [1/4] Health Check...", end="", flush=True)
    try:
        req = urllib.request.Request(
            url=f"{url}/health",
            method="GET",
            headers={"Accept": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            print(" ✅")
            results["tests"]["health"] = {
                "status": "ok",
                "data": data
            }
    except Exception as e:
        print(f" ❌ ({str(e)[:50]})")
        results["tests"]["health"] = {"status": "failed", "error": str(e)}
        return results

    # Test 2: List Models
    print("   [2/4] List Models...", end="", flush=True)
    try:
        req = urllib.request.Request(
            url=f"{url}/v1/models",
            method="GET",
            headers={"Accept": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            print(" ✅")
            results["tests"]["list_models"] = {
                "status": "ok",
                "count": len(data.get("data", []))
            }
    except Exception as e:
        print(f" ❌ ({str(e)[:50]})")
        results["tests"]["list_models"] = {"status": "failed", "error": str(e)}

    # Test 3: Model Status
    print("   [3/4] Model Status...", end="", flush=True)
    try:
        req = urllib.request.Request(
            url=f"{url}/status/models",
            method="GET",
            headers={"Accept": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            print(" ✅")
            results["tests"]["model_status"] = {
                "status": "ok",
                "active_model": data.get("active_model")
            }
    except Exception as e:
        print(f" ❌ ({str(e)[:50]})")
        results["tests"]["model_status"] = {"status": "failed", "error": str(e)}

    # Test 4: Chat Completion
    print("   [4/4] Chat Completion...", end="", flush=True)
    try:
        payload = {
            "messages": [{"role": "user", "content": "Hi"}],
            "max_tokens": 64
        }
        req = urllib.request.Request(
            url=f"{url}/v1/chat/completions",
            method="POST",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Accept": "application/json"
            }
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            if "choices" in data and len(data["choices"]) > 0:
                response_text = data["choices"][0]["message"]["content"][:50]
                print(f" ✅")
                results["tests"]["chat"] = {
                    "status": "ok",
                    "response_length": len(data["choices"][0]["message"]["content"]),
                    "model_used": data.get("chosen_model"),
                    "tokens": data.get("usage")
                }
            else:
                print(" ⚠️")
                results["tests"]["chat"] = {"status": "partial", "error": "No choices"}
    except urllib.error.HTTPError as e:
        print(f" ❌ (HTTP {e.code})")
        results["tests"]["chat"] = {
            "status": "failed",
            "error": f"HTTP {e.code}"
        }
    except Exception as e:
        print(f" ❌ ({str(e)[:50]})")
        results["tests"]["chat"] = {"status": "failed", "error": str(e)}

    return results


def print_results(results: dict):
    """Pretty print test results"""
    print("\n" + "=" * 70)
    print("TEST RESULTS")
    print("=" * 70)
    print(f"Endpoint: {results['endpoint']}")
    print(f"Model: {results['model']}\n")

    passed = 0
    failed = 0

    for test_name, test_result in results["tests"].items():
        status = test_result.get("status", "unknown")
        if status == "ok":
            print(f"✅ {test_name.upper()}: PASSED")
            passed += 1
        elif status == "partial":
            print(f"⚠️  {test_name.upper()}: PARTIAL")
        else:
            print(f"❌ {test_name.upper()}: FAILED")
            if "error" in test_result:
                print(f"   Error: {test_result['error']}")
            failed += 1

        # Print details if available
        if "data" in test_result and isinstance(test_result["data"], dict):
            for key, value in test_result["data"].items():
                if isinstance(value, (str, int, bool)):
                    print(f"   {key}: {value}")

    summary = f"\n{passed} passed, {failed} failed"
    if failed == 0:
        print("\n" + "=" * 70)
        print("🎉 ALL TESTS PASSED!")
        print("=" * 70)
    else:
        print("\n" + "=" * 70)
        print(f"⚠️  SOME TESTS FAILED{summary}")
        print("=" * 70)

    return failed == 0


def main():
    parser = argparse.ArgumentParser(
        description="Test SupremeAI Dual Model AirLLM Setup"
    )
    parser.add_argument(
        "url",
        help="NGROK public URL (e.g., https://xxxxx.ngrok-free.dev)"
    )
    parser.add_argument(
        "--model",
        default="gemma",
        choices=["gemma", "llama"],
        help="Active model (default: gemma)"
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output as JSON"
    )

    args = parser.parse_args()

    try:
        results = test_endpoint(args.url.rstrip("/"), args.model)

        if args.json:
            print(json.dumps(results, indent=2))
        else:
            success = print_results(results)
            sys.exit(0 if success else 1)

    except KeyboardInterrupt:
        print("\n\n⚠️  Test cancelled")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
