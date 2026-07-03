# 📄 ফাইল: infrastructure/firebase_functions/firebase_functions_v1/lib/index.d.ts

**প্রকার:** .ts  
**সাইজ:** 362 বাইট  
**আপডেট:** 2026-07-03T15:24:11.490301

---

## কোড

```ts
import * as functions from 'firebase-functions/v1';
/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
export declare const onUserSignUp: functions.CloudFunction<import("firebase-admin/auth").UserRecord>;
//# sourceMappingURL=index.d.ts.map
```