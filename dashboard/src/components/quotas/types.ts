export interface UserQuota {
  uid: string;
  email: string;
  displayName: string;
  tier: string;
  isActive: boolean;
  currentUsage: number;
  monthlyQuota: number;
  createdAt: string;
}

export interface QuotaStatsData {
  totalUsers: number;
  activeQuotas: number;
  overLimit: number;
}
