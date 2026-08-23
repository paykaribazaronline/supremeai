import type { ChatMessage, Skill, Checkpoint, AdminSubTab, HealthMap } from '../../types';
import { LoginView } from './auth/AdminLogin';
import { AuthenticatedView } from './auth/AdminAuthenticated';
import DashboardErrorBoundary from './DashboardErrorBoundary';

interface AdminConsoleProps {
  adminAuthenticated: boolean;
  adminEmail: string;
  setAdminEmail: (val: string) => void;
  totpSetupRequired: boolean;
  provisioningUri: string;
  totpSecret: string;
  onResetTotp: () => void;
  adminError: string;
  handleAdminLogin: (password?: string) => void;
  handleAdminOtpVerify: () => void;
  handleAdminLogout: () => void;
  actionStatus: string;
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
  healthMap: HealthMap;
  newUsername: string;
  setNewUsername: (val: string) => void;
  newUserRole: string;
  setNewUserRole: (val: string) => void;
  newUserPerms: string;
  setNewUserPerms: (val: string) => void;
  otpRequired: boolean;
  adminOtp: string;
  setAdminOtp: (val: string) => void;
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}

export function AdminConsole(props: AdminConsoleProps) {
  return (
    <div className="dashboard-aurora h-screen w-screen flex flex-col overflow-hidden">
      <DashboardErrorBoundary>
        {!props.adminAuthenticated ? (
          <LoginView
            adminEmail={props.adminEmail}
            setAdminEmail={props.setAdminEmail}
            adminError={props.adminError}
            handleAdminLogin={props.handleAdminLogin}
            otpRequired={props.otpRequired}
            adminOtp={props.adminOtp}
            setAdminOtp={props.setAdminOtp}
            totpSetupRequired={props.totpSetupRequired}
            provisioningUri={props.provisioningUri}
            totpSecret={props.totpSecret}
            onResetTotp={props.onResetTotp}
          />
        ) : (
          <AuthenticatedView {...props} />
        )}
      </DashboardErrorBoundary>
    </div>
  );
}