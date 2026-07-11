# 📄 ফাইল: apps/studio-client/src/pages/admin/AdminShell.tsx

**প্রকার:** .tsx  
**সাইজ:** 7,648 বাইট  
**আপডেট:** 2026-07-11T17:37:52.681235

---

## কোড

```tsx
import React, { useEffect, useState } from "react";
import { useAdminStore } from "../../store/adminStore";
import { useStore } from "../../store/useStore";
import { AdminConsole } from "../../components/admin/AdminConsole";
import { getApiBaseUrl } from "../../utils/api";

export function AdminShell() {
  const {
    adminAuthenticated,
    adminPassword,
    setAdminPassword,
    adminError,
    handleAdminLogin,
    otpRequired,
    adminOtp,
    setAdminOtp,
    handleAdminLogout,
    actionStatus,
    setActionStatus,
  } = useAdminStore();

  const { systemConfig } = useStore();
  const [adminEmail, setAdminEmail] = useState(systemConfig.adminEmail);
  const [totpSetupRequired] = useState(false);
  const [totpSecret] = useState("");
  const [provisioningUri] = useState("");
  const [adminSubTab, setAdminSubTab] = useState<any>("dashboard");
  const [skillQuery, setSkillQuery] = useState("");
  const [skillsList] = useState<any[]>([]);
  const [checkpointsList] = useState<any[]>([]);
  const [adminMessages, setAdminMessages] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [saveStatus, setSaveStatus] = useState("");
  const [liveLogs, setLiveLogs] = useState<string[]>([]);
  const [costReport, setCostReport] = useState("");
  const [healthMap, setHealthMap] = useState<any>({});
  const [newUsername, setNewUsername] = useState("");
  const [newUserRole, setNewUserRole] = useState("Operator");
  const [newUserPerms, setNewUserPerms] = useState("read,write");
  const [adminUsers, setAdminUsers] = useState<any[]>([]);
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
    
    // TODO: Phase 3 - Implement RBAC check here
    // e.g. const hasAdminRole = checkUserRole('SUPER_ADMIN');
    // if (!hasAdminRole) { ... }

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

  // Utility logic for tokens
  const getAdminToken = () => {
    return localStorage.getItem('adminToken') || '';
  }

  const handleAdminOtpVerify = () => {
    handleAdminLogin();
  };

  const handleInstallSkill = (name: string) => {
    console.log("Install skill", name);
  };

  const handleDeleteCheckpoint = (taskId: string) => {
    console.log("Delete checkpoint", taskId);
  };

  const handleTriggerDeploy = () => {
    setActionStatus("TRIGGERING DEPLOY...");
    const API_BASE = getApiBaseUrl();
    const headers = {
      "Authorization": `Bearer ${getAdminToken()}`,
      "Content-Type": "application/json"
    };
    fetch(`${API_BASE}/admin-api/deploy`, { method: "POST", headers })
      .then(res => res.json())
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
    console.log("Send admin message", adminInput);
    setAdminInput("");
  };

  const handleSaveRules = () => {
    setSaveStatus("SAVING...");
    setTimeout(() => setSaveStatus("SAVED"), 1000);
  };

  const handleSaveUser = () => {
    if (!newUsername) return;
    const API_BASE = getApiBaseUrl();
    const headers = {
      "Authorization": `Bearer ${getAdminToken()}`,
      "Content-Type": "application/json"
    };
    fetch(`${API_BASE}/admin-api/users`, {
      method: "POST",
      headers,
      body: JSON.stringify({ username: newUsername, role: newUserRole, permissions: newUserPerms.split(",") })
    })
      .then(res => res.json())
      .then(() => {
        setAdminUsers(prev => [...prev, { username: newUsername, role: newUserRole, permissions: newUserPerms.split(",") }]);
        setNewUsername("");
      })
      .catch(err => console.error("Error creating user:", err));
  };

  const handleDeleteUser = (username: string) => {
    const API_BASE = getApiBaseUrl();
    const headers = {
      "Authorization": `Bearer ${getAdminToken()}`,
      "Content-Type": "application/json"
    };
    fetch(`${API_BASE}/admin-api/users/${username}`, { method: "DELETE", headers })
      .then(res => res.json())
      .then(() => {
        setAdminUsers(prev => prev.filter(u => u.username !== username));
      })
      .catch(err => console.error("Error deleting user:", err));
  };

  const handleSaveConfig = () => {
    const API_BASE = getApiBaseUrl();
    const headers = {
      "Authorization": `Bearer ${getAdminToken()}`,
      "Content-Type": "application/json"
    };
    fetch(`${API_BASE}/admin-api/config`, {
      method: "POST",
      headers,
      body: JSON.stringify(envConfig)
    })
      .then(res => {
        if (!res.ok) throw new Error("Failed to save config");
        return res.json();
      })
      .then(() => {
        console.log("Environment config saved successfully.");
      })
      .catch(err => console.error("Error saving environment config:", err));
  };

  return (
    <AdminConsole
      adminAuthenticated={adminAuthenticated}
      adminPassword={adminPassword}
      setAdminPassword={setAdminPassword}
      adminEmail={adminEmail}
      setAdminEmail={setAdminEmail}
      totpSetupRequired={totpSetupRequired}
      totpSecret={totpSecret}
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

```