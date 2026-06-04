# 🖼️ Feature: OCR Text Extractor (`AdminOCR.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: VISION COMPREHENSION]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminOCR.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **File Upload Dragzone:** ইমেজ বা স্ক্রিনশট ফাইল আপলোডের প্রিমিয়াম ড্রপজোন।
    *   **Live Image Cropper:** ইমেজের নির্দিষ্ট অংশ ক্রপ বা কাট করার ফ্রেম উইন্ডো।
    *   **Extracted Text Editor:** ইমেজ থেকে পড়া লেখার ডাইনামিক ওয়ার্ডপ্যাড এডিটর।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `DataController.java` / `analysis/AnalysisController.java`
*   **প্রধান API রাউট:** `POST /api/ocr/extract`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "success": true,
      "text": "ইমেজে পাওয়া বাংলা বা ইংরেজি লেখা...",
      "confidence": 0.96
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার ড্রপজোনে একটি ইমেজ আপলোড করে `EXTRACT TEXT` বোতামে চাপ দেন।
2. ব্যাকএন্ড ইমেজটি নিয়ে লোকাল OCR মডিউল বা সার্ভিস এপিআই-তে পাঠায়।
3. প্রসেসিং শেষে নিষ্কাশিত র টেক্সট ফ্রন্টএন্ড এডিটরে ফেরত এসে ডিসপ্লে হয়।
