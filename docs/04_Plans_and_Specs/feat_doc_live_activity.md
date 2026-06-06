# ⏳ Feature: Session Live Activity Tracker (`AdminLiveActivity.tsx`)

> **Status:** 🟢 Updated for v5 Architecture

> **[CLASSIFICATION: SYSTEM AUDITING]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)

- **কম্পোনেন্ট:** `dashboard/src/pages/AdminLiveActivity.tsx`
- **ভিজুয়াল উইজেট:**
  - **Active Session Feed:** বর্তমানে ড্যাশবোর্ডে লগইন থাকা লাইভ ডেভেলপার বা ইউজারের কাজের স্ট্যাটাস ফিড।
  - **Timeline Activity Map:** প্ল্যাটফর্মে কবে কোন সময় সর্বোচ্চ কাজ হয়েছে তার টাইমলাইন প্রোগ্রেস চার্ট।
  - **Active Session Ban Trigger:** স্পর্শকাতর সেশন ব্লক করার বাটন।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)

- **কন্ট্রোলার:** `com.supremeai.controller.ChatController.java`
- **প্রধান API রাউট:** `GET /api/admin/chat/actions/pending`
- **ডেটা ফরম্যাট:**
  ```json
  {
    "activeSessionsCount": 2,
    "sessions": [{ "id": "SES_771", "user": "admin", "duration": "45m" }]
  }
  ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)

1. লাইভ ট্র্যাকিং পেজ চালু হলে ফ্রন্টএন্ড এক্সপ্রেস বা স্প্রিং সেশন গেটওয়ে কুয়েরি করে।
2. ব্যাকএন্ড সক্রিয় সেশন অথ টোকেন ভ্যালিডেশন স্টেট ও কানেকশন স্পিড ক্যালকুলেট করে।
3. লাইভ সেশনগুলোর প্রোফাইল টেবিল ড্যাশবোর্ডে রেন্ডার হয়।
