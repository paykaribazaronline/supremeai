// RoleContext.tsx - User role and permissions provider
import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authUtils } from '../lib/authUtils';

interface RoleContextType {
  isAdmin: boolean;
  isAuthenticated: boolean;
  isGuest: boolean;
  user: any;
  refreshUser: () => void;
}

const RoleContext = createContext<RoleContextType | undefined>(undefined);

export const useRole = () => {
  const context = useContext(RoleContext);
  if (!context) {
    throw new Error('useRole must be used within a RoleProvider');
  }
  return context;
};

interface RoleProviderProps {
  children: ReactNode;
}

export const RoleProvider: React.FC<RoleProviderProps> = ({ children }) => {
  const [isAdmin, setIsAdmin] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isGuest, setIsGuest] = useState(false);
  const [user, setUser] = useState<any>(null);

  const refreshUser = () => {
    const token = authUtils.getToken();
    const currentUser = authUtils.getCurrentUser();

    setIsAuthenticated(!!token && token !== 'GUEST_MODE');
    setIsGuest(token === 'GUEST_MODE');
    setUser(currentUser);

    // Check admin status: rely on backend-assigned roles/tiers
    const isAdminUser =
      currentUser?.role === 'admin' ||
      currentUser?.tier === 'admin' ||
      currentUser?.tier === 'ADMIN';

    console.log('[RoleContext] Refreshing user:', currentUser?.email);
    console.log('[RoleContext] Raw role/tier:', currentUser?.role, '/', currentUser?.tier);
    console.log('[RoleContext] Calculated isAdmin:', isAdminUser);

    setIsAdmin(isAdminUser);
  };

  useEffect(() => {
    refreshUser();
    // Listen for storage changes (login/logout in other tabs)
    const handleStorage = () => refreshUser();
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  return (
    <RoleContext.Provider value={{ isAdmin, isAuthenticated, isGuest, user, refreshUser }}>
      {children}
    </RoleContext.Provider>
  );
};
