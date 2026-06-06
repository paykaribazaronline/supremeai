import requests
import json
import time

endpoints = {
    "Qwen Coder": "https://supreme-ai-qwen-coder-565236080752.us-central1.run.app",
    "Llama 3.1": "https://supreme-ai-llama-3-1-565236080752.us-central1.run.app",
    "Phi 3": "https://supreme-ai-phi-3-565236080752.us-central1.run.app",
    "Nomic Embed": "https://supreme-ai-nomic-embed-565236080752.us-central1.run.app",
    "DeepSeek": "https://supreme-ai-deepseek-pro-565236080752.us-central1.run.app",
}


def test_model(name, url):
    print(f"\n🚀 Investigating {name}...")

    # First, list available models
    try:
        tags_response = requests.get(f"{url}/api/tags", timeout=30)
        if tags_response.status_code == 200:
            models_info = tags_response.json().get("models", [])
            if models_info:
                real_name = models_info[0]["name"]
                print(f"📦 Found model: {real_name}")

                # Now test generate
                api_url = f"{url}/api/generate"
                payload = {
                    "model": real_name,
                    "prompt": "Say 'Hello' and nothing else.",
                    "stream": False,
                }
                start_time = time.time()
                response = requests.post(api_url, json=payload, timeout=90)
                duration = time.time() - start_time
                if response.status_code == 200:
                    result = response.json().get("response", "").strip()
                    print(f"✅ Success ({duration:.2f}s): {result}")
                else:
                    print(f"❌ Generate Error {response.status_code}: {response.text}")
            else:
                print(f"⚠️ No models found in /api/tags")
        else:
            print(f"❌ Tags Error {tags_response.status_code}: {tags_response.text}")
    except Exception as e:
        print(f"❌ Connection Failed: {str(e)}")


if __name__ == "__main__":
    print("🌍 SupremeAI Cloud-Native Discovery & Test")
    for name, url in endpoints.items():
        test_model(name, url)
