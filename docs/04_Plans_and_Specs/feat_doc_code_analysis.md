# 💻 Feature: Interactive Codebase Explorer (`AdminCodeAnalysis.tsx`)

> **Status:** 🟢 Updated for v5 Architecture

> **[CLASSIFICATION: COGNITIVE AGENT]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)

- **কম্পোনেন্ট:** `dashboard/src/pages/AdminCodeAnalysis.tsx`
- **ভিজুয়াল উইজেট:**
  - **Project File Tree Explorer:** ডিরেক্টরির ফাইল ও সাব-ডিরেক্টরির ইন্টারেক্টিভ কাস্টম ট্রি লিস্ট।
  - **Syntax Code Reader:** ক্লিক করা ফাইলের কোড সুন্দর সিনট্যাক্স লাইট কালারে রেন্ডার করে।
  - **AI Code Insight Panel:** কোডের পসিবল বাগ বা এরর নিয়ে এআই অ্যানালাইসিস রিডার।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)

- **কন্ট্রোলার:** `analysis/AnalysisController.java` / `IdeAssistantController.java`
- **প্রধান API রাউট:** `GET /api/analysis/files`
- **ডেটা ফরম্যাট:**
  ```json
  {
    "filePath": "src/main/resources/application.properties",
    "content": "raw properties text...",
    "suggestions": ["Add database timeout configuration"]
  }
  ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)

1. ইউজার প্রজেক্ট ট্রির যেকোনো জাভা বা জেএস ফাইলে ক্লিক করেন।
2. ফ্রন্টএন্ড সোর্স ফাইল পাথ এপিআই গেটওয়ে দিয়ে ব্যাকএন্ডে পাঠায়।
3. ব্যাকএন্ড জাভা সোর্স থেকে লাইন বাই লাইন ফাইল রিড করে ড্যাশবোর্ডে দেখায় এবং এআই কোড রিভিউ ডিসপ্লে করে।
