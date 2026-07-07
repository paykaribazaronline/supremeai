# 📄 ফাইল: apps/desktop/src-tauri/secure-store.ts

**প্রকার:** .ts  
**সাইজ:** 1,202 বাইট  
**আপডেট:** 2026-07-07T22:11:19.840495

---

## কোড

```ts
// apps/desktop/src-ui/src/lib/secure-store.ts

import { Store } from "tauri-plugin-store-api";

// একটি নতুন স্টোর তৈরি করুন। এটি একটি .dat ফাইল তৈরি করবে।
const store = new Store(".settings.dat");

const JWT_KEY = "supremeai_jwt";

/**
 * নিরাপদে JWT টোকেন সংরক্ষণ করে।
 * @param token - সংরক্ষণ করার জন্য JWT টোকেন।
 */
export async function setSecureToken(token: string): Promise<void> {
    await store.set(JWT_KEY, token);
    await store.save(); // পরিবর্তনগুলো ডিস্কে ফ্লাশ করে
}

/**
 * সংরক্ষিত JWT টোকেন পুনরুদ্ধার করে।
 * @returns সংরক্ষিত টোকেন অথবা null।
 */
export async function getSecureToken(): Promise<string | null> {
    return await store.get<string>(JWT_KEY);
}

/**
 * সংরক্ষিত JWT টোকেন মুছে ফেলে।
 */
export async function removeSecureToken(): Promise<void> {
    await store.delete(JWT_KEY);
    await store.save();
}
```