import os
import urllib.request

api_key = os.environ.get("RENDER_API_KEY", "")
api_key_backup = os.environ.get("RENDER_API_KEY_BACKUP", "")

# প্রতিটি সার্ভিস আইডিকে তার নিজ নিজ অ্যাকাউন্টের API Key-র সাথে ম্যাপ করা হচ্ছে
# বাংলা মন্তব্য: প্রাইমারি ও ব্যাকআপ অ্যাকাউন্টের সঠিক সার্ভিস আইডি সেট করা হলো
service_mappings = [
    {"sid": "srv-d9d3n58js32c738n79k0", "key": api_key},
    {"sid": "srv-d9e4q5rrjlhs73bnh71g", "key": api_key_backup},
]

for service in service_mappings:
    sid = service["sid"]
    key = service["key"]
    if not key:
        print(f"Skipping deploy for {sid}: API key not set")
        continue

    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }

    req = urllib.request.Request(
        f"https://api.render.com/v1/services/{sid}/deploys",
        data=b"{}",
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(req) as response:
            print(f"Triggered deploy for {sid}: {response.status}")
    except Exception as e:
        print(f"Failed deploy for {sid}: {e}")
