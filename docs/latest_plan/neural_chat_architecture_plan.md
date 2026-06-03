# 🧠 NEURAL_CHAT_INTELLIGENCE_BN: 100% Active Hybrid Agentic Routing

This document defines the finalized **"Local-First, Hybrid Nano-Cloud, Magic Loop & Failover"** architecture for Neural Chat. It ensures that the AI answers 100% of user queries instantly, intelligently, and without expensive cloud infrastructure.

---

## ১. Smart Intent Routing & Edge Processing (লেভেল-০ এবং লেভেল-১)
যখন ইউজার কোনো মেসেজ পাঠায়, অন-ডিভাইস এজ মডেল **SuperFly (94M)** এবং `AutonomousQuestioningEngine` প্রথমেই তার উদ্দেশ্য (Intent) নির্ধারণ করে:
*   **GREETING (অভিবাদন):** ইউজার যদি "hi", "hello", "কেমন আছেন" লেখে, তবে সিস্টেমটি ক্লাউডে কোনো API কল করবে না। এটি মোবাইলের অন-ডিভাইস মেমরিতে রানিং **SuperFly** মডেল থেকে সরাসরি তাৎক্ষণিকভাবে (০.১ সেকেন্ডে) উত্তর দেবে।
*   **AMBIGUOUS (অস্পষ্ট প্রশ্ন):** যদি প্রশ্নটি অসম্পূর্ণ হয়, তবে এআই সরাসরি "বুঝতে পারিনি" না বলে ইউজারকে **Interactive Clarification** অপশন (৩-৪টি সম্ভাব্য প্রশ্ন এবং একটি Custom Input) প্রদান করবে।
*   **FACTUAL / TASK:** বৈধ প্রশ্নগুলো পরবর্তী ধাপে (RAG/Hybrid Routing) পাঠানো হবে।

---

## ২. Precision RAG & Pure Java NLP (লেভেল-২)
বৈধ প্রশ্নগুলোর জন্য সিস্টেম প্রথমে লোকাল ফায়ারবেস মেমরিতে (`system_learning`) উত্তর খুঁজবে:
*   এখানে কোনো দামি এক্সটার্নাল এআই বা API ব্যবহার করা হয় না।
*   এটি সম্পূর্ণ নিজস্ব **Pure Java N-Gram (Tri-gram) Cosine Similarity** অ্যালগরিদম ব্যবহার করে সঠিক মিল খুঁজে বের করতে পারে।
*   যদি উত্তরের মিল বা Similarity Score **০.৫৫ (৫৫%)** এর বেশি হয়, তবে এটি সরাসরি সেই ডেটা ব্যবহার করে উত্তর তৈরি করবে।

---

## ৩. The Magic Loop, Dynamic Web Navigation & ChickenBrain (লেভেল-৩)
যদি লোকাল ডেটাবেসে সঠিক উত্তর না থাকে (Score < 0.55), তবে সিস্টেম একটি অসাধারণ "ম্যাজিক লুপ" বা সেন্ট্রাল ক্লাউড প্রসেসিং চালু করে:
*   **ChickenBrain Cloud Processing:** সেন্ট্রাল ক্লাউড প্রসেসর হিসেবে আমরা অত্যন্ত লাইটওয়েট এবং কোয়ান্টাম-কম্প্রেসড **ChickenBrain** মডেলটি ব্যবহার করি (যা Ollama/vLLM সার্ভারে রান করে)। এটি পূর্বের অত্যন্ত ব্যয়বহুল ও ভারী এআই মডেলগুলোকে রিপ্লেস করেছে।
*   **Dynamic Target URL Generation:** ChickenBrain নিজে থেকে সিদ্ধান্ত নেয় এই প্রশ্নের উত্তরটি কোন ওয়েবসাইট থেকে সবচেয়ে ভালো পাওয়া যাবে (যেমন: StackOverflow, pub.dev)।
*   **Parallel Scraping:** `BrowserService` সেই নির্দিষ্ট লিংকে বা ডিফল্ট সার্চ ইঞ্জিনে গিয়ে রিয়েল-টাইম ডেটা স্ক্র্যাপ করে আনে।
*   **Synthesis & Auto-Learning:** স্ক্র্যাপড ডেটা থেকে ChickenBrain একটি নিখুঁত, আপ-টু-ডেট এবং সুগঠিত উত্তর তৈরি করে এবং তা স্বয়ংক্রিয়ভাবে ডেটাবেসে (`system_learning`) সেভ করে রাখে।

---

## ৪. Tiny AI Pocket Lab & Multi-AI Failover (লেভেল-৪)
ক্লাউড ব্যাকএন্ড যদি ডাউন থাকে, রেট-লিমিট খায় বা কোনো অত্যন্ত জটিল কোড অডিটিংয়ের প্রয়োজন হয়:
*   **Tiny AI Pocket Lab Edge Tunneling:** সিস্টেমটি স্বয়ংক্রিয়ভাবে প্রিমিয়াম ব্যবহারকারীর নিজস্ব **Tiny AI Pocket Lab** ডিভাইসে (৮০জিবি র‍্যাম সমৃদ্ধ লোকাল সুপারকম্পিউটার) এপিআই রিকোয়েস্ট ফরোয়ার্ড করবে। এর মাধ্যমে ক্লাউড কস্ট সম্পূর্ণ জিরো হয়ে যায় এবং ডেটার সর্বোচ্চ গোপনীয়তা বজায় থাকে।
*   **Multi-AI Failover Guarantee:** লোকাল নোড উপলব্ধ না থাকলে এটি স্বয়ংক্রিয়ভাবে `MultiAIVotingService` (এআই কাউন্সিল)-কে কল করবে, যা ব্যাকগ্রাউন্ডে ৩টি ব্যাকআপ লাইটওয়েট এআই দিয়ে বেস্ট উত্তরটি তৈরি করে ১০০% আপটাইম নিশ্চিত করবে।
