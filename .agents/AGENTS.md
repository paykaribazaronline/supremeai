# SupremeAI 2.0 Agent Rules

- **Full Path Terminal Commands (Bangla/English):** Whenever suggesting or explaining terminal commands (e.g. `npx`, `pnpm`, `npm`, `python`, `git`), always provide the commands with the exact target directory location or full absolute path (e.g. specifying `F:\supremeai backup\apps\studio-client` or using `cd` guides with full paths) so that the user knows exactly where to run the command.

- **Render Deployment Failure Logging (Bangla/English):** যেকোনো সময় Render ডিপ্লয় ফেইল করলে (Render Deploy Failure), প্রতিটি ফেইল্ড সার্ভিসের সম্পূর্ণ র (raw) লগ সংগ্রহ করে `render_deployment_failure_logs.md` নামে একটি ডেসক্রিপ্টিভ মার্কডাউন ফাইল (Artifact) তৈরি করতে হবে।
