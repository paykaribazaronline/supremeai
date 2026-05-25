# 🔍 Feature: DOM Reverse Engineer Parser (`AdminReverseEngineer.tsx`)

> **[CLASSIFICATION: WEB TOPOLOGY]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminReverseEngineer.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Web Topology Canvas:** রিভার্স ইঞ্জিন দিয়ে পড়া যেকোনো অ্যাপের বাটনের পিক্সেল পজিশনিং গ্রিড।
    *   **Interactive Node Map:** কঙ্কাল আকারে সাজানো DOM নোড ও অ্যাক্সেসিবিলিটি এলিমেন্ট রিডার।
    *   **Reverse Logic Console:** সাইটের ব্যাকগ্রাউন্ড জাভাস্ক্রিপ্ট স্ক্রিপ্ট জেনারেটর প্যানেল।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `analysis/AnalysisController.java`
*   **প্রধান API রাউট:** `POST /api/analysis/reverse-dom`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "nodes": [
        { "id": "BTN_1", "role": "button", "label": "Submit", "x": 450, "y": 210 }
      ]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ব্রাউজার অটোমেশন পেজ লোড শেষ করলে ইউজার `Reverse DOM` বাটনে চাপ দেন।
2. ব্যাকএন্ড পেজের accessibility HTML সোর্স স্ট্রাকচার ফিল্টার করে র ডম স্ট্রাকচার তৈরি করে।
3. ড্যাশবোর্ডে অ্যাপটির বাটনের গ্রাফিক্যাল নোড কানেক্টিভিটি অ্যানিমেশন শো করে।
