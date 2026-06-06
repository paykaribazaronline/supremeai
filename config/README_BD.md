# config/ ফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি সমস্ত সিস্টেম কনফিগারেশন ফাইলগুলো ধারণ করে, যা SupremeAI প্রকল্পের কার্যকারিতার জন্য অপরিহার্য।

## ফাইলগুলো

| ফাইল                             | বিস্তারণ | ব্যবহার                   |
| -------------------------------- | -------- | ------------------------- |
| `core_knowledge.json`            | JSON     | মূল জ্ঞান ডাটাবেস         |
| `autonomous_seed_knowledge.json` | JSON     | স্বয়ংস্রুতি জ্ঞান বীজ    |
| `feature-registry.json`          | JSON     | ফিচার রেজিস্ট্রি          |
| `firebase.json`                  | JSON     | Firebase কনফিগ            |
| `firestore.indexes.json`         | JSON     | Firestore ইন্ডেক্স        |
| `database.rules.json`            | JSON     | ডাটাবেস সিকিউরিটি রুলস    |
| `firestore.rules`                | Rules    | Firestore সিকিউরিটি রুলস  |
| `mcp-config.yml`                 | YAML     | MCP সার্ভার কনফিগ         |
| `rotation_config.json`           | JSON     | API রোটেশন কনফিগ          |
| `service-account.json`           | JSON     | Google সার্ভিস অ্যাকাউন্ট |
| `skills-lock.json`               | JSON     | স্কিল্স লক ডাটা           |

## গুরুত্বপূর্ণ নোট

- এই ফাইলগুলো এডিট করার আগে ব্যাকআপ রাখতে হবে
- প্রোডাকশনে এই ফাইলগুলো সিকিউরিত্বের জন্য এনক্রিপ্ট করা যায়
- `service-account.json` বিশেষভাবে সংবেদনশীল
