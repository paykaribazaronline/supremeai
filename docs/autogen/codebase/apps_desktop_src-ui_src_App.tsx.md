# 📄 ফাইল: apps/desktop/src-ui/src/App.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,657 বাইট  
**আপডেট:** 2026-07-05T00:31:19.037385

---

## কোড

```tsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ChatPage from './pages/ChatPage';
import SkillsPage from './pages/SkillsPage';
import EvolutionPage from './pages/EvolutionPage';
import AdminPage from './pages/AdminPage';
import LoginPage from './pages/LoginPage';
import './App.css';
// Use shared DashboardShell from packages
import { DashboardShell as SharedDashboardShell } from '../../../../packages/ui-components/src/components/DashboardShell';
import { useAuthStore } from './stores/authStore';

const NavButton = ({ to, label }: { to: string; label: string }) => (
  <NavLink to={to} className={({ isActive }) => `nav-btn ${isActive ? 'active' : ''}`}>
    {label}
  </NavLink>
);

function App() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <Router>
      <SharedDashboardShell isServerOnline={true}>
        <div className="app-content">
          <main className="main-content">
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/" element={isAuthenticated ? <ChatPage /> : <LoginPage />} />
              <Route path="/skills" element={isAuthenticated ? <SkillsPage /> : <LoginPage />} />
              <Route path="/evolution" element={isAuthenticated ? <EvolutionPage /> : <LoginPage />} />
              <Route path="/admin" element={isAuthenticated ? <AdminPage /> : <LoginPage />} />
              <Route path="*" element={isAuthenticated ? <ChatPage /> : <LoginPage />} />
            </Routes>
          </main>
        </div>
      </SharedDashboardShell>
    </Router>
  );
}

export default App;
```