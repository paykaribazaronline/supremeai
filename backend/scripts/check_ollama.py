#!/usr/bin/env python3
"""
Ollama Auto-Fix / Health-Check Script
"""
import io
import json
import sys

import httpx


if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

# ── কনফিগ (config.py থেকে) ──────────────────────────────
OLLAMA_URL = "http://127.0.0.1:11434"
MODELS_TO_CHECK = [
    "qwen2.5:0.5b",
    "llama3.2:latest",
]
PULL_TIMEOUT = 300  # seconds — বড় মডেল হিসেবে সময় দিবো

# ANSI রঙ (terminal-র জন্য)
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
RESET = "\033[0m"


def bprint(msg: str, color: str = "") -> None:
    """বাংলা/ইংলিশ মিক্সড প্রিন্ট"""
    print(f"{color}{msg}{RESET}")


# ──────────────────────────────────────────────
# ১. Hebmin Server Health Check
# ──────────────────────────────────────────────
def check_server() -> bool:
    bprint("\n🔍 [ধাপ 1] Ollama সার্ভার চেক করা হচ্ছে...", CYAN)
    try:
        resp = httpx.get(f"{OLLAMA_URL}/api/tags", timeout=5.0)
        if resp.status_code == 200:
            bprint(f"✅ সার্ভার চালু আছে! ({OLLAMA_URL})", GREEN)
            return True
        else:
            bprint(f"❌ সার্ভার রেসপন্স দিল {resp.status_code}", RED)
            return False
    except httpx.ConnectError:
        bprint(f"❌ সার্ভারে কানেক্ট করা যাচ্ছে না! — {OLLAMA_URL}", RED)
        bprint(
            "   🔧 সমাধান: `ollama serve` চালু করুন বা Windows-তে Ollama এপ খুলুন", YELLOW
        )
        return False
    except Exception as e:
        bprint(f"❌ এরর: {e}", RED)
        return False


# ──────────────────────────────────────────────
# ২. दल exceed Downloaded Models List
# ──────────────────────────────────────────────
def list_models() -> list[str]:
    resp = httpx.get(f"{OLLAMA_URL}/api/tags", timeout=10.0)
    resp.raise_for_status()
    data = resp.json()
    return [m["name"] for m in data.get("models", [])]


# ──────────────────────────────────────────────
# ৩. Missing Model আছে কিনা চেক ও Auto-Pull
# ──────────────────────────────────────────────
def ensure_model(model_name: str) -> bool:
    available = list_models()

    if model_name in available:
        bprint(f"  ✅ মডেল '{model_name}' ইতিমধ্যে downloaded আছে", GREEN)
        return True

    bprint(f"  ⚠️  মডেল '{model_name}' পাওয়া যাচ্ছে না, pull শুরু হচ্ছে...", YELLOW)
    bprint("     (এটাতে কিছু সময় লাগতে পারে, কিন্ডি অপেক্ষা করুন)", YELLOW)

    try:
        with httpx.Client(timeout=PULL_TIMEOUT) as client:
            resp = client.post(
                f"{OLLAMA_URL}/api/pull",
                json={"name": model_name, "stream": True},
                timeout=PULL_TIMEOUT,
            )
            last_status = ""
            for line in resp.iter_lines():
                if not line:
                    continue
                try:
                    event = json.loads(line)
                    status = event.get("status", "")
                    # শুধুমাত্র progress বের করবো
                    if status and status != last_status:
                        bprint(f"     ... {status}", CYAN)
                        last_status = status
                    if event.get("completed") and event.get("digest"):
                        digest = event["digest"]
                        bprint(f"     → pulling layer {digest[:20]}...", CYAN)
                    # শেষে status = "success" আসলে Complete
                    if status == "success":
                        bprint(f"  ✅ '{model_name}' Pull সম্পূর্ণ!", GREEN)
                        return True
                except json.JSONDecodeError:
                    continue
            # streme শেষে status==success না হলে
            if last_status == "success":
                bprint(f"  ✅ '{model_name}' Pull সম্পূর্ণ!", GREEN)
                return True
    except httpx.TimeoutException:
        bprint(f"  ❌ '{model_name}' pull হল时间内 শেষ হয়নি ({PULL_TIMEOUT}s)", RED)
        return False
    except Exception as e:
        bprint(f"  ❌ Pull এরর: {e}", RED)
        return False

    return False


# ──────────────────────────────────────────────
# ৪.实际ে Generation Test চালিয়ে দেখি কিনা
# ──────────────────────────────────────────────
def test_generation(model_name: str) -> bool:
    bprint(f"\n🧪 [ধাপ 4] '{model_name}' দিয়ে টেস্ট জেনারেশন চেক...", CYAN)
    payload = {
        "model": model_name,
        "prompt": "Say hello in Bangla: only one short sentence.",
        "stream": False,
    }
    try:
        resp = httpx.post(
            f"{OLLAMA_URL}/api/generate",
            json=payload,
            timeout=60.0,
        )
        resp.raise_for_status()
        data = resp.json()
        text = data.get("response", "").strip()
        if text:
            bprint(f'  ✅ জেনারেশন করছে! উত্তর: "{text[:120]}"', GREEN)
            return True
        else:
            bprint("  ⚠️  রেসপন্স খালি পেলাম", YELLOW)
            return False
    except httpx.TimeoutException:
        bprint("  ❌ জেনারেশন timeout (৬০s)", RED)
        return False
    except Exception as e:
        bprint(f"  ❌ এরর: {e}", RED)
        return False


# ──────────────────────────────────────────────
# মেইন লজিক
# ──────────────────────────────────────────────
def main() -> int:
    bprint("=" * 55)
    bprint("  Ollama Auto-Fix / Health-Check Script (বাংলা explain)", CYAN)
    bprint("=" * 55)

    # Step 1: Health
    if not check_server():
        return 1

    # Step 2 & 3: Ensure each model
    bprint("\n📋 [ধাপ 2-3] মডেল চেক ও auto-pull...", CYAN)
    all_ready = True
    for model in MODELS_TO_CHECK:
        ready = ensure_model(model)
        if not ready:
            all_ready = False

    if not all_ready:
        bprint("\n⚠️  কিছু মডেল পাওয়া যাচ্ছে না, পরবর্তী চক্রেই পুনরায় চেক করবেন।", YELLOW)
        return 1

    # Step 4: Generation Test (সব মডেলের mixin এ কম/common model দ=strategy)
    bprint("\n🧪 [ধাপ 4] টেক্সট জেনারেশন চেক...", CYAN)
    test_model = (
        "qwen2.5:0.5b" if "qwen2.5:0.5b" in list_models() else MODELS_TO_CHECK[0]
    )
    if test_generation(test_model):
        bprint("\n🎉 সবকিছু ঠিক আছে! Ollama এই জবটি করতে পারবে।", GREEN)
        return 0
    else:
        bprint("\n⚠️  মডেল পাওয়া গেলেও generation কাজ করছে না।", YELLOW)
        return 1


if __name__ == "__main__":
    sys.exit(main())
