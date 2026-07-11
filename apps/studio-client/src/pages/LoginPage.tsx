import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useStore } from '../store/useStore';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  // We simulate a login mutation here
  // In a real app this uses useAuth or useStore().login
  // The UI/UX Blueprint suggests using glassmorphism and the Supreme colors

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please fill out all fields.');
      return;
    }
    // Simulate setting authenticated state
    // We can just use the store
    // useStore.getState().setIsAuthenticated(true);
    // (We'll assume the store has a generic login or we just push to /workspace)
    // For now we just navigate to /workspace
    navigate('/workspace');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center relative overflow-hidden page-transition-enter-active">
      {/* Neon particles placeholder */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-cyan-900/20 via-slate-950 to-slate-950"></div>
      
      <div className="z-10 w-full max-w-md p-8 glass-card border border-cyan-500/20 shadow-2xl rounded-3xl backdrop-blur-xl">
        <h1 className="text-4xl font-bold text-center mb-2 bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent">
          ⚡ SUPREME AI
        </h1>
        <p className="text-center text-cyan-400 font-mono tracking-widest mb-8">2.0</p>
        
        {error && <div className="mb-4 text-red-400 text-sm text-center">{error}</div>}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <input 
              type="email" 
              placeholder="Email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="w-full bg-slate-900/50 border border-slate-700 focus:border-cyan-500 rounded-xl px-4 py-3 text-slate-200 outline-none transition-all glow-input"
            />
          </div>
          <div>
            <input 
              type="password" 
              placeholder="Password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full bg-slate-900/50 border border-slate-700 focus:border-cyan-500 rounded-xl px-4 py-3 text-slate-200 outline-none transition-all glow-input"
            />
          </div>
          
          <button 
            type="submit"
            className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-600 to-purple-600 hover:from-cyan-500 hover:to-purple-500 text-white font-bold transition-all shadow-[0_0_15px_rgba(0,243,255,0.3)]"
          >
            লগইন করুন
          </button>
          
          <button 
            type="button"
            className="w-full py-3 rounded-xl border border-cyan-500/30 hover:bg-cyan-500/10 text-slate-300 transition-all mt-4"
          >
            Google দিয়ে লগইন
          </button>
        </form>
      </div>
    </div>
  );
};
