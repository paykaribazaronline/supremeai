# 📄 ফাইল: apps/studio-client/src/App.test.tsx

**প্রকার:** .tsx  
**সাইজ:** 4,711 বাইট  
**আপডেট:** 2026-07-08T01:53:18.653125

---

## কোড

```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('./services/chatService', () => ({
  getAethelResponse: vi.fn().mockResolvedValue('Mock Aethel backend response'),
}));

import { App } from './App';
import { getAethelResponse } from './services/chatService';

// Mock ResizeObserver for ReactFlow in JSDOM
class MockResizeObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}
global.ResizeObserver = MockResizeObserver as any;

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

global.EventSource = MockEventSource as any;

describe('App component', () => {
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

    expect(screen.getByTestId('header-title')).toBeInTheDocument();
    expect(screen.getByTestId('core-status')).toBeInTheDocument();
  });

  // বাংলা মন্তব্য: চ্যাট ট্যাব সক্রিয় করে চ্যাট কনসোল রেন্ডারিং চেক করা হচ্ছে
  it('renders chat console when chat tab is active', () => {
    render(
      <MemoryRouter initialEntries={['/workspace']}>
        <App />
      </MemoryRouter>
    );

    // চ্যাট ট্যাবে ক্লিক করা হচ্ছে
    fireEvent.click(screen.getByTestId('tab-chat'));

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
    fireEvent.click(screen.getByTestId('tab-chat'));

    const input = screen.getByTestId('chat-input');
    fireEvent.change(input, { target: { value: 'Test message' } });

    const sendButton = screen.getByTestId('chat-submit');
    fireEvent.click(sendButton);

    expect(screen.getByText('Test message')).toBeInTheDocument();
    expect(screen.getByText('Analyzing request "Test message"... Processing on central core.')).toBeInTheDocument();
    expect(getAethelResponse).toHaveBeenCalledWith('Test message', expect.any(Array));
  });
});

```