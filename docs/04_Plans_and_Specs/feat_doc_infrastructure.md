# 🖥️ Feature: JVM & Server Infrastructure Indicator (`AdminInfrastructure.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: SYSTEM HEALTH]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminInfrastructure.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **OS Storage Meter:** সার্ভারের ডিস্ক ব্যবহারের ধারণক্ষমতার গোল রিঙিং গেজ।
    *   **JVM Memory Monitor:** জাভা ভার্চুয়াল মেশিনের সক্রিয় মেমোরি কালেকশন ট্র্যাকার।
    *   **Thread Execution Grid:** সক্রিয় থ্রেড ও তাদের পারফরম্যান্স বার।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `AdminInfrastructureController.java`
*   **প্রধান API রাউট:** `GET /api/infrastructure/health`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "diskFreeBytes": 45899345920,
      "jvmAllocatedBytes": 2147483648,
      "activeThreads": 24
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ফ্রন্টএন্ড পেজটি ইনফ্রাস্ট্রাকচার মেট্রিক্সের এপিআই ৩ সেকেন্ড পরপর রিমোট কুয়েরি করে।
2. ব্যাকএন্ডে জাভা `OSMXBean` থেকে অপারেটিং সিস্টেমের ডিস্ক ও প্রসেস ডাটা রিড করে।
3. প্রোগ্রেস উইজেটে ডাইনামিক লাইভ মেট্রিক্স রেন্ডার হয়।
