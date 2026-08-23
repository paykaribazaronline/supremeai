import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { componentEventBus } from '../../../lib/componentEventBus';
import { eventBus, Events } from '../../../lib/eventBus';
import { useUnifiedStore } from '../../../store/unifiedStore';
import {
  Globe, ArrowLeft, ArrowRight, RotateCw, Plus, X, Star, Camera,
  Monitor, Smartphone, Tablet, ZoomIn, ZoomOut, Maximize2, Minimize2,
  Shield, Wifi, WifiOff, Loader2, Code, Terminal, Sparkles,
  Clock, Download, ExternalLink, Lock, Unlock, AlertTriangle,
  Bot, MessageSquare, Eye, EyeOff, Copy, Check, ChevronDown
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

// ════════════════════════════════════════════════════════════════════
// TYPES
// ════════════════════════════════════════════════════════════════════

interface BrowserTab {
  id: string;
  url: string;
  title: string;
  favicon?: string;
  isLoading: boolean;
  error?: string;
  lastVisited: number;
  bookmarked: boolean;
}

interface Bookmark {
  id: string;
  url: string;
  title: string;
  category: 'service' | 'tool' | 'doc' | 'frequent';
  icon?: React.ReactNode;
}

interface HistoryEntry {
  url: string;
  title: string;
  timestamp: number;
  tabId: string;
}

interface ConsoleMessage {
  type: 'log' | 'error' | 'warn' | 'info';
  content: string;
  timestamp: number;
  source?: string;
}

interface AIBrowserAction {
  type: 'summarize' | 'explain' | 'extract_links' | 'find_issues' | 'interact';
  payload?: any;
}

// ════════════════════════════════════════════════════════════════════
// DEFAULT BOOKMARKS (SupremeAI Services)
// ════════════════════════════════════════════════════════════════════

const DEFAULT_BOOKMARKS: Bookmark[] = [
  { id: 'b1', url: 'https://supremeai-admin.onrender.com', title: 'Admin Backend', category: 'service', icon: <Server size={12} /> },
  { id: 'b2', url: 'https://supremeai-backend-docker.onrender.com', title: 'Main Backend', category: 'service', icon: <Database size={12} /> },
  { id: 'b3', url: 'https://supremeai-scraper-6nwi.onrender.com', title: 'Scraper Service', category: 'service', icon: <Activity size={12} /> },
  { id: 'b4', url: 'https://dash.cloudflare.com', title: 'Cloudflare Dashboard', category: 'tool', icon: <Cloud size={12} /> },
  { id: 'b5', url: 'https://dashboard.render.com', title: 'Render Dashboard', category: 'tool', icon: <Monitor size={12} /> },
  { id: 'b6', url: 'https://github.com/SaifulHaqueNiloy/supremeai', title: 'GitHub Repository', category: 'tool', icon: <GitBranch size={12} /> },
  { id: 'b7', url: 'https://supabase.com/dashboard', title: 'Supabase Console', category: 'service', icon: <Database size={12} /> },
  { id: 'b8', url: 'https://console.upstash.com', title: 'Upstash Redis', category: 'service', icon: <Database size={12} /> },
  { id: 'b9', url: 'https://docs.supremeai.dev', title: 'Documentation', category: 'doc', icon: <FileText size={12} /> },
  { id: 'b10', url: 'https://status.supremeai.dev', title: 'Status Page', category: 'service', icon: <Activity size={12} /> },
];

// ════════════════════════════════════════════════════════════════════
// MAIN COMPONENT
// ════════════════════════════════════════════════════════════════════

interface CrownJewelBrowserProps {
  initialUrl?: string;
  showAIAssistant?: boolean;
  showDevTools?: boolean;
  height?: string | 'full';
  onUrlChange?: (url: string) => void;
  onPageDetect?: (data: { title: string; url: string; type: string }) => void;
  serviceHealthStatus?: Record<string, 'healthy' | 'degraded' | 'down'>;
  enableMemorySave?: boolean;
  userId?: string;
}

export const CrownJewelBrowser: React.FC<CrownJewelBrowserProps> = ({
  initialUrl = 'https://supremeai-a.web.app',
  showAIAssistant = true,
  showDevTools = false,
  height = 'full',
  onUrlChange,
  onPageDetect,
  serviceHealthStatus = {},
  enableMemorySave = true,
  userId,
}) => {
  // ── Core State ──
  const [tabs, setTabs] = useState<BrowserTab[]>([
    {
      id: 'tab-1',
      url: initialUrl,
      title: 'New Tab',
      isLoading: false,
      lastVisited: Date.now(),
      bookmarked: false,
    }
  ]);
  const [activeTabId, setActiveTabId] = useState<string>('tab-1');
  const [bookmarks, setBookmarks] = useState<Bookmark[]>(DEFAULT_BOOKMARKS);
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  
  // ── UI State ──
  const [showBookmarks, setShowBookmarks] = useState(false);
  const [showDevToolsPanel, setShowDevToolsPanel] = useState(showDevTools);
  const [showAIPanel, setShowAIPanel] = useState(showAIAssistant);
  const [zoomLevel, setZoomLevel] = useState(100);
  const [deviceMode, setDeviceMode] = useState<'desktop' | 'tablet' | 'mobile'>('desktop');
  const [isLoading, setIsLoading] = useState(false);
  const [consoleMessages, setConsoleMessages] = useState<ConsoleMessage[]>([]);
  const [aiInput, setAiInput] = useState('');
  const [aiResponse, setAiResponse] = useState('');
  const [isAIProcessing, setIsAIProcessing] = useState(false);
  const [urlInputValue, setUrlInputValue] = useState(initialUrl);
  const [showHistory, setShowHistory] = useState(false);
  const [screenshotMode, setScreenshotMode] = useState(false);
  const [securityScanResult, setSecurityScanResult] = useState<{
  score: number;
  issues: string[];
  } | null>(null);

  const iframeRef = useRef<HTMLIFrameElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── Computed Values ──
  const activeTab = useMemo(() => tabs.find(t => t.id === activeTabId) || tabs[0], [tabs, activeTabId]);
  const canGoBack = historyIndex > 0;
  const canGoForward = historyIndex < history.length - 1;
  
  const deviceWidths = {
    desktop: '100%',
    tablet: '768px',
    mobile: '375px',
  };

  const addAlert = useUnifiedStore(s => s.addAlert);
  const addBrowseSession = useUnifiedStore(s => s.addBrowseSession);
  const setLastSecurityScan = useUnifiedStore(s => s.setLastSecurityScan);

  // ════════════════════════════════════════════════════════════════════
  // NAVIGATION FUNCTIONS
  // ════════════════════════════════════════════════════════════════════

  const navigateTo = useCallback((url: string, tabId?: string) => {
    const targetTabId = tabId || activeTabId;
    const normalizedUrl = normalizeUrl(url);
    
    try {
      const domain = new URL(normalizedUrl).hostname;
      const isServiceDown = Object.entries(serviceHealthStatus).some(
        ([service, status]) => domain.includes(service) && status === 'down'
      );
      
      if (isServiceDown) {
        addAlert({
          severity: 'warning',
          source: 'CrownJewelBrowser',
          message: `⚠️ Navigating to potentially down service: ${domain}`
        });
      }
    } catch { /* Invalid URL, continue anyway */ }
    
    setIsLoading(true);
    setUrlInputValue(normalizedUrl);
    
    // Update tab
    setTabs(prev => prev.map(tab => 
      tab.id === targetTabId 
        ? { ...tab, url: normalizedUrl, isLoading: true, lastVisited: Date.now() }
        : tab
    ));

    // Add to history
    const newEntry: HistoryEntry = {
      url: normalizedUrl,
      title: '',
      timestamp: Date.now(),
      tabId: targetTabId,
    };
    
    setHistory(prev => {
      const newHistory = prev.slice(0, historyIndex + 1);
      newHistory.push(newEntry);
      return newHistory;
    });
    setHistoryIndex(prev => prev + 1);

    onUrlChange?.(normalizedUrl);
    
    componentEventBus.emitBrowserUrlChange(normalizedUrl);
    
    if (enableMemorySave && userId) {
      addBrowseSession({
        url: normalizedUrl,
        title: `Browsing: ${normalizedUrl.substring(0, 50)}`,
        timestamp: Date.now(),
        tabId: targetTabId
      });
    
      fetch('/api/browser/browse-session', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: normalizedUrl, userId, timestamp: Date.now(), tabId: targetTabId })
      }).catch(() => {});
    }
  }, [activeTabId, historyIndex, onUrlChange, serviceHealthStatus, enableMemorySave, userId, addAlert, addBrowseSession]);

  const goBack = useCallback(() => {
    if (canGoBack && history[historyIndex - 1]) {
      const entry = history[historyIndex - 1];
      setUrlInputValue(entry.url);
      setHistoryIndex(prev => prev - 1);
      updateTabUrl(entry.url);
    }
  }, [canGoBack, history, historyIndex]);

  const goForward = useCallback(() => {
    if (canGoForward && history[historyIndex + 1]) {
      const entry = history[historyIndex + 1];
      setUrlInputValue(entry.url);
      setHistoryIndex(prev => prev + 1);
      updateTabUrl(entry.url);
    }
  }, [canGoForward, history, historyIndex]);

  const refresh = useCallback(() => {
    setIsLoading(true);
    if (iframeRef.current) {
      iframeRef.current.src = iframeRef.current.src;
    }
    addConsoleMessage('info', 'Page refreshed');
  }, []);

  // ════════════════════════════════════════════════════════════════════
  // TAB MANAGEMENT
  // ════════════════════════════════════════════════════════════════════

  const createNewTab = useCallback(() => {
    const newTab: BrowserTab = {
      id: `tab-${Date.now()}`,
      url: 'about:blank',
      title: 'New Tab',
      isLoading: false,
      lastVisited: Date.now(),
      bookmarked: false,
    };
    setTabs(prev => [...prev, newTab]);
    setActiveTabId(newTab.id);
  }, []);

  const closeTab = useCallback((tabId: string) => {
    if (tabs.length <= 1) return; // Don't close last tab
    
    setTabs(prev => {
      const newTabs = prev.filter(t => t.id !== tabId);
      if (activeTabId === tabId) {
        const closedIndex = prev.findIndex(t => t.id === tabId);
        const nextActive = newTabs[Math.min(closedIndex, newTabs.length - 1)];
        setActiveTabId(nextActive?.id || newTabs[0].id);
      }
      return newTabs;
    });
  }, [tabs, activeTabId]);

  // ════════════════════════════════════════════════════════════════════
  // BOOKMARK MANAGEMENT
  // ════════════════════════════════════════════════════════════════════

  const toggleBookmark = useCallback((tabId?: string) => {
    const targetId = tabId || activeTabId;
    const tab = tabs.find(t => t.id === targetId);
    if (!tab) return;

    if (tab.bookmarked) {
      setBookmarks(prev => prev.filter(b => b.url !== tab.url));
    } else {
      const newBookmark: Bookmark = {
        id: `bm-${Date.now()}`,
        url: tab.url,
        title: tab.title || tab.url,
        category: 'frequent',
      };
      setBookmarks(prev => [...prev, newBookmark]);
    }

    setTabs(prev => prev.map(t => 
      t.id === targetId ? { ...t, bookmarked: !t.bookmarked } : t
    ));
  }, [activeTabId, tabs]);

  // ════════════════════════════════════════════════════════════════════
  // AI ASSISTANT FUNCTIONS
  // ════════════════════════════════════════════════════════════════════

  const handleAIAction = async (action: AIBrowserAction) => {
    setIsAIProcessing(true);
    setAiResponse('');

    try {
      addConsoleMessage('info', `🤖 Running AI action: ${action.type}...`);
      
      const response = await fetch('/api/browser/ai-action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: action.type,
          url: activeTab?.url,
          payload: action.payload,
          context: await getPageContent() // Try to get page content
        })
      });

      if (!response.ok) {
        throw new Error(`AI service error: ${response.status}`);
      }
      
      const data = await response.json();

      switch (action.type) {
        case 'summarize':
          setAiResponse(data.response || data.summary || 'Summary generated');
          break;
        case 'explain':
          setAiResponse(data.response || data.analysis || 'Analysis complete');
          break;
        case 'extract_links':
          setAiResponse(formatLinksFromData(data.links));
          break;
        case 'find_issues':
          setAiResponse(formatIssuesFromData(data.issues));
          
          // ✅ Auto-create alert for critical issues
          if (data.criticalIssues?.length > 0) {
            data.criticalIssues.forEach((issue: string) => {
              addAlert({ severity: 'error', source: 'Browser-AI', message: issue });
            });
          }
          break;
        case 'interact':
          setAiResponse(data.response || 'AI response received');
          break;
      }
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Unknown error';
      setAiResponse(`❌ AI processing failed: ${errorMsg}\n\nThe AI service may be temporarily unavailable. Please check:\n- Service health monitor\n- Network connection\n- Backend logs`);
      
      addAlert({
        severity: 'warning',
        source: 'CrownJewelBrowser-AI',
        message: `AI action '${action.type}' failed: ${errorMsg}`
      });
    } finally {
      setIsAIProcessing(false);
    }
  };

  // ════════════════════════════════════════════════════════════════════
  // DEVTOOLS FUNCTIONS
  // ════════════════════════════════════════════════════════════════════

  const addConsoleMessage = useCallback((
    type: ConsoleMessage['type'], 
    content: string,
    source?: string
  ) => {
    setConsoleMessages(prev => [
      ...prev.slice(-99), // Keep last 100 messages
      { type, content, timestamp: Date.now(), source }
    ]);
  }, []);

  const clearConsole = () => setConsoleMessages([]);

  const runSecurityScan = async () => {
    setIsAIProcessing(true);
    
    try {
      // ✅ REAL SECURITY SCAN VIA BACKEND
      addConsoleMessage('info', '🔒 Initiating security scan...');
      
      const response = await fetch('/api/browser/security-scan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: activeTab?.url })
      });
      
      if (!response.ok) {
        throw new Error(`Security scan service error: ${response.status}`);
      }
      
      const result = await response.json();
      
      const scanResult = {
        score: result.score || 0,
        issues: result.issues || []
      };
      
      setSecurityScanResult(scanResult);
      
      // ✅ UPDATE UNIFIED STORE
      setLastSecurityScan({
        url: activeTab?.url || '',
        score: scanResult.score,
        issues: scanResult.issues,
        timestamp: Date.now()
      });
      
      addConsoleMessage('info', `✅ Security scan complete. Score: ${scanResult.score}/100`);
      
      // ✅ TRIGGER SECURITY DASHBOARD UPDATE via event bus
      componentEventBus.emit('security:scan-complete', result);
      
      // ✅ AUTO-ALERT FOR LOW SCORES
      if (scanResult.score < 70) {
        addAlert({
          severity: scanResult.score < 50 ? 'critical' : 'error',
          source: 'CrownJewelBrowser-Security',
          message: `Low security score (${scanResult.score}/100) for ${activeTab?.url}`
        });
      }
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Unknown error';
      setAiResponse(`❌ Security scan failed: ${errorMsg}`);
      addConsoleMessage('error', `Security scan error: ${errorMsg}`);
      
      addAlert({
        severity: 'warning',
        source: 'CrownJewelBrowser-Security',
        message: `Security scan failed: ${errorMsg}`
      });
    } finally {
      setIsAIProcessing(false);
    }
  };

  // ════════════════════════════════════════════════════════════════════
  // SCREENSHOT & CAPTURE
  // ════════════════════════════════════════════════════════════════════

  const takeScreenshot = async () => {
    if (!iframeRef.current) return;
    
    try {
      // ✅ REAL SCREENSHOT CAPTURE VIA BACKEND
      addConsoleMessage('log', '📸 Capturing screenshot...');
      setIsLoading(true);
      
      const response = await fetch('/api/browser/screenshot', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          url: activeTab?.url,
          width: deviceMode === 'mobile' ? 375 : deviceMode === 'tablet' ? 768 : 1280,
          height: 800,
          fullPage: false
        })
      });
      
      if (!response.ok) {
        throw new Error(`Screenshot service error: ${response.status}`);
      }
      
      // Get screenshot as blob and open in new tab
      const blob = await response.blob();
      const screenshotUrl = URL.createObjectURL(blob);
      window.open(screenshotUrl, '_blank');
      
      addConsoleMessage('log', '✅ Screenshot captured successfully');
      
      // ✅ OPTIONAL: Save to gallery (non-blocking)
      if (userId) {
        fetch('/api/browser/screenshots', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userId, url: activeTab?.url, timestamp: Date.now() })
        }).catch(() => {});
      }
    } catch (err) {
      addConsoleMessage('error', `Screenshot failed: ${err}`);
      addAlert({
        severity: 'warning',
        source: 'CrownJewelBrowser-Screenshot',
        message: `Screenshot capture failed: ${err instanceof Error ? err.message : err}`
      });
    } finally {
      setIsLoading(false);
    }
  };

  // ════════════════════════════════════════════════════════════════════
  // UTILITY FUNCTIONS
  // ════════════════════════════════════════════════════════════════════

  const normalizeUrl = (url: string): string => {
    if (!url) return 'about:blank';
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('about:')) {
      return url;
    }
    // Default to https
    return `https://${url}`;
  };

  const updateTabUrl = (url: string) => {
    setTabs(prev => prev.map(tab =>
      tab.id === activeTabId ? { ...tab, url } : tab
    ));
  };

  const handleIframeLoad = () => {
    setIsLoading(false);
    setTabs(prev => prev.map(tab =>
      tab.id === activeTabId ? { ...tab, isLoading: false } : tab
    ));
    addConsoleMessage('log', `Page loaded: ${activeTab?.url}`);
    
    eventBus.emit(Events.BROWSER_PAGE_LOADED, {
      url: activeTab?.url,
      timestamp: Date.now(),
      source: 'crown_jewel_browser',
    });

    // Try to detect page info
    try {
      const iframe = iframeRef.current;
      if (iframe?.contentDocument?.title) {
        const title = iframe.contentDocument.title;
        setTabs(prev => prev.map(tab =>
          tab.id === activeTabId ? { ...tab, title: title || 'Loading...' } : tab
        ));
        onPageDetect?.({ title, url: activeTab?.url || '', type: 'page' });
      }
    } catch (e) {
      // Cross-origin restriction - expected
    }
  };

  const handleIframeError = () => {
    setIsLoading(false);
    setTabs(prev => prev.map(tab =>
      tab.id === activeTabId 
        ? { ...tab, isLoading: false, error: 'Failed to load' } 
        : tab
    ));
    addConsoleMessage('error', `Failed to load: ${activeTab?.url}`);
  };

  const copyUrl = () => {
    navigator.clipboard.writeText(activeTab?.url || '');
    addConsoleMessage('log', 'URL copied to clipboard');
  };

  // ════════════════════════════════════════════════════════════════════
  // RENDER
  // ════════════════════════════════════════════════════════════════════

  return (
    <div className={`flex flex-col bg-[#0a0e1a] border border-cyan-500/20 rounded-xl overflow-hidden shadow-2xl shadow-cyan-500/5 ${
      height === 'full' ? 'h-full' : height
    }`}>
      
      {/* ═══ BROWSER TOOLBAR ═══ */}
      <div className="bg-[#0d1117] border-b border-cyan-500/20 px-3 py-2">
        
        {/* Row 1: Navigation & Tabs */}
        <div className="flex items-center gap-2 mb-2">
          
          {/* Navigation Controls */}
          <div className="flex items-center gap-1 bg-[#161b22] rounded-lg p-1">
            <button
              onClick={goBack}
              disabled={!canGoBack}
              className="p-1.5 hover:bg-cyan-500/10 disabled:opacity-30 disabled:cursor-not-allowed rounded transition-colors"
              title="Back"
            >
              <ArrowLeft size={14} className="text-slate-300" />
            </button>
            <button
              onClick={goForward}
              disabled={!canGoForward}
              className="p-1.5 hover:bg-cyan-500/10 disabled:opacity-30 disabled:cursor-not-allowed rounded transition-colors"
              title="Forward"
            >
              <ArrowRight size={14} className="text-slate-300" />
            </button>
            <button
              onClick={refresh}
              className={`p-1.5 hover:bg-cyan-500/10 rounded transition-colors ${isLoading ? 'animate-spin' : ''}`}
              title="Refresh"
            >
              <RotateCw size={14} className="text-slate-300" />
            </button>
            <button
              onClick={() => setShowHistory(!showHistory)}
              className="p-1.5 hover:bg-cyan-500/10 rounded transition-colors"
              title="History"
            >
              <Clock size={14} className="text-slate-300" />
            </button>
          </div>

          {/* URL Bar */}
          <div className="flex-1 flex items-center bg-[#161b22] rounded-lg px-3 py-1.5 group focus-within:ring-2 focus-within:ring-cyan-500/30">
            <Lock size={12} className="text-green-400 mr-2 flex-shrink-0" />
            <input
              type="text"
              value={urlInputValue}
              onChange={(e) => setUrlInputValue(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') navigateTo(urlInputValue);
                if (e.key === 'Escape') setUrlInputValue(activeTab?.url || '');
              }}
              placeholder="Enter URL or search..."
              className="flex-1 bg-transparent text-sm text-slate-200 outline-none placeholder:text-slate-500 font-mono"
            />
            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button onClick={copyUrl} className="p-1 hover:bg-slate-700 rounded" title="Copy URL">
                <Copy size={12} className="text-slate-400" />
              </button>
              <button onClick={() => navigateTo(urlInputValue)} className="p-1 hover:bg-slate-700 rounded" title="Go">
                <ExternalLink size={12} className="text-slate-400" />
              </button>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-1">
            {/* Bookmarks Toggle */}
            <button
              onClick={() => setShowBookmarks(!showBookmarks)}
              className={`p-1.5 rounded transition-colors ${showBookmarks ? 'bg-yellow-500/20 text-yellow-400' : 'hover:bg-slate-700 text-slate-400'}`}
              title="Bookmarks"
            >
              <Star size={14} fill={showBookmarks ? 'currentColor' : 'none'} />
            </button>

            {/* Bookmark Current Tab */}
            <button
              onClick={() => toggleBookmark()}
              className={`p-1.5 rounded transition-colors ${activeTab?.bookmarked ? 'bg-yellow-500/20 text-yellow-400' : 'hover:bg-slate-700 text-slate-400'}`}
              title="Bookmark this page"
            >
              <Star size={14} fill={activeTab?.bookmarked ? 'currentColor' : 'none'} />
            </button>

            {/* Screenshot */}
            <button
              onClick={takeScreenshot}
              className="p-1.5 hover:bg-slate-700 text-slate-400 rounded transition-colors"
              title="Take Screenshot"
            >
              <Camera size={14} />
            </button>

            {/* Security Scan */}
            <button
              onClick={runSecurityScan}
              className="p-1.5 hover:bg-slate-700 text-slate-400 rounded transition-colors"
              title="Run Security Scan"
            >
              <Shield size={14} />
            </button>

            {/* Dev Tools Toggle */}
            <button
              onClick={() => setShowDevToolsPanel(!showDevToolsPanel)}
              className={`p-1.5 rounded transition-colors ${showDevToolsPanel ? 'bg-purple-500/20 text-purple-400' : 'hover:bg-slate-700 text-slate-400'}`}
              title="Developer Tools"
            >
              <Code size={14} />
            </button>

            {/* AI Assistant Toggle */}
            <button
              onClick={() => setShowAIPanel(!showAIPanel)}
              className={`p-1.5 rounded-lg transition-all ${showAIPanel ? 'bg-gradient-to-r from-cyan-500/20 to-purple-500/20 text-cyan-400 shadow-lg shadow-cyan-500/10' : 'hover:bg-slate-700 text-slate-400'}`}
              title="AI Assistant"
            >
              <Bot size={14} className={showAIPanel ? 'animate-pulse' : ''} />
            </button>
          </div>
        </div>

        {/* Row 2: Tab Bar */}
        <div className="flex items-center gap-1 overflow-x-auto scrollbar-hide">
          {tabs.map((tab) => (
            <div
              key={tab.id}
              onClick={() => setActiveTabId(tab.id)}
              className={`group flex items-center gap-2 px-3 py-1.5 rounded-t-lg cursor-pointer max-w-[180px] transition-all ${
                tab.id === activeTabId
                  ? 'bg-[#1c2128] text-white border-t-2 border-t-cyan-400'
                  : 'bg-[#161b22] text-slate-400 hover:bg-[#1c2128]/50 hover:text-slate-200'
              }`}
            >
              {tab.isLoading ? (
                <Loader2 size={10} className="animate-spin text-cyan-400 flex-shrink-0" />
              ) : (
                <Globe size={10} className="flex-shrink-0" />
              )}
              <span className="truncate text-xs font-medium">{tab.title}</span>
              <button
                onClick={(e) => { e.stopPropagation(); closeTab(tab.id); }}
                className="opacity-0 group-hover:opacity-100 p-0.5 hover:bg-red-500/20 rounded transition-all"
              >
                <X size={10} />
              </button>
            </div>
          ))}
          <button
            onClick={createNewTab}
            className="p-1.5 hover:bg-slate-700 text-slate-400 rounded transition-colors"
            title="New Tab"
          >
            <Plus size={14} />
          </button>
        </div>
      </div>

      {/* ═══ MAIN CONTENT AREA ═══ */}
      <div className="flex-1 flex overflow-hidden">
        
        {/* Bookmarks Panel (Dropdown) */}
        <AnimatePresence>
          {showBookmarks && (
            <motion.div
              initial={{ width: 0, opacity: 0 }}
              animate={{ width: 220, opacity: 1 }}
              exit={{ width: 0, opacity: 0 }}
              className="bg-[#0d1117] border-r border-cyan-500/20 overflow-y-auto"
            >
              <div className="p-3">
                <h4 className="text-xs font-bold text-cyan-400 uppercase tracking-wider mb-3">Bookmarks</h4>
                
                {['service', 'tool', 'doc'].map(category => (
                  <div key={category} className="mb-4">
                    <h5 className="text-[10px] text-slate-500 uppercase mb-2">{category}s</h5>
                    <div className="space-y-1">
                      {bookmarks
                        .filter(b => b.category === category)
                        .map(bookmark => (
                          <button
                            key={bookmark.id}
                            onClick={() => { navigateTo(bookmark.url); setShowBookmarks(false); }}
                            className="w-full flex items-center gap-2 px-2 py-1.5 rounded hover:bg-slate-800 text-left transition-colors"
                          >
                            {bookmark.icon || <Globe size={12} />}
                            <span className="text-xs text-slate-300 truncate">{bookmark.title}</span>
                          </button>
                        ))
                      }
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Browser Viewport */}
        <div className="flex-1 flex flex-col bg-[#0d1117] relative">
          
          {/* Loading Indicator */}
          <AnimatePresence>
            {isLoading && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute top-2 left-1/2 transform -translate-x-1/2 z-10 bg-[#161b22] px-3 py-1 rounded-full flex items-center gap-2 shadow-lg"
              >
                <Loader2 size={12} className="animate-spin text-cyan-400" />
                <span className="text-xs text-slate-300">Loading...</span>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Device Mode Frame */}
          <div className={`flex-1 flex ${deviceMode !== 'desktop' ? 'justify-center bg-[#1c2128] p-4' : ''}`}>
            <div
              style={{ width: deviceWidths[deviceMode], maxWidth: '100%' }}
              className={`${deviceMode !== 'desktop' ? 'bg-white rounded-lg shadow-2xl overflow-hidden h-full' : 'h-full'} relative`}
            >
              {/* Mobile Device Frame (Tablet/Mobile mode) */}
              {deviceMode !== 'desktop' && (
                <div className="absolute top-0 left-0 right-0 h-6 bg-black flex items-center justify-center">
                  <div className="w-16 h-4 bg-gray-800 rounded-full" />
                </div>
              )}

              <iframe
                ref={iframeRef}
                src={activeTab?.url}
                className="w-full h-full border-0 bg-white"
                sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals allow-presentation"
                onLoad={handleIframeLoad}
                onError={handleIframeError}
                title="SupremeAI Browser"
                style={{
                  transform: `scale(${zoomLevel / 100})`,
                  transformOrigin: 'top left',
                }}
              />
            </div>
          </div>

          {/* Error Overlay */}
          {activeTab?.error && (
            <div className="absolute inset-0 flex items-center justify-center bg-[#0d1117]/90 backdrop-blur-sm">
              <div className="text-center p-6">
                <AlertTriangle size={48} className="mx-auto text-red-400 mb-4" />
                <h3 className="text-lg font-bold text-white mb-2">Failed to Load Page</h3>
                <p className="text-sm text-slate-400 mb-4">{activeTab.error}</p>
                <button
                  onClick={refresh}
                  className="px-4 py-2 bg-cyan-500 text-black font-bold rounded-lg hover:bg-cyan-400 transition-colors"
                >
                  Try Again
                </button>
              </div>
            </div>
          )}

          {/* Security Scan Result Overlay */}
          <AnimatePresence>
            {securityScanResult && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className="absolute top-4 right-4 bg-[#161b22] border border-green-500/30 rounded-lg p-4 max-w-xs shadow-xl"
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-green-400">Security Score</span>
                  <button onClick={() => setSecurityScanResult(null)}>
                    <X size={12} className="text-slate-400" />
                  </button>
                </div>
                <div className="text-2xl font-bold text-white mb-2">{securityScanResult.score}/100</div>
                <div className="space-y-1">
                  {securityScanResult.issues.map((issue, i) => (
                    <div key={i} className="text-[10px] text-slate-300">{issue}</div>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* AI Assistant Panel */}
        <AnimatePresence>
          {showAIPanel && (
            <motion.div
              initial={{ width: 0, opacity: 0 }}
              animate={{ width: 320, opacity: 1 }}
              exit={{ width: 0, opacity: 0 }}
              className="bg-gradient-to-b from-[#0d1117] to-[#161b22] border-l border-purple-500/30 flex flex-col overflow-hidden"
            >
              {/* AI Header */}
              <div className="p-3 border-b border-purple-500/20">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Bot size={16} className="text-purple-400 animate-pulse" />
                    <span className="text-sm font-bold text-purple-300">AI Assistant</span>
                  </div>
                  <button onClick={() => setShowAIPanel(false)} className="p-1 hover:bg-slate-800 rounded">
                    <X size={12} className="text-slate-400" />
                  </button>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="p-3 border-b border-slate-800">
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { action: 'summarize' as const, label: 'Summarize', icon: <FileText size={12} /> },
                    { action: 'explain' as const, label: 'Explain', icon: <Code size={12} /> },
                    { action: 'extract_links' as const, label: 'Links', icon: <Globe size={12} /> },
                    { action: 'find_issues' as const, label: 'Issues', icon: <AlertTriangle size={12} /> },
                  ].map(({ action, label, icon }) => (
                    <button
                      key={action}
                      onClick={() => handleAIAction({ type: action })}
                      disabled={isAIProcessing}
                      className="flex items-center gap-2 px-2 py-1.5 bg-slate-800 hover:bg-slate-700 rounded text-xs text-slate-300 transition-colors disabled:opacity-50"
                    >
                      {icon}
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Chat Interface */}
              <div className="flex-1 flex flex-col p-3 overflow-hidden">
                <div className="flex-1 overflow-y-auto space-y-3 mb-3">
                  {aiResponse && (
                    <div className="bg-purple-900/20 rounded-lg p-3 text-xs text-slate-200 leading-relaxed">
                      {aiResponse.split('\n').map((line, i) => (
                        <p key={i} className="mb-1">{line}</p>
                      ))}
                    </div>
                  )}
                  {!aiResponse && !isAIProcessing && (
                    <div className="text-center text-slate-500 text-xs py-8">
                      <Bot size={24} className="mx-auto mb-2 opacity-50" />
                      <p>Select an action or ask me anything about this page.</p>
                    </div>
                  )}
                  {isAIProcessing && (
                    <div className="flex items-center gap-2 text-xs text-purple-400">
                      <Loader2 size={12} className="animate-spin" />
                      Analyzing page...
                    </div>
                  )}
                </div>

                {/* Input */}
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={aiInput}
                    onChange={(e) => setAiInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && aiInput.trim()) {
                        handleAIAction({ type: 'interact', payload: { question: aiInput } });
                        setAiInput('');
                      }
                    }}
                    placeholder="Ask about this page..."
                    className="flex-1 bg-slate-800 rounded-lg px-3 py-2 text-xs text-white outline-none placeholder:text-slate-500"
                  />
                  <button
                    onClick={() => {
                      if (aiInput.trim()) {
                        handleAIAction({ type: 'interact', payload: { question: aiInput } });
                        setAiInput('');
                      }
                    }}
                    disabled={!aiInput.trim() || isAIProcessing}
                    className="p-2 bg-purple-600 hover:bg-purple-500 rounded-lg disabled:opacity-50 transition-colors"
                  >
                    <Send size={12} />
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* DevTools Panel (Bottom) */}
        <AnimatePresence>
          {showDevToolsPanel && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 200, opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="bg-[#1c2128] border-t border-slate-700 flex flex-col overflow-hidden"
            >
              {/* DevTools Header */}
              <div className="flex items-center justify-between px-3 py-1 bg-[#161b22] border-b border-slate-700">
                <div className="flex items-center gap-3">
                  {['Console', 'Network', 'Elements'].map(tab => (
                    <button key={tab} className="text-[10px] text-slate-400 hover:text-white px-2 py-0.5">
                      {tab}
                    </button>
                  ))}
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={clearConsole} className="text-[10px] text-slate-400 hover:text-white">
                    Clear
                  </button>
                  <button onClick={() => setShowDevToolsPanel(false)}>
                    <X size={10} className="text-slate-400" />
                  </button>
                </div>
              </div>

              {/* Console Output */}
              <div className="flex-1 overflow-y-auto p-2 font-mono text-[11px] space-y-0.5">
                {consoleMessages.length === 0 ? (
                  <div className="text-slate-500 text-center py-4">Console is empty</div>
                ) : (
                  consoleMessages.map((msg, i) => (
                    <div
                      key={i}
                      className={`${
                        msg.type === 'error' ? 'text-red-400' :
                        msg.type === 'warn' ? 'text-yellow-400' :
                        msg.type === 'info' ? 'text-blue-400' :
                        'text-slate-300'
                      }`}
                    >
                      <span className="text-slate-500 mr-2">[{new Date(msg.timestamp).toLocaleTimeString()}]</span>
                      {msg.content}
                    </div>
                  ))
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* ═══ STATUS BAR ═══ */}
      <div className="bg-[#0d1117] border-t border-cyan-500/20 px-3 py-1 flex items-center justify-between text-[10px] text-slate-500">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1">
            {isLoading ? (
              <Loader2 size={8} className="animate-spin text-cyan-400" />
            ) : (
              <Check size={8} className="text-green-400" />
            )}
            {activeTab?.url || 'about:blank'}
          </span>
          <span>{activeTab?.title || 'Loading...'}</span>
        </div>
        <div className="flex items-center gap-4">
          {/* Device Mode Switcher */}
          <div className="flex items-center gap-1">
            <button
              onClick={() => setDeviceMode('desktop')}
              className={`p-1 rounded ${deviceMode === 'desktop' ? 'bg-slate-700 text-white' : 'text-slate-500 hover:text-slate-300'}`}
              title="Desktop"
            >
              <Monitor size={12} />
            </button>
            <button
              onClick={() => setDeviceMode('tablet')}
              className={`p-1 rounded ${deviceMode === 'tablet' ? 'bg-slate-700 text-white' : 'text-slate-500 hover:text-slate-300'}`}
              title="Tablet"
            >
              <Tablet size={12} />
            </button>
            <button
              onClick={() => setDeviceMode('mobile')}
              className={`p-1 rounded ${deviceMode === 'mobile' ? 'bg-slate-700 text-white' : 'text-slate-500 hover:text-slate-300'}`}
              title="Mobile"
            >
              <Smartphone size={12} />
            </button>
          </div>

          {/* Zoom Controls */}
          <div className="flex items-center gap-1">
            <button onClick={() => setZoomLevel(z => Math.max(25, z - 25))} className="p-1 hover:bg-slate-800 rounded">
              <ZoomOut size={10} />
            </button>
            <span className="w-10 text-center">{zoomLevel}%</span>
            <button onClick={() => setZoomLevel(z => Math.min(200, z + 25))} className="p-1 hover:bg-slate-800 rounded">
              <ZoomIn size={10} />
            </button>
          </div>

          {/* Security Score Badge */}
          {securityScanResult && (
            <span className={`px-2 py-0.5 rounded ${
              securityScanResult.score >= 80 ? 'bg-green-900/30 text-green-400' :
              securityScanResult.score >= 60 ? 'bg-yellow-900/30 text-yellow-400' :
              'bg-red-900/30 text-red-400'
            }`}>
              Sec: {securityScanResult.score}
            </span>
          )}
        </div>
      </div>

      {/* History Panel (Overlay) */}
      <AnimatePresence>
        {showHistory && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm z-20 flex items-start justify-center pt-20"
            onClick={() => setShowHistory(false)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
              className="bg-[#161b22] border border-cyan-500/30 rounded-xl shadow-2xl w-[500px] max-h-[400px] overflow-hidden"
            >
              <div className="p-4 border-b border-slate-700 flex items-center justify-between">
                <h3 className="font-bold text-white flex items-center gap-2">
                  <Clock size={16} className="text-cyan-400" />
                  History
                </h3>
                <button onClick={() => setShowHistory(false)}>
                  <X size={14} className="text-slate-400" />
                </button>
              </div>
              <div className="overflow-y-auto max-h-[320px] p-2">
                {history.length === 0 ? (
                  <div className="text-center text-slate-500 py-8">No history yet</div>
                ) : (
                  [...history].reverse().map((entry, i) => (
                    <button
                      key={i}
                      onClick={() => { navigateTo(entry.url); setShowHistory(false); }}
                      className="w-full text-left px-3 py-2 hover:bg-slate-800 rounded flex items-center justify-between"
                    >
                      <div>
                        <div className="text-sm text-white">{entry.title || entry.url}</div>
                        <div className="text-[10px] text-slate-500 truncate max-w-[300px]">{entry.url}</div>
                      </div>
                      <div className="text-[10px] text-slate-500">
                        {new Date(entry.timestamp).toLocaleTimeString()}
                      </div>
                    </button>
                  ))
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

// ════════════════════════════════════════════════════════════════════
// ✅ NEW HELPER FUNCTIONS
// ════════════════════════════════════════════════════════════════════

/**
 * Attempts to extract text content from iframe for AI context
 * Handles cross-origin restrictions gracefully
 */
async function getPageContent(): Promise<string> {
  return ''; // Implement based on your proxy setup
}

function formatLinksFromData(links: any[]): string {
  if (!links?.length) return 'No links found.';
  return `🔗 **Links Extracted (${links.length} found)**\n\n${
    links.map((link: any, i: number) => `${i+1}. [${link.text || link.url}](${link.url})`).join('\n')
  }`;
}

function formatIssuesFromData(issues: any[]): string {
  if (!issues?.length) return '✅ No issues found!';
  
  const critical = issues.filter((i: any) => i.severity === 'critical');
  const warnings = issues.filter((i: any) => i.severity === 'warning');
  
  return `🚨 **Issues Detected (${issues.length} total)**\n\n**Critical (${critical.length}):**\n${
    critical.map((i: any) => `- ⚠️ ${i.message}`).join('\n') || 'None'
  }\n\n**Warnings (${warnings.length}):**\n${
    warnings.map((i: any) => `- ⚡ ${i.message}`).join('\n') || 'None'
  }\n\n**Suggestions:** Run security scan for detailed remediation steps.`;
}

// Icon imports that were used above
import { Server, Database, Activity, Cloud, GitBranch, FileText, GitBranch as GitBranchIcon, Send } from 'lucide-react';