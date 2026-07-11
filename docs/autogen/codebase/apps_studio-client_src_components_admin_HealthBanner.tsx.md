# 📄 ফাইল: apps/studio-client/src/components/admin/HealthBanner.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,674 বাইট  
**আপডেট:** 2026-07-11T11:14:17.636998

---

## কোড

```tsx
import { useQuery } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { apiClient } from '../../services/apiClient';
// বাংলা মন্তব্য: টোকেন গার্ড — টোকেন ছাড়া health-map রিকোয়েস্ট যাবে না, 401 স্টর্ম ঠেকাবে
import { getAdminToken } from '../../services/adminTokenStore';

const HealthBanner: React.FC = () => {
  const { data: health } = useQuery({
    queryKey: ['dashboard', 'health'],
    queryFn: () => apiClient.get<{ gcp: { status: string }; railway: { status: string }; render: { status: string } }>('/admin-api/health-map'),
    refetchInterval: (query: any) => query.state.error ? false : 30000,
    // বাংলা মন্তব্য: টোকেন না থাকলে কোয়েরি ডিসেবল — অপ্রয়োজনীয় 401 ঠেকাতে
    enabled: !!getAdminToken(),
    staleTime: 20_000,
  });

  const isDegraded = (health?.gcp && health.gcp.status === 'degraded') || (health?.railway && health.railway.status === 'degraded') || (health?.render && health.render.status === 'degraded');
  return (
    <AnimatePresence>
      {isDegraded && (
        <motion.div
          initial={{ opacity: 0, y: -50 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -50 }}
          className="fixed top-0 w-full bg-red-900/80 text-white p-2 text-center z-[100]"
        >
          ⚠️ System Degraded: Check Health Map for details.
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default HealthBanner;

```