import { create } from 'zustand';

interface DashboardState {
  isDeploymentModalOpen: boolean;
  systemStatus: 'healthy' | 'degraded' | 'critical';
  activePanel: string | null;
  setDeploymentModal: (isOpen: boolean) => void;
  updateSystemStatus: (status: 'healthy' | 'degraded' | 'critical') => void;
  setActivePanel: (panel: string | null) => void;
  
  // বাংলা মন্তব্য: সুপ্রিম ড্যাশবোর্ড মোড ('simple' অথবা 'advanced') এবং ইন্টারেক্টিভ চ্যাটের উইন্ডো স্ট্যাটাস
  dashboardMode: 'simple' | 'advanced';
  chatTabTerminalOpen: boolean;
  chatTabBrowserOpen: boolean;
  toggleDashboardMode: () => void;
  toggleTerminal: () => void;
  toggleBrowser: () => void;
}

export const useDashboardStore = create<DashboardState>((set) => ({
  isDeploymentModalOpen: false,
  systemStatus: 'healthy',
  activePanel: null,
  setDeploymentModal: (isOpen) => set({ isDeploymentModalOpen: isOpen }),
  updateSystemStatus: (status) => set({ systemStatus: status }),
  setActivePanel: (panel) => set({ activePanel: panel }),
  
  // বাংলা মন্তব্য: ডিফল্টভাবে 'simple' মোড সেট করা হলো এবং টগলার ফাংশনগুলো যুক্ত করা হলো
  dashboardMode: 'simple',
  chatTabTerminalOpen: true, // অ্যাডভান্সড মোডে এগুলো বাই ডিফল্ট অন থাকবে
  chatTabBrowserOpen: true,
  toggleDashboardMode: () => set((s) => ({ dashboardMode: s.dashboardMode === 'simple' ? 'advanced' : 'simple' })),
  toggleTerminal: () => set((s) => ({ chatTabTerminalOpen: !s.chatTabTerminalOpen })),
  toggleBrowser: () => set((s) => ({ chatTabBrowserOpen: !s.chatTabBrowserOpen })),
}));

