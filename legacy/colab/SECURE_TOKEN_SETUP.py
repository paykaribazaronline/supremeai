#!/usr/bin/env python3
"""
SupremeAI Colab Setup - Setup Environment Variables Only (No Secrets in Repo)
Run this to configure your tokens for Colab without committing them to GitHub
"""

import sys
from pathlib import Path

def create_env_file():
    """Create .env file with tokens for Colab"""
    print("\n🔐 SupremeAI Colab Secure Setup")
    print("=" * 70)
    print("This creates a .env file (NOT committed to GitHub) with your tokens")
    print("=" * 70)

    env_file = Path.home() / ".supremeai_colab.env"
    
    print("\n📝 Configuration to paste in Colab Cell (before Cell 2):")
    print("=" * 70)
    print("""
# ADD THIS TO COLAB BEFORE RUNNING CELL 2:
from google.colab import userdata

HF_TOKEN_1 = userdata.get('HF_TOKEN_1')
HF_TOKEN_2 = userdata.get('HF_TOKEN_2')
NGROK_TOKEN_1 = userdata.get('NGROK_TOKEN_1')
NGROK_TOKEN_2 = userdata.get('NGROK_TOKEN_2')

import os
os.environ['HF_TOKEN_1'] = HF_TOKEN_1
os.environ['HF_TOKEN_2'] = HF_TOKEN_2
os.environ['NGROK_TOKEN_1'] = NGROK_TOKEN_1
os.environ['NGROK_TOKEN_2'] = NGROK_TOKEN_2

print("✅ Tokens loaded from Colab Secrets")
""")
    print("=" * 70)

    print("\n📍 How to add Secrets to Colab:")
    print("=" * 70)
    print("""
1. In Colab, left sidebar → 🔑 Secrets
2. Create NEW secret "HF_TOKEN_1" → Paste: hf_YOUR_FIRST_TOKEN_HERE
3. Create NEW secret "HF_TOKEN_2" → Paste: hf_YOUR_SECOND_TOKEN_HERE
4. Create NEW secret "NGROK_TOKEN_1" → Paste: your_ngrok_account_1_token_here
5. Create NEW secret "NGROK_TOKEN_2" → Paste: your_ngrok_account_2_token_here
6. Click "Add secret"
7. Now run the Cell above to load them into environment variables
""")
    print("=" * 70)

    print("\n✅ Why This Approach?")
    print("- Your secrets NEVER committed to GitHub")
    print("- Secrets stored safely in Colab's secure vault")
    print("- No risk of credential leak in git history")
    print("- Easy to rotate tokens without code changes")

if __name__ == "__main__":
    create_env_file()
