export interface User {
  uid: string;
  email: string;
  displayName: string;
  tier: string;
  isActive: boolean;
  currentUsage: number;
  monthlyQuota: number;
  createdAt: string | null;
  lastLoginAt: string | null;
}

export type UserSortField =
  | "email"
  | "displayName"
  | "tier"
  | "isActive"
  | "currentUsage"
  | "monthlyQuota"
  | "lastLoginAt"
  | "usagePercent";
