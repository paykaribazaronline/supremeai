# ruff: noqa: T201, BLE001, E501, PLW1508, SIM105
import asyncio
import os
import sys
from pathlib import Path

import requests


def setup_env():
    backend_path = Path(__file__).resolve().parent.parent / "backend"
    sys.path.insert(0, str(backend_path))

async def check_billing_anomaly():
    setup_env()
    from core.pubsub import PubSub
    
    # Real API Polling for OpenRouter
    openrouter_key = os.getenv("OPENROUTER_API_KEY")
    current_cost = 0.0
    
    if openrouter_key:
        try:
            resp = requests.get(
                "https://openrouter.ai/api/v1/auth/key", 
                headers={"Authorization": f"Bearer {openrouter_key}"},
                timeout=10
            )
            if resp.status_code == 200:
                data = resp.json()
                usage = data.get("data", {}).get("usage", 0.0)
                current_cost = float(usage)
        except Exception as e:
            print(f"Error fetching OpenRouter billing: {e}")
            
    # Fallback to dummy data if API fails or key is missing
    if current_cost == 0.0:
        current_cost = float(os.getenv("CURRENT_COST", 120.0))
        
    threshold = float(os.getenv("DAILY_COST_THRESHOLD", "100.0"))
    
    if current_cost > threshold:
        discord_url = os.getenv("DISCORD_WEBHOOK_URL")
        if discord_url:
            msg = f"🚨 **DEFCON 1: Billing Spike Detected!**\nCurrent Usage: ${current_cost:.2f} (Threshold: ${threshold})"
            try:
                requests.post(discord_url, json={"content": msg}, timeout=10)
                print("Alert sent to Discord.")
            except Exception as e:
                print(f"Failed to send Discord alert: {e}")
        else:
            print(f"🚨 DEFCON 1: Billing Spike Detected! Usage: ${current_cost:.2f}")
            
        # Trigger System Throttle via unified PubSub
        bus = PubSub()
        await bus.publish("system_events", {"event": "SYSTEM_THROTTLE_REQUIRED", "severity": "high", "cost": current_cost})
        print("Emitted SYSTEM_THROTTLE_REQUIRED to EventBus.")
    else:
        print(f"Billing is within normal thresholds. Current: ${current_cost:.2f}")

if __name__ == "__main__":
    asyncio.run(check_billing_anomaly())
