import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

// বাংলা মন্তব্য: ইউজার রেজিস্ট্রেশন পেজ (নিয়ন থিম) — ব্লার-মুক্ত ও শার্প টেক্সটের জন্য তৈরি করা হয়েছে
export const RegisterPage: React.FC = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const register = useAuthStore((state) => state.register);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!email || !password || !name) {
      setError('দয়া করে সব ফিল্ড পূরণ করুন।');
      return;
    }

    setIsLoading(true);
    try {
      // বাংলা মন্তব্য: আসল অথেনটিকেশন — authStore এর মাধ্যমে ব্যাকএন্ডে রেজিস্টার করছে
      await register(email, name, password);
      navigate('/workspace');
    } catch (err) {
      setError('রেজিস্ট্রেশন ব্যর্থ হয়েছে। ইমেইল বা পাসওয়ার্ড যাচাই করুন।');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--supremeai-color-bg-void-light)] dark:bg-[var(--supremeai-color-bg-void-dark)] flex flex-col items-center justify-center relative overflow-hidden">
      {/* বাংলা মন্তব্য: নিয়ন পালস ইফেক্ট (কার্ডের বাইরে, কার্ড ব্লার-মুক্ত থাকবে) */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-accent-primary/10 via-transparent to-transparent pointer-events-none"></div>

      <div className="z-10 w-full max-w-md p-8 bg-[var(--supremeai-color-bg-elevated-light)] dark:bg-[var(--supremeai-color-bg-elevated-dark)] border border-[var(--supremeai-color-border-accent-light)] dark:border-[var(--supremeai-color-border-accent-dark)] shadow-2xl rounded-3xl">
        <h1 className="text-4xl font-bold text-center mb-2 bg-gradient-to-r from-accent-primary to-neon-purple bg-clip-text text-transparent">
          ⚡ SUPREME AI
        </h1>
        <p className="text-center text-neon-blue font-semibold tracking-wide mb-8">Create Your Core</p>

        {error && <div className="mb-4 text-danger text-sm text-center font-medium">{error}</div>}

        <form onSubmit={handleRegister} className="space-y-4">
          <div>
            <input
              type="text"
              placeholder="Full Name"
              value={name}
              onChange={e => setName(e.target.value)}
              className="w-full bg-[var(--supremeai-color-bg-void-light)] dark:bg-[var(--supremeai-color-bg-void-dark)] border border-[var(--supremeai-color-border-default-light)] dark:border-[var(--supremeai-color-border-default-dark)] focus:border-neon-blue rounded-xl px-4 py-3 text-foreground outline-none transition-all"
            />
          </div>
          <div>
            <input
              type="email"
              placeholder="Email / Identity"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="w-full bg-[var(--supremeai-color-bg-void-light)] dark:bg-[var(--supremeai-color-bg-void-dark)] border border-[var(--supremeai-color-border-default-light)] dark:border-[var(--supremeai-color-border-default-dark)] focus:border-neon-blue rounded-xl px-4 py-3 text-foreground outline-none transition-all"
            />
          </div>
          <div>
            <input
              type="password"
              placeholder="Passphrase"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full bg-[var(--supremeai-color-bg-void-light)] dark:bg-[var(--supremeai-color-bg-void-dark)] border border-[var(--supremeai-color-border-default-light)] dark:border-[var(--supremeai-color-border-default-dark)] focus:border-neon-blue rounded-xl px-4 py-3 text-foreground outline-none transition-all"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-accent-primary to-neon-purple hover:brightness-110 text-white font-bold transition-all disabled:opacity-50 disabled:pointer-events-none"
          >
            {isLoading ? 'CREATING...' : 'CREATE ACCOUNT'}
          </button>

          <button
            type="button"
            className="w-full py-3 rounded-xl border border-[var(--supremeai-color-border-accent-light)] dark:border-[var(--supremeai-color-border-accent-dark)] hover:bg-neon-blue/10 text-text-secondary transition-all mt-4 font-semibold"
          >
            Authenticate with Google
          </button>
        </form>

        <p className="text-center text-sm text-text-secondary mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-neon-blue font-medium hover:underline ml-1">
            Sign In
          </Link>
        </p>
      </div>
    </div>
  );
};
