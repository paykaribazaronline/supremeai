# 📱 Feature: Visual Mobile Simulator Runtime (`AdminSimulator.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: VISUAL PLAYBACK]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminSimulator.tsx` / `SimulatorPreview.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Mobile Mock Frame:** আইফোন বা ট্যাবলেট সাইজের ফ্রেম ভিউপোর্ট।
    *   **Simulator Refresh Button:** ভার্চুয়াল পেজ রিলোড করার বোতাম।
    *   **Interactive Input Mapping:** সিমুলেটরে টাইপ ও ফোকাস করার প্যানেল।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `SimulatorController.java` / `SimulatorRuntimeController.java`
*   **প্রধান API রাউট:** `GET /api/simulator/preview/{appId}/screenshot`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "success": true,
      "base64Frame": "iVBORw0KGgoAAAANSUhEUg...",
      "dimensions": { "width": 375, "height": 812 }
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার কোনো ডেভলপ হওয়া অ্যাপের সিমুলেশন পেজ ওপেন করেন।
2. ব্যাকএন্ড সিমুলেটর রানটাইমে অ্যাপটি লোড করে এবং তার স্ক্রিন প্রিভিউ জেনারেট করে।
3. সংগৃহীত মোবাইল ভিউপোর্ট ফ্রন্টএন্ডে রিয়েল-টাইমে লাইভ রেন্ডার হয়।
