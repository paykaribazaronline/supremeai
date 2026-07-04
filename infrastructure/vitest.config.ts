// বাংলা মন্তব্য: Cloudflare Worker (cloudflare_worker.js) এর ইউনিট টেস্টের জন্য vitest কনফিগ।
// আগে infrastructure-এ কোনো টেস্ট/কনফিগ ছিল না, তাই Core CI-র "Cloudflare Worker (Test)"
// জব vitest খুঁজে না পেয়ে ব্যর্থ হতো; এই কনফিগ + __tests__ যোগ করে জবটি কার্যকর করা হলো।
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
    globals: true,
    include: ['__tests__/**/*.test.{js,ts}'],
  },
});
