#!/usr/bin/env python3
"""
SupremeAI Dual Model Setup Helper
Manages HuggingFace & NGROK token configuration for Gemma 2B + Llama-3.3-70B
"""

import json
import os
import sys
from pathlib import Path


def create_token_config():
    """Interactive token configuration"""
    print("\n" + "=" * 70)
    print("SupremeAI Dual Model Token Configuration")
    print("=" * 70)

    config = {
        "huggingface": {
            "tokens": [],
            "account": "same"
        },
        "ngrok": {
            "tokens": [],
            "accounts": []
        }
    }

    # HuggingFace Tokens
    print("\n🔑 HuggingFace Token Setup (Same Account)")
    print("   Get tokens from: https://huggingface.co/settings/tokens")
    for i in range(2):
        while True:
            token = input(f"   Token #{i+1} (hf_...): ").strip()
            if token.startswith("hf_") and len(token) > 10:
                config["huggingface"]["tokens"].append(token)
                print(f"   ✅ Token #{i+1} saved")
                break
            print("   ❌ Invalid format (must start with 'hf_' and be >10 chars)")

    # NGROK Tokens
    print("\n🔑 NGROK Token Setup (Different Accounts)")
    print("   Get tokens from: https://dashboard.ngrok.com/get-started/your-authtoken")
    accounts_info = ["Account 1 (Primary)", "Account 2 (Fallback)"]
    for i, acc_name in enumerate(accounts_info):
        while True:
            token = input(f"   {acc_name} ({accounts_info[i]}): ").strip()
            if len(token) > 5:
                config["ngrok"]["tokens"].append(token)
                config["ngrok"]["accounts"].append(accounts_info[i])
                print(f"   ✅ {acc_name} saved")
                break
            print("   ❌ Invalid token")

    return config


def generate_colab_setup(config):
    """Generate ready-to-use Colab Cell 2 code"""
    hf_token_1 = config["huggingface"]["tokens"][0]
    hf_token_2 = config["huggingface"]["tokens"][1]
    ngrok_token_1 = config["ngrok"]["tokens"][0]
    ngrok_token_2 = config["ngrok"]["tokens"][1]

    code = f'''# Step 2: Configure Dual Model Setup with Token Rotation
# ========================================================

# DUAL MODELS CONFIGURATION
MODELS = {{
    'gemma': {{
        'id': 'google/gemma-2b-it',
        'compression': '4bit',
        'max_tokens': 256,
        'description': 'Google Gemma 2B (Lightweight, Fast)'
    }},
    'llama': {{
        'id': 'meta-llama/Llama-3.3-70B-Instruct',
        'compression': '4bit',
        'max_tokens': 512,
        'description': 'Meta Llama 3.3 70B (Powerful, Slower)'
    }}
}}

# SELECT WHICH MODEL TO RUN (change to 'gemma' or 'llama')
ACTIVE_MODEL = 'gemma'  # ← CHOOSE: 'gemma' or 'llama'

# HuggingFace Tokens (same account, fallback)
HF_TOKENS = [
    '{hf_token_1}',  # Primary (same account)
    '{hf_token_2}'   # Backup (same account)
]

# NGROK Tokens (different accounts)
NGROK_TOKENS = {{
    'primary': '{ngrok_token_1}',      # Account 1
    'fallback': '{ngrok_token_2}'      # Account 2 (for quick restart)
}}

# Load active model config
MODEL_CONFIG = MODELS[ACTIVE_MODEL]
MODEL_ID = MODEL_CONFIG['id']
COMPRESSION = MODEL_CONFIG['compression']
MAX_NEW_TOKENS = MODEL_CONFIG['max_tokens']
PORT = 8081

print('=' * 60)
print('SupremeAI Dual Model Setup')
print('=' * 60)
print(f'Active Model:    {{ACTIVE_MODEL.upper()}}')
print(f'Model ID:        {{MODEL_ID}}')
print(f'Model Desc:      {{MODEL_CONFIG["description"]}}')
print(f'Compression:     {{COMPRESSION}}')
print(f'Max Tokens:      {{MAX_NEW_TOKENS}}')
print(f'HF Tokens:       {{len(HF_TOKENS)}} available (same account, fallback)')
print(f'NGROK Tokens:    {{len(NGROK_TOKENS)}} available (2 different accounts)')
print('=' * 60)
'''
    return code


def save_config(config, output_file="colab_token_config.json"):
    """Save config to file"""
    with open(output_file, "w") as f:
        json.dump(config, f, indent=2)
    print(f"\n✅ Configuration saved to: {output_file}")


def print_summary(config):
    """Print setup summary"""
    print("\n" + "=" * 70)
    print("✅ SETUP COMPLETE - Summary")
    print("=" * 70)
    print(f"\n🔹 HuggingFace:")
    print(f"   Account: Same (both tokens from same account)")
    print(f"   Token 1: {config['huggingface']['tokens'][0][:20]}...")
    print(f"   Token 2: {config['huggingface']['tokens'][1][:20]}...")
    print(f"\n🔹 NGROK:")
    for i, acc in enumerate(config["ngrok"]["accounts"]):
        token = config["ngrok"]["tokens"][i]
        print(f"   {acc}: {token[:20]}...")
    print(f"\n🔹 Models:")
    print(f"   - google/gemma-2b-it (Fast, 256 tokens)")
    print(f"   - meta-llama/Llama-3.3-70B-Instruct (Powerful, 512 tokens)")
    print("\n" + "=" * 70)


def main():
    print("\n🚀 SupremeAI Dual Model Setup")
    
    if len(sys.argv) > 1 and sys.argv[1] == "--config":
        # Load existing config
        try:
            with open("colab_token_config.json", "r") as f:
                config = json.load(f)
            print_summary(config)
            return
        except FileNotFoundError:
            print("❌ Config file not found")
            return

    # Interactive setup
    try:
        config = create_token_config()
        print_summary(config)
        
        # Generate Colab code
        colab_code = generate_colab_setup(config)
        
        # Save
        save_config(config)
        
        # Print Colab code
        with_colab = input("\n📝 Print Colab Cell 2 code? (y/n): ").strip().lower()
        if with_colab == "y":
            print("\n" + "=" * 70)
            print("COPY THIS CODE INTO COLAB CELL 2:")
            print("=" * 70)
            print(colab_code)
            print("=" * 70)
        
        print("\n✅ Setup complete!")
        print("📍 Next: Open Colab and paste the code into Cell 2")
        
    except KeyboardInterrupt:
        print("\n\n⚠️  Setup cancelled")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
