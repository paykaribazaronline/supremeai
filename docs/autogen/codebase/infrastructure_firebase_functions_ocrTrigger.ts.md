# 📄 ফাইল: infrastructure/firebase_functions/ocrTrigger.ts

**প্রকার:** .ts  
**সাইজ:** 456 বাইট  
**আপডেট:** 2026-07-05T20:27:26.369037

---

## কোড

```ts
# SupremeAI — Firebase OCR Trigger
Provides a sample Cloud Function (Realtime Database + Firestore) that initiates an OCR task when a document is queued.
Use this as a reference; integrate into your actual functions source.

### Realtime Database reference implementation
- Database path: `/ocr-queue/{pushId}`
- Expected fields: `{ file_path: string, mime: string }`
- Result: writes `{ status: 'completed', result: any }` under `/ocr-results/{pushId}`

```