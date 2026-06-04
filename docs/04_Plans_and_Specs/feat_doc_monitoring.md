# 📻 Feature: System Event & Logging Streams (`AdminMonitoring.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: RUNTIME AUDITING]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminMonitoring.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Live Exception Console:** জাভা এরর বা রানটাইম এক্সেপশন ও ওয়ার্নিং লাইভ স্ট্রিম টার্মিনাল।
    *   **Log Filter Bar:** ইনফো, এরর, ওয়ার্নিং টাইপ অনুযায়ী লগ ছাঁটাই প্যানেল।
    *   **Download Log Archive Button:** পুরো লগ ফাইল জিপ হিসেবে ডাউনলোড করার বোতাম।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `MonitoringController.java`
*   **প্রধান API রাউট:** `GET /api/monitoring/events`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "logs": [
        { "timestamp": "ISO", "level": "ERROR", "message": "NullPointer in..." }
      ]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার মনিটরিং পেজে ঢুকলে ব্যাকএন্ডের সাথে লাইভ পোলিং লুপ সক্রিয় হয়।
2. ব্যাকএন্ড `logback` বা লগার অ্যাপেন্ডার থেকে লাইভ ইভেন্ট ডাটা এক্সট্রাক্ট করে এক্সপ্রেস এপিআই-তে স্ট্রিমিং লুপ পাঠায়।
3. ড্যাশবোর্ডের টার্মিনালে রঙিন ফন্ট দিয়ে লগ ডেটা ভেসে ওঠে।
