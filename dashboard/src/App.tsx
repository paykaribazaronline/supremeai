// App.tsx
import React, { useState, useCallback } from 'react';
import { BrowserRouter as Router, Route, Switch, Redirect } from 'react-router-dom';
import ChatWithAI from './components/ChatWithAI';
import ProgressMonitor from './components/ProgressMonitor';
import KingModePanel from './components/KingModePanel';
import AuditLog from './components/AuditLog';
import ThreeDashboard from './components/ThreeDashboard';
import LoginPage from './pages/LoginPage';
import AdminDashboardUnified from './pages/AdminDashboardUnified';

function isAuthenticated(): boolean {
    return !!localStorage.getItem('supremeai_token');
}

const App: React.FC = () => {
    const [authed, setAuthed] = useState<boolean>(isAuthenticated());

    const handleLoginSuccess = useCallback(() => {
        setAuthed(true);
    }, []);

    if (!authed) {
        return <LoginPage onLoginSuccess={handleLoginSuccess} />;
    }

    return (
        <Router>
            <div>
                <nav>
                    {/* Sidebar navigation here */}
                </nav>
                <Switch>
                    <Route path="/admin" component={AdminDashboardUnified} />
                    <Route path="/dashboard/3d" component={ThreeDashboard} />
                    <Route path="/chat" component={ChatWithAI} />
                    <Route path="/progress" component={ProgressMonitor} />
                    <Route path="/kingmode" component={KingModePanel} />
                    <Route path="/audit" component={AuditLog} />
                    <Redirect from="/" to="/chat" />
                </Switch>
            </div>
        </Router>
    );
};

export default App;
