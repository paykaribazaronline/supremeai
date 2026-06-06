import os
import re
import firebase_admin
from firebase_admin import credentials, firestore

# Configuration
MASTER_DOC_PATH = r"c:\Users\n\supremeai\docs\04_Plans_and_Specs\main plan\SupremeAI_Complete_Documentation.md"
# Default path to service account; can be overridden via environment variable
CRED_PATH = os.getenv("FIREBASE_CREDENTIALS_FILE", r"c:\Users\n\supremeai\src\main\resources\firebase-service-account.json")

def update_plan_progress():
    """
    Synchronizes Plan completion percentages in the Master Documentation
    with real-time data from Firestore 'plans' collection.
    """
    print("🚀 Starting Plan Progress Synchronization...")

    # 1. Initialize Firestore
    if not os.path.exists(CRED_PATH):
        print(f"❌ Error: Firebase credentials not found at {CRED_PATH}")
        print("   Please set FIREBASE_CREDENTIALS_FILE or place the key in src/main/resources/")
        return

    try:
        if not firebase_admin._apps:
            cred = credentials.Certificate(CRED_PATH)
            firebase_admin.initialize_app(cred)
        db = firestore.client()
    except Exception as e:
        print(f"❌ Error initializing Firebase: {e}")
        return

    # 2. Fetch data from Firestore
    # Expects a 'plans' collection where document IDs are plan numbers (e.g., "1", "22")
    # and contain 'completion' (numeric 0-100) and 'status' (string) fields.
    plan_data = {}
    try:
        plans_ref = db.collection("plans")
        docs = plans_ref.stream()
        for doc in docs:
            data = doc.to_dict()
            plan_data[doc.id] = {
                "completion": str(data.get("completion", "")),
                "status": str(data.get("status", "")).lower().strip()
            }
        print(f"📊 Fetched data for {len(plan_data)} plans from Firestore.")
    except Exception as e:
        print(f"❌ Error fetching Firestore data: {e}")
        return

    if not plan_data:
        print("⚠️ No plan progress found in Firestore collection 'plans'.")
        return

    # 3. Read and Parse Master Doc
    if not os.path.exists(MASTER_DOC_PATH):
        print(f"❌ Error: Master Doc not found at {MASTER_DOC_PATH}")
        return

    with open(MASTER_DOC_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    updated_content = content
    changes_count = 0

    # Regex to find markdown table rows starting with a Plan ID.
    # Groups: 1=FullMatch, 2=PlanID, 3=Name, 4=Status, 5=Prefix (~), 6=Old Percent, 7=Suffix
    row_pattern = re.compile(r"(\| (\d+)\s+\| (.*?) \| (.*?) \| (\s*~?)(\d+)% (\s*\|)")

    def row_updater(match):
        nonlocal changes_count
        plan_id = match.group(2)
        name_col = match.group(3)
        status_col = match.group(4)
        prefix = match.group(5)
        percent_val = match.group(6)
        suffix = match.group(7)

        if plan_id in plan_data:
            data = plan_data[plan_id]
            new_percent = data["completion"]
            new_status_raw = data["status"]
            
            # Map status if recognized, otherwise keep existing string
            new_status_formatted = STATUS_MAPPING.get(new_status_raw, status_col.strip())
            
            row_changed = False
            final_status = status_col
            final_percent = percent_val

            if new_percent and percent_val != new_percent:
                final_percent = new_percent
                row_changed = True
            
            if new_status_raw and status_col.strip() != new_status_formatted:
                final_status = f" {new_status_formatted} "
                row_changed = True

            if row_changed:
                changes_count += 1
                return f"| {plan_id} | {name_col} |{final_status}| {prefix}{final_percent}% {suffix}"
        
        return match.group(0)

    updated_content = row_pattern.sub(row_updater, content)

    # 4. Save results if modified
    if changes_count > 0:
        with open(MASTER_DOC_PATH, "w", encoding="utf-8") as f:
            f.write(updated_content)
        print(f"✅ Success! Updated {changes_count} progress indicators in the Master Doc.")
    else:
        print("✨ Perfection: Master Doc is already in sync with Firestore.")

if __name__ == "__main__":
    update_plan_progress()