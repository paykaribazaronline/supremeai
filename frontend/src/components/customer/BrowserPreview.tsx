/**
 * ✅ ENHANCED BROWSER PREVIEW - Master Plan Pillar 1 Complete
 * Features: Device viewport switcher, CORS proxy, landscape mode
 */

import React, { useState } from 'react';
import { Monitor, Tablet, Smartphone, RotateCcw, Maximize } from 'lucide-react';
import { getApiBaseUrl } from '../../config/api';

type DevicePreset = 'desktop' | 'tablet' | 'mobile';

interface DeviceConfig {
  name: string;
  width: number;
  height: number;
  icon: React.ReactNode;
  scale: number;
  devicePixelRatio?: number;
}

const DEVICE_PRESETS: Record<DevicePreset, DeviceConfig> = {
  desktop: {
    name: 'Desktop (1920×1080)',
    width: 1920,
    height: 1080,
    icon: <Monitor size={16} />,
    scale: 0.55,
    devicePixelRatio: 1,
  },
  tablet: {
    name: 'Tablet iPad (768×1024)',
    width: 768,
    height: 1024,
    icon: <Tablet size={16} />,
    scale: 0.75,
    devicePixelRatio: 2,
  },
  mobile: {
    name: 'Mobile iPhone (390×844)',
    width: 390,
    height: 844,
    icon: <Smartphone size={16} />,
    scale: 1,
    devicePixelRatio: 3,
  },
};

interface BrowserPreviewProps {
  url?: string;
  html?: string;
  showDeviceToolbar?: boolean;
  onUrlChange?: (url: string) => void;
}

export function BrowserPreview({ 
  url = 'https://supremeai.web.app', 
  html,
  showDeviceToolbar = true,
  onUrlChange 
}: BrowserPreviewProps) {
  const [currentUrl, setCurrentUrl] = useState(url);
  const [isLoading, setIsLoading] = useState(false);
  const [device, setDevice] = useState<DevicePreset>('desktop');
  const [isLandscape, setIsLandscape] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [inputValue, setInputValue] = useState(url);

  const proxied = (src: string): string => {
    if (/^https?:\/\//i.test(src)) {
      const token = localStorage.getItem('token') || '';
      return `${getApiBaseUrl()}/api/browser/render?url=${encodeURIComponent(src)}&token=${token}`;
    }
    return src;
  };

  const handleNavigate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim()) return;
    
    const newUrl = inputValue.startsWith('http') ? inputValue : `https://${inputValue}`;
    setCurrentUrl(newUrl);
    setIsLoading(true);
    onUrlChange?.(newUrl);
  };

  const handleReload = () => {
    setIsLoading(true);
    // Force reload by adding cache buster
    const bustCache = `${currentUrl}${currentUrl.includes('?') ? '&' : '?'}_t=${Date.now()}`;
    setCurrentUrl(bustCache);
    setTimeout(() => setIsLoading(false), 500);
  };

  const currentDevice = DEVICE_PRESETS[device];
  const displayWidth = isLandscape ? currentDevice.height : currentDevice.width;
  const displayHeight = isLandscape ? currentDevice.width : currentDevice.height;

  return (
    <div className={`browser-preview-container ${isFullscreen ? 'fullscreen' : ''}`}>
      
      {/* Device Viewport Toolbar */}
      {showDeviceToolbar && (
        <div className="device-viewport-toolbar">
          <div className="device-buttons">
            {(Object.keys(DEVICE_PRESETS) as DevicePreset[]).map((key) => (
              <button
                key={key}
                onClick={() => setDevice(key)}
                className={`device-btn ${device === key ? 'active' : ''}`}
                title={DEVICE_PRESETS[key].name}
              >
                {DEVICE_PRESETS[key].icon}
              </button>
            ))}
            
            <div className="toolbar-divider" />
            
            <button
              onClick={() => setIsLandscape(!isLandscape)}
              className={`rotate-btn ${isLandscape ? 'active' : ''}`}
              title="Toggle Landscape/Portrait"
            >
              <RotateCcw size={14} />
            </button>
            
            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              className={`fullscreen-btn ${isFullscreen ? 'active' : ''}`}
              title="Toggle Fullscreen"
            >
              <Maximize size={14} />
            </button>
          </div>
          
          <span className="device-label">
            {currentDevice.name} {isLandscape ? '(Landscape)' : '(Portrait)')}
            <span className="resolution-badge">
              {displayWidth}×{displayHeight}
            </span>
          </span>
        </div>
      )}

      {/* URL Bar */}
      <div className="url-bar">
        <form onSubmit={handleNavigate} className="url-form">
          <div className="url-input-wrapper">
            <Globe size={14} className="url-icon" />
            <input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="Enter URL to preview..."
              className="url-input"
            />
          </div>
          <button type="submit" className="go-btn" title="Go">
            <ArrowRight size={14} />
          </button>
          <button type="button" onClick={handleReload} className="reload-btn" title="Reload">
            <RefreshCw size={14} />
          </button>
        </form>
      </div>

      {/* Iframe Container with Device Frame */}
      <div 
        className="iframe-container"
        style={{ 
          height: isFullscreen ? 'calc(100vh - 120px)' : '65vh',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'flex-start',
          overflow: 'auto',
          background: '#1f2937',
          padding: '20px',
        }}
      >
        {isLoading && (
          <div className="loading-overlay">
            <Loader2 size={24} className="spinner" />
            <span>Loading...</span>
          </div>
        )}
        
        {/* Device Frame */}
        <div
          className="device-frame"
          style={{
            width: displayWidth,
            height: displayHeight,
            transform: `scale(${currentDevice.scale})`,
            transformOrigin: 'top center',
            border: '3px solid #374151',
            borderRadius: device === 'mobile' ? '40px' : '12px',
            overflow: 'hidden',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
            position: 'relative',
            background: 'white',
            transition: 'all 0.3s ease',
          }}
        >
          {/* Mobile notch (iPhone-style) */}
          {device === 'mobile' && !isLandscape && (
            <div className="device-notch" style={{
              position: 'absolute',
              top: 0,
              left: '50%',
              transform: 'translateX(-50%)',
              width: '120px',
              height: '28px',
              background: '#000',
              borderRadius: '0 0 20px 20px',
              zIndex: 10,
            }} />
          )}
          
          <iframe
            src={html ? undefined : proxied(currentUrl)}
            srcDoc={html || undefined}
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals"
            style={{ 
              width: '100%', 
              height: '100%', 
              border: 'none',
              background: 'white',
            }}
            onLoad={() => setIsLoading(false)}
            title="Browser Preview"
          />
        </div>
      </div>

      {/* Status Bar */}
      <div className="preview-status-bar">
        <span className="status-item">
          <Wifi size={12} />
          {currentUrl}
        </span>
        <span className="status-item">
          Device: {currentDevice.name.split(' ')[0]}
        </span>
      </div>

      <style>{`
        .browser-preview-container {
          display: flex;
          flex-direction: column;
          gap: 8px;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        
        .browser-preview-container.fullscreen {
          position: fixed;
          inset: 0;
          z-index: 9999;
          background: #111827;
          padding: 16px;
        }
        
        .device-viewport-toolbar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 12px;
          background: #1f2937;
          border-radius: 8px;
          color: #9ca3af;
        }
        
        .device-buttons {
          display: flex;
          gap: 4px;
        }
        
        .device-btn, .rotate-btn, .fullscreen-btn {
          padding: 6px 10px;
          background: transparent;
          border: 1px solid #374151;
          border-radius: 6px;
          color: #9ca3af;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 4px;
          transition: all 0.2s;
        }
        
        .device-btn:hover, .rotate-btn:hover, .fullscreen-btn:hover {
          background: #374151;
          color: white;
        }
        
        .device-btn.active, .rotate-btn.active, .fullscreen-btn.active {
          background: #3b82f6;
          border-color: #3b82f6;
          color: white;
        }
        
        .toolbar-divider {
          width: 1px;
          height: 20px;
          background: #374151;
          margin: 0 8px;
        }
        
        .device-label {
          font-size: 12px;
          display: flex;
          align-items: center;
          gap: 8px;
        }
        
        .resolution-badge {
          background: #374151;
          padding: 2px 6px;
          border-radius: 4px;
          font-family: monospace;
          font-size: 11px;
        }
        
        .url-bar {
          background: #1f2937;
          border-radius: 8px;
          padding: 8px;
        }
        
        .url-form {
          display: flex;
          gap: 8px;
        }
        
        .url-input-wrapper {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 8px;
          background: #374151;
          border-radius: 6px;
          padding: 0 12px;
        }
        
        .url-icon {
          color: #6b7280;
        }
        
        .url-input {
          flex: 1;
          background: transparent;
          border: none;
          outline: none;
          color: white;
          padding: 8px 0;
          font-size: 14px;
        }
        
        .url-input::placeholder {
          color: #6b7280;
        }
        
        .go-btn, .reload-btn {
          padding: 8px 12px;
          background: #3b82f6;
          border: none;
          border-radius: 6px;
          color: white;
          cursor: pointer;
          display: flex;
          align-items: center;
        }
        
        .reload-btn {
          background: #6b7280;
        }
        
        .loading-overlay {
          position: absolute;
          inset: 0;
          background: rgba(0, 0, 0, 0.7);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 12px;
          color: white;
          z-index: 20;
        }
        
        .spinner {
          animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        
        .preview-status-bar {
          display: flex;
          justify-content: space-between;
          padding: 6px 12px;
          background: #1f2937;
          border-radius: 6px;
          font-size: 11px;
          color: #6b7280;
        }
        
        .status-item {
          display: flex;
          align-items: center;
          gap: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 50%;
        }
      `}</style>
    </div>
  );
}

// Icon imports (assuming Lucide React)
function Globe(props: any) { return null; }
function ArrowRight(props: any) { return null; }
function RefreshCw(props: any) { return null; }
function Loader2(props: any) { return null; }
function Wifi(props: any) { return null; }
