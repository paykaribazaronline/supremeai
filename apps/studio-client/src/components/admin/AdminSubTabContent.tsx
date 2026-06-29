import type { AdminSubTab, ChatMessage } from '../../types';
import { CommandCenter, LiveLogs, CostAuditor, HealthMap, UserManager, ConfigEditor, ModelRouter, EnhancedSkillMarketplace, MemoryBrowser, CloudOrchestrator, ObservabilityDashboard, ThreatDetection, VisualRulesBuilder, CICDVisualizer, GithubIntegration, BackupRestore } from '.';
import { RateLimitManager } from './RateLimitManager';
import { X } from 'lucide-react';

interface SubTabContentProps {
  adminSubTab: AdminSubTab;
  setAdminSubTab: (tab: AdminSubTab) => void;
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
  handleTriggerDeploy: () => void;
}

export function SubTabContent(props: SubTabContentProps) {
  const { adminSubTab, setAdminSubTab, adminMessages, loading, adminInput, setAdminInput, handleSendAdmin, rulesJson, setRulesJson, saveStatus, handleSaveRules } = props;
  
  const isOverlayOpen = adminSubTab !== 'command-center';

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-[var(--bg-main)] relative transition-colors duration-500">
      {/* Background Canvas always rendered */}
      <div className={`absolute inset-0 transition-opacity duration-300 ${isOverlayOpen ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
        <CommandCenter />
      </div>

      {/* Glassmorphic Modal Overlay for other modules */}
      {isOverlayOpen && (
        <div className="absolute inset-4 z-50 flex flex-col bg-[var(--bg-panel)] border border-[var(--border-accent)] shadow-2xl backdrop-blur-xl rounded-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 transition-colors">
          
          <div className="flex justify-between items-center p-4 border-b border-[var(--border-accent)] bg-black/5 dark:bg-[#0c1222]/80 transition-colors">
            <span className="text-sm font-bold tracking-widest text-[var(--accent-primary)] uppercase flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-green-500 dark:bg-[#00ff66] animate-pulse"></span>
              MODULE: {adminSubTab.replace('-', ' ')}
            </span>
            <button 
              onClick={() => setAdminSubTab('command-center')}
              className="p-1.5 hover:bg-black/10 dark:hover:bg-white/10 rounded-lg transition-colors group"
              title="Close Module & Return to Canvas"
            >
              <X size={18} className="text-gray-500 dark:text-gray-400 group-hover:text-red-500" />
            </button>
          </div>

          <div className="flex-1 overflow-auto relative">
            {adminSubTab === 'sandbox' && (
              <SandboxView
                adminMessages={adminMessages}
                loading={loading}
                adminInput={adminInput}
                setAdminInput={setAdminInput}
                handleSendAdmin={handleSendAdmin}
                rulesJson={rulesJson}
                setRulesJson={setRulesJson}
                saveStatus={saveStatus}
                handleSaveRules={handleSaveRules}
              />
            )}
            
            {adminSubTab === 'logs' && <LiveLogs liveLogs={props.liveLogs} setLiveLogs={props.setLiveLogs} />}
            {adminSubTab === 'costs' && <CostAuditor costReport={props.costReport} />}
            {adminSubTab === 'health' && <HealthMap healthMap={props.healthMap} />}
            {adminSubTab === 'users' && <UserManager {...props} />}
            {adminSubTab === 'config' && <ConfigEditor envConfig={props.envConfig} setEnvConfig={props.setEnvConfig} handleSaveConfig={props.handleSaveConfig} />}
            {adminSubTab === 'model-router' && <ModelRouter />}
            {adminSubTab === 'skills' && <EnhancedSkillMarketplace />}
            {adminSubTab === 'memory' && <MemoryBrowser />}
            {adminSubTab === 'cloud' && <CloudOrchestrator />}
            {adminSubTab === 'observability' && <ObservabilityDashboard />}
            {adminSubTab === 'threats' && <ThreatDetection />}
            {adminSubTab === 'rules' && <VisualRulesBuilder />}
            {adminSubTab === 'cicd' && <CICDVisualizer />}
            {adminSubTab === 'github' && <GithubIntegration />}
            {adminSubTab === 'backups' && <BackupRestore />}
            {adminSubTab === 'rate-limits' && <RateLimitManager />}
          </div>
        </div>
      )}
    </div>
  );
}

function SandboxView({
  adminMessages, loading, adminInput, setAdminInput, handleSendAdmin,
  rulesJson, setRulesJson, saveStatus, handleSaveRules,
}: {
  adminMessages: ChatMessage[];
  loading: boolean;
  adminInput: string;
  setAdminInput: (val: string) => void;
  handleSendAdmin: () => void;
  rulesJson: string;
  setRulesJson: (val: string) => void;
  saveStatus: string;
  handleSaveRules: () => void;
}) {
  return (
    <div className="flex-grow flex flex-row overflow-hidden">
      <div className="w-1/2 border-r border-[#00f3ff]/10 flex flex-col bg-[#05070a]/50">
        <div className="h-8 border-b border-slate-850 bg-[#0c0f17] px-4 flex items-center justify-between">
          <span className="text-[10px] font-bold text-slate-400 tracking-wider uppercase font-mono">Sandbox Terminal</span>
        </div>
        <SandboxMessages adminMessages={adminMessages} loading={loading} />
        <SandboxInput adminInput={adminInput} setAdminInput={setAdminInput} handleSendAdmin={handleSendAdmin} />
      </div>
      <div className="w-1/2 flex flex-col bg-[#050608]">
        <SandboxRulesHeader saveStatus={saveStatus} handleSaveRules={handleSaveRules} />
        <SandboxRulesEditor rulesJson={rulesJson} setRulesJson={setRulesJson} />
      </div>
    </div>
  );
}

function SandboxMessages({ adminMessages, loading }: { adminMessages: ChatMessage[]; loading: boolean }) {
  return (
    <div className="flex-grow p-4 overflow-y-auto flex flex-col gap-4">
      {adminMessages.map(msg => (
        <div key={msg.id} className={`max-w-[85%] flex flex-col gap-1 ${msg.sender === 'user' ? 'self-end items-end' : 'self-start'}`}>
          <div className={`p-3 rounded-xl text-xs leading-relaxed ${
            msg.sender === 'user'
              ? 'bg-[#00f3ff] text-[#020205] font-bold shadow-[0_4px_12px_rgba(0,243,255,0.2)]'
              : 'bg-white/[0.02] border border-slate-800 text-[#00ff66] font-mono'
          }`}>
            {msg.text}
          </div>
          <span className="text-[9px] text-slate-500 px-1 font-mono">{msg.timestamp}</span>
        </div>
      ))}
      {loading && (
        <div className="text-xs text-slate-400 animate-pulse font-mono flex items-center gap-2">
          <span className="w-1.5 h-1.5 bg-[#00f3ff] rounded-full animate-bounce"></span>
          Synchronizing Neural Link...
        </div>
      )}
    </div>
  );
}

function SandboxInput({ adminInput, setAdminInput, handleSendAdmin }: { adminInput: string; setAdminInput: (val: string) => void; handleSendAdmin: () => void }) {
  return (
    <div className="p-4 border-t border-slate-800 bg-black/30">
      <div className="flex gap-2">
        <input
          type="text"
          placeholder="Input direct testing command to God Layer..."
          value={adminInput}
          onChange={e => setAdminInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSendAdmin()}
          className="flex-grow bg-[#07090f] border border-slate-800 rounded-lg px-4 py-2.5 text-xs text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono"
        />
        <button
          onClick={handleSendAdmin}
          className="bg-[#00f3ff] text-black font-bold px-4 py-2.5 rounded-lg text-xs uppercase hover:bg-cyan-400 transition-colors font-mono"
        >
          RUN
        </button>
      </div>
    </div>
  );
}

function SandboxRulesHeader({ saveStatus, handleSaveRules }: { saveStatus: string; handleSaveRules: () => void }) {
  return (
    <div className="h-8 border-b border-slate-850 bg-[#0c0f17] px-4 flex items-center justify-between">
      <span className="text-[10px] font-bold text-slate-400 tracking-wider uppercase font-mono">Constitutional Rules</span>
      <div className="flex items-center gap-3">
        {saveStatus && <span className="text-[10px] text-slate-400 font-mono">{saveStatus}</span>}
        <button
          onClick={handleSaveRules}
          className="bg-emerald-500 hover:bg-emerald-400 text-black text-[10px] font-bold px-2 py-0.5 rounded transition-colors font-mono uppercase"
        >
          Apply
        </button>
      </div>
    </div>
  );
}

function SandboxRulesEditor({ rulesJson, setRulesJson }: { rulesJson: string; setRulesJson: (val: string) => void }) {
  return (
    <div className="flex-1 p-3">
      <textarea
        className="w-full h-full bg-black/40 border border-slate-900 rounded-lg p-4 text-[#00ff66] font-mono text-xs leading-relaxed outline-none resize-none focus:border-[#00f3ff]/30 focus:shadow-[0_0_15px_rgba(0,243,255,0.05)] transition-all"
        spellCheck="false"
        value={rulesJson}
        onChange={e => setRulesJson(e.target.value)}
      />
    </div>
  );
}