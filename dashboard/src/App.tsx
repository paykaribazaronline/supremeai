// App.tsx
import React from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import ChatWithAI from './components/ChatWithAI';
import CommandPanel from './components/CommandPanel';
import ProgressMonitor from './components/ProgressMonitor';
import DecisionHistory from './components/DecisionHistory';
import KingModePanel from './components/KingModePanel';
import AuditLog from './components/AuditLog';

const App: React.FC = () => {
    return (
        <Router>
            <div>
                <nav>
                    {/* Sidebar navigation here */}
                </nav>
                <Switch>
                    <Route path="/chat" component={ChatWithAI} />
                    <Route path="/command" component={CommandPanel} />
                    <Route path="/progress" component={ProgressMonitor} />
                    <Route path="/history" component={DecisionHistory} />
                    <Route path="/kingmode" component={KingModePanel} />
                    <Route path="/audit" component={AuditLog} />
                </Switch>
            </div>
        </Router>
    );
};

export default App;
