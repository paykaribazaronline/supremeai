# 📄 ফাইল: apps/studio-client/src/pages/IntegrationsManager.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,533 বাইট  
**আপডেট:** 2026-07-07T17:46:01.348963

---

## কোড

```tsx
import React, { useState } from 'react';

export const IntegrationsManager: React.FC = () => {
  // টেস্টিংয়ের জন্য লোকাল স্টেট, পরবর্তীতে এটি ব্যাকএন্ড থেকে আসবে
  const [githubStatus, setGithubStatus] = useState<'Disconnected' | 'Connected'>('Disconnected');

  const handleGithubConnect = () => {
    // সরাসরি আমাদের ব্যাকএন্ডের OAuth লিংকে রিডাইরেক্ট করবে
    window.location.href = 'http://localhost:8000/api/v1/integrations/github/link';
  };

  return (
    <div className="p-8 text-white bg-gray-900 min-h-screen">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-bold mb-2">🔗 Universal Integrations Hub</h1>
        <p className="text-gray-400 mb-8">
          Connect your favorite tools and platforms to empower SupremeAI's autonomous capabilities.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* 🟢 GitHub Card (Active) */}
          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700 flex flex-col items-center shadow-lg transition-transform hover:-translate-y-1 hover:border-blue-500">
            <div className="text-5xl mb-4">🐙</div>
            <h2 className="text-xl font-bold mb-2">GitHub</h2>
            <p className="text-sm text-gray-400 text-center mb-6">
              Automate code pushes, PR creation, and repository management.
            </p>
            <button
              onClick={handleGithubConnect}
              className={`w-full py-2.5 rounded-lg font-bold transition-all duration-200 ${
                githubStatus === 'Connected'
                  ? 'bg-green-600/20 text-green-400 border border-green-500/50 hover:bg-green-600/30'
                  : 'bg-white text-black hover:bg-gray-200 shadow-md'
              }`}
            >
              {githubStatus === 'Connected' ? '✅ Connected' : 'Connect GitHub'}
            </button>
          </div>

          {/* 🟡 Facebook Card (Coming Soon) */}
          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700 flex flex-col items-center shadow-lg opacity-50 grayscale hover:grayscale-0 transition-all duration-300">
            <div className="text-5xl mb-4">📘</div>
            <h2 className="text-xl font-bold mb-2">Facebook</h2>
            <p className="text-sm text-gray-400 text-center mb-6">
              Auto-post updates, manage pages, and reply to comments.
            </p>
            <button disabled className="w-full py-2.5 rounded-lg font-bold bg-gray-700 text-gray-500 cursor-not-allowed">
              Coming Soon
            </button>
          </div>

          {/* 🟡 Instagram Card (Coming Soon) */}
          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700 flex flex-col items-center shadow-lg opacity-50 grayscale hover:grayscale-0 transition-all duration-300">
            <div className="text-5xl mb-4">📸</div>
            <h2 className="text-xl font-bold mb-2">Instagram</h2>
            <p className="text-sm text-gray-400 text-center mb-6">
              AI-driven visual content publishing and scheduling.
            </p>
            <button disabled className="w-full py-2.5 rounded-lg font-bold bg-gray-700 text-gray-500 cursor-not-allowed">
              Coming Soon
            </button>
          </div>

        </div>
      </div>
    </div>
  );
};

```