# 📄 ফাইল: apps/studio-client/src/pages/auth/RegisterScreen.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,241 বাইট  
**আপডেট:** 2026-07-11T13:28:09.065195

---

## কোড

```tsx
import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { useAuthStore } from '../../store/authStore';

export const RegisterScreen = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const login = useAuthStore((state) => state.login);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password || !name) return;
    
    setIsLoading(true);
    try {
      await login(email, name);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[var(--supremeai-color-bg-void-light)] dark:bg-[var(--supremeai-color-bg-void-dark)] p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-2 text-center">
          <div className="mx-auto bg-[var(--supremeai-color-brand-500)] h-12 w-12 rounded-xl flex items-center justify-center mb-4">
            <span className="text-white font-bold text-xl">SAI</span>
          </div>
          <CardTitle className="text-3xl font-bold">Create Account</CardTitle>
          <CardDescription>
            Enter your details to create a new workspace
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleRegister} className="space-y-4">
            <div className="space-y-4">
              <Input
                label="Full Name"
                type="text"
                placeholder="John Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <Input
                label="Email Address"
                type="email"
                placeholder="developer@supremeai.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              <Input
                label="Password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                helperText="Must be at least 8 characters."
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              className="w-full h-11 text-base mt-6"
              disabled={isLoading || !email || !password || !name}
            >
              {isLoading ? 'Creating account...' : 'Create Account'}
            </Button>
          </form>
        </CardContent>
        <CardFooter className="flex justify-center text-sm text-[var(--supremeai-color-neutral-500)]">
          Already have an account?{' '}
          <button className="text-[var(--supremeai-color-brand-500)] font-medium hover:underline ml-1">
            Sign In
          </button>
        </CardFooter>
      </Card>
    </div>
  );
};

```