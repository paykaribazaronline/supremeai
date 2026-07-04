// বাংলা মন্তব্য: একটি সেশনের চ্যাট ভিউ — ফলো-আপ মেসেজ পাঠানো যায় এবং ব্যাকএন্ড থেকে উত্তর আসে
import { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Send } from 'lucide-react';
import { getAethelResponse } from '../../services/chatService';
import { type DashboardSession, loadSessions, upsertSession } from './sessionStore';

interface SessionDetailPageProps {
  sessionId: string;
  onBack: () => void;
}

export function SessionDetailPage({ sessionId, onBack }: SessionDetailPageProps) {
  const [session, setSession] = useState<DashboardSession | null>(null);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const found = loadSessions().find((s) => s.id === sessionId) || null;
    setSession(found);
  }, [sessionId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ behavior: 'smooth' });
  }, [session?.messages.length]);

  const handleSend = async () => {
    if (!input.trim() || sending || !session) return;
    setSending(true);
    const updated: DashboardSession = {
      ...session,
      status: 'running',
      messages: [
        ...session.messages,
        { id: Date.now(), sender: 'User', text: input.trim(), timestamp: new Date().toLocaleTimeString() },
      ],
    };
    setSession(updated);
    upsertSession(updated);
    const text = input.trim();
    setInput('');

    try {
      const history = updated.messages.map((m) => ({
        role: m.sender === 'User' ? ('user' as const) : ('assistant' as const),
        content: m.text,
      }));
      const responseText = await getAethelResponse(text, history);
      updated.messages = [
        ...updated.messages,
        { id: Date.now(), sender: 'SupremeAI', text: responseText, timestamp: new Date().toLocaleTimeString() },
      ];
      updated.status = 'finished';
    } catch (error) {
      updated.messages = [
        ...updated.messages,
        {
          id: Date.now(),
          sender: 'SupremeAI',
          text: `AI backend error: ${error instanceof Error ? error.message : 'Unable to process message.'}`,
          timestamp: new Date().toLocaleTimeString(),
        },
      ];
      updated.status = 'error';
    } finally {
      setSession({ ...updated });
      upsertSession(updated);
      setSending(false);
    }
  };

  if (!session) {
    return (
      <div className="max-w-3xl mx-auto px-6 py-10 text-center">
        <p className="text-sm text-slate-500 mb-4">Session not found.</p>
        <button onClick={onBack} className="text-xs text-blue-400 hover:text-blue-300">
          ← Back to sessions
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-6 py-6 flex flex-col h-full">
      <div className="flex items-center gap-3 mb-4">
        <button
          onClick={onBack}
          aria-label="Back to sessions"
          className="text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft size={16} />
        </button>
        <h1 className="text-sm font-medium text-white truncate flex-1">{session.title}</h1>
        <span
          className={`text-[10px] px-2 py-0.5 rounded-full border ${
            session.status === 'finished'
              ? 'text-emerald-400 border-emerald-400/30'
              : session.status === 'error'
                ? 'text-rose-400 border-rose-400/30'
                : 'text-blue-400 border-blue-400/30'
          }`}
        >
          {session.status}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto flex flex-col gap-3 mb-4 min-h-[300px]">
        {session.messages.map((msg) => (
          <div
            key={msg.id}
            className={`max-w-[85%] rounded-xl px-4 py-2.5 text-sm ${
              msg.sender === 'User'
                ? 'self-end bg-blue-600/80 text-white'
                : 'self-start bg-white/[0.05] text-slate-200 border border-white/[0.06]'
            }`}
          >
            <p className="whitespace-pre-wrap break-words">{msg.text}</p>
            <p className="text-[10px] opacity-50 mt-1">{msg.timestamp}</p>
          </div>
        ))}
        {sending && (
          <div className="self-start text-xs text-slate-500 animate-pulse px-2">SupremeAI is working…</div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="flex items-end gap-2 rounded-xl border border-white/10 bg-white/[0.03] p-2 focus-within:border-blue-500/50 transition-colors">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          placeholder="Send a follow-up message..."
          rows={2}
          className="flex-1 bg-transparent text-sm text-white placeholder-slate-500 outline-none resize-none"
        />
        <button
          onClick={handleSend}
          disabled={!input.trim() || sending}
          aria-label="Send message"
          className="p-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 disabled:text-slate-500 text-white transition-colors"
        >
          <Send size={14} />
        </button>
      </div>
    </div>
  );
}
