// App.tsx
import React, { useState, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { supremeTheme } from './lib/theme';
import { authUtils } from './lib/authUtils';
import ChatWithAI from './components/ChatWithAI';
import ProgressMonitor from './components/ProgressMonitor';
import KingModePanel from './components/KingModePanel';
import AuditLog from './components/AuditLog';
import ThreeDashboard from './components/ThreeDashboard';
import LoginPage from './pages/LoginPage';
import AdminDashboardUnified from './pages/AdminDashboardUnified';

const App: React.FC = () => {
    const [authed, setAuthed] = useState<boolean>(authUtils.isAuthenticated());

    const handleLoginSuccess = useCallback(() => {
        setAuthed(true);
    }, []);

    return (
        <ConfigProvider theme={supremeTheme}>
            <div style={{ fontFamily: 'Inter, system-ui, -apple-system, sans-serif' }}>
                {!authed ? (
                    <LoginPage onLoginSuccess={handleLoginSuccess} />
                ) : (
                    <Router>
                        <Routes>
                            <Route path="/admin" element={<AdminDashboardUnified />} />
                            <Route path="/dashboard/3d" element={<ThreeDashboard />} />
                            <Route path="/chat" element={<ChatWithAI />} />
                            <Route path="/progress" element={<ProgressMonitor />} />
                            <Route path="/kingmode" element={<KingModePanel />} />
                            <Route path="/audit" element={<AuditLog />} />
                            <Route path="*" element={<Navigate to="/admin" replace />} />
                        </Routes>
                    </Router>
                )}
            </div>
        </ConfigProvider>
    );
};

export default App;
