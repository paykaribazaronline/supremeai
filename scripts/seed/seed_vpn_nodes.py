#!/usr/bin/env python3
"""
SupremeAI VPN Nodes Seed Script
Seeds Firebase with a pool of VPN/Proxy nodes for the Browser Automation engine.
This ensures Playwright can bypass bot-detection, CAPTCHAs, and IP tracking.
"""

import uuid
import time
import os

FIREBASE_PROJECT_ID = "supremeai-a"
CREDENTIALS_FILE = os.getenv("FIREBASE_CREDENTIALS_FILE")

VPN_NODES = {
    "vpn_us_east_01": {
        "id": "vpn_us_east_01",
        "region": "US-East (New York)",
        "host": "us-east-stealth.supremeai.net",
        "port": 443,
        "protocol": "SOCKS5",
        "isPremium": True,
        "isActive": True,
        "stealthEnabled": True,
        "lastTestedAt": int(time.time() * 1000)
    },
    "vpn_eu_west_01": {
        "id": "vpn_eu_west_01",
        "region": "EU-West (London)",
        "host": "eu-west-proxy.supremeai.net",
        "port": 8080,
        "protocol": "HTTP",
        "isPremium": False,
        "isActive": True,
        "stealthEnabled": True,
        "lastTestedAt": int(time.time() * 1000)
    },
    "vpn_asia_sg_01": {
        "id": "vpn_asia_sg_01",
        "region": "Asia (Singapore)",
        "host": "sg-secure.supremeai.net",
        "port": 8443,
        "protocol": "SOCKS5",
        "isPremium": True,
        "isActive": True,
        "stealthEnabled": True,
        "lastTestedAt": int(time.time() * 1000)
    }
}

if __name__ == "__main__":
    import sys
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        print("❌ Firebase Admin SDK not installed. Run: pip install firebase-admin")
        sys.exit(1)

    print("🚀 Seeding SupremeAI VPN / Proxy Pool...")
    
    try:
        if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
            cred = credentials.Certificate(CREDENTIALS_FILE)
            firebase_admin.initialize_app(cred)
        else:
            firebase_admin.initialize_app()
            
        db = firestore.client()
        col = db.collection("vpns")
        
        for doc_id, data in VPN_NODES.items():
            col.document(doc_id).set(data)
            
        print(f"✅ Successfully seeded {len(VPN_NODES)} VPN nodes into Firestore 'vpns' collection.")
    except Exception as e:
        print(f"❌ Seeding failed: {e}")