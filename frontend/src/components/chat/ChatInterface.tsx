// apps/studio-client/src/components/chat/ChatInterface.tsx
// Primary Agent Chat Interface
// বাংলা মন্তব্য: মূল এজেন্ট চ্যাট ইন্টারফেস, যা useStore থেকে চ্যাট হিস্ট্রি ব্যবহার করে।

import React, { useState, useRef, useEffect } from 'react';
import { useStore } from '../../store/useStore';
import { UnifiedChatBubble } from './UnifiedChatBubble';
import { apiClient } from '../../services/apiClient';
import { useEventBus } from '../../hooks/useEventBus';
import { eventBus, Events } from '../../lib/eventBus';
import { Volume2, VolumeX } from 'lucide-react';

export const ChatInterface: React.FC = () => {
  const { chatHistory, addMessage, isOrchestrating, triggerOrchestration } = useStore();
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [voiceEnabled, setVoiceEnabled] = useState(false);
  const [audioQueue, setAudioQueue] = useState<string[]>([]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [chatHistory]);

  useEventBus(Events.SYSTEM_ALERT, (payload: any) => {
    addMessage({
      role: 'system',
      content: `[SYSTEM ALERT] ${payload.message || JSON.stringify(payload)}`
    });
  });

  // Listen for voice messages ready
  useEventBus(Events.VOICE_MESSAGE_READY, (data: any) => {
    if (voiceEnabled && data.audioUrl) {
      setAudioQueue(prev => [...prev, data.audioUrl]);
    }
  });
  
  // Listen for browser context sharing
  useEventBus(Events.CHAT_MESSAGE_SENT, (data: any) => {
    if (data.source === 'browser_context' && data.content) {
      setInput(data.content);  // Pre-fill with browser URL/context
    }
  });

  const handleSend = async () => {
    if (!input.trim()) return;

    const userMessage = input.trim();
    setInput('');

    // Add user message
    addMessage({ role: 'user', content: userMessage });

    // Emit message sent event (for billing, cost tracking, etc.)
    eventBus.emit(Events.CHAT_MESSAGE_SENT, {
      role: 'user',
      content: userMessage,
      timestamp: Date.now(),
      estimatedTokens: Math.ceil(userMessage.length / 4),
      source: 'chat_interface',
    });

    // Trigger orchestration
    triggerOrchestration(true);

    try {
      const response = await apiClient.post<{ response?: string }>('/api/orchestrate', {
        message: userMessage,
        idempotency_key: crypto.randomUUID(),
      });

      const assistantResponse = response.response || JSON.stringify(response);
      // Add assistant response
      addMessage({
        role: 'assistant',
        content: assistantResponse
      });

      // Request TTS if voice enabled
      if (voiceEnabled && assistantResponse) {
        eventBus.emit(Events.TTS_GENERATED, {
          text: assistantResponse,
          timestamp: Date.now(),
        });
      }
    } catch (error: unknown) {
      addMessage({
        role: 'assistant',
        content: error instanceof Error ? `Error: ${error.message}` : 'Error: Failed to get response'
      });
    } finally {
      triggerOrchestration(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="flex flex-col h-full">
      {/* Toolbar area */}
      <div className="flex justify-end p-2 border-b border-slate-800">
        <button
          onClick={() => {
            setVoiceEnabled(!voiceEnabled);
            eventBus.emit(Events.VOICE_TOGGLED, {
              enabled: !voiceEnabled,
              timestamp: Date.now(),
            });
          }}
          className={`p-2 rounded-lg transition-colors ${voiceEnabled ? 'bg-blue-600 text-white' : 'text-slate-400 hover:bg-slate-800'}`}
          title={voiceEnabled ? 'Disable voice responses' : 'Enable voice responses'}
        >
          {voiceEnabled ? <Volume2 size={18} /> : <VolumeX size={18} />}
        </button>
      </div>

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {chatHistory.map((msg) => (
          <div key={msg.id}>
            <UnifiedChatBubble
              text={msg.content}
              sender={msg.role === 'user' ? 'user' : 'system'}
              timestamp={new Date(msg.timestamp).toLocaleTimeString()}
            />
            {voiceEnabled && msg.role === 'assistant' && (msg as any).audioUrl && (
              <audio 
                controls 
                src={(msg as any).audioUrl} 
                className="mt-2"
                preload="none"
              />
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 border-t border-slate-800">
        <div className="flex gap-2">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Type your message to the AI agent..."
            className="flex-1 bg-slate-800 text-white rounded-lg p-3 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
            rows={2}
            disabled={isOrchestrating}
          />
          <button
            onClick={handleSend}
            disabled={isOrchestrating || !input.trim()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isOrchestrating ? 'Sending...' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatInterface;
