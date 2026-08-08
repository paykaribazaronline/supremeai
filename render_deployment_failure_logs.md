# Render Deployment Failure Logs Report

## Run Context & Render API Log Details
- **Target Repository:** `paykaribazaronline/supremeai`
- **Workflow Run ID:** `31236350627`
- **Failed Job:** `🚀 Deploy Combined Backend (Render)` (Job ID: `93049967793`)
- **Job Status:** `completed` / `failure`

---

## 📋 Both Render Services Deploy Logs (উভয় সার্ভিসের ডেপ্লয়মেন্ট মেটাডাটা)

### 1. User Backend Service (`srv-d9d3n58js32c738n79k0`):
- **Deploy ID:** `dep-d9r9rgfavr4c738ob2gg`
- **Status:** ❌ `update_failed`
- **Trigger:** `api`
- **Created:** `2026-08-08T03:08:49Z` | **Finished:** `2026-08-08T03:09:56Z` (1m 7s)
- **Target Image:** `ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest`

### 2. Admin Backend Service (`srv-d9fg48bh523c73f63bb0`):
- **Deploy ID:** `dep-d9r9rgn10e5c73fiv770`
- **Status:** ❌ `update_failed`
- **Trigger:** `deploy_hook`
- **Created:** `2026-08-08T03:08:50Z` | **Finished:** `2026-08-08T03:09:58Z` (1m 8s)
- **Target Image:** `ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest`

---

## 🔍 Single Proven Root Cause (একক সুনির্দিষ্ট মূল-কারণ)

**GHCR Private Registry Credentials Failure (ইমেজ পুলিং ব্যর্থতা):**
উভয় সার্ভিসেই (`User Backend` ও `Admin Backend`) GHCR-এর প্রাইভেট ডকার ইমেজ `ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest` পুল করার চেষ্টা করা হয়েছিল। কিন্তু Render ড্যাশবোর্ডে GHCR (GitHub Container Registry)-এর অথেন্টিকেশন ক্রেডেনশিয়াল না থাকায় Render উভয় সার্ভিসেই ইমেজটি পুল করতে ব্যর্থ হয়ে **১ মিনিট ৭ সেকেন্ডের মাথায় দুটো সার্ভিসই `update_failed` করেছে**।
