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

  // বাংলা মন্তব্য: ফায়ারবেস লগইন সার্ভিস এন্ডপয়েন্ট
  firebaseLogin: async (idToken: string): Promise<{ status: string; token?: string; uid?: string; email?: string }> => {
    return apiClient.post<{ status: string; token?: string; uid?: string; email?: string }>('/api/admin/firebase-login', { id_token: idToken });
  },

  // বাংলা মন্তব্য: ফায়ারবেস টিওটিপি ৭ ডিজিট কনফিগারেশন সেটআপ সার্ভিস এন্ডপয়েন্ট
  firebaseTotpSetup: async (idToken: string): Promise<{ secret: string; provisioning_uri: string }> => {
    return apiClient.post<{ secret: string; provisioning_uri: string }>('/api/admin/firebase-totp-setup', { id_token: idToken });
  },

  // বাংলা মন্তব্য: ফায়ারবেস ওটিপি কোড ৭ ডিজিট যাচাইকরণ সার্ভিস এন্ডপয়েন্ট
  firebaseTotpVerify: async (idToken: string, otp: string): Promise<{ status: string; token: string }> => {
    return apiClient.post<{ status: string; token: string }>('/api/admin/firebase-totp-verify', { id_token: idToken, otp });
  },
};
