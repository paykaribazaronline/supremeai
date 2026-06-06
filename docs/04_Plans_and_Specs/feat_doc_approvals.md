# 🗳️ Feature: Action Approvals Gateway (`AdminApprovals.tsx`)

> **Status:** 🟢 Updated for v5 Architecture

> **[CLASSIFICATION: AGENT GOVERNANCE]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)

- **কম্পোনেন্ট:** `dashboard/src/pages/AdminApprovals.tsx`
- **ভিজুয়াল উইজেট:**
  - **Pending Actions Ledger:** অনুমোদনের অপেক্ষায় থাকা স্পর্শকাতর এআই সিদ্ধান্তের কার্ড গ্রিড।
  - **Approve / Reject Action Buttons:** ইউজারের ডিসিশন ইনপুট বোতাম।
  - **Change Diff Viewer:** এআই কোডের কী পরিবর্তন করতে চায়, তার ডাবল-প্যানেল ডিফারেন্স ভিউয়ার।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)

- **কন্ট্রোলার:** `VotingController.java`
- **প্রধান API রাউট:** `POST /api/approvals/vote`
- **ডেটা ফরম্যাট:**
  ```json
  {
    "actionId": "ACT_9011",
    "status": "APPROVED",
    "targetFile": "api-router.js"
  }
  ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)

1. এআই কোনো গুরুত্বপূর্ণ ফাইল চেঞ্জ করার সিদ্ধান্ত নিলে ব্যাকএন্ড থ্রেডটি পজ (Pause) করে ফায়ারবেসে পেন্ডিং ট্র্যাকার তৈরি করে।
2. ফ্রন্টএন্ড পেন্ডিং ডিসিশন কার্ড লোড করে ইউজারের মতামত চায়।
3. ইউজার এপ্রুভ বাটনে চাপ দিলে ফ্লো আনলক হয় এবং চেঞ্জ কমিট হয়ে যায়।
