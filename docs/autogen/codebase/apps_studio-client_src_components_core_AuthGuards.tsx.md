# 📄 ফাইল: apps/studio-client/src/components/core/AuthGuards.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,398 বাইট  
**আপডেট:** 2026-07-11T10:59:17.907358

---

## কোড

```tsx
import React, { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
// import { useStore } from "../../store/useStore";

// Mocking an auth hook for now - in reality this would use useStore or Firebase Auth
const useAuthStatus = () => {
  const [isChecking, setIsChecking] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    // Simulate token check delay for smooth UX transition
    const timer = setTimeout(() => {
      // Temporarily hardcoded to true for development UI testing, 
      // Replace with actual token validation
      setIsAuthenticated(true);
      setIsChecking(false);
    }, 800);
    
    return () => clearTimeout(timer);
  }, []);

  return { isChecking, isAuthenticated };
};

const LoadingSpinner = () => (
  <div className="flex h-screen w-full flex-col items-center justify-center bg-slate-950">
    <div className="relative flex items-center justify-center">
      <div className="absolute h-16 w-16 animate-ping rounded-full bg-[var(--supremeai-color-brand-primary-dark)] opacity-20"></div>
      <div className="h-16 w-16 animate-spin rounded-full border-4 border-slate-800 border-t-[var(--supremeai-color-brand-primary-dark)] shadow-[0_0_15px_var(--supremeai-color-brand-primary-dark)]"></div>
      <div className="absolute h-8 w-8 rounded-full bg-slate-900 border border-[var(--supremeai-color-brand-primary-dark)] flex items-center justify-center">
        <span className="h-2 w-2 rounded-full bg-[var(--supremeai-color-brand-primary-dark)] shadow-[0_0_8px_var(--supremeai-color-brand-primary-dark)] animate-pulse"></span>
      </div>
    </div>
    <div className="mt-6 font-mono text-sm tracking-widest text-[var(--supremeai-color-brand-primary-dark)] uppercase">
      Authenticating...
    </div>
  </div>
);

export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { isChecking, isAuthenticated } = useAuthStatus();

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

export const GuestRoute = ({ children }: { children: React.ReactNode }) => {
  const { isChecking, isAuthenticated } = useAuthStatus();

  if (isChecking) {
    return <LoadingSpinner />;
  }

  return !isAuthenticated ? <>{children}</> : <Navigate to="/workspace" replace />;
};

```