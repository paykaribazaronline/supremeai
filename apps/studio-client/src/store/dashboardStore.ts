import { create } from 'zustand';

interface DashboardState {
  isDeploymentModalOpen: boolean;
  systemStatus: 'healthy' | 'degraded' | 'critical';
  activePanel: string | null;
  setDeploymentModal: (isOpen: boolean) => void;
  updateSystemStatus: (status: 'healthy' | 'degraded' | 'critical') => void;
  setActivePanel: (panel: string | null) => void;
}

export const useDashboardStore = create<DashboardState>((set) => ({
  isDeploymentModalOpen: false,
  systemStatus: 'healthy',
  activePanel: null,
  setDeploymentModal: (isOpen) => set({ isDeploymentModalOpen: isOpen }),
  updateSystemStatus: (status) => set({ systemStatus: status }),
  setActivePanel: (panel) => set({ activePanel: panel }),
}));
