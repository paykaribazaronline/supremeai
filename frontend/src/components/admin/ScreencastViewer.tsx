/**
 * ✅ LIVE SCREENCAST VIEWER - Master Plan Pillar 6 Complete
 * Displays real-time browser stream with mouse/keyboard control
 */

import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Maximize, Minimize, MousePointer, Keyboard, Hand, Video, VideoOff } from 'lucide-react';
import { getWsBaseUrl } from '../../config/api';

interface ScreencastViewerProps {
  sessionId: string;
  takeoverToken: string;
  onTakeoverComplete?: () => void;
  onReturnControl?: () => void;
  className?: string;
}

interface ScreencastStats {
  fps: number;
  frameCount: number;
  isConnected: boolean;
  latency: number;
}

export function ScreencastViewer({ 
  sessionId, 
  takeoverToken, 
  onTakeoverComplete,
  onReturnControl,
  className = ''
}: ScreencastViewerProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isControlling, setIsControlling] = useState(false);
  const [stats, setStats] = useState<ScreencastStats>({
    fps: 0,
    frameCount: 0,
    isConnected: false,
    latency: 0,
  });
  const lastFrameTime = useRef<number>(Date.now());
  const fpsFrames = useRef<number[]>([]);
  const imageRef = useRef<HTMLImageElement | null>(null);

  // Connect to screencast WebSocket
  useEffect(() => {
    const wsUrl = `${getWsBaseUrl()}/ws/session/${sessionId}/takeover?token=${takeoverToken}`;
    
    wsRef.current = new WebSocket(wsUrl);
    
    wsRef.current.onopen = () => {
      setIsConnected(true);
      setStats(prev => ({ ...prev, isConnected: true }));
      console.log('[Screencast] Connected to session:', sessionId);
    };
    
    wsRef.current.onmessage = async (event) => {
      try {
        const message = JSON.parse(event.data);
        
        if (message.channel === 'screencast' && message.type === 'frame') {
          // Calculate latency
          const receiveTime = Date.now();
          const latency = receiveTime - message.timestamp;
          
          // Decode and render JPEG frame
          if (!imageRef.current) {
            imageRef.current = new Image();
          }
          
          const img = imageRef.current;
          img.onload = () => {
            const canvas = canvasRef.current;
            if (!canvas) return;
            
            const ctx = canvas.getContext('2d');
            if (!ctx) return;
            
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);
            
            // Calculate FPS (rolling average over last 10 frames)
            const now = Date.now();
            const delta = now - lastFrameTime.current;
            if (delta > 0) {
              fpsFrames.current.push(1000 / delta);
              if (fpsFrames.current.length > 10) {
                fpsFrames.current.shift();
              }
              const avgFps = Math.round(
                fpsFrames.current.reduce((a, b) => a + b, 0) / fpsFrames.current.length
              );
              
              setStats({
                fps: avgFps,
                frameCount: message.frame_number,
                isConnected: true,
                latency,
              });
            }
            lastFrameTime.current = now;
          };
          
          img.src = `data:image/jpeg;base64,${message.data}`;
        }
        
        if (message.channel === 'screencast' && message.type === 'keepalive') {
          // Frame unchanged, just update counter
          setStats(prev => ({
            ...prev,
            frameCount: message.frame_number,
          }));
        }
        
        if (message.channel === 'screencast' && message.status === 'unavailable') {
          console.error('[Screencast] Unavailable:', message.message);
          setIsConnected(false);
          setStats(prev => ({ ...prev, isConnected: false }));
        }
        
        if (message.channel === 'input_ack') {
          // Input was received by server
          console.log('[Screencast] Input ack:', message.action);
        }
        
      } catch (e) {
        console.error('[Screencast] Failed to parse message:', e);
      }
    };
    
    wsRef.current.onclose = () => {
      setIsConnected(false);
      setStats(prev => ({ ...prev, isConnected: false }));
      console.log('[Screencast] Disconnected');
    };
    
    wsRef.current.onerror = (error) => {
      console.error('[Screencast] WebSocket error:', error);
      setIsConnected(false);
    };
    
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [sessionId, takeoverToken]);

  // Mouse event handlers for takeover control
  const handleCanvasMouseMove = useCallback((e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isControlling || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;
    
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.round(e.clientX - rect.left);
    const y = Math.round(e.clientY - rect.top);
    
    wsRef.current.send(JSON.stringify({
      action: 'mouse.move',
      data: { x, y },
    }));
  }, [isControlling]);
  
  const handleCanvasMouseDown = useCallback((e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isControlling || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;
    
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.round(e.clientX - rect.left);
    const y = Math.round(e.clientY - rect.top);
    
    wsRef.current.send(JSON.stringify({
      action: 'mouse.click',
      data: { 
        x, 
        y, 
        delay: 50,
        button: e.button === 2 ? 'right' : 'left',
      },
    }));
  }, [isControlling]);
  
  const handleCanvasMouseWheel = useCallback((e: React.WheelEvent<HTMLCanvasElement>) => {
    if (!isControlling || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;
    e.preventDefault();
    
    wsRef.current.send(JSON.stringify({
      action: 'mouse.wheel',
      data: { delta_x: e.deltaX, delta_y: e.deltaY },
    }));
  }, [isControlling]);
  
  // Keyboard handler
  useEffect(() => {
    if (!isControlling || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;
    
    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't capture if user is typing in an input
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }
      
      // ESC to return control
      if (e.key === 'Escape') {
        handleReturnControl();
        return;
      }
      
      wsRef.current?.send(JSON.stringify({
        action: 'keyboard.press',
        data: { key: e.key },
      }));
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isControlling]);
  
  // Return control to AI
  const handleReturnControl = useCallback(() => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;
    
    wsRef.current.send(JSON.stringify({
      action: 'return_control',
      data: {},
    }));
    
    setIsControlling(false);
    onReturnControl?.();
  }, [onReturnControl]);
  
  // Take control
  const handleTakeControl = useCallback(() => {
    setIsControlling(true);
    onTakeoverComplete?.();
  }, [onTakeoverComplete]);

  return (
    <div className={`screencast-viewer ${className}`}>
      {/* Control Bar */}
      <div className="screencast-controls">
        <div className="connection-status">
          <div className={`status-dot ${isConnected ? 'connected' : 'disconnected'}`} />
          <span className="status-text">{isConnected ? 'Live' : 'Disconnected'}</span>
          
          <div className="stats-group">
            <span className="stat-item" title="Frames per second">
              <Video size={12} />
              {stats.fps} FPS
            </span>
            <span className="stat-item" title="Total frames received">
              #{stats.frameCount}
            </span>
            <span className="stat-item" title="Network latency">
              {stats.latency}ms
            </span>
          </div>
        </div>
        
        <div className="control-actions">
          {!isControlling ? (
            <button
              onClick={handleTakeControl}
              className="takeover-btn"
              title="Take control (HITL)"
              disabled={!isConnected}
            >
              <Hand size={16} />
              Take Control
            </button>
          ) : (
            <button
              onClick={handleReturnControl}
              className="return-btn"
              title="Return control to AI (or press ESC)"
            >
              <MousePointer size={16} />
              Return to AI
            </button>
          )}
        </div>
      </div>
      
      {/* Canvas for rendering screencast */}
      <canvas
        ref={canvasRef}
        className={`screencast-canvas ${isControlling ? 'controlling' : 'view-only'}`}
        onMouseMove={handleCanvasMouseMove}
        onMouseDown={handleCanvasMouseDown}
        onWheel={handleCanvasMouseWheel}
        onContextMenu={(e) => e.preventDefault()} // Prevent context menu
      />
      
      {/* Instructions overlay when controlling */}
      {isControlling && (
        <div className="control-instructions">
          <Keyboard size={12} />
          <span>
            Move mouse to control • Click to interact • Type for keyboard • 
            <strong>ESC</strong> to return control
          </span>
        </div>
      )}
      
      {/* Disconnected overlay */}
      {!isConnected && (
        <div className="disconnected-overlay">
          <VideoOff size={32} />
          <span>Screencast disconnected</span>
          <span className="reconnect-hint">Reconnecting automatically...</span>
        </div>
      )}

      <style>{`
        .screencast-viewer {
          display: flex;
          flex-direction: column;
          background: #0f1118;
          border-radius: 8px;
          overflow: hidden;
          border: 1px solid #374151;
        }
        
        .screencast-controls {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 12px;
          background: #1f2937;
          border-bottom: 1px solid #374151;
        }
        
        .connection-status {
          display: flex;
          align-items: center;
          gap: 12px;
          color: #9ca3af;
          font-size: 13px;
        }
        
        .status-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #ef4444;
        }
        
        .status-dot.connected {
          background: #22c55e;
          box-shadow: 0 0 8px rgba(34, 197, 94, 0.5);
          animation: pulse 2s infinite;
        }
        
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
        
        .stats-group {
          display: flex;
          gap: 12px;
          margin-left: 16px;
        }
        
        .stat-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-family: monospace;
          font-size: 12px;
          color: #6b7280;
        }
        
        .control-actions {
          display: flex;
          gap: 8px;
        }
        
        .takeover-btn, .return-btn {
          padding: 6px 14px;
          border-radius: 6px;
          font-size: 13px;
          font-weight: 500;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 6px;
          transition: all 0.2s;
          border: none;
        }
        
        .takeover-btn {
          background: #3b82f6;
          color: white;
        }
        
        .takeover-btn:hover:not(:disabled) {
          background: #2563eb;
        }
        
        .takeover-btn:disabled {
          background: #4b5563;
          cursor: not-allowed;
          opacity: 0.5;
        }
        
        .return-btn {
          background: #f59e0b;
          color: #000;
        }
        
        .return-btn:hover {
          background: #d97706;
        }
        
        .screencast-canvas {
          width: 100%;
          flex: 1;
          min-height: 300px;
          background: #000;
          display: block;
        }
        
        .screencast-canvas.view-only {
          cursor: default;
        }
        
        .screencast-canvas.controlling {
          cursor: crosshair;
        }
        
        .control-instructions {
          position: absolute;
          bottom: 40px;
          left: 50%;
          transform: translateX(-50%);
          background: rgba(0, 0, 0, 0.8);
          color: white;
          padding: 8px 16px;
          border-radius: 20px;
          font-size: 12px;
          display: flex;
          align-items: center;
          gap: 8px;
          pointer-events: none;
          opacity: 0.9;
          backdrop-filter: blur(4px);
        }
        
        .disconnected-overlay {
          position: absolute;
          inset: 0;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 12px;
          background: rgba(0, 0, 0, 0.9);
          color: #9ca3af;
        }
        
        .reconnect-hint {
          font-size: 12px;
          color: #6b7280;
        }
      `}</style>
    </div>
  );
}

export default ScreencastViewer;
