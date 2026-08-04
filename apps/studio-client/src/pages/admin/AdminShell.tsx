import React, { useEffect, useState } from "react";
import { useAdminStore } from "../../store/adminStore";
import { useStore } from "../../store/useStore";
import { AdminConsole } from "../../components/admin/AdminConsole";
import { apiClient } from "../../services/apiClient";
import { Shield } from "lucide-react";
import type { AdminSubTab, AdminUser, Skill, Checkpoint, ChatMessage, HealthMap } from "../../types";

export function AdminShell() {
  const {
    adminAuthenticated,
    adminRole,
    adminEmail,
    setAdminEmail,
    adminError,
    handleAdminLogin,
    otpRequired,
    adminOtp,
    setAdminOtp,
    totpSetupRequired,
    provisioningUri,
    handleAdminLogout,
    actionStatus,
    setActionStatus,
  } = useAdminStore();

  const { systemConfig } = useStore();
  const [adminSubTab, setAdminSubTab] = useState<AdminSubTab>("dashboard");
  const [skillQuery, setSkillQuery] = useState("");
  const [skillsList] = useState<Skill[]>([]);
  const [checkpointsList] = useState<Checkpoint[]>([]);
  const [adminMessages, setAdminMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [saveStatus, setSaveStatus] = useState("");
  const [liveLogs, setLiveLogs] = useState<string[]>([]);
  const [costReport, setCostReport] = useState("");
  const [healthMap, setHealthMap] = useState<HealthMap>({ gcp: { status: 'unknown', latency: '', region: '' }, railway: { status: 'unknown', latency: '', region: '' }, render: { status: 'unknown', latency: '', region: '' } });
  const [newUsername, setNewUsername] = useState("");
  const [newUserRole, setNewUserRole] = useState("Operator");
  const [newUserPerms, setNewUserPerms] = useState("read,write");
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([]);
  const [envConfig, setEnvConfig] = useState<Record<string, string>>({});
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');
  const [adminInput, setAdminInput] = useState("");
  const [rulesJson, setRulesJson] = useState("");

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  useEffect(() => {
    if (!adminAuthenticated) return;

    if (adminRole !== 'admin') {
      if (import.meta.env.DEV) console.warn("RBAC: User is not an admin.");
    }

    const loadEnvConfig = async () => {
      setEnvConfig({
        "ENV": import.meta.env.VITE_ENV ?? "local",
        "DEBUG": import.meta.env.VITE_DEBUG ?? "false",
        "PORT": import.meta.env.VITE_PORT ?? "8000",
        "GCP_REGION": import.meta.env.VITE_GCP_REGION ?? "us-central1"
      });
    };

    loadEnvConfig();

  }, [adminAuthenticated]);

  const handleAdminOtpVerify = () => {
    handleAdminLogin();
  };

  const handleInstallSkill = (name: string) => {
    if (import.meta.env.DEV) console.warn("Install skill", name);
  };

  const handleDeleteCheckpoint = (taskId: string) => {
    if (import.meta.env.DEV) console.warn("Delete checkpoint", taskId);
  };

  const handleTriggerDeploy = () => {
    setActionStatus("TRIGGERING DEPLOY...");
    apiClient.post('/admin-api/deploy')
      .then(() => {
        setActionStatus("DEPLOY TRIGGERED");
        setTimeout(() => setActionStatus(""), 2000);
      })
      .catch(() => {
        setActionStatus("DEPLOY FAILED");
        setTimeout(() => setActionStatus(""), 2000);
      });
  };

  const handleSendAdmin = () => {
    if (import.meta.env.DEV) console.warn("Send admin message", adminInput);
    setAdminInput("");
  };

  const handleSaveRules = () => {
    setSaveStatus("SAVING...");
    setTimeout(() => setSaveStatus("SAVED"), 1000);
  };

  const handleSaveUser = () => {
    if (!newUsername) return;
    apiClient.post('/admin-api/users', {
      username: newUsername,
      role: newUserRole,
      permissions: newUserPerms.split(",")
    })
      .then(() => {
        setAdminUsers(prev => [...prev, { username: newUsername, role: newUserRole, permissions: newUserPerms.split(",") }]);
        setNewUsername("");
      })
      .catch(err => {
        if (import.meta.env.DEV) console.error("Error creating user:", err);
      });
  };

  const handleDeleteUser = (username: string) => {
    apiClient.delete(`/admin-api/users/${encodeURIComponent(username)}`)
      .then(() => {
        setAdminUsers(prev => prev.filter(u => u.username !== username));
      })
      .catch(err => {
        if (import.meta.env.DEV) console.error("Error deleting user:", err);
      });
  };

  const handleSaveConfig = () => {
    apiClient.post('/admin-api/config', envConfig)
      .then(() => {
        if (import.meta.env.DEV) console.warn("Environment config saved successfully.");
      })
      .catch(err => {
        if (import.meta.env.DEV) console.error("Error saving environment config:", err);
      });
  };

  if (adminAuthenticated && adminRole !== 'admin') {
    return (
      <div className="flex h-screen bg-[#0A0A0A] text-white items-center justify-center font-sans">
        <div className="w-[400px] p-8 rounded-2xl bg-white/5 border border-red-500/30 text-center flex flex-col items-center gap-4">
          <Shield className="w-16 h-16 text-red-500" />
          <h1 className="text-2xl font-semibold">Access Denied</h1>
          <p className="text-sm text-gray-400">You do not have the required "admin" role to access this dashboard.</p>
          <button
            onClick={handleAdminLogout}
            className="mt-4 px-6 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 font-medium rounded-lg transition-colors border border-red-500/50"
          >
            Logout
          </button>
        </div>
      </div>
    );
  }

  return (
    <AdminConsole
      adminAuthenticated={adminAuthenticated}
      adminEmail={adminEmail}
      setAdminEmail={setAdminEmail}
      totpSetupRequired={totpSetupRequired}
      provisioningUri={provisioningUri}
      adminError={adminError}
      handleAdminLogin={handleAdminLogin}
      handleAdminOtpVerify={handleAdminOtpVerify}
      handleAdminLogout={handleAdminLogout}
      actionStatus={actionStatus}
      gcpHealth={null}
      cloudStats={null}
      skillQuery={skillQuery}
      setSkillQuery={setSkillQuery}
      skills={skillsList}
      handleInstallSkill={handleInstallSkill}
      checkpoints={checkpointsList}
      handleDeleteCheckpoint={handleDeleteCheckpoint}
      adminSubTab={adminSubTab}
      setAdminSubTab={setAdminSubTab}
      handleTriggerDeploy={handleTriggerDeploy}
      adminMessages={adminMessages}
      loading={loading}
      adminInput={adminInput}
      setAdminInput={setAdminInput}
      handleSendAdmin={handleSendAdmin}
      rulesJson={rulesJson}
      setRulesJson={setRulesJson}
      saveStatus={saveStatus}
      handleSaveRules={handleSaveRules}
      liveLogs={liveLogs}
      setLiveLogs={setLiveLogs}
      costReport={costReport}
      healthMap={healthMap}
      newUsername={newUsername}
      setNewUsername={setNewUsername}
      newUserRole={newUserRole}
      setNewUserRole={setNewUserRole}
      newUserPerms={newUserPerms}
      setNewUserPerms={setNewUserPerms}
      handleSaveUser={handleSaveUser}
      adminUsers={adminUsers}
      handleDeleteUser={handleDeleteUser}
      envConfig={envConfig}
      setEnvConfig={setEnvConfig}
      handleSaveConfig={handleSaveConfig}
      otpRequired={otpRequired}
      adminOtp={adminOtp}
      setAdminOtp={setAdminOtp}
      theme={theme}
      toggleTheme={toggleTheme}
    />
  );
}