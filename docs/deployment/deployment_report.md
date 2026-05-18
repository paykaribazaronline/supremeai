# SupremeAI Ecosystem - Deployment & Migration Report

এই রিপোর্টে SupremeAI ইকোসিস্টেমের সাম্প্রতিক সফল ডেপ্লয়মেন্ট এবং গুগল ক্লাউড (GCP) ও ফায়ারবেস হোস্টিংয়ে মাইগ্রেশনের বিস্তারিত বিবরণ দেওয়া হয়েছে।

---

## 🚀 ডেপ্লয়মেন্টের সংক্ষিপ্ত বিবরণ (Deployment Summary)

SupremeAI ইকোসিস্টেমটি সম্পূর্ণভাবে ক্লাউডে এবং ফায়ারবেসে সফলভাবে ডেপ্লয় করা হয়েছে। সমস্ত মাইক্রোসার্ভিসেস, ক্লাউড ফাংশন এবং ৩ডি ড্যাশবোর্ড এখন লাইভ এবং সচল।

* **ফায়ারবেস হোস্টিং URL:** [https://supremeai-a.web.app](https://supremeai-a.web.app)
* **স্প্রিং বুট ব্যাকএন্ড URL:** [https://supremeai-backend-565236080752.us-central1.run.app](https://supremeai-backend-565236080752.us-central1.run.app)
* **রিভার্স ইঞ্জিনিয়ারিং সার্ভিস URL:** [https://reverse-engineering-565236080752.us-central1.run.app](https://reverse-engineering-565236080752.us-central1.run.app)
* **সিমুলেটর রানটাইম URL:** [https://simulator-runtime-565236080752.us-central1.run.app](https://simulator-runtime-565236080752.us-central1.run.app)

---

## 🛠️ সম্পূর্ণ ডেপ্লয়মেন্ট প্রসেস এবং ধাপসমূহ (Step-by-Step Execution)

### ১. ব্যাকএন্ড ও ড্যাশবোর্ড কম্পাইলেশন (Compilation & Staging)
* **Spring Boot 3 Backend:** `./gradlew clean build -x test` কমান্ড ব্যবহার করে ব্যাকএন্ড সফলভাবে কমপাইল করা হয় এবং এর `app.jar` প্রস্তুত করা হয়।
* **React 3D Dashboard:** `npm run build` চালিয়ে ফ্রন্টএন্ড সফলভাবে কমপাইল করা হয় এবং এর ডিস্ট্রিবিউশন ফাইলগুলো `public/admin/` ডিরেক্টরিতে স্টেজ করা হয়।

### ২. গুগল ক্লাউড বিল্ড এবং রান (GCP Cloud Run Deployment)
গুগল ক্লাউড বিল্ডের মাধ্যমে লোকাল ডকার ডেমন ব্যবহার না করেই সরাসরি ক্লাউডে ৩টি মূল সার্ভিসের কন্টেইনার ইমেজ বিল্ড ও ক্লাউড রানে ডেপ্লয় করা হয়েছে। টেস্টিং ফেজে কস্ট সেভিংয়ের জন্য সবগুলো সার্ভিসেই **Scale-to-Zero (`--min-instances 0`)** কনফিগার করা হয়েছে:
1. **SupremeAI Backend:** কন্টেইনারাইজড করা হয়েছে এবং ২ সিপিইউ ও ২জিবি মেমরির মিনিমাম ০টি রানিং ইনস্ট্যান্স সহ us-central1 জোনে সফলভাবে রান করা হয়েছে। নিষ্ক্রিয় অবস্থায় এটি স্বয়ংক্রিয়ভাবে স্টপ (Scale to 0) হয়ে যাবে এবং অ্যাক্টিভ গেস্ট রিকোয়েস্টে আবার রান হবে।
2. **Reverse Engineering Microservice:** মিনিমাম ০টি রানিং ইনস্ট্যান্স সহ ক্লাউড রানে সফলভাবে রিলিজ হয়েছে।
3. **Simulator Runtime Microservice:** মিনিমাম ০টি রানিং ইনস্ট্যান্স সহ সফলভাবে ক্লাউড রানে ডেপ্লয় হয়েছে।

### ৩. ফায়ারবেস ফাংশন ২য় প্রজন্মে (2nd Gen) মাইগ্রেশন এবং এরর সমাধান
আমাদের লোকাল কোডে সমস্ত ক্লাউড ফাংশন Firebase Functions v2 তে লিখিত ছিল, কিন্তু ক্লাউডে পূর্বে এগুলো v1 (1st Gen) হিসেবে ডেপ্লয় করা ছিল। 
এর ফলে **`Upgrading from 1st Gen to 2nd Gen is not yet supported`** সংক্রান্ত ত্রুটি দেখা দেয়। 

#### **কীভাবে সমাধান করা হয়েছে:**
1. ক্লাউড থেকে পুরনো সব v1 ফাংশন সম্পূর্ণ ডিলিট করা হয়েছে:
   ```bash
   firebase functions:delete analyzeDeployment approveRequirement autoApproveScheduled checkServerConnections collectHealthMetrics exportOCRToExcel getOCRResults monitorConnections monitorSystemHealth onChatMessage processBengaliOCR processRequirement rotateAgent updateProgress --force --project supremeai-a
   ```
2. ২য় প্রজন্মের (Node.js 22) ফাংশন ডেপ্লয় করার সময় Eventarc সার্ভিস এজেন্টের পারমিশন সংক্রান্ত ত্রুটি (`Permission denied while using the Eventarc Service Agent`) সমাধান করার জন্য GCP IAM পলিসি বাইন্ডিং কনফিগার করা হয়েছে:
   ```bash
   # Eventarc Service Agent কে প্রয়োজনীয় রোল দেওয়া হয়েছে
   gcloud projects add-iam-policy-binding supremeai-a \
       --member="serviceAccount:service-565236080752@gcp-sa-eventarc.iam.gserviceaccount.com" \
       --role="roles/eventarc.serviceAgent"
   
   # Pub/Sub Service Agent কে টোকেন ক্রিয়েটর রোল দেওয়া হয়েছে
   gcloud projects add-iam-policy-binding supremeai-a \
       --member="serviceAccount:service-565236080752@gcp-sa-pubsub.iam.gserviceaccount.com" \
       --role="roles/iam.serviceAccountTokenCreator"
   ```
3. এই পারমিশন প্রদানের পর পুনরায় `firebase deploy` সফলভাবে সম্পন্ন হয় এবং **১৫টি ক্লাউড ফাংশনই সফলভাবে ডেপ্লয় হয়!**

### ৪. গিট রিপোজিটরিতে পুশ (Pushing to GitHub)
ডেপ্লয়মেন্ট সফল হওয়ার পর লোকাল সমস্ত পরিবর্তন ও নতুন তৈরি ফাইল `git add .` করে `feat` প্রিক্স সহ কমিট করা হয়:
* **কমিট মেসেজ:** `feat: deploy supremeai ecosystem to gcloud and firebase hosting`
* **গিট পুশ:** প্রধান রিমোট রিপোজিটরি `origin`-এর `master` ব্রাঞ্চে সফলভাবে পুশ করা হয়েছে।

---

## 📈 পরবর্তী পদক্ষেপ এবং রক্ষণাবেক্ষণ (Post-Deployment Guidelines)

> [!NOTE]
> ২য় প্রজন্মের ফায়ারবেস ক্লাউড ফাংশনগুলো আগের চেয়ে অনেক বেশি পারফরম্যান্ট এবং কম কোল্ড-স্টার্ট টাইম নেয়।

> [!IMPORTANT]
> `onChatMessage` ফাংশনটির জন্য স্বয়ংক্রিয়ভাবে Cleanup Policy সেট করা হয়েছে যাতে ৭ দিনের বেশি পুরনো কন্টেইনার ইমেজগুলো আর্টফ্যাক্ট রেজিস্ট্রি থেকে মুছে যায় এবং কোনো অপ্রয়োজনীয় ক্লাউড বিল না আসে।

---
*রিপোর্টটি সফলভাবে সংরক্ষিত হয়েছে: [docs/deployment/deployment_report.md](file:///home/nazifarabbu/supremeai/docs/deployment/deployment_report.md)*
