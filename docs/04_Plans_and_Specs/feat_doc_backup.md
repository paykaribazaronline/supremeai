# 💾 Feature: Database Emulator Backup Snapshots (`AdminBackup.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: SYSTEM RECOVERY]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminBackup.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Backup History Grid:** বিগত ব্যাকআপ ফাইলগুলোর সাইজ ও টাইমের তালিকা।
    *   **Trigger Backup Button:** ফায়ারস্টোর এমুলেটর এক্সপোর্টের বোতাম।
    *   **Zip File Restorer:** ব্যাকআপ আপলোড করে ডাটা রিকভার করার ড্রপজোন উইজেট.

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `FirebaseEmulatorController.java`
*   **প্রধান API রাউট:** `POST /api/backup/export`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "success": true,
      "snapshotPath": "/workspace/backups/snapshot_177969.zip",
      "timestamp": "ISO Date"
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার `Trigger Backup` বাটনে ক্লিক করলে এপিআই ব্যাকএন্ডকে রিকোয়েস্ট পাঠায়।
2. ব্যাকএন্ড অপারেটিং সিস্টেম শেলে `firebase emulators:export` কমান্ড রান করায়।
3. এমুলেটরের সম্পূর্ণ র ডাটা জিপ ফাইল আকারে ব্যাকআপ ডিরেক্টরিতে সেভ হয়ে ড্যাশবোর্ডে স্ট্যাটাস দেখায়।
