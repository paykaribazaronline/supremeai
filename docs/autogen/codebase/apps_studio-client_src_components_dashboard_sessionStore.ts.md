# 📄 ফাইল: apps/studio-client/src/components/dashboard/sessionStore.ts

**প্রকার:** .ts  
**সাইজ:** 5,250 বাইট  
**আপডেট:** 2026-07-05T19:37:54.406286

---

## কোড

```ts
// বাংলা মন্তব্য: সেশন (Devin-স্টাইল টাস্ক/চ্যাট সেশন) — localStorage + ব্যাকএন্ড API সিঙ্ক
// VaultPage.tsx-এর মত ব্যাকএন্ড API কল করে ডেটা পার্সিস্ট করা হয় (Firestore-ভিত্তিক সেশন সিঙ্ক)
import { apiClient } from '../../services/apiClient';

export interface SessionMessage {
  id: number;
  sender: 'User' | 'SupremeAI';
  text: string;
  timestamp: string;
}

export type SessionStatus = 'running' | 'finished' | 'error';

export interface DashboardSession {
  id: string;
  title: string;
  status: SessionStatus;
  created_at: string;
  updated_at: string;
  messages: SessionMessage[];
}

const STORAGE_KEY = 'supremeai_dashboard_sessions';
// বাংলা মন্তব্য: সেশন আপডেট হলে অন্য পেজ (যেমন সেশন ডিটেইল ভিউ) যাতে রিফ্রesh করতে পারে সেজন্য কাস্টম ইভেন্ট
export const SESSIONS_UPDATED_EVENT = 'supremeai:sessions-updated';

// বাংলা মন্তব্য: লোকাল ক্যাশে — ব্যাকএন্ড API কল ব্যর্থ হলে localStorage-এ ফলব্যাক
function loadLocalSessions(): DashboardSession[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveLocalSessions(sessions: DashboardSession[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch {
    // বাংলা মন্তব্য: স্টোরেজ কোটা শেষ হলে নীরবে উপেক্ষা করা হয়
  }
}

// বাংলা মন্তব্য: ব্যাকএন্ড থেকে সেশন লোড করে, ব্যর্থ হলে localStorage থেকে পড়ে
export async function loadSessions(): Promise<DashboardSession[]> {
  try {
    const data = await apiClient.get<{ sessions: DashboardSession[] }>('/api/browser/sessions');
    const sessions = data.sessions || [];
    // বাংলা মন্তব্য: ব্যাকএন্ড ডেটা localStorage-এ ক্যাশে করে রাখি
    saveLocalSessions(sessions);
    return sessions;
  } catch {
    // বাংলা মন্তব্য: API কল ব্যর্থ — localStorage ফলব্যাক
    return loadLocalSessions();
  }
}

// বাংলা মন্তব্য: ব্যাকএন্ডে সেশন সেভ/আপডেট করে, ব্যর্থ হলে localStorage-এ ফলব্যাক
export async function saveSessions(sessions: DashboardSession[]): Promise<void> {
  try {
    // বাংলা মন্তব্য: ব্যাকএন্ডে প্রতিটি সেশন আপডেট/তৈরি করি
    for (const session of sessions) {
      try {
        await apiClient.put(`/api/browser/sessions/${session.id}`, session);
      } catch {
        // বাংলা মন্তব্য: সেশন না থাকলে নতুন করে তৈরি করি
        await apiClient.post('/api/browser/sessions', session);
      }
    }
  } catch {
    // বাংলা মন্তব্য: API কল ব্যর্থ — localStorage ফলব্যাক
  }
  // বাংলা মন্তব্য: সর্বদা localStorage-এ ক্যাশে আপডেট করি
  saveLocalSessions(sessions);
}

export function createSession(prompt: string): DashboardSession {
  const now = new Date().toISOString();
  return {
    id: `sess-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title: prompt.length > 60 ? `${prompt.slice(0, 60)}…` : prompt,
    status: 'running',
    created_at: now,
    updated_at: now,
    messages: [
      {
        id: Date.now(),
        sender: 'User',
        text: prompt,
        timestamp: new Date().toLocaleTimeString(),
      },
    ],
  };
}

export async function upsertSession(session: DashboardSession): Promise<DashboardSession[]> {
  const sessions = await loadSessions();
  const idx = sessions.findIndex((s) => s.id === session.id);
  const updated = { ...session, updated_at: new Date().toISOString() };
  if (idx >= 0) sessions[idx] = updated;
  else sessions.unshift(updated);
  await saveSessions(sessions);
  window.dispatchEvent(new CustomEvent(SESSIONS_UPDATED_EVENT));
  return sessions;
}

export async function deleteSession(id: string): Promise<DashboardSession[]> {
  const sessions = await loadSessions();
  const filtered = sessions.filter((s) => s.id !== id);
  try {
    await apiClient.delete(`/api/browser/sessions/${id}`);
  } catch {
    // বাংলা মন্তব্য: API কল ব্যর্থ — নীরবে উপেক্ষা
  }
  saveLocalSessions(filtered);
  window.dispatchEvent(new CustomEvent(SESSIONS_UPDATED_EVENT));
  return filtered;
}
```