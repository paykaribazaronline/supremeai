#!/usr/bin/env python3
import json, os, sys, urllib.request

ml_url     = sys.argv[1] if len(sys.argv) > 1 else ""
ml_api_key = sys.argv[2] if len(sys.argv) > 2 else ""
model_name = sys.argv[3] if len(sys.argv) > 3 else ""
project_id = sys.argv[4] if len(sys.argv) > 4 else ""

payload = {
    "fields": {
        "mlServiceUrl":  {"stringValue": ml_url},
        "mlApiKey":      {"stringValue": ml_api_key},
        "modelName":     {"stringValue": model_name},
        "maxTextLength": {"integerValue": 5000},
        "rateLimitRps":  {"integerValue": 100},
        "updatedAt":     {"timestampValue": ""},
    }
}

try:
    token = ""
    if os.path.exists("/tmp/.gcloud_fs_token"):
        token = open("/tmp/.gcloud_fs_token").read().strip()
    if not token:
        print("SKIP  no gcloud token — Firestore doc managed by Terraform.")
        sys.exit(0)

    url = "https://firestore.googleapis.com/v1/projects/{pid}/databases/(default)/documents/ml_config/sentiment-ml-v1".format(pid=project_id)
    req = urllib.request.Request(
        url + "?currentDocument.exists=true",
        data    = json.dumps(payload).encode("utf-8"),
        headers = {
            "Authorization": "Bearer " + token,
            "Content-Type":  "application/json",
        },
        method = "PATCH",
    )
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
    print("OK  Firestore ML config updated: " + result.get("name", "sentiment-ml-v1"))
except Exception as exc:
    print("SKIP  Firestore update: " + str(exc))
