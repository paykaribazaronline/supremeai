// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেলের স্মোক টেস্ট — সাইডবার নেভিগেশন ও পেজ রাউটিং যাচাই
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';

vi.mock('../../services/apiClient', () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const sessionsStore: Record<string, any> = {};
  return {
    apiClient: {
      get: vi.fn().mockImplementation((path: string) => {
        if (path === '/api/browser/sessions') return Promise.resolve({ sessions: Object.values(sessionsStore) });
        return Promise.resolve({ items: [], keys: [], total: 0 });
      }),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      post: vi.fn().mockImplementation((path: string, body?: any) => {
        if (path === '/api/browser/sessions' && body?.id) {
          sessionsStore[body.id] = body;
        }
        return Promise.resolve({});
      }),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      put: vi.fn().mockImplementation((_path: string, body?: any) => {
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
import { MemoryRouter } from 'react-router-dom';

const renderShell = () =>
  render(
    <MemoryRouter>
      <DashboardShell
        theme="dark"
        toggleTheme={vi.fn()}
        isServerOnline={true}
        workspace={<div data-testid="legacy-workspace">Workspace content</div>}
      />
    </MemoryRouter>
  );

describe('DashboardShell', () => {
  beforeEach(() => {
    window.location.hash = '';
    localStorage.clear();
  });

  it('renders sidebar with all new navigation items', () => {
    renderShell();
    expect(screen.getByTestId('dashboard-sidebar')).toBeInTheDocument();

    const expectedNavs = ['workspace', 'agent', 'ide', 'skills', 'integrations', 'analytics', 'profile'];
    for (const nav of expectedNavs) {
      expect(screen.getByTestId(`nav-${nav}`)).toBeInTheDocument();
    }
  });

  it('renders the Code Editor panel with header and initial code structure', () => {
    renderShell();
    expect(screen.getByText('index.tsx')).toBeInTheDocument();
    expect(screen.getByText(/Hello World!/i)).toBeInTheDocument();
  });

  it('renders the AI Assistant panel with user messages and input field', () => {
    renderShell();
    expect(screen.getByRole('heading', { name: /AI Assistant/i })).toBeInTheDocument();
    expect(screen.getByText('How can I optimize this function?')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Ask AI anything...')).toBeInTheDocument();
  });

  it('toggles chat history drawer and starts new chat session', () => {
    renderShell();
    
    // Toggle history drawer
    const historyBtn = screen.getByLabelText('Chat History');
    fireEvent.click(historyBtn);
    expect(screen.getByText(/Past Conversations/i)).toBeInTheDocument();
    expect(screen.getByText('Swarm Telemetry Audit')).toBeInTheDocument();

    // Start new chat
    const newChatBtn = screen.getByLabelText('New Chat');
    fireEvent.click(newChatBtn);
    expect(screen.getByText('Start a Conversation')).toBeInTheDocument();
    expect(screen.getByText('⚡ Optimize Code')).toBeInTheDocument();
  });

  it('renders stats cards showing active projects and completed tasks count', () => {
    renderShell();
    expect(screen.getByText('Active Projects')).toBeInTheDocument();
    expect(screen.getByText('Tasks Completed')).toBeInTheDocument();
    expect(screen.getByText('24')).toBeInTheDocument();
    expect(screen.getByText('142')).toBeInTheDocument();
  });

  it('renders server online status indicator', () => {
    renderShell();
    expect(screen.getByText(/Server Status: Online/i)).toBeInTheDocument();
  });
});
