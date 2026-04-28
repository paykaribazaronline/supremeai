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

// Lazy load admin pages for code splitting
const AdminDashboardUnified = lazy(() => import('./pages/AdminDashboardUnified'));
const AdminUsers = lazy(() => import('./pages/AdminUsers'));
const AdminSettings = lazy(() => import('./pages/AdminSettings'));
const AdminProjects = lazy(() => import('./pages/AdminProjects'));
const AdminProviders = lazy(() => import('./pages/AdminProviders'));
const AdminLogs = lazy(() => import('./pages/AdminLogs'));
const AdminNotifications = lazy(() => import('./pages/AdminNotifications'));
const AdminBackup = lazy(() => import('./pages/AdminBackup'));
const AdminReports = lazy(() => import('./pages/AdminReports'));
const AdminMonitoring = lazy(() => import('./pages/AdminMonitoring'));
const AdminPerformance = lazy(() => import('./pages/AdminPerformance'));
const AdminOCR = lazy(() => import('./pages/AdminOCR'));
const AdminAPIKeys = lazy(() => import('./pages/AdminAPIKeys'));

// Existing specialty components
const ChatWithAI = lazy(() => import('./components/ChatWithAI'));
const ProgressMonitor = lazy(() => import('./components/ProgressMonitor'));
const KingModePanel = lazy(() => import('./components/KingModePanel'));
const AuditLog = lazy(() => import('./components/AuditLog'));
const ThreeDashboard = lazy(() => import('./components/ThreeDashboard'));
const VideoTutorials = lazy(() => import('./components/VideoTutorials'));

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
                        </div>}>
                            <Router>
                                <Routes>
                                    {/* Admin Panel Unified Dashboard - Default Route */}
                                    <Route path="/admin" element={<AdminDashboardUnified />} />

                                    {/* Admin Panel Sub-routes - Single URL Rule */}
                                    <Route path="/admin/users" element={<AdminUsers />} />
                                    <Route path="/admin/settings" element={<AdminSettings />} />
                                    <Route path="/admin/projects" element={<AdminProjects />} />
                                    <Route path="/admin/providers" element={<AdminProviders />} />
                                    <Route path="/admin/apikeys" element={<AdminAPIKeys />} />
                                    <Route path="/admin/logs" element={<AdminLogs />} />
                                    <Route path="/admin/notifications" element={<AdminNotifications />} />
                                    <Route path="/admin/backup" element={<AdminBackup />} />
                                    <Route path="/admin/reports" element={<AdminReports />} />
                                    <Route path="/admin/monitoring" element={<AdminMonitoring />} />
                                    <Route path="/admin/performance" element={<AdminPerformance />} />
                                    <Route path="/admin/ocr" element={<AdminOCR />} />
                                    <Route path="/admin/console" element={<Navigate to="/admin" replace />} />

                                    {/* Existing admin features moved under /admin */}
                                    <Route path="/admin/chat" element={<ChatWithAI />} />
                                    <Route path="/admin/progress" element={<ProgressMonitor />} />
                                    <Route path="/admin/kingmode" element={<KingModePanel />} />
                                    <Route path="/admin/audit" element={<AuditLog />} />
                                    <Route path="/admin/tutorials" element={<VideoTutorials />} />
                                    <Route path="/admin/3d" element={<ThreeDashboard />} />

                                    {/* Legacy route redirects for backward compatibility */}
                                    <Route path="/admin.html" element={<Navigate to="/admin" replace />} />
                                    <Route path="/admin-dashboard.html" element={<Navigate to="/admin" replace />} />
                                    <Route path="/admin-users.html" element={<Navigate to="/admin/users" replace />} />
                                    <Route path="/admin-settings.html" element={<Navigate to="/admin/settings" replace />} />
                                    <Route path="/admin-projects.html" element={<Navigate to="/admin/projects" replace />} />
                                    <Route path="/admin-providers.html" element={<Navigate to="/admin/providers" replace />} />
                                    <Route path="/admin-apikeys.html" element={<Navigate to="/admin/apikeys" replace />} />
                                    <Route path="/admin-logs.html" element={<Navigate to="/admin/logs" replace />} />
                                    <Route path="/admin-notifications.html" element={<Navigate to="/admin/notifications" replace />} />
                                    <Route path="/admin-backup.html" element={<Navigate to="/admin/backup" replace />} />
                                    <Route path="/admin-reports.html" element={<Navigate to="/admin/reports" replace />} />
                                    <Route path="/monitoring-dashboard.html" element={<Navigate to="/admin/monitoring" replace />} />
                                    <Route path="/performance-dashboard.html" element={<Navigate to="/admin/performance" replace />} />
                                    <Route path="/bengali-ocr.html" element={<Navigate to="/admin/ocr" replace />} />
                                    <Route path="/admin-console.html" element={<Navigate to="/admin" replace />} />

                                    {/* Legacy root redirects */}
                                    <Route path="/chat" element={<Navigate to="/admin/chat" replace />} />
                                    <Route path="/progress" element={<Navigate to="/admin/progress" replace />} />
                                    <Route path="/kingmode" element={<Navigate to="/admin/kingmode" replace />} />
                                    <Route path="/audit" element={<Navigate to="/admin/audit" replace />} />
                                    <Route path="/tutorials" element={<Navigate to="/admin/tutorials" replace />} />
                                    <Route path="/dashboard/3d" element={<Navigate to="/admin/3d" replace />} />

                                    {/* Catch-all redirect to admin */}
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
