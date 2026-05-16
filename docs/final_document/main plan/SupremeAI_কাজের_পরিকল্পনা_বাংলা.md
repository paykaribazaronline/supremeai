# SupremeAI কাজের পরিকল্পনা (Bangla Work Plan)
**তারিখ:** ২০২৬-০৫-১২ | **সংস্করণ:** ১.০

---

## ইউজার ডিটেইলস:

**User ID:** 5o8h2pqr7f  
**Workspace:** /home/nazifarabbu/OneDrive/supremeai  
**Branch:** master  
**Last Commit:** 2026-05-10

---

## সাম্পর্তিক কাজসমূহ (যে কাজগুলো করা হয়েছে):

### Backend (Java/Spring Boot):
1. ✅ AIProviderFactory.java-এ ৭টি নতুন প্রোভাইডার যোগ করা হয়েছে
   - hf_mistral, hf_llama, hf_codellama, hf_phi (HuggingFace)
   - render_tinyllama, render_phi3, render_qwen (Render)

2. ✅ SupremeCloudProvider.java রিফ্যাক্টর করা হয়েছে
   - HF Inference API এবং OpenAI-compatible ফরম্যাট দুটোই সমর্থন

3. ✅ MultiAIVotingService.java আপডেট
   - TEN_AI_MODELS এর বদলে DEFAULT_PROVIDERS + ALL_PROVIDERS ব্যবহার

4. ✅ application.yml আপডেট
   - RENDER_* URL এনভায়রনমেন্ট ভ্যারিয়েবল যোগ

### স্থিতি:
- কোড সফলভাবে কম্পাইল হয় (১১টি ওয়ার্নিং আছে)
- নতুন প্রোভাইডারগুলোর জন্য HuggingFace Inference Endpoints এবং Render Free Tier ব্যবহার করা হবে

---

## বর্তমান সমস্যাবলী (Current Issues):

১. HuggingFace Inference Endpoints স্বয়ংক্রিয়ভাবে ডিপ্লয় করতে হবে
২. Render-এ Docker ইমেজ ডেপ্লয় করতে হবে
৩. এনভায়রনমেন্ট ভ্যারিয়েবলের জন্য এন্ডপয়েন্ট URL সেট করতে হবে
৪. Ensemble voting-এর সাথে নতুন প্রোভাইডারগুলো টেস্ট করতে হবে

---

## পরবর্তী ধাপগুলো (Next Steps):

### উচ্চ অগ্রাধিকার (High Priority):
১. HuggingFace-এ মডেল ডেপ্লয় করা (Serverless Inference Endpoint)
২. Render-এ ছোট মডেল (TinyLlama-1.1B) ডেপ্লয় করা
৩. Environment Variables সেট আপ করা
৪. নতুন AI প্রোভাইডারদের সাথে য ensemble voting টেস্ট

### মাঝার্থে অগ্রাধিকার (Medium Priority):
৫. বাংলা ভাষায় ডকুমেন্টেশন সম্পূর্ণ করা
৬. Vite ড্যাশবোর্ডের জন্য বাংলা i18n সম্পূর্ণ করা
৭. পরীক্ষা চালানো (./gradlew test)

---

## ফাইল লোকেশন (File Locations):

### মূল কোড ফাইল:
- `src/main/java/com/supremeai/provider/AIProviderFactory.java`
- `src/main/java/com/supremeai/provider/SupremeCloudProvider.java`
- `src/main/java/com/supremeai/service/MultiAIVotingService.java`
- `src/main/resources/application.yml`

### বাংলা ডকুমেন্টেশন:
- `docs/final_document/main plan/SupremeAI_প্রকল্প_সম্পূর্ণ_পরিচিতি_বাংলা.md`
- `dashboard/src/i18n/bn.json`
- `supremeai/assets/i18n/bn.json`

---

## কমান্ডসমূহ (Commands):

```bash
# Backend চালানো:
./gradlew bootRun

# টেস্ট চালানো:
./gradlew test

# Dashboard ডেভ মোড:
cd dashboard && npm run dev
```

---

**স্ট্যাটাস:** কাজ চলমান | **পরবর্তী কাজ:** HuggingFace/Render এ মডেল ডেপ্লয়