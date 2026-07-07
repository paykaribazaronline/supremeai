# 📄 ফাইল: apps/studio-client/src/components/core/GlobalConfigInitializer.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,276 বাইট  
**আপডেট:** 2026-07-07T21:58:43.498372

---

## কোড

```tsx
import React, { useEffect, useState } from 'react';
import { useStore } from '../../store/useStore';
import { getApiBaseUrl } from '../../utils/api';
import { AppDefaults } from '../../config/constants';
import { setApiConcurrency } from '../../services/apiClient';

interface GlobalConfigInitializerProps {
  children: React.ReactNode;
}

export const GlobalConfigInitializer: React.FC<GlobalConfigInitializerProps> = ({ children }) => {
  const { isConfigLoaded, setConfig } = useStore();
  const [error, setError] = useState<string | null>(null);

  const fetchConfig = async () => {
    setError(null);
    try {
      const res = await fetch(`${getApiBaseUrl()}/api/config/public`);
      if (res.ok) {
        const data = await res.json();
        setConfig(data);
        if (data.maxConcurrency) {
          setApiConcurrency(data.maxConcurrency);
        }
      } else {
        throw new Error(`Failed to load config: ${res.statusText}`);
      }
    } catch (err) {
      console.error("Config fetch error:", err);
      // Fallback to safe defaults on network error
      setConfig(AppDefaults);
      setError("Failed to connect to SupremeAI core. Using safe-default configurations.");
    }
  };

  useEffect(() => {
    if (!isConfigLoaded) {
      fetchConfig();
    }
  }, [isConfigLoaded]);

  if (!isConfigLoaded) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-[#0a0a0a] text-white">
        <div className="flex flex-col items-center gap-4">
          <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-t-2 border-indigo-500"></div>
          <p className="text-sm font-medium tracking-wide text-gray-400">Initializing Core Telemetry...</p>
        </div>
      </div>
    );
  }

  return (
    <>
      {error && (
        <div className="fixed top-0 z-50 flex w-full items-center justify-between bg-yellow-600/90 px-4 py-2 text-sm font-semibold text-white backdrop-blur-md">
          <span>{error}</span>
          <button
            onClick={() => window.location.reload()}
            className="rounded bg-yellow-700 px-3 py-1 hover:bg-yellow-800 focus:outline-none"
          >
            Retry Connection
          </button>
        </div>
      )}
      {children}
    </>
  );
};

```