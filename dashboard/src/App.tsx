// App.tsx
import React, { useState, useCallback } from 'react';
import { BrowserRouter as Router, Route, Switch, Redirect } from 'react-router-dom';
import ChatWithAI from './components/ChatWithAI';
import CommandPanel from './components/CommandPanel';
import ProgressMonitor from './components/ProgressMonitor';
import DecisionHistory from './components/DecisionHistory';
import KingModePanel from './components/KingModePanel';
import AuditLog from './components/AuditLog';
import ThreeDashboard from './components/ThreeDashboard';
import CostDashboard from './components/CostDashboard';
import LoginPage from './pages/LoginPage';

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
                    <Route path="/dashboard/3d" component={ThreeDashboard} />
                    <Route path="/chat" component={ChatWithAI} />
                    <Route path="/command" component={CommandPanel} />
                    <Route path="/progress" component={ProgressMonitor} />
                    <Route path="/history" component={DecisionHistory} />
                    <Route path="/kingmode" component={KingModePanel} />
                    <Route path="/audit" component={AuditLog} />
                    <Route path="/costs" component={CostDashboard} />
                    <Redirect from="/" to="/chat" />
                </Switch>
            </div>
        </Router>
    );
};

export default App;
