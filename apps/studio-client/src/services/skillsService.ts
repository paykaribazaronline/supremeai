// apps/studio-client/src/services/skillsService.ts
// বাংলা মন্তব্য: ব্যাকএন্ডের /api/skills/catalog এন্ডপয়েন্ট থেকে
// রোল-ভিত্তিক স্কিল ক্যাটালগ ফেচ করার সার্ভিস লেয়ার।

import { getApiBaseUrl } from '../utils/api';
import { getAuthHeaders } from './apiClient';

export type SkillStatus = 'active' | 'deprecated' | 'experimental' | 'coming_soon';

export interface SkillManifest {
  skill_id: string;
  name: string;
  description: string;
  version: string;
  category: string;
  status: SkillStatus;
  tags: string[];
  allowed_roles: string[];
  input_schema: Record<string, unknown>;
  output_schema: Record<string, unknown>;
}

export interface CatalogResponse {
  skills: SkillManifest[];
  total: number;
  user_role: string;
}

// বাংলা মন্তব্য: /api/skills/catalog এন্ডপয়েন্ট থেকে স্কিল লিস্ট ফেচ করে।
// ব্যাকএন্ড নিজেই JWT রোল পার্স করে ফিল্টার করা স্কিল রিটার্ন করে।
export const fetchSkillCatalog = async (): Promise<CatalogResponse> => {
  const API_BASE = getApiBaseUrl();
  const response = await fetch(`${API_BASE}/api/skills/catalog`, {
    method: 'GET',
    headers: await getAuthHeaders(),
  });

  if (!response.ok) {
    const err = await response.json().catch(() => ({ detail: 'Unknown error' }));
    throw new Error(err.detail || `HTTP ${response.status}`);
  }

  return response.json();
};

// বাংলা মন্তব্য: লাইভনেস প্রোব — UI হার্টবিট থেকে /api/v1/live চেক করে
export const checkLiveness = async (): Promise<boolean> => {
  const API_BASE = getApiBaseUrl();
  try {
    const response = await fetch(`${API_BASE}/api/v1/live`, {
      method: 'GET',
      headers: { 'Cache-Control': 'no-cache' },
    });
    return response.ok;
  } catch {
    return false;
  }
};

// বাংলা মন্তব্য: রেডিনেস প্রোব — DB ও Redis সহ সম্পূর্ণ dependency চেক
export const checkReadiness = async (): Promise<{ ready: boolean; subsystems: Record<string, string> }> => {
  const API_BASE = getApiBaseUrl();
  try {
    const response = await fetch(`${API_BASE}/api/v1/ready`, {
      method: 'GET',
      headers: { 'Cache-Control': 'no-cache' },
    });
    const data = await response.json();
    return { ready: response.ok, subsystems: data.subsystems || {} };
  } catch {
    return { ready: false, subsystems: {} };
  }
};

// বাংলা মন্তব্য: স্কিল ক্যাটালগের স্ট্যাটাস রঙ ম্যাপিং হেল্পার
export const getStatusBadge = (status: SkillStatus): { label: string; color: string } => {
  const map: Record<SkillStatus, { label: string; color: string }> = {
    active: { label: '✅ Active', color: 'var(--supremeai-color-success, #22c55e)' },
    experimental: { label: '🧪 Experimental', color: 'var(--supremeai-color-warning, #f59e0b)' },
    deprecated: { label: '⚠️ Deprecated', color: 'var(--supremeai-color-danger, #ef4444)' },
    coming_soon: { label: '🔜 Coming Soon', color: 'var(--supremeai-color-neutral-400, #9ca3af)' },
  };
  return map[status] ?? { label: status, color: '#6b7280' };
};
