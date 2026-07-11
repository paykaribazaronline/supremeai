# 📄 ফাইল: apps/studio-client/src/App.tsx

**প্রকার:** .tsx  
**সাইজ:** 7,472 বাইট  
**আপডেট:** 2026-07-11T13:51:38.441916

---

## কোড

```tsx
import React, { useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useStore } from "./store/useStore";

import { ThemeSyncProvider } from './providers/ThemeSyncProvider';
import { GlobalConfigInitializer } from "./components/core/GlobalConfigInitializer";
import { ProtectedRoute, GuestRoute } from "./components/core/AuthGuards";
import { ToastProvider } from './components/ui/Toast';

// Pages
import { AdminShell } from "./pages/admin/AdminShell";
import { LoginScreen } from './pages/auth/LoginScreen';
import { RegisterScreen } from './pages/auth/RegisterScreen';
import { AgentWorkspace } from './pages/AgentWorkspace';
import { IntegrationsManager } from './pages/IntegrationsManager';
import { ArchitectTower } from './pages/ArchitectTower';
import SwarmMap from './components/SwarmMap';
import EvolutionForge from './pages/EvolutionForge/EvolutionForge';
import { DashboardShell } from "./components/dashboard/DashboardShell";
import { UserDashboard } from "./components/customer/UserDashboard";

// Services & Hooks
import { getAethelResponse } from "./services/chatService";
import type { ChatMessage } from "./services/chatService";
import { useServerStream } from "./hooks/useServerStream";
import ErrorBoundary from './components/admin/DashboardErrorBoundary';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error: any) => {
        const msg = error?.message || '';
        if (
          error?.status === 401 || error?.status === 403 || error?.status === 429 ||
          msg.includes('401') || msg.includes('403') || msg.includes('429') ||
          msg.includes('Rate limit') || msg.includes('Unauthorized')
        ) return false;
        return failureCount < 2;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex + Math.random() * 500, 15000),
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

const PORTAL_TYPE = import.meta.env.VITE_PORTAL_TYPE || 'user';

export const App: React.FC = () => {
  return (
    <ThemeSyncProvider>
      <ToastProvider>
        <AppContent />
      </ToastProvider>
    </ThemeSyncProvider>
  );
};

const AppContent: React.FC = () => {
  const { isServerOnline, deployGate } = useStore();
  const { streamStatus } = useServerStream();
  
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [code, setCode] = useState('// Click Preview or Save to interact with the workspace code');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

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
    setCode(code);
  };

  const handlePreview = (code: string) => {
    setCode(code);
  };

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
                 ADMIN PORTAL
              ========================================= */
              <>
                <Route path="/" element={<Navigate to="/admin" replace />} />
                <Route path="/admin/*" element={<AdminShell />} />
                <Route path="*" element={<Navigate to="/admin" replace />} />
              </>
            ) : (
              /* =========================================
                 USER PORTAL (State Machine Routing)
              ========================================= */
              <>
                {/* GUEST STATE */}
                <Route path="/login" element={
                  <GuestRoute>
                    <LoginScreen />
                  </GuestRoute>
                } />
                <Route path="/register" element={
                  <GuestRoute>
                    <RegisterScreen />
                  </GuestRoute>
                } />
                <Route path="/" element={<Navigate to="/workspace" replace />} />
                
                {/* AUTHENTICATED STATE */}
                <Route path="/workspace/agent" element={
                  <ProtectedRoute>
                    <AgentWorkspace />
                  </ProtectedRoute>
                } />
                <Route path="/integrations" element={
                  <ProtectedRoute>
                    <IntegrationsManager />
                  </ProtectedRoute>
                } />
                <Route path="/architect-tower" element={
                  <ProtectedRoute>
                    <ArchitectTower />
                  </ProtectedRoute>
                } />
                <Route path="/swarm" element={
                  <ProtectedRoute>
                    <SwarmMap />
                  </ProtectedRoute>
                } />
                <Route path="/evolution-forge" element={
                  <ProtectedRoute>
                    <EvolutionForge />
                  </ProtectedRoute>
                } />
                <Route path="/workspace/*" element={
                  <ProtectedRoute>
                    <DashboardShell
                      theme={theme}
                      toggleTheme={toggleTheme}
                      isServerOnline={isServerOnline}
                      workspace={legacyWorkspace}
                    />
                  </ProtectedRoute>
                } />

                {/* Users trying to access admin are redirected */}
                <Route path="/admin/*" element={<Navigate to="/" replace />} />
              </>
            )}
          </Routes>
        </GlobalConfigInitializer>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

```