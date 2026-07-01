import { useQuery } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { apiClient } from '../../services/apiClient';

const HealthBanner: React.FC = () => {
  const { data: health } = useQuery({
    queryKey: ['dashboard', 'health'],
    queryFn: () => apiClient.get<{ gcp: { status: string }; railway: { status: string }; render: { status: string } }>('/admin-api/health-map'),
    refetchInterval: 30000,
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
