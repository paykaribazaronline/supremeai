# 📄 ফাইল: apps/studio-client/src/components/dashboard/sessionStore.ts

**প্রকার:** .ts  
**সাইজ:** 2,625 বাইট  
**আপডেট:** 2026-07-04T04:38:12.345684

---

## কোড

```ts
// বাংলা মন্তব্য: সেশন (Devin-স্টাইল টাস্ক/চ্যাট সেশন) localStorage-এ সংরক্ষণের ইউটিলিটি
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
// বাংলা মন্তব্য: সেশন আপডেট হলে অন্য পেজ (যেমন সেশন ডিটেইল ভিউ) যাতে রিফ্রেশ করতে পারে সেজন্য কাস্টম ইভেন্ট
export const SESSIONS_UPDATED_EVENT = 'supremeai:sessions-updated';

export function loadSessions(): DashboardSession[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveSessions(sessions: DashboardSession[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch {
    // বাংলা মন্তব্য: স্টোরেজ কোটা শেষ হলে নীরবে উপেক্ষা করা হয়
  }
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

export function upsertSession(session: DashboardSession): DashboardSession[] {
  const sessions = loadSessions();
  const idx = sessions.findIndex((s) => s.id === session.id);
  const updated = { ...session, updated_at: new Date().toISOString() };
  if (idx >= 0) sessions[idx] = updated;
  else sessions.unshift(updated);
  saveSessions(sessions);
  window.dispatchEvent(new CustomEvent(SESSIONS_UPDATED_EVENT));
  return sessions;
}

export function deleteSession(id: string): DashboardSession[] {
  const sessions = loadSessions().filter((s) => s.id !== id);
  saveSessions(sessions);
  return sessions;
}

```