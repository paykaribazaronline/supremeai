# 📄 ফাইল: apps/studio-client/src/components/AdminConsole.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,219 বাইট  
**আপডেট:** 2026-07-11T11:29:21.283291

---

## কোড

```tsx
import type { ChatMessage, Skill, Checkpoint, CloudStats, GcpHealth, AdminSubTab } from '../types';
import { useHydrated } from '../store/customerStore';
import { LoginView } from './admin/AdminLogin';
import { AuthenticatedView } from './admin/AdminAuthenticated';

interface AdminConsoleProps {
  adminAuthenticated: boolean;
  adminPassword: string;
  setAdminPassword: (val: string) => void;
  adminEmail: string;
  setAdminEmail: (val: string) => void;

  adminError: string;
  handleAdminLogin: () => void;
  handleAdminLogout: () => void;
  actionStatus: string;
  gcpHealth: GcpHealth | null;
  cloudStats: CloudStats | null;
  skillQuery: string;
  setSkillQuery: (val: string) => void;
  skills: Skill[];
  handleInstallSkill: (name: string) => void;
  checkpoints: Checkpoint[];
  handleDeleteCheckpoint: (taskId: string) => void;
  adminSubTab: AdminSubTab;
  setAdminSubTab: (tab: AdminSubTab) => void;
  handleTriggerDeploy: () => void;
  adminMessages: ChatMessage[];
  loading: boolean;
  adminInput: string;
  setAdminInput: (val: string) => void;
  handleSendAdmin: () => void;
  rulesJson: string;
  setRulesJson: (val: string) => void;
  saveStatus: string;
  handleSaveRules: () => void;
  liveLogs: string[];
  setLiveLogs: (logs: string[]) => void;
  costReport: string;
  healthMap: any;
  newUsername: string;
  setNewUsername: (val: string) => void;
  newUserRole: string;
  setNewUserRole: (val: string) => void;
  newUserPerms: string;
  setNewUserPerms: (val: string) => void;
  handleSaveUser: () => void;
  adminUsers: any[];
  handleDeleteUser: (username: string) => void;
  envConfig: Record<string, string>;
  setEnvConfig: React.Dispatch<React.SetStateAction<Record<string, string>>>;
  handleSaveConfig: () => void;
  otpRequired: boolean;
  adminOtp: string;
  setAdminOtp: (val: string) => void;
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}

export function AdminConsole(props: AdminConsoleProps) {
  return (
    <div className="flex-grow flex flex-col overflow-hidden bg-[var(--background)] text-[var(--foreground)]">
      {!props.adminAuthenticated ? (
        <LoginView {...props} />
      ) : (
        <AuthenticatedView {...props} />
      )}
    </div>
  );
}

```