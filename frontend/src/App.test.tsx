import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('./services/chatService', () => ({
  getAethelResponse: vi.fn().mockImplementation(() => new Promise(() => {})),
}));

vi.mock('./services/apiClient', () => ({
  getRawToken: vi.fn().mockReturnValue(null),
  apiClient: {
    get: vi.fn().mockImplementation((path: string) => {
      if (path === '/api/browser/sessions') return new Promise(() => {}); // never resolves
      return Promise.resolve({ items: [], keys: [], total: 0 });
    }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

import { App } from './App';
import { getAethelResponse } from './services/chatService';

vi.mock('./components/core/AuthGuards', () => ({
  ProtectedRoute: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  GuestRoute: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock ResizeObserver for ReactFlow in JSDOM
class MockResizeObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}
global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;

// Mock the EvolutionForgeWidget subcomponent to simplify App tests
vi.mock('./App', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./App')>();
  return {
    ...actual,
    EvolutionForgeWidget: () => <div data-testid="evolution-forge">// AI Evolution Forge Mock</div>,
  };
});

const mockFetchGateStatus = vi.fn();
const mockExecuteGateOverride = vi.fn();
const mockSetServerStatus = vi.fn();
const mockForgeNewSkill = vi.fn();

const storeState = {
  isServerOnline: true,
  setServerStatus: mockSetServerStatus,
  streamLogs: ['log 1', 'log 2'],
  deployGate: {
    status: 'UNLOCKED',
    reason: 'Initial deploy clean',
  },
  fetchGateStatus: mockFetchGateStatus,
  executeGateOverride: mockExecuteGateOverride,
  isForging: false,
  forgeFeedback: null,
  forgeSuccessCode: null,
  forgeNewSkill: mockForgeNewSkill,
  isConfigLoaded: true,
  setConfig: vi.fn(),
};

vi.mock('./store/useStore', () => ({
  useStore: () => storeState,
}));

// Mock EventSource globally
class MockEventSource {
  url: string;
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  close = vi.fn();
  constructor(url: string) {
    this.url = url;
    if (this.onopen) {
      this.onopen();
    }
  }
}

global.EventSource = MockEventSource as unknown as typeof EventSource;

vi.mock('./hooks/useServerStream', () => ({
  useServerStream: () => ({ streamStatus: 'connected' }),
}));

// Mock InteractiveChatTab to simplify chat tab tests
// বাংলা মন্তব্য: চ্যাট ট্যাবের মেসেজ এবং ইনপুট অ্যাকশনগুলো যাতে টেস্ট করতে সুবিধা হয়, সে জন্য mock প্রপস ওয়্যার আপ করা হলো
interface MockChatMessage {
  id: string | number;
  sender: string;
  text: string;
}

interface MockInteractiveChatTabProps {
  messages?: MockChatMessage[];
  input?: string;
  onInputChange?: (value: string) => void;
  onSend?: () => void;
}

vi.mock('./components/admin/InteractiveChatTab', () => ({
  InteractiveChatTab: ({ messages, input, onInputChange, onSend }: MockInteractiveChatTabProps) => (
    <div>
      <div data-testid="chat-header">Chat</div>
      <div data-testid="chat-messages">
        {messages?.map((msg) => (
          <div key={msg.id}>
            <span>{msg.sender}</span>
            <span>{msg.text}</span>
          </div>
        ))}
      </div>
      <input
        data-testid="chat-input"
        value={input || ''}
        onChange={(e) => onInputChange?.(e.target.value)}
      />
      <button data-testid="chat-submit" onClick={onSend}>Send</button>
    </div>
  ),
}));

vi.mock('./services/adminTokenStore', () => ({
  adminTokenStore: {
    getDecodedToken: vi.fn().mockReturnValue(null),
    isAuthenticated: vi.fn().mockReturnValue(false),
  },
}));

// Mock getApiBaseUrl used by InteractiveChatTab and other components
vi.mock('./utils/api', () => ({
  getApiBaseUrl: vi.fn().mockReturnValue('https://supremeai-backend.onrender.com'),
}));

// Mock useDashboardStore used by InteractiveChatTab
vi.mock('./store/dashboardStore', () => ({
  useDashboardStore: () => ({
    dashboardMode: 'simple',
    chatTabTerminalOpen: false,
    chatTabBrowserOpen: false,
    toggleTerminal: vi.fn(),
    toggleBrowser: vi.fn(),
  }),
}));

describe('App component', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  beforeEach(() => {
    vi.clearAllMocks();
    storeState.isServerOnline = true;
    storeState.deployGate.status = 'UNLOCKED';
    storeState.deployGate.reason = 'Initial deploy clean';
    // বাংলা মন্তব্য: লিগ্যাসি ওয়ার্কস্পেস এখন Devin-স্টাইল শেলের #/workspace রুটে রেন্ডার হয়, তাই টেস্টের আগে hash সেট করা হলো
    window.location.hash = '#/workspace';
  });

  // বাংলা মন্তব্য: UI টেক্সট পরিবর্তন হওয়া সত্ত্বেও টেস্ট যাতে স্ট্যাবল থাকে সে জন্য data-testid ব্যবহার করা হলো
  it('renders header, title, and health status', () => {
    render(
      <MemoryRouter initialEntries={['/workspace']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getAllByTestId('header-title')[0]).toBeInTheDocument();
    expect(screen.getAllByTestId('core-status')[0]).toBeInTheDocument();
  });

  // বাংলা মন্তব্য: চ্যাট ট্যাব সক্রিয় করে চ্যাট কনসোল রেন্ডারিং চেক করা হচ্ছে
  it('renders chat console when chat tab is active', () => {
    render(
      <MemoryRouter initialEntries={['/workspace']}>
        <App />
      </MemoryRouter>
    );

    // চ্যাট ট্যাবে ক্লিক করা হচ্ছে
    fireEvent.click(screen.getAllByTestId('tab-chat')[0]);

    expect(screen.getByTestId('chat-header')).toBeInTheDocument();
  });

  // বাংলা মন্তব্য: চ্যাট প্যানেলে মেসেজ টাইপ ও সাবমিট করে প্রসেসিং সফলভাবে হচ্ছে কিনা টেস্ট করা হচ্ছে
  it('allows user to send messages in the chat console', async () => {
    render(
      <MemoryRouter initialEntries={['/workspace']}>
        <App />
      </MemoryRouter>
    );

    // চ্যাট ট্যাবে ক্লিক করা হচ্ছে
    fireEvent.click(screen.getAllByTestId('tab-chat')[0]);

    const input = screen.getByTestId('chat-input');
    fireEvent.change(input, { target: { value: 'Test message' } });

    const sendButton = screen.getByTestId('chat-submit');
    fireEvent.click(sendButton);

    expect(screen.getAllByText('Test message')[0]).toBeInTheDocument();
    expect(screen.getAllByText('Analyzing request "Test message"... Processing on central core.')[0]).toBeInTheDocument();
    expect(getAethelResponse).toHaveBeenCalledWith('Test message', expect.any(Array));
  });
});
