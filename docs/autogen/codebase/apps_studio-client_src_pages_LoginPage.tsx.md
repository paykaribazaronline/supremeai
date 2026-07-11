# 📄 ফাইল: apps/studio-client/src/pages/LoginPage.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,962 বাইট  
**আপডেট:** 2026-07-11T17:37:52.680010

---

## কোড

```tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please fill out all fields.');
      return;
    }
    // Simulate auth logic
    navigate('/workspace');
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center relative overflow-hidden page-transition-enter-active">
      {/* Neon pulse effects */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-accent-primary/20 via-background to-background"></div>
      
      <div className="z-10 w-full max-w-md p-8 bg-card-bg border border-border-accent shadow-2xl rounded-3xl backdrop-blur-xl">
        <h1 className="text-4xl font-bold text-center mb-2 bg-gradient-to-r from-accent-primary to-neon-purple bg-clip-text text-transparent">
          ⚡ SUPREME AI
        </h1>
        <p className="text-center text-neon-blue font-mono tracking-widest mb-8">Enter the Core</p>
        
        {error && <div className="mb-4 text-danger text-sm text-center font-mono">{error}</div>}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <input 
              type="email" 
              placeholder="Email / Identity"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="w-full bg-input-bg border border-input-border focus:border-neon-blue rounded-xl px-4 py-3 text-foreground outline-none transition-all glow-input font-mono text-sm"
            />
          </div>
          <div>
            <input 
              type="password" 
              placeholder="Passphrase"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full bg-input-bg border border-input-border focus:border-neon-blue rounded-xl px-4 py-3 text-foreground outline-none transition-all glow-input font-mono text-sm"
            />
          </div>
          
          <button 
            type="submit"
            className="w-full py-3 rounded-xl bg-gradient-to-r from-accent-primary to-neon-purple hover:brightness-110 text-white font-bold transition-all shadow-[0_0_15px_var(--supremeai-color-brand-primary-dark)]"
          >
            INITIALIZE SESSION
          </button>
          
          <button 
            type="button"
            className="w-full py-3 rounded-xl border border-border-accent hover:bg-neon-blue/10 text-text-secondary transition-all mt-4 font-mono text-sm"
          >
            Authenticate with Google
          </button>
        </form>
      </div>
    </div>
  );
};


```