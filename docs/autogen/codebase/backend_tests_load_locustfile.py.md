# 📄 ফাইল: backend/tests/load/locustfile.py

**প্রকার:** .py  
**সাইজ:** 4,731 বাইট  
**আপডেট:** 2026-07-11T19:51:42.238894

---

## কোড

```py
import json
import uuid
import time
import random
from locust import HttpUser, task, between, events

# ডাটাবেজ স্ট্রেস করতে র্যান্ডম রিকোয়েস্ট কিওয়ার্ড পুল
TEST_KEYWORDS = ["Amazon laptops", "Zara mens jacket", "Nike running shoes", "Ebay smartphone prices", "H&M summer dress"]


class SupremeAILoadTestUser(HttpUser):
    # রিয়ালিস্টিক ইউজার বিহেভিয়ার: প্রতিটি টাস্কের পর ১ থেকে ৩ সেকেন্ড পজ
    wait_time = between(1.0, 3.0)

    def on_start(self):
        """ইউজার বুট হওয়ার সময় ভ্যালিড এনভায়রনমেন্ট সিক্রেট টোকেন লোড করবে"""
        # প্রডাকশনে পুশ করার সময় secrets বা env থেকে আসল ভ্যালিড টেস্ট টোকেনটি এখানে বসাবে
        self.auth_token = (
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0X3VzZXIiLCJyb2xlIjoiYWRtaW4iLCJpYXQiOjE3MDQxNTM2MDB9.s1MP9mE1Jq_xV8u_D5-uF..."
        )
        self.headers = {"Content-Type": "application/json", "Authorization": f"Bearer {self.auth_token}"}

    @task(2)
    def fetch_skills(self):
        """GET /api/skills এন্ডপয়েন্টের ল্যাটেন্সি এবং ডিবি পুল টেস্ট"""
        # timeout=(connect_timeout, read_timeout) কড়াভাবে মেইনটেইন করা হয়েছে
        with self.client.get("/api/skills", headers=self.headers, timeout=(5, 10), catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed to fetch skills: {response.status_code} - {response.text}")

    @task(1)
    def execute_and_stream_task(self):
        """৮০/১৫/৫ রাউটার, প্লে-রাইট স্যান্ডবক্স এবং এসএসই লং-পোলিং-এর আসল কম্বাইন্ড স্ট্রেস টেস্ট"""
        task_id = f"load-test-task-{uuid.uuid4().hex[:8]}"
        random_keyword = random.choice(TEST_KEYWORDS)

        # ১. POST /api/task (Playwright Blueprint/Sandbox Trigger)
        payload = {
            "task_id": task_id,
            "requirement": f"Track and extract information for {random_keyword} from target store",
            "task_type": "web_scraping",
        }

        # কড়া ৫ সেকেন্ড কানেক্ট টাইমআউট জিজিপি ক্লাউড রানের জন্য
        with self.client.post("/api/task", json=payload, headers=self.headers, timeout=(5, 30), catch_response=True) as response:
            if response.status_code not in [200, 201, 202]:
                response.failure(f"Task submission failed: {response.status_code} - {response.text}")
                return
            response.success()

        # ২. GET /api/task/stream/{task_id} (Real-time SSE Connection Hold)
        start_time = time.time()
        try:
            # stream=True এবং কড়া read_timeout দিয়ে কানেকশন ওপেন রাখা হচ্ছে
            with self.client.get(
                f"/api/task/stream/{task_id}", headers=self.headers, stream=True, timeout=(5, 45), catch_response=True
            ) as sse_response:
                if sse_response.status_code != 200:
                    sse_response.failure(f"SSE Gate Blocked: {sse_response.status_code}")
                    return

                # রিয়েল-টাইম চাঙ্ক বাফার রিড লুপ
                for line in sse_response.iter_lines():
                    if line:
                        # ইভেন্ট রিসিভ সিমুলেশন
                        # কন্টেইনার যেন মেমোরি লিক না করে সেজন্য সর্বোচ্চ ২০ সেকেন্ড কানেকশন ধরে রাখা হবে
                        if time.time() - start_time > 20.0:
                            break

                sse_response.success()

        except Exception as e:  # noqa: BLE001
            events.request.fire(
                request_type="SSE_STREAM",
                name="/api/task/stream/[id]",
                response_time=(time.time() - start_time) * 1000,
                response_length=0,
                exception=e,
            )

```