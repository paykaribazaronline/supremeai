import React, { useEffect, useRef, useState } from 'react';
import { useStore } from '../../store/useStore';
import { AppDefaults } from '../../config/constants';
import { apiClient, setApiConcurrency } from '../../services/apiClient';
import { selfHealingState } from '../../core/stateManagement';

interface GlobalConfigInitializerProps {
  children: React.ReactNode;
}

// বাংলা মন্তব্য: সেলফ-হিলিং রিট্রাই এর বাউন্ডেড লিমিট ও ব্যাকঅফ (ইনফিনিট লুপ প্রতিরোধ)।
// পাবলিক কনফিগ ব্যর্থ হলে দ্রুত fallback নিতে ১ রিট্রাই যথেষ্ট।
const MAX_RETRIES = 1;
const BASE_BACKOFF_MS = 1000;
// বাংলা মন্তব্য: Render কোল্ড স্টার্ট ৩০-৫০ সেকেন্ড হতে পারে; ৮ সেকেন্ডের মধ্যে রিয়েল কনফিগ না এলে
// সাথে সাথে AppDefaults দিয়ে UI রেন্ডার শুরু করব (non-blocking) — দীর্ঘ কালো স্ক্রিন রোধ।
const CONFIG_DEADLINE_MS = 8000;

export const GlobalConfigInitializer: React.FC<GlobalConfigInitializerProps> = ({ children }) => {
  const { setConfig } = useStore();
  const [error, setError] = useState<string | null>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    // বাংলা মন্তব্য: শুধু একবারই ইনিশিয়ালাইজ করব; isConfigLoaded পরিবর্তনে রি-রান করবে না
    // (রি-রান করলে ইন-ফ্লাইট ব্যাকগ্রাউন্ড ফেচ বাতিল হয়ে রিয়েল কনফিগ কখনো আসত না)।
    if (startedRef.current) return;
    startedRef.current = true;

    let cancelled = false;
    let retryCount = 0;
    let backoffTimer: ReturnType<typeof setTimeout> | null = null;
    let deadlineTimer: ReturnType<typeof setTimeout> | null = null;

    const applyConfig = (data: any) => {
      if (cancelled) return;
      setConfig(data);
      if (data?.maxConcurrency) {
        setApiConcurrency(data.maxConcurrency);
      }
      // বাংলা মন্তব্য: selfHealing ফ্ল্যাগ backend/AppDefaults থেকে এসেছে কিনা তা স্টেট ম্যানেজারে রিপোর্ট করি
      if (data?.features?.selfHealing !== undefined) {
        selfHealingState.setEnabled(Boolean(data.features.selfHealing));
      }
    };

    const fetchConfig = async () => {
      if (cancelled) return;
      setError(null);
      try {
        const data = await apiClient.get<any>('/api/config/public');
        if (cancelled) return;
        // বাংলা মন্তব্য: রিয়েল কনফিগ এসেছে — AppDefaults fallback-এর ওপর আপডেট করবে
        applyConfig(data);
      } catch (err) {
        if (cancelled) return;
        console.error("Config fetch error:", err);
        selfHealingState.reportError(String(err), 'CONFIG_FETCH_FAILED');

        // বাংলা মন্তব্য: সেলফ-হিলিং চালু থাকলে bounded retry করবে, নাহলে সরাসরি safe-default।
        const healingEnabled = useStore.getState().systemConfig?.features?.selfHealing
          ?? AppDefaults.features.selfHealing;
        if (healingEnabled && retryCount < MAX_RETRIES) {
          retryCount += 1;
          const delay = BASE_BACKOFF_MS * Math.pow(2, retryCount - 1);
          console.warn(`[Self-Healing] Retry ${retryCount}/${MAX_RETRIES} in ${delay}ms`);
          backoffTimer = setTimeout(fetchConfig, delay);
          return;
        }

        // বাংলা মন্তব্য: শুধু তখনই fallback করব যদি এখনও কোনো কনফিগ লোড না হয়ে থাকে
        if (!useStore.getState().isConfigLoaded) {
          applyConfig(AppDefaults);
          setError("Failed to connect to SupremeAI core. Using safe-default configurations.");
        }
      }
    };

    const onDeviceOnline = () => {
      // বাংলা মন্তব্য: অফলাইন→অনলাইন হলে নিজে থেকেই কনফিগ রি-ফেচ করে স্টেট রিস্টোর
      const healingEnabled = useStore.getState().systemConfig?.features?.selfHealing
        ?? AppDefaults.features.selfHealing;
      if (healingEnabled && !useStore.getState().isConfigLoaded) {
        console.warn('[Self-Healing] Device back online. Restoring config.');
        retryCount = 0;
        fetchConfig();
      }
    };

    // বাংলা মন্তব্য: রিয়েল কনফিগ ব্যাকগ্রাউন্ডে আনবে (non-blocking) — UI আটকাবে না
    fetchConfig();

        // বাংলা মন্তব্য: ৮ সেকেন্ড ডেডলাইন — রিয়েল কনফিগ না এলে সাথে সাথে AppDefaults apply করে UI আনব্লক করব
        deadlineTimer = setTimeout(() => {
          if (cancelled) return;
          if (!useStore.getState().isConfigLoaded) {
            console.warn(`[Config] Deadline ${CONFIG_DEADLINE_MS}ms exceeded. Falling back to safe defaults.`);
            applyConfig(AppDefaults);
            setError("Connecting to SupremeAI core is taking longer than expected. Showing safe-default view.");
          }
        }, CONFIG_DEADLINE_MS);

    if (typeof window !== 'undefined') {
      window.addEventListener('online', onDeviceOnline);
    }

    return () => {
      cancelled = true;
      if (backoffTimer) clearTimeout(backoffTimer);
      if (deadlineTimer) clearTimeout(deadlineTimer);
      if (typeof window !== 'undefined') {
        window.removeEventListener('online', onDeviceOnline);
      }
    };
  }, [setConfig]);

  // বাংলা মন্তব্য: আর কোনো ব্লকিং স্পিনার নেই — কনফিগ লোড হোক বা না হোক children সবসময় রেন্ডার হবে
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
