// ChatWithAI.tsx - Enhanced with Multi-Chat Sessions
import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, Space, message, Tag, Tooltip, Modal, Badge } from 'antd';
import {
    SendOutlined,
    RobotOutlined,
    CopyOutlined,
    ThunderboltOutlined,
    DatabaseOutlined,
    PlusOutlined,
    DeleteOutlined,
    EditOutlined,
    MessageOutlined,
    AudioOutlined,
    PictureOutlined,
    CloseCircleOutlined,
    LoadingOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';
import AISuggestionInformer from './AISuggestionInformer';

interface ChatMessage {
    id: string;
    sender: 'user' | 'ai';
    agent: string;
    content: string;
    timestamp: string;
    confidence?: number;
    intent?: string;
    status?: 'pending' | 'completed' | 'error';
    image?: string;
}

interface ChatSession {
    id: string;
    name: string;
    messages: ChatMessage[];
    createdAt: string;
}

interface ChatWithAIProps {
    chatFont?: string;
}

const ChatWithAI: React.FC<ChatWithAIProps> = ({ chatFont = 'font-mono' }) => {
    // Session State
    const [sessions, setSessions] = useState<ChatSession[]>([]);
    const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
    const [isRenameModalVisible, setIsRenameModalVisible] = useState(false);
    const [sessionToRename, setSessionToRename] = useState<ChatSession | null>(null);
    const [newName, setNewName] = useState('');

    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [selectedAgent, setSelectedAgent] = useState('all');
    const [agents, setAgents] = useState<any[]>([]);
    const [knowledge, setKnowledge] = useState<{rules: any[], plans: any[], actions: any[]}>({ rules: [], plans: [], actions: [] });
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Voice & Image States
    const [isRecording, setIsRecording] = useState(false);
    const [recognition, setRecognition] = useState<any>(null);
    const [attachedImage, setAttachedImage] = useState<string | null>(null);
    const [attachedImageName, setAttachedImageName] = useState<string>('');

    // Speech Recognition setup on mount
    useEffect(() => {
        const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
        if (SpeechRecognition) {
            const recog = new SpeechRecognition();
            recog.continuous = true;
            recog.interimResults = true;
            recog.lang = 'bn-BD'; // Support Bengali by default

            recog.onresult = (event: any) => {
                let interimTranscript = '';
                let finalTranscript = '';
                for (let i = event.resultIndex; i < event.results.length; ++i) {
                    if (event.results[i].isFinal) {
                        finalTranscript += event.results[i][0].transcript;
                    } else {
                        interimTranscript += event.results[i][0].transcript;
                    }
                }
                const transcription = finalTranscript || interimTranscript;
                if (transcription.trim()) {
                    setInput(prev => {
                        if (prev.endsWith(' ') || prev === '') {
                            return prev + transcription;
                        } else {
                            return prev + ' ' + transcription;
                        }
                    });
                }
            };

            recog.onerror = (event: any) => {
                console.error('Speech recognition error:', event.error);
                if (event.error === 'not-allowed') {
                    message.error('মাইক্রোফোন অ্যাক্সেসের অনুমতি নেই।');
                } else {
                    message.error('ভয়েস সনাক্তকরণে সমস্যা হয়েছে: ' + event.error);
                }
                setIsRecording(false);
            };

            recog.onend = () => {
                setIsRecording(false);
            };

            setRecognition(recog);
        }
    }, []);

    const toggleRecording = () => {
        if (!recognition) {
            message.warning('আপনার ব্রাউজার ভয়েস সনাক্তকরণ সমর্থন করে না। Google Chrome ব্যবহার করার চেষ্টা করুন।');
            return;
        }

        if (isRecording) {
            recognition.stop();
            setIsRecording(false);
            message.info('ভয়েস ইনপুট বন্ধ করা হয়েছে।');
        } else {
            try {
                recognition.start();
                setIsRecording(true);
                message.success('ভয়েস ইনপুট সক্রিয় হয়েছে... কথা বলুন।');
            } catch (err) {
                console.error(err);
                recognition.stop();
                setIsRecording(false);
            }
        }
    };

    const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        if (file.size > 4 * 1024 * 1024) {
            message.error('ফাইলের সাইজ ৪ মেগাবাইটের কম হতে হবে।');
            return;
        }

        const reader = new FileReader();
        reader.onload = () => {
            if (typeof reader.result === 'string') {
                setAttachedImage(reader.result);
                setAttachedImageName(file.name);
                message.success('ছবি সংযুক্ত করা হয়েছে।');
            }
        };
        reader.onerror = () => {
            message.error('ছবি প্রসেস করতে ত্রুটি হয়েছে।');
        };
        reader.readAsDataURL(file);
    };

    const removeAttachedImage = () => {
        setAttachedImage(null);
        setAttachedImageName('');
        message.info('সংযুক্ত ছবি মুছে ফেলা হয়েছে।');
    };

    // Load sessions from localStorage on mount
    useEffect(() => {
        const savedSessions = localStorage.getItem('supremeai_chat_sessions');
        if (savedSessions) {
            try {
                const parsed = JSON.parse(savedSessions);
                setSessions(parsed);
                if (parsed.length > 0) {
                    setActiveSessionId(parsed[0].id);
                } else {
                    createNewSession();
                }
            } catch (e) {
                console.error('Failed to parse saved sessions');
                createNewSession();
            }
        } else {
            createNewSession();
        }
        
        fetchAgents();
        fetchKnowledge();
    }, []);

    // Save sessions to localStorage whenever they change
    useEffect(() => {
        if (sessions.length >= 0) {
            localStorage.setItem('supremeai_chat_sessions', JSON.stringify(sessions));
        }
    }, [sessions]);

    const activeSession = sessions.find(s => s.id === activeSessionId);
    const messages = activeSession?.messages || [];

    const createNewSession = () => {
        const newSession: ChatSession = {
            id: Date.now().toString(),
            name: 'New Chat',
            messages: [],
            createdAt: new Date().toISOString()
        };
        setSessions(prev => [newSession, ...prev]);
        setActiveSessionId(newSession.id);
    };

    const deleteSession = (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        const filtered = sessions.filter(s => s.id !== id);
        setSessions(filtered);
        if (activeSessionId === id) {
            setActiveSessionId(filtered.length > 0 ? filtered[0].id : null);
            if (filtered.length === 0) {
                // We'll create one in the useEffect if needed, but let's do it here for UX
                setTimeout(() => {
                    if (filtered.length === 0) createNewSession();
                }, 0);
            }
        }
        message.success('Chat deleted');
    };

    const handleRename = (session: ChatSession, e: React.MouseEvent) => {
        e.stopPropagation();
        setSessionToRename(session);
        setNewName(session.name);
        setIsRenameModalVisible(true);
    };

    const saveNewName = () => {
        if (sessionToRename && newName.trim()) {
            setSessions(prev => prev.map(s => 
                s.id === sessionToRename.id ? { ...s, name: newName.trim() } : s
            ));
            setIsRenameModalVisible(false);
            message.success('Chat renamed');
        }
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const fetchAgents = async () => {
        try {
            const isAuthenticated = authUtils.isAuthenticated();
            if (!isAuthenticated) {
                setAgents([]);
                return;
            }

            const response = await authUtils.fetchWithAuth('/api/admin/providers/configured');
            if (response.ok) {
                const result = await response.json();
                const rawData = result.data?.providers || (Array.isArray(result.data) ? result.data : []);
                
                // Map providers to agent format
                const mappedAgents = rawData.map((p: any) => ({
                    id: p.id,
                    name: p.name,
                    status: p.status || 'online',
                    type: p.type || 'llm'
                }));
                
                setAgents(mappedAgents);
            }
        } catch (error) {
            console.error('Failed to fetch agents');
        }
    };

    const fetchKnowledge = async () => {
        try {
            const isAuthenticated = authUtils.isAuthenticated();
            if (!isAuthenticated) {
                setKnowledge({ rules: [], plans: [], actions: [] });
                return;
            }

            const [rulesRes, plansRes, actionsRes] = await Promise.all([
                authUtils.fetchWithAuth('/api/admin/rules').catch(() => null),
                authUtils.fetchWithAuth('/api/admin/plans').catch(() => null),
                authUtils.fetchWithAuth('/api/admin/chat/actions/pending').catch(() => null)
            ]);
            const rules = rulesRes?.ok ? await rulesRes.json() : [];
            const plans = plansRes?.ok ? await plansRes.json() : [];
            const actions = actionsRes?.ok ? await actionsRes.json() : [];
            setKnowledge({ 
                rules: Array.isArray(rules) ? rules.slice(0, 5) : [], 
                plans: Array.isArray(plans) ? plans.slice(0, 5) : [],
                actions: Array.isArray(actions) ? actions.slice(0, 5) : []
            });
        } catch (error) {
            console.error('Failed to fetch knowledge context');
        }
    };

    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();
        if ((!input.trim() && !attachedImage) || loading || !activeSessionId) return;

        const userMessage: ChatMessage = {
            id: Date.now().toString(),
            sender: 'user',
            agent: 'You',
            content: input,
            image: attachedImage || undefined,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            status: 'completed'
        };

        // Update session messages
        let currentSessions = [...sessions];
        const sessionIndex = currentSessions.findIndex(s => s.id === activeSessionId);
        if (sessionIndex !== -1) {
            currentSessions[sessionIndex].messages.push(userMessage);
            
            // Smart Name: If first message or default name, generate name
            if (currentSessions[sessionIndex].messages.length === 1 || currentSessions[sessionIndex].name === 'New Chat') {
                const words = input.trim() ? input.split(' ') : ['Attached Image'];
                currentSessions[sessionIndex].name = words.slice(0, 4).join(' ') + (words.length > 4 ? '...' : '');
            }
            
            setSessions(currentSessions);
        }

        const currentInput = input;
        const currentImage = attachedImage;
        const currentImageName = attachedImageName;

        setInput('');
        setAttachedImage(null);
        setAttachedImageName('');
        setLoading(true);

        try {
            const currentSession = sessions.find(s => s.id === activeSessionId);
            const history = currentSession ? currentSession.messages : [];
            
            // Format message body to include image markdown if present so backend models can see it
            const messageBody = currentImage 
                ? `${currentInput}\n\n[সংযুক্ত ছবি: ${currentImageName}]\n![${currentImageName}](${currentImage})`
                : currentInput;

            const response = await authUtils.fetchWithAuth('/api/chat/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    message: messageBody,
                    agentId: selectedAgent === 'all' ? null : selectedAgent,
                    sessionId: activeSessionId,
                    messages: history,
                }),
            });

            if (response.ok) {
                const data = await response.json();
                const aiMessage: ChatMessage = {
                    id: (Date.now() + 1).toString(),
                    sender: 'ai',
                    agent: data.agent_name || 'Neural Core',
                    content: data.message || 'Processing optimized.',
                    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                    confidence: data.confidence ? Math.round(data.confidence * 100) : 98,
                    intent: data.intent || 'NORMAL',
                    status: 'completed',
                };
                
                setSessions(prev => prev.map(s => 
                    s.id === activeSessionId ? { ...s, messages: [...s.messages, aiMessage] } : s
                ));
            }
        } catch (error: any) {
            message.error('Request failed');
        } finally {
            setLoading(false);
        }
    };

    const playVoice = async (text: string) => {
        try {
            const response = await authUtils.fetchWithAuth('/api/voicebox/speak', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text, profile: 'default' })
            });
            if (response.ok) {
                const data = await response.json();
                if (data && data.audio_url) {
                    const audio = new Audio(data.audio_url);
                    audio.play();
                } else if (data && data.audio) {
                    const audio = new Audio(`data:audio/wav;base64,${data.audio}`);
                    audio.play();
                } else {
                    const utterance = new SpeechSynthesisUtterance(text);
                    utterance.lang = 'bn-BD';
                    window.speechSynthesis.speak(utterance);
                }
            } else {
                const utterance = new SpeechSynthesisUtterance(text);
                utterance.lang = 'bn-BD';
                window.speechSynthesis.speak(utterance);
            }
        } catch (err) {
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.lang = 'bn-BD';
            window.speechSynthesis.speak(utterance);
        }
    };

    return (
        <div className="flex flex-1 h-full glass-panel text-white overflow-hidden rounded-2xl">
            {/* Left Sidebar: Sessions */}
            <div className="w-64 bg-[#020205]/60 border-r border-white/10 flex flex-col backdrop-blur-xl">
                <div className="p-4 border-b border-white/10">
                    <Button 
                        type="primary" 
                        icon={<PlusOutlined />} 
                        block 
                        onClick={createNewSession}
                        className="bg-cyan-500/90 hover:bg-cyan-400 border-none h-11 font-black uppercase tracking-widest flex items-center justify-center gap-2 rounded-xl transition-all hover:shadow-[0_0_20px_rgba(16,185,129,0.4)]"
                    >
                        New Neural Session
                    </Button>
                </div>
                <div className="flex-1 overflow-y-auto p-3 space-y-1 custom-scrollbar bg-black/20">
                    {sessions.map(s => (
                        <div 
                            key={s.id}
                            onClick={() => setActiveSessionId(s.id)}
                            className={`group flex items-center justify-between p-3 rounded-xl cursor-pointer transition-all duration-200 ${
                                activeSessionId === s.id 
                                ? 'bg-gradient-to-r from-cyan-400/20 to-transparent border border-cyan-400/30 shadow-[0_0_15px_rgba(16,185,129,0.1)]' 
                                : 'hover:bg-white/5 border border-transparent opacity-60 hover:opacity-100'
                            }`}
                        >
                            <div className="flex items-center gap-3 overflow-hidden flex-1">
                                <div className={`w-2 h-2 rounded-full transition-all ${activeSessionId === s.id ? 'bg-cyan-400 shadow-[0_0_8px_#00f3ff]' : 'bg-white/10'}`} />
                                <span className={`text-[12px] truncate font-bold uppercase tracking-tight ${activeSessionId === s.id ? 'text-white' : 'text-white/40'}`}>{s.name}</span>
                            </div>
                            <div className={`flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity`}>
                                <Tooltip title="Rename">
                                    <EditOutlined 
                                        className="text-xs text-white/30 hover:text-white" 
                                        onClick={(e) => handleRename(s, e)}
                                    />
                                </Tooltip>
                                <Tooltip title="Delete">
                                    <DeleteOutlined 
                                        className="text-xs text-white/30 hover:text-red-500" 
                                        onClick={(e) => deleteSession(s.id, e)}
                                    />
                                </Tooltip>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Main Column: Chat Interface */}
            <div className="flex-1 flex flex-col relative">
                {/* Header */}
                <div className="px-6 py-4 bg-white/[0.01] border-b border-white/10 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-cyan-400/20 rounded-lg">
                            <RobotOutlined className="text-cyan-400 text-lg" />
                        </div>
                        <div>
                            <h3 className="text-sm font-bold text-white mb-0">{activeSession?.name || 'Neural Chat'}</h3>
                            <span className="text-[10px] text-cyan-400/80 uppercase tracking-widest font-bold">SupremeAI Neural Core</span>
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                        <select
                            value={selectedAgent}
                            onChange={(e) => setSelectedAgent(e.target.value)}
                            className="bg-black/60 border border-white/10 text-[11px] px-4 py-2 rounded-lg text-white/80 outline-none hover:border-cyan-400/50 transition-all focus:ring-1 focus:ring-cyan-400/30"
                        >
                            <option value="all">Dynamic Routing (All)</option>
                            {agents.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                        </select>
                    </div>
                </div>

                {/* Chat Area */}
                <div className="flex-1 overflow-y-auto custom-scrollbar bg-[#050505]">
                    <div className="max-w-4xl mx-auto w-full p-6 space-y-8">
                        {messages.length === 0 ? (
                            <div className="h-[60vh] flex flex-col items-center justify-center text-white/20">
                                <div className="w-20 h-20 bg-white/[0.02] rounded-3xl flex items-center justify-center mb-6 border border-white/5 shadow-2xl relative overflow-hidden group">
                                    <div className="absolute inset-0 bg-cyan-400/5 animate-pulse" />
                                    <MessageOutlined className="text-4xl text-cyan-400/40 relative z-10 group-hover:scale-110 transition-transform" />
                                </div>
                                <span className="text-[10px] font-black tracking-[0.4em] uppercase opacity-40 animate-pulse">Neural Synchronization Initiated</span>
                            </div>
                        ) : (
                            messages.map((msg) => (
                                <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2 duration-300`}>
                                    <div className={`max-w-[85%] ${msg.sender === 'user' ? 'order-2' : 'order-1'}`}>
                                        <div className={`flex items-center gap-3 mb-2 px-1 ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                                            <span className="text-[9px] font-black text-white/30 uppercase tracking-widest">
                                                {msg.sender === 'ai' ? msg.agent : 'Authorized User'} • {msg.timestamp}
                                            </span>
                                            {msg.sender === 'ai' && (
                                                <div className="flex items-center gap-1">
                                                    <div className="w-1 h-1 rounded-full bg-cyan-400" />
                                                    <span className="text-[8px] text-cyan-400/60 font-bold">{msg.confidence}% CONFIDENCE</span>
                                                </div>
                                            )}
                                        </div>
                                        <div className={`px-6 py-5 rounded-2xl text-[14px] leading-relaxed shadow-2xl transition-all hover:shadow-cyan-900/5 ${
                                            msg.sender === 'user'
                                            ? 'bg-gradient-to-br from-cyan-500/20 to-cyan-900/5 border border-cyan-400/20 text-white rounded-tr-none'
                                            : 'bg-white/[0.03] border border-white/10 text-white/90 rounded-tl-none backdrop-blur-xl'
                                        }`}>
                                            {msg.image && (
                                                <div className="mb-4 relative rounded-xl overflow-hidden border border-white/10 group cursor-pointer max-w-sm">
                                                    <img 
                                                        src={msg.image} 
                                                        alt="Attached file" 
                                                        className="w-full h-auto object-cover max-h-60 rounded-xl transition-transform duration-300 group-hover:scale-105"
                                                        onClick={() => {
                                                            Modal.info({
                                                                title: <span className="text-white font-bold uppercase tracking-wider">সংযুক্ত ছবি</span>,
                                                                icon: null,
                                                                width: 800,
                                                                centered: true,
                                                                content: (
                                                                    <div className="flex justify-center p-2 bg-[#0c0c0c] rounded-xl border border-white/10 mt-4 overflow-hidden">
                                                                        <img src={msg.image} alt="Preview" className="max-w-full max-h-[70vh] object-contain rounded-lg" />
                                                                    </div>
                                                                ),
                                                                okText: 'বন্ধ করুন',
                                                                styles: { body: { backgroundColor: '#0a0a0a', color: 'white' } },
                                                                className: 'dark-modal'
                                                            });
                                                        }}
                                                    />
                                                </div>
                                            )}
                                            <div className="whitespace-pre-wrap">{msg.content}</div>
                                        </div>
                                        {msg.sender === 'ai' && (
                                            <div className="flex gap-4 mt-3 px-1">
                                                <button
                                                    onClick={() => { navigator.clipboard.writeText(msg.content); message.success('Encrypted Data Copied'); }}
                                                    className="text-[9px] text-white/20 hover:text-cyan-300 transition-all flex items-center gap-1.5 uppercase font-black tracking-wider border-none bg-transparent cursor-pointer"
                                                >
                                                    <CopyOutlined className="text-xs" /> Copy
                                                </button>
                                                <button
                                                    type="button"
                                                    onClick={() => playVoice(msg.content)}
                                                    className="text-[9px] text-white/20 hover:text-cyan-300 transition-all flex items-center gap-1.5 uppercase font-black tracking-wider border-none bg-transparent cursor-pointer"
                                                >
                                                    <AudioOutlined className="text-xs" /> শুনুন (Listen)
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                        <div ref={messagesEndRef} />
                    </div>
                </div>

                {/* Input Area */}
                <div className="p-6 bg-gradient-to-t from-black to-transparent border-t border-white/5 relative z-10">
                    <div className="max-w-4xl mx-auto w-full">
                        {/* Image Preview Container */}
                        {attachedImage && (
                            <div className="mb-3 p-3 bg-white/[0.03] border border-white/10 rounded-xl flex items-center justify-between animate-in fade-in slide-in-from-bottom-2 duration-200 backdrop-blur-md">
                                <div className="flex items-center gap-3">
                                    <div className="w-12 h-12 rounded-lg overflow-hidden border border-white/10 relative group">
                                        <img src={attachedImage} alt="Upload preview" className="w-full h-full object-cover" />
                                    </div>
                                    <div className="flex flex-col">
                                        <span className="text-xs font-bold text-white/80">{attachedImageName || 'ছবি.png'}</span>
                                        <span className="text-[10px] text-cyan-400/80 uppercase tracking-widest font-black">IMAGE READY</span>
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => { setAttachedImage(null); setAttachedImageName(''); }}
                                    className="p-2 text-white/40 hover:text-red-500 hover:bg-white/5 rounded-lg transition-all border-none bg-transparent cursor-pointer"
                                >
                                    <DeleteOutlined />
                                </button>
                            </div>
                        )}
                        <form onSubmit={handleSendMessage} className="relative group flex gap-3">
                            <button
                                type="button"
                                onClick={() => setSessions(prev => prev.map(s => s.id === activeSessionId ? { ...s, messages: [] } : s))}
                                className="h-16 px-5 flex items-center justify-center bg-[#8b5cf6]/20 hover:bg-[#8b5cf6]/30 text-[#8b5cf6] border border-[#8b5cf6]/30 rounded-2xl font-bold transition-all shadow-[0_0_15px_rgba(139,92,246,0.15)] hover:shadow-[0_0_25px_rgba(139,92,246,0.3)] cursor-pointer"
                                title="Reset Context"
                            >
                                <ThunderboltOutlined className="text-xl" />
                            </button>
                            <div className="relative flex-1">
                                <Input
                                    placeholder={isRecording ? "ভয়েস সনাক্ত করা হচ্ছে... কথা বলুন..." : "Neural Input Channel [Type your command]..."}
                                    value={input}
                                    onChange={(e) => setInput(e.target.value)}
                                    disabled={loading}
                                    className={`h-16 bg-[#020205]/60 border border-[#00f3ff]/20 text-white placeholder:text-[#00f3ff]/30 rounded-2xl px-6 pr-44 focus:bg-[#020205]/80 focus:border-[#00f3ff]/60 transition-all shadow-2xl backdrop-blur-md font-mono ${isRecording ? 'border-red-500/50 shadow-[0_0_15px_rgba(239,68,68,0.2)]' : 'hover:border-[#00f3ff]/40 shadow-[0_0_15px_rgba(0,243,255,0.05)]'}`}
                                    prefix={
                                        <div className="flex items-center gap-2 mr-3 border-r border-[#00f3ff]/20 pr-3">
                                            <input 
                                                type="file" 
                                                id="chat-image-upload" 
                                                accept="image/*" 
                                                onChange={handleImageUpload} 
                                                className="hidden" 
                                            />
                                            <Tooltip title="ছবি সংযুক্ত করুন">
                                                <button
                                                    type="button"
                                                    onClick={() => document.getElementById('chat-image-upload')?.click()}
                                                    className="p-2 hover:bg-[#00f3ff]/10 text-[#00f3ff]/60 hover:text-[#00f3ff] rounded-lg transition-all flex items-center justify-center border-none bg-transparent cursor-pointer"
                                                >
                                                    <PictureOutlined className="text-lg" />
                                                </button>
                                            </Tooltip>
                                            <Tooltip title={isRecording ? "রেকর্ডিং বন্ধ করুন" : "ভয়েস ইনপুট"}>
                                                <button
                                                    type="button"
                                                    onClick={toggleRecording}
                                                    className={`p-2 rounded-lg transition-all flex items-center justify-center border-none cursor-pointer ${
                                                        isRecording 
                                                        ? 'bg-red-500/20 text-red-500 animate-pulse hover:bg-red-500/30' 
                                                        : 'bg-transparent text-[#00f3ff]/60 hover:text-[#00f3ff] hover:bg-[#00f3ff]/10'
                                                    }`}
                                                >
                                                    {isRecording ? <LoadingOutlined className="text-lg" /> : <AudioOutlined className="text-lg" />}
                                                </button>
                                            </Tooltip>
                                        </div>
                                    }
                                />
                            <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-3">
                                <AISuggestionInformer 
                                    context="admin_chat" 
                                    onSelect={(val) => setInput(val)} 
                                />
                                <button
                                    type="submit"
                                    disabled={loading || (!input.trim() && !attachedImage)}
                                    className="h-12 px-8 bg-cyan-500 hover:bg-cyan-400 disabled:bg-white/5 text-white rounded-xl font-black uppercase tracking-widest transition-all disabled:cursor-not-allowed flex items-center gap-2 shadow-[0_0_20px_rgba(5,150,105,0.3)] hover:shadow-[0_0_30px_rgba(5,150,105,0.5)] border-none"
                                >
                                    {loading ? <ThunderboltOutlined spin className="text-lg" /> : <SendOutlined className="text-lg" />}
                                    <span className="hidden sm:inline">{loading ? 'PROCESSING' : 'EXECUTE'}</span>
                                </button>
                            </div>
                        </div>
                        </form>
                        <div className="flex items-center justify-between mt-4 px-2">
                            <div className="flex items-center gap-2">
                                <div className="w-1 h-1 rounded-full bg-cyan-400 animate-ping" />
                                <span className="text-[8px] text-white/20 font-black tracking-[0.3em] uppercase">Security Level: High</span>
                            </div>
                            <p className="text-[9px] text-white/10 font-black tracking-[0.2em] uppercase m-0">
                                AI-Driven Autonomy System • Core v4.2 Stable
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Right Column: Knowledge Context (Optimized) */}
            <div className="w-80 bg-white/[0.01] border-l border-white/10 hidden xl:flex flex-col">
                <div className="p-6">
                    <div className="flex items-center gap-3 mb-8">
                        <DatabaseOutlined className="text-cyan-400" />
                        <h4 className="text-xs font-black text-white uppercase tracking-[0.2em] mb-0">System Context</h4>
                    </div>
                    
                    {knowledge.rules && knowledge.rules.length > 0 ? (
                        <div className="mb-8">
                            <div className="flex items-center justify-between mb-4">
                                <span className="text-[10px] font-black text-white/40 uppercase tracking-widest">Active Rules</span>
                                <Badge count={knowledge.rules.length} style={{ backgroundColor: '#00f3ff', fontSize: '9px', fontWeight: 'bold' }} />
                            </div>
                            <div className="space-y-3">
                                {knowledge.rules.map((r, i) => (
                                    <div key={i} className="p-4 bg-white/[0.02] border border-white/5 rounded-xl text-[11px] text-white/60 leading-relaxed hover:bg-white/[0.04] transition-colors shadow-sm">
                                        {r.content || r.message}
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div className="flex flex-col items-center justify-center py-12 opacity-20">
                            <DatabaseOutlined className="text-3xl mb-3" />
                            <span className="text-[10px] uppercase font-bold tracking-widest">No Active Rules</span>
                        </div>
                    )}
                </div>
            </div>

            {/* Rename Modal */}
            <Modal
                title={<span className="text-white font-bold uppercase tracking-wider">Rename Chat Session</span>}
                open={isRenameModalVisible}
                onOk={saveNewName}
                onCancel={() => setIsRenameModalVisible(false)}
                okText="Save Changes"
                cancelText="Cancel"
                centered
                className="dark-modal"
                styles={{ body: { backgroundColor: '#0a0a0a', borderBottomLeftRadius: '12px', borderBottomRightRadius: '12px' } }}
            >
                <div className="py-4">
                    <label className="block text-[10px] font-black text-white/30 uppercase tracking-[0.2em] mb-3">New Session Name</label>
                    <Input 
                        value={newName} 
                        onChange={(e) => setNewName(e.target.value)}
                        className="bg-white/[0.05] border-white/10 text-white h-12 rounded-xl focus:border-cyan-400/50"
                        placeholder="Enter a descriptive name..."
                        onPressEnter={saveNewName}
                        autoFocus
                    />
                </div>
            </Modal>

            <style>{`
                .custom-scrollbar::-webkit-scrollbar {
                    width: 4px;
                }
                .custom-scrollbar::-webkit-scrollbar-track {
                    background: transparent;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb {
                    background: rgba(255, 255, 255, 0.05);
                    border-radius: 10px;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb:hover {
                    background: rgba(16, 185, 129, 0.2);
                }
                .dark-modal .ant-modal-content {
                    background-color: #0a0a0a;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 16px;
                    overflow: hidden;
                }
                .dark-modal .ant-modal-header {
                    background-color: #0a0a0a;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
                }
                .dark-modal .ant-modal-title {
                    color: white;
                }
                .dark-modal .ant-modal-close-x {
                    color: rgba(255, 255, 255, 0.4);
                }
                .dark-modal .ant-btn-primary {
                    background-color: #059669;
                    border: none;
                }
                .dark-modal .ant-btn-default {
                    background-color: transparent;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    color: white;
                }
            `}</style>
        </div>
    );
};

export default ChatWithAI;