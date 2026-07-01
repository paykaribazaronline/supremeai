// Authentication Service for SupremeAI 2.0
// বাংলা মন্তব্য: এটি অ্যাডমিন লগইন ও ফায়ারবেস অথেন্টিকেশন সার্ভিস প্রোভাইড করে।

import { apiClient } from './apiClient';

export const authService = {
  adminLogin: async (password: string): Promise<{ status: string; message?: string }> => {
    return apiClient.post<{ status: string; message?: string }>('/api/admin/login', { password });
  },

  // অ্যাডমিন ওটিপি যাচাই করার পদ্ধতি
  adminVerifyOtp: async (password: string, otp: string): Promise<{ status: string; token: string }> => {
    return apiClient.post<{ status: string; token: string }>('/api/admin/verify', { password, otp });
  },
};
