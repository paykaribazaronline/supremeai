// RoleContext.tsx - User role and permissions provider
import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from "react";

import { authUtils } from "../lib/authUtils";

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
    throw new Error("useRole must be used within a RoleProvider");
  }
  return context;
};

interface RoleProviderProps {
  children: ReactNode;
}

export const RoleProvider: React.FC<RoleProviderProps> = ({ children }) => {
  const [isAdmin, setIsAdmin] = useState(() => authUtils.isAdmin());
  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    const token = authUtils.getToken();
    return !!token && token !== "GUEST_MODE";
  });
  const [isGuest, setIsGuest] = useState(
    () => authUtils.getToken() === "GUEST_MODE",
  );
  const [user, setUser] = useState<any>(() => authUtils.getCurrentUser());

  const refreshUser = () => {
    const token = authUtils.getToken();
    const currentUser = authUtils.getCurrentUser();

    setIsAuthenticated(!!token && token !== "GUEST_MODE");
    setIsGuest(token === "GUEST_MODE");
    setUser(currentUser);

    // Check admin status: rely on backend-assigned roles/tiers
    const isAdminUser = authUtils.isAdmin();

    console.log("[RoleContext] Refreshing user:", currentUser?.email);
    console.log(
      "[RoleContext] Raw role/tier:",
      currentUser?.role,
      "/",
      currentUser?.tier,
    );
    console.log("[RoleContext] Calculated isAdmin:", isAdminUser);

    setIsAdmin(isAdminUser);
  };

  useEffect(() => {
    refreshUser();

    // Listen for storage events (e.g., login/logout in another tab)
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === "supremeai_token" || e.key === "supremeai_user") {
        refreshUser();
      }
    };

    window.addEventListener("storage", handleStorageChange);
    return () => {
      window.removeEventListener("storage", handleStorageChange);
    };
  }, []);

  return (
    <RoleContext.Provider
      value={{ isAdmin, isAuthenticated, isGuest, user, refreshUser }}
    >
      {children}
    </RoleContext.Provider>
  );
};
