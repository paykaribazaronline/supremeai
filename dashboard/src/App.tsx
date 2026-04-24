// App.tsx
import React, { useState, useCallback, useEffect, lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, Spin } from 'antd';
import { supremeTheme } from './lib/theme';
import { authUtils } from './lib/authUtils';
import LoginPage from './pages/LoginPage';
import OnboardingWizard from './components/OnboardingWizard';
import i18n from './i18n/conf';

// Initialize i18n
i18n.changeLanguage(localStorage.getItem('language') || 'en');

const ChatWithAI = lazy(() => import('./components/ChatWithAI'));
const ProgressMonitor = lazy(() => import('./components/ProgressMonitor'));
const KingModePanel = lazy(() => import('./components/KingModePanel'));
const AuditLog = lazy(() => import('./components/AuditLog'));
const ThreeDashboard = lazy(() => import('./components/ThreeDashboard'));
const AdminDashboardUnified = lazy(() => import('./pages/AdminDashboardUnified'));

const App: React.FC = () => {
    const [authed, setAuthed] = useState<boolean>(authUtils.isAuthenticated());
    const [showOnboarding, setShowOnboarding] = useState<boolean>(false);

    useEffect(() => {
        if (authed && !localStorage.getItem('onboarding_complete')) {
            setShowOnboarding(true);
        }
    }, [authed]);

    const handleLoginSuccess = useCallback(() => {
        setAuthed(true);
        if (!localStorage.getItem('onboarding_complete')) {
            setShowOnboarding(true);
        }
    }, []);

    const handleOnboardingComplete = useCallback((dontShowAgain: boolean) => {
        setShowOnboarding(false);
        if (dontShowAgain) {
            localStorage.setItem('onboarding_complete', 'true');
        }
    }, []);

    return (
        <ConfigProvider theme={supremeTheme}>
            <div style={{ fontFamily: 'Inter, system-ui, -apple-system, sans-serif' }}>
                {!authed ? (
                    <LoginPage onLoginSuccess={handleLoginSuccess} />
                ) : (
                    <>
                        {showOnboarding && (
                            <OnboardingWizard onComplete={handleOnboardingComplete} />
                        )}
                        <Suspense fallback={<div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                            <Spin size="large" tip="Loading..." />
                        </div>}
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
                        </Suspense>
                    </>
                )}
            </div>
        </ConfigProvider>
    );
};

export default App;
