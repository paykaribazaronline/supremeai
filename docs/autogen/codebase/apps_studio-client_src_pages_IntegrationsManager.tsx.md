# 📄 ফাইল: apps/studio-client/src/pages/IntegrationsManager.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,064 বাইট  
**আপডেট:** 2026-07-11T14:41:19.417659

---

## কোড

```tsx
import React, { useState } from 'react';
import { getApiBaseUrl } from '../utils/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Button } from '../components/ui/Button';

export const IntegrationsManager: React.FC = () => {
  const [githubStatus, setGithubStatus] = useState<'Disconnected' | 'Connected'>('Disconnected');

  const handleGithubConnect = () => {
    const API_BASE = getApiBaseUrl();
    window.location.href = `${API_BASE}/api/v1/integrations/github/link`;
  };

  return (
    <div className="p-8 max-w-5xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-bold mb-2">🔗 Universal Integrations Hub</h1>
        <p className="text-[var(--supremeai-color-neutral-500)]">
          Connect your favorite tools and platforms to empower SupremeAI's autonomous capabilities.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* 🟢 GitHub Card (Active) */}
        <Card className="hover:border-[var(--supremeai-color-brand-primary-light)] dark:hover:border-[var(--supremeai-color-brand-primary-dark)] transition-colors">
          <CardHeader className="text-center">
            <div className="text-5xl mb-2">🐙</div>
            <CardTitle>GitHub</CardTitle>
            <CardDescription>
              Automate code pushes, PR creation, and repository management.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              onClick={handleGithubConnect}
              variant={githubStatus === 'Connected' ? 'outline' : 'primary'}
              className="w-full"
            >
              {githubStatus === 'Connected' ? '✅ Connected' : 'Connect GitHub'}
            </Button>
          </CardContent>
        </Card>

        {/* 🟡 Facebook Card (Coming Soon) */}
        <Card className="opacity-60 grayscale">
          <CardHeader className="text-center">
            <div className="text-5xl mb-2">📘</div>
            <CardTitle>Facebook</CardTitle>
            <CardDescription>
              Auto-post updates, manage pages, and reply to comments.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button disabled variant="outline" className="w-full cursor-not-allowed">
              Coming Soon
            </Button>
          </CardContent>
        </Card>

        {/* 🟡 Instagram Card (Coming Soon) */}
        <Card className="opacity-60 grayscale">
          <CardHeader className="text-center">
            <div className="text-5xl mb-2">📸</div>
            <CardTitle>Instagram</CardTitle>
            <CardDescription>
              AI-driven visual content publishing and scheduling.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button disabled variant="outline" className="w-full cursor-not-allowed">
              Coming Soon
            </Button>
          </CardContent>
        </Card>

      </div>
    </div>
  );
};

```