# 📄 ফাইল: apps/studio-client/src/components/dashboard/DashboardShell.test.tsx

**প্রকার:** .tsx  
**সাইজ:** 4,928 বাইট  
**আপডেট:** 2026-07-07T18:09:12.406557

---

## কোড

```tsx
// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেলের স্মোক টেস্ট — সাইডবার নেভিগেশন ও পেজ রাউটিং যাচাই
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';

vi.mock('../../services/apiClient', () => {
  const sessionsStore: Record<string, any> = {};
  return {
    apiClient: {
      get: vi.fn().mockImplementation((path: string) => {
        if (path === '/api/browser/sessions') return Promise.resolve({ sessions: Object.values(sessionsStore) });
        return Promise.resolve({ items: [], keys: [], total: 0 });
      }),
      post: vi.fn().mockImplementation((path: string, body?: any) => {
        if (path === '/api/browser/sessions' && body?.id) {
          sessionsStore[body.id] = body;
        }
        return Promise.resolve({});
      }),
      put: vi.fn().mockImplementation((path: string, body?: any) => {
        if (body?.id) sessionsStore[body.id] = body;
        return Promise.resolve({});
      }),
      delete: vi.fn().mockImplementation((path: string) => {
        const id = path.split('/').pop();
        if (id) delete sessionsStore[id];
        return Promise.resolve({});
      }),
    },
  };
});

vi.mock('../../services/chatService', () => ({
  getAethelResponse: vi.fn().mockResolvedValue('Mock response'),
}));

import { DashboardShell } from './DashboardShell';

const renderShell = () =>
  render(
    <DashboardShell
      theme="dark"
      toggleTheme={vi.fn()}
      isServerOnline={true}
      workspace={<div data-testid="legacy-workspace">Workspace content</div>}
    />
  );

describe('DashboardShell', () => {
  beforeEach(() => {
    window.location.hash = '';
    localStorage.clear();
  });

  it('renders sidebar with all navigation items', () => {
    renderShell();
    expect(screen.getByTestId('dashboard-sidebar')).toBeInTheDocument();
    for (const nav of [
      'sessions',
      'workspace',
      'vault',
      'automation',
      'knowledge',
      'secrets',
      'usage',
      'settings',
      'site-actions',
      'llm-gateway',
      'admin',
    ]) {
      expect(screen.getByTestId(`nav-${nav}`)).toBeInTheDocument();
    }
    expect(screen.getByTestId('sidebar-server-status')).toHaveTextContent('Online');
  });

  it.skip('renders the Sujon live background in idle state by default', () => {
    renderShell();
    const bg = screen.getByTestId('sujon-background');
    expect(bg).toBeInTheDocument();
    expect(bg).toHaveAttribute('data-sujon-state', 'idle');
  });

  it.skip('navigates to the Web Authorization Vault page', async () => {
    renderShell();
    await act(async () => {
      fireEvent.click(screen.getByTestId('nav-vault'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(await screen.findByTestId('vault-connection-status')).toBeInTheDocument();
  });

  it.skip('navigates to the Site Actions registry editor', async () => {
    renderShell();
    await act(async () => {
      fireEvent.click(screen.getByTestId('nav-site-actions'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(await screen.findByTestId('sa-save-btn')).toBeInTheDocument();
  });

  it('shows sessions page with composer by default', () => {
    renderShell();
    expect(screen.getByTestId('session-composer')).toBeInTheDocument();
    expect(screen.getByTestId('start-session-btn')).toBeInTheDocument();
  });

  it('navigates to workspace page rendering legacy dashboard', async () => {
    renderShell();
    await act(async () => {
      fireEvent.click(screen.getByTestId('nav-workspace'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(screen.getByTestId('legacy-workspace')).toBeInTheDocument();
  });

  it('navigates to knowledge page', async () => {
    renderShell();
    await act(async () => {
      fireEvent.click(screen.getByTestId('nav-knowledge'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(screen.getByTestId('knowledge-search-input')).toBeInTheDocument();
  });

  it('starts a new session from the composer', async () => {
    renderShell();
    fireEvent.change(screen.getByTestId('session-composer'), {
      target: { value: 'Build a landing page' },
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId('start-session-btn'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    // বাংলা মন্তব্য: সেশন ডিটেইল পেজ async loadSessions() কল করে — তাই find* ব্যবহার করা হয়
    const elements = await screen.findAllByText(/Session Cockpit:/i, {}, { timeout: 3000 });
    expect(elements.length).toBeGreaterThan(0);
  });
});

```