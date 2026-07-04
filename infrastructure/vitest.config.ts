// বাংলা মন্তব্য: Cloudflare Worker (cloudflare_worker.js) এর ইউনিট টেস্টের জন্য vitest কনফিগ।
// আগে infrastructure-এ কোনো টেস্ট/কনফিগ ছিল না, তাই Core CI-র "Cloudflare Worker (Test)"
// জব vitest খুঁজে না পেয়ে ব্যর্থ হতো; এই কনফিগ + __tests__ যোগ করে জবটি কার্যকর করা হলো।
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

// বাংলা মন্তব্য: `root` কে এই ফাইলের ডিরেক্টরিতে (infrastructure/) পিন করা হলো, নাহলে
// রিপো-রুট থেকে চালালে vitest পুরো মনোরেপোর টেস্ট তুলে নিত (55টি) এবং include প্যাটার্ন
// ভুল রিজলভ হতো। root পিন করায় শুধু infrastructure/__tests__ চলে।
export default defineConfig({
  root: fileURLToPath(new URL('.', import.meta.url)),
  test: {
    environment: 'node',
    globals: true,
    include: ['__tests__/**/*.test.{js,ts}'],
  },
});
