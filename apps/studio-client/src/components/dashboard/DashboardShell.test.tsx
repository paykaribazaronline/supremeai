// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেলের স্মোক টেস্ট — সাইডবার নেভিগেশন ও পেজ রাউটিং যাচাই
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';

vi.mock('../../services/apiClient', () => ({
  apiClient: {
    get: vi.fn().mockResolvedValue({ items: [], keys: [], total: 0 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

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

  it('renders the Sujon live background in idle state by default', () => {
    renderShell();
    const bg = screen.getByTestId('sujon-background');
    expect(bg).toBeInTheDocument();
    expect(bg).toHaveAttribute('data-sujon-state', 'idle');
  });

  it('navigates to the Web Authorization Vault page', async () => {
    renderShell();
    await act(async () => {
      fireEvent.click(screen.getByTestId('nav-vault'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(screen.getByTestId('vault-connection-status')).toBeInTheDocument();
  });

  it('navigates to the Site Actions registry editor', async () => {
    renderShell();
    await act(async () => {
      fireEvent.click(screen.getByTestId('nav-site-actions'));
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });
    expect(screen.getByTestId('sa-save-btn')).toBeInTheDocument();
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
    expect(screen.getAllByText('Build a landing page').length).toBeGreaterThan(0);
  });
});
