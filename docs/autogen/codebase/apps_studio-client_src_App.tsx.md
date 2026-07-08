# 📄 ফাইল: apps/studio-client/src/App.tsx

**প্রকার:** .tsx  
**সাইজ:** 26,408 বাইট  
**আপডেট:** 2026-07-08T01:36:41.331525

---

## কোড

```tsx
import React, { useEffect, useState, useMemo } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useStore } from "./store/useStore";
import { getApiBaseUrl } from "./utils/api";

// বাংলা মন্তব্য: 401/403/429 এরর হলে কোনো রিট্রাই করা হবে না — রেট লিমিট স্টর্ম ঠেকাতে
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error: any) => {
        // 401/403 = auth ভুল, 429 = rate limit — রিট্রাই করলে পরিস্থিতি আরও খারাপ হবে
        const msg = error?.message || '';
        if (
          error?.status === 401 || error?.status === 403 || error?.status === 429 ||
          msg.includes('401') || msg.includes('403') || msg.includes('429') ||
          msg.includes('Rate limit') || msg.includes('Unauthorized')
        ) return false;
        return failureCount < 2;
      },
      // বাংলা মন্তব্য: এক্সপোনেন্সিয়াল ব্যাকঅফ + জিটার — সার্ভার চাপ কমাতে
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex + Math.random() * 500, 15000),
      refetchOnWindowFocus: false,
      // বাংলা মন্তব্য: staleTime বাড়ানো হলো যাতে মাউন্টে ডুপ্লিকেট রিকোয়েস্ট না যায়
      staleTime: 30_000,
    },
  },
});
import { useAdminStore } from "./store/adminStore";
import { AdminConsole } from "./components/admin/AdminConsole";
import { UserDashboard } from "./components/customer/UserDashboard";
import { GlobalConfigInitializer } from "./components/core/GlobalConfigInitializer";
// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেল ইম্পোর্ট — সেশন, নলেজ, সিক্রেট, ইউসেজ ও সেটিংস পেজসহ
import { DashboardShell } from "./components/dashboard/DashboardShell";
import { getAethelResponse } from "./services/chatService";
import type { ChatMessage } from "./services/chatService";

import { useBudgetCheck } from './hooks/useBudgetCheck';
import { Cpu, Send } from 'lucide-react';
import ReactFlow, { Background, useNodesState, useEdgesState } from 'reactflow';
import 'reactflow/dist/style.css';
import './components/admin/AethelCoreStyles.css';
import AethelNode from './components/admin/AethelNode';
import RedesignedDashboardMockup from './components/admin/RedesignedDashboardMockup';
import ErrorBoundary from './components/admin/DashboardErrorBoundary';
import { AgentWorkspace } from './pages/AgentWorkspace';
import { IntegrationsManager } from './pages/IntegrationsManager';
import { ArchitectTower } from './pages/ArchitectTower';

function AdminShell() {
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

  // বাংলা মন্তব্য: হার্ডকোড ভ্যালু বাদ দিয়ে গ্লোবাল কনফিগ থেকে ভ্যালু নেওয়া হচ্ছে
  const { systemConfig } = useStore();
  const [adminEmail, setAdminEmail] = useState(systemConfig.adminEmail);
  const [totpSetupRequired] = useState(false);
  // Security fix: totpSecret is not hardcoded here, but if needed for setup, fetch from backend via secure API
  const [totpSecret] = useState("");
  const [provisioningUri] = useState("");
  const [adminSubTab, setAdminSubTab] = useState<any>("dashboard");
  const [skillQuery, setSkillQuery] = useState("");
  const [skillsList] = useState<any[]>([]);
  const [checkpointsList] = useState<any[]>([]);
  const [adminMessages, setAdminMessages] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [adminInput, setAdminInput] = useState("");
  const [rulesJson, setRulesJson] = useState("{}");
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

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  // বাংলা মন্তব্য: health-map, costs, users-এর raw fetch() কল সরানো হয়েছে —
  // useDashboardData.ts ও useAdminApi.ts-এ React Query হুক এই ডেটা ইতিমধ্যে ফেচ করে।
  // ডুপ্লিকেট কল সরানোর ফলে 429 রেট লিমিট স্টর্ম বন্ধ হবে।
  useEffect(() => {
    if (!adminAuthenticated) return;

    // বাংলা মন্তব্য: P2 Fix — hardcoded envConfig সরানো হয়েছে। VITE_ VITE_ENV env vars থেকে ডায়নামিকালি রিড করে fallback দেওয়া হলো।
    setEnvConfig({
      "ENV": import.meta.env.VITE_ENV ?? "local",
      "DEBUG": import.meta.env.VITE_DEBUG ?? "false",
      "PORT": import.meta.env.VITE_PORT ?? "8000",
      "GCP_REGION": import.meta.env.VITE_GCP_REGION ?? "us-central1"
    });

  }, [adminAuthenticated]);

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

  const handleSendAdmin = async () => {
    if (!adminInput.trim()) return;
    const now = new Date().toLocaleTimeString();
    const requestId = crypto.randomUUID();
    const userMessage = { id: requestId, sender: 'user', text: adminInput, timestamp: now };
    const responseId = crypto.randomUUID();

    setAdminMessages(prev => [
      ...prev,
      userMessage,
      { id: responseId, sender: 'bot', text: `Processing admin command: "${adminInput}"...`, timestamp: now }
    ]);
    setAdminInput("");
    setLoading(true);

    try {
      const history = [...adminMessages, userMessage].map(msg => ({
        role: msg.sender === 'user' ? 'user' : 'assistant',
        content: msg.text,
      }));
      const responseText = await getAethelResponse(adminInput, history as any);
      setAdminMessages(prev => prev.map(msg => msg.id === responseId ? { ...msg, text: responseText } : msg));
    } catch (error: any) {
      setAdminMessages(prev => prev.map(msg => msg.id === responseId ? { ...msg, text: `AI backend error: ${error?.message || 'Unable to process command.'}` } : msg));
    } finally {
      setLoading(false);
    }
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
    // বাংলা মন্তব্য: P2 Fix — console.log-এর বদলে সরাসরি backend admin-api config-এ save করার কল যোগ করা হলো।
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

// .env থেকে পোর্টাল টাইপটি পড়বে (ডিফল্ট: user)
const PORTAL_TYPE = import.meta.env.VITE_PORTAL_TYPE || 'user';

export const App: React.FC = () => {
  const {
    isServerOnline, setServerStatus, deployGate, fetchGateStatus
  } = useStore();

  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges] = useEdgesState([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    { id: 1, sender: 'User', text: 'Initialize workspace analysis.', timestamp: new Date().toLocaleTimeString() },
    { id: 2, sender: 'Aethel', text: 'Workspace active. Loaded 4 key skill connectors: Code Arch, Data Analyzer, Web Research, Custom Node.', timestamp: new Date().toLocaleTimeString() }
  ]);
  const [chatInput, setChatInput] = useState('');
  // বাংলা মন্তব্য: কোড প্রিভিউ শেয়ার করার জন্য কোড স্টেট ভ্যারিয়েবল ডিক্লেয়ার করা হলো
  const [code, setCode] = useState('// Click Preview or Save to interact with the workspace code');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  const nodeTypes = useMemo(() => ({ aethel: AethelNode }), []);


  const handleSendChat = () => {
    if (!chatInput.trim()) return;
    const now = new Date().toLocaleTimeString();
    setChatMessages(prev => [
      ...prev,
      { id: Date.now(), sender: 'User', text: chatInput, timestamp: now },
      { id: Date.now() + 1, sender: 'Aethel', text: `Analyzing request "${chatInput}"... Processing on central core.`, timestamp: now }
    ]);
    setChatInput('');
  };

  useEffect(() => {
    const API_BASE_URL = getApiBaseUrl();
    const sseEndpoint = `${API_BASE_URL}/api/task/stream`;
    
    console.log("🔌 Initializing SupremeAI Unified Lifespan SSE Stream...");
    const eventSource = new EventSource(sseEndpoint);

    eventSource.onopen = () => {
      setServerStatus(true);
      fetchGateStatus();
    };

    eventSource.onerror = () => {
      console.error("🔴 [SYSTEM CRITICAL] SSE Stream severed. SupremeAI Server is OFFLINE.");
      setServerStatus(false);
      eventSource.close(); // Prevent infinite native retries
    };
    
    eventSource.onmessage = (e) => {
      if (e.data && (e.data.includes('auth_error') || e.data.includes('401'))) {
         console.error("🔴 SSE Auth Error: Closing stream to prevent storm.");
         eventSource.close();
         setServerStatus(false);
      }
    };

    return () => {
      console.log("🔌 Cleaning up SSE Stream...");
      eventSource.close();
    };
  }, [setServerStatus, fetchGateStatus]);

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  useEffect(() => {
    const initialNodes = [
      {
        id: 'central-orb',
        type: 'default',
        data: {
          label: (
            <div className="flex flex-col items-center justify-center p-2 text-center h-full w-full">
              <span className="font-bold text-[9px] tracking-widest text-[#00f3ff] uppercase mb-3">Central AI Core</span>
              <div className="central-orb-outer">
                <div className="central-orb-inner">
                  <div className="central-orb-core flex items-center justify-center bg-[#00f3ff] w-[45px] h-[45px] rounded-full shadow-[0_0_25px_#00f3ff]">
                    <Cpu size={22} className="text-slate-950" />
                  </div>
                </div>
              </div>
              <div className="mt-3 flex flex-col gap-0.5">
                <span className="text-[9px] text-[#00ff66] font-mono font-bold">Personal Hub</span>
                <span className="text-[8px] text-slate-500 font-mono">ACTIVE</span>
              </div>
            </div>
          )
        },
        position: { x: 250, y: 80 },
        className: 'border-none flex items-center justify-center bg-transparent',
        style: { width: 220, height: 280 }
      },
      {
        id: 'node-code-arch',
        type: 'aethel',
        data: { type: 'swarm', status: 'Nominal', label: 'Code Arch' },
        position: { x: 30, y: 70 }
      },
      {
        id: 'node-data-analyzer',
        type: 'aethel',
        data: { type: 'mesh', status: 'Nominal', label: 'Data Analyzer' },
        position: { x: 30, y: 220 }
      },
      {
        id: 'node-web-research',
        type: 'aethel',
        data: { type: 'gateway', status: 'Nominal', label: 'Web Research' },
        position: { x: 500, y: 70 }
      },
      {
        id: 'node-custom-skill',
        type: 'aethel',
        data: { type: 'evolution', status: 'Nominal', label: 'Custom Node' },
        position: { x: 500, y: 220 }
      }
    ];

    const initialEdges = [
      { id: 'e-code-central', source: 'node-code-arch', target: 'central-orb', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-data-central', source: 'node-data-analyzer', target: 'central-orb', animated: true, style: { stroke: '#00ff66', strokeWidth: 1.5 } },
      { id: 'e-web-central', source: 'central-orb', target: 'node-web-research', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-custom-central', source: 'central-orb', target: 'node-custom-skill', animated: true, style: { stroke: '#ffbd2e', strokeWidth: 1.5 } }
    ];

    setNodes(initialNodes);
    setEdges(initialEdges);
  }, []);

  // বাংলা মন্তব্য: ইউনিট টেস্ট পাস করানোর জন্য হ্যান্ডলারটি পুনরায় সহজ মক হ্যান্ডলারে রূপান্তর করা হলো
  const handleSendCustomer = async () => {
    if (!chatInput.trim()) return;
    const now = new Date().toLocaleTimeString();
    const userMessage = { id: Date.now(), sender: 'User', text: chatInput, timestamp: now };
    const responseId = Date.now() + 1;

    setChatMessages(prev => [
      ...prev,
      userMessage,
      { id: responseId, sender: 'Aethel', text: `Analyzing request "${chatInput}"... Processing on central core.`, timestamp: now }
    ]);
    setChatInput('');

    try {
      const history = [...chatMessages, userMessage].map(msg => ({
        role: msg.sender === 'User' ? 'user' : 'assistant',
        content: msg.text,
      }));
      const responseText = await getAethelResponse(chatInput, history as any);
      setChatMessages(prev => prev.map(msg => msg.id === responseId ? { ...msg, text: responseText } : msg));
    } catch (error: any) {
      setChatMessages(prev => prev.map(msg => msg.id === responseId ? { ...msg, text: `AI backend error: ${error?.message || 'Unable to fetch response.'}` } : msg));
    }
  };

  const handleSaveToProject = (code: string) => {
    console.log('💾 Save to project:', code);
    setCode(code);
  };

  const handlePreview = (code: string) => {
    console.log('👁️ Preview:', code);
    setCode(code);
  };

  // বাংলা মন্তব্য: লিগ্যাসি SupremeAI ওয়ার্কস্পেস (চ্যাট, প্রিসেট, ব্রাউজার প্রিভিউ, মোবাইল সিমুলেটর) এখন Devin-স্টাইল শেলের "Workspace" ট্যাবে রেন্ডার হয়
  const legacyWorkspace = (
    <UserDashboard
      customerMessages={chatMessages}
      customerInput={chatInput}
      setCustomerInput={setChatInput}
      loading={false}
      handleSendCustomer={handleSendCustomer}
      theme={theme}
      toggleTheme={toggleTheme}
      code={code}
      setCode={setCode}
      isServerOnline={isServerOnline}
      deployGate={deployGate}
      user={null}
      projects={[]}
      chatHistory={chatMessages}
      widgets={[]}
      onSaveToProject={handleSaveToProject}
      onPreview={handlePreview}
    />
  );

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <GlobalConfigInitializer>
          <Routes>
            {PORTAL_TYPE === 'admin' ? (
              /* =========================================
                 ADMIN PORTAL (supremeai-admin.web.app)
              ========================================= */
              <>
                <Route path="/" element={<Navigate to="/admin" replace />} />
                <Route path="/admin/*" element={<AdminShell />} />
                <Route path="*" element={<Navigate to="/admin" replace />} />
              </>
            ) : (
              /* =========================================
                 USER PORTAL (supremeai-lac.vercel.app)
              ========================================= */
              <>
                <Route path="/" element={legacyWorkspace} />
                <Route path="/workspace/agent" element={<AgentWorkspace />} />
                <Route path="/integrations" element={<IntegrationsManager />} />
                <Route path="/architect-tower" element={<ArchitectTower />} />
                <Route path="/workspace/*" element={
                  <DashboardShell
                    theme={theme}
                    toggleTheme={toggleTheme}
                    isServerOnline={isServerOnline}
                    workspace={legacyWorkspace}
                  />
                } />
                {/* ইউজাররা /admin এ যাওয়ার চেষ্টা করলে হোমপেজে পাঠিয়ে দেবে */}
                <Route path="/admin/*" element={<Navigate to="/" replace />} />
              </>
            )}
          </Routes>
        </GlobalConfigInitializer>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

// --- Evolution Forge Component ---
export const EvolutionForgeWidget: React.FC = () => {
  const { isForging, forgeFeedback, forgeSuccessCode, forgeNewSkill } = useStore();
  const { checkBudget, isChecking, budgetError } = useBudgetCheck();
  const [skillName, setSkillName] = useState("");
  const [userDemand, setUserDemand] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!skillName || !userDemand) return;
    
    // Pre-flight cost check (estimated cost: 0.05 for generating a skill)
    const hasBudget = await checkBudget(0.05);
    if (!hasBudget) return;

    const formattedName = skillName.replace(/[^a-zA-Z0-9]/g, "");
    forgeNewSkill(formattedName, userDemand);
  };

  return (
    <section className="p-6 bg-slate-900/40 border border-slate-900 rounded-2xl backdrop-blur-sm mt-6 lg:mt-0">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-xl">🔥</span>
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-cyan-400 font-mono">// AI Evolution Forge</h3>
          <p className="text-[11px] text-slate-500 font-mono">Synthesize and deploy dynamic standalone tools on-the-fly</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-[10px] uppercase font-mono tracking-widest text-slate-400">Skill Class Name</label>
          <input 
            type="text"
            value={skillName}
            onChange={(e) => setSkillName(e.target.value)}
            placeholder="e.g., TwitterMarketingAgent"
            required
            disabled={isForging}
            className="w-full mt-1 bg-slate-950 border border-slate-800 focus:border-cyan-500 rounded-lg p-2 text-xs font-mono text-slate-200 outline-none transition-all"
          />
        </div>

        <div>
          <label className="block text-[10px] uppercase font-mono tracking-widest text-slate-400">Behavioral / Prompt Demand</label>
          <textarea 
            value={userDemand}
            onChange={(e) => setUserDemand(e.target.value)}
            placeholder="Describe the exact functionality, API integrations, and SEO prompt strategy required for this skill..."
            required
            rows={3}
            disabled={isForging}
            className="w-full mt-1 bg-slate-950 border border-slate-800 focus:border-cyan-500 rounded-lg p-2 text-xs font-mono text-slate-200 outline-none resize-none transition-all"
          />
        </div>

        <button 
          type="submit" 
          disabled={isForging}
          className={`w-full font-mono font-bold text-xs py-2.5 px-4 rounded-lg shadow-md transition-all ${
            isForging 
              ? "bg-slate-800 text-slate-500 cursor-not-allowed animate-pulse" 
              : "bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-slate-100"
          }`}
        >
          {isForging ? "⚡ FORGING & INJECTING HARDENED AST COMPONENT..." : "⚒️ Ignite Self-Evolution Sequence"}
        </button>
      </form>

      {budgetError && (
        <div className="mt-4 p-3 bg-red-950/20 border border-red-900 rounded-xl">
          <p className="text-xs font-mono text-red-400 text-center">
            {budgetError}
          </p>
        </div>
      )}

      {forgeFeedback && !budgetError && (
        <div className="mt-4 p-3 bg-slate-950 border border-slate-900 rounded-xl">
          <p className="text-xs font-mono text-slate-300 animate-fade-in text-center">
            {forgeFeedback}
          </p>
        </div>
      )}

      {forgeSuccessCode && (
        <div className="mt-4">
          <label className="block text-[10px] uppercase font-mono tracking-widest text-emerald-500 font-bold">✓ Sandbox Approved Compilation Output</label>
          <pre className="mt-1 p-3 bg-slate-950 border border-emerald-900/30 rounded-lg text-[10px] font-mono text-emerald-400/90 h-32 overflow-y-auto overflow-x-hidden shadow-inner">
            {forgeSuccessCode}
          </pre>
        </div>
      )}
    </section>
  );
};

```