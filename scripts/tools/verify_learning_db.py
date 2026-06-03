#!/usr/bin/env python3
"""
Verify SupremeAI Learning Database
This script connects to Firebase and proves that AI learning data is actually stored.
"""
import os
import firebase_admin
from firebase_admin import credentials, firestore

CREDENTIALS_FILE = os.getenv("FIREBASE_CREDENTIALS_FILE")

def check_db():
    print("🔍 Checking SupremeAI Learning Database (Firebase)...")
    try:
        # Initialize Firebase securely
        if not firebase_admin._apps:
            if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
                cred = credentials.Certificate(CREDENTIALS_FILE)
                firebase_admin.initialize_app(cred)
            else:
                # Fallback to default auth (gcloud)
                firebase_admin.initialize_app() 
                
        db = firestore.client()
        collections_to_check = ["system_learning", "patterns", "copilot_error_detection"]
        
        total_records = 0
        for col in collections_to_check:
            docs = list(db.collection(col).stream())
            count = len(docs)
            total_records += count
            print(f"\n✅ Found {count} records in '{col}' collection.")
            
            # Print the first item as proof!
            if count > 0:
                doc_data = docs[0].to_dict()
                print(f"   👉 Example Data Proof: {str(doc_data)[:100]}...")
                
        print("\n" + "=" * 60)
        print(f"🎯 Proof: A total of {total_records} AI Learning patterns are securely stored in your Database!")
        print("=" * 60)
        
    except Exception as e:
        print(f"❌ Could not connect to database. Error: {e}")

if __name__ == "__main__":
    check_db()