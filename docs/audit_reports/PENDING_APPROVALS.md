# Pending Admin Approvals (HIGH-tier changes)

_নিয়ম: [`AGENT_GOVERNANCE_ADDENDUM.md`](../long-term-maintenance/AGENT_GOVERNANCE_ADDENDUM.md)-এর Blast-Radius Classification অনুযায়ী কোনো কাজ HIGH-tier হলে এখানে row যোগ হবে, apply হবে না যতক্ষণ না admin "APPROVED" লেখেন।_

**Admin-এর কাজ:** নিচের টেবিলে Decision কলামে লিখুন `APPROVED` বা `REJECTED` — ব্যাখ্যা লেখার দরকার নেই, এক শব্দই যথেষ্ট।

| ID | তারিখ | কী পরিবর্তন | কেন HIGH-tier | Diff/লোকেশন | Decision |
|---|---|---|---|---|---|
| CQ-002 | 2026-08-06 | `service_preflight_check.py`-এ Render/Vercel check repo-aware করা (staging repo-তে blocking না রেখে warning-only) | CI gate logic, deploy-related | চ্যাট history-তে patch ready আছে, এখনো repo-তে apply হয়নি | _(pending)_ |
