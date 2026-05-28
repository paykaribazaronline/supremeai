// ChatWithAI.tsx - Premium Designed Neural Chat Component
import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, message, Tooltip, Modal, Badge } from 'antd';
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
            name: 'নতুন চ্যাট সেশন',
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
                setTimeout(() => {
                    if (filtered.length === 0) createNewSession();
                }, 0);
            }
        }
        message.success('চ্যাট সেশন মুছে ফেলা হয়েছে');
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
            message.success('চ্যাটের নাম পরিবর্তন করা হয়েছে');
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
            if (currentSessions[sessionIndex].messages.length === 1 || currentSessions[sessionIndex].name === 'New Chat' || currentSessions[sessionIndex].name === 'নতুন চ্যাট সেশন') {
                const words = input.trim() ? input.split(' ') : ['সংযুক্ত ছবি'];
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
                    confidence: data.confidence ? Math.round(data.confidence * 100) : undefined,
                    intent: data.intent || 'NORMAL',
                    status: 'completed',
                };
                
                setSessions(prev => prev.map(s => 
                    s.id === activeSessionId ? { ...s, messages: [...s.messages, aiMessage] } : s
                ));
            } else {
                // API failed - provide local fallback response
                let errorMsg = 'সিস্টেম প্রক্রিয়াকরণে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন বা ব্যাকএন্ড লগ পরীক্ষা করুন।';
                try {
                    const data = await response.json();
                    if (data && data.message) errorMsg = data.message;
                    else if (data && data.error) errorMsg = data.error;
                } catch(e) {}

                const fallbackMessage: ChatMessage = {
                    id: (Date.now() + 1).toString(),
                    sender: 'ai',
                    agent: 'সিস্টেম রেসপন্স',
                    content: errorMsg,
                    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                    status: 'completed',
                };
                
                setSessions(prev => prev.map(s => 
                    s.id === activeSessionId ? { ...s, messages: [...s.messages, fallbackMessage] } : s
                ));
            }
        } catch (error: any) {
            // Network error - provide local fallback response
            const fallbackMessage: ChatMessage = {
                id: (Date.now() + 1).toString(),
                sender: 'ai',
                agent: 'লোকাল নেটওয়ার্ক',
                content: `সার্ভারের সাথে সংযোগ স্থাপন করা যাচ্ছে না। ত্রুটি: ${error.message || 'Unknown network error'}`,
                timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                status: 'completed',
            };
            
            setSessions(prev => prev.map(s => 
                s.id === activeSessionId ? { ...s, messages: [...s.messages, fallbackMessage] } : s
            ));
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
        <div className="glass-panel text-white neural-chat-container">
            {/* Left Sidebar: Sessions */}
            <div className="neural-chat-sidebar">
                <div className="sidebar-header">
                    <Button 
                        type="primary" 
                        icon={<PlusOutlined />} 
                        block 
                        onClick={createNewSession}
                        className="new-session-glow-btn"
                        style={{
                            background: 'linear-gradient(135deg, var(--neon-blue), var(--neon-purple))',
                            border: 'none',
                            height: '46px',
                            borderRadius: '12px',
                            fontWeight: 800,
                            fontSize: '11px',
                            letterSpacing: '1px',
                            textTransform: 'uppercase',
                            color: '#ffffff',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: '8px',
                            boxShadow: '0 0 15px rgba(0, 243, 255, 0.25)'
                        }}
                    >
                        নতুন নিউরাল চ্যাট
                    </Button>
                </div>
                <div className="sessions-list custom-scrollbar">
                    {sessions.map(s => (
                        <div 
                            key={s.id}
                            onClick={() => setActiveSessionId(s.id)}
                            className={`session-item ${activeSessionId === s.id ? 'active' : ''}`}
                        >
                            <div className="session-name-container">
                                <div className="session-dot" />
                                <span className="session-name-text">{s.name}</span>
                            </div>
                            <div className="session-actions">
                                <Tooltip title="নাম পরিবর্তন">
                                    <EditOutlined 
                                        className="action-icon" 
                                        onClick={(e) => handleRename(s, e)}
                                    />
                                </Tooltip>
                                <Tooltip title="মুছে ফেলুন">
                                    <DeleteOutlined 
                                        className="action-icon delete" 
                                        onClick={(e) => deleteSession(s.id, e)}
                                    />
                                </Tooltip>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Main Column: Chat Interface */}
            <div className="neural-chat-content">
                {/* Header */}
                <div className="chat-content-header">
                    <div className="header-info-container">
                        <div className="header-icon-wrapper">
                            <RobotOutlined style={{ color: 'var(--neon-blue)', fontSize: '20px' }} />
                        </div>
                        <div>
                            <h3 className="header-title-text">{activeSession?.name || 'নিউরাল চ্যাট (Neural Chat)'}</h3>
                            <span className="header-subtitle-text">SUPREMEAI NEURAL CORE SYSTEM</span>
                        </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                        <span style={{ fontSize: '11px', color: 'rgba(255, 255, 255, 0.4)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '1px' }}>
                            মডেল সিলেক্ট করুন:
                        </span>
                        <select
                            value={selectedAgent}
                            onChange={(e) => setSelectedAgent(e.target.value)}
                            className="custom-agent-select"
                        >
                            <option value="all">Dynamic Routing (All Models)</option>
                            {agents.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                        </select>
                    </div>
                </div>

                {/* Chat Messages Area */}
                <div className="chat-messages-area custom-scrollbar">
                    {messages.length === 0 ? (
                        <div style={{ height: '60vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: 'rgba(255,255,255,0.15)' }}>
                            <div style={{
                                width: '80px', height: '80px',
                                background: 'rgba(0, 243, 255, 0.05)',
                                borderRadius: '24px',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                border: '1px solid rgba(0, 243, 255, 0.15)',
                                boxShadow: '0 0 30px rgba(0, 243, 255, 0.05)',
                                marginBottom: '20px',
                                position: 'relative'
                            }}>
                                <MessageOutlined style={{ fontSize: '32px', color: 'var(--neon-blue)' }} />
                                <div style={{ position: 'absolute', inset: 0, border: '1.5px solid var(--neon-blue)', borderRadius: '24px', opacity: 0.3, animation: 'pulse 2s infinite ease-in-out' }} />
                            </div>
                            <span style={{ fontSize: '10px', fontWeight: 800, letterSpacing: '4px', textTransform: 'uppercase', color: 'var(--neon-blue)', opacity: 0.6 }}>
                                নিউরাল সিঙ্ক সেশন চালু হয়েছে
                            </span>
                        </div>
                    ) : (
                        <div className="messages-max-width-wrapper">
                            {messages.map((msg) => (
                                <div key={msg.id} className={`message-row ${msg.sender === 'user' ? 'user' : 'ai'}`}>
                                    <div className="message-bubble-wrapper">
                                        <div className={`message-meta-header ${msg.sender === 'user' ? 'user' : 'ai'}`}>
                                            <span className="meta-sender-name">
                                                {msg.sender === 'ai' ? msg.agent : 'অনুমোদিত অপারেটর (Operator)'} • {msg.timestamp}
                                            </span>
                                        </div>
                                        <div className={`message-bubble ${msg.sender === 'user' ? 'user' : 'ai'}`}>
                                            {msg.image && (
                                                <div style={{ marginBottom: '12px', borderRadius: '12px', overflow: 'hidden', border: '1px solid rgba(255, 255, 255, 0.1)', maxWidth: '320px', cursor: 'pointer' }}>
                                                    <img 
                                                        src={msg.image} 
                                                        alt="Attached file" 
                                                        style={{ width: '100%', height: 'auto', display: 'block', transition: 'transform 0.3s' }}
                                                        onClick={() => {
                                                            Modal.info({
                                                                title: <span style={{ color: 'white', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '1px' }}>সংযুক্ত ছবি প্রিভিউ</span>,
                                                                icon: null,
                                                                width: 800,
                                                                centered: true,
                                                                content: (
                                                                    <div style={{ display: 'flex', justifyContent: 'center', padding: '12px', background: '#08080f', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.1)', marginTop: '16px' }}>
                                                                        <img src={msg.image} alt="Preview" style={{ maxWidth: '100%', maxHeight: '70vh', borderRadius: '8px', objectFit: 'contain' }} />
                                                                    </div>
                                                                ),
                                                                okText: 'বন্ধ করুন',
                                                                okButtonProps: { style: { background: 'var(--neon-blue)', border: 'none', color: '#000', fontWeight: 'bold' } },
                                                                styles: { body: { backgroundColor: '#05050a', color: 'white' } },
                                                                className: 'dark-modal'
                                                            });
                                                        }}
                                                    />
                                                </div>
                                            )}
                                            <div style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>{msg.content}</div>
                                            
                                            {msg.sender === 'ai' && (
                                                <div className="message-bubble-footer">
                                                    <div className="bubble-footer-actions">
                                                        <button
                                                            onClick={() => { navigator.clipboard.writeText(msg.content); message.success('টেক্সট কপি করা হয়েছে'); }}
                                                            className="footer-action-btn"
                                                        >
                                                            <CopyOutlined /> কপি করুন
                                                        </button>
                                                        <button
                                                            type="button"
                                                            onClick={() => playVoice(msg.content)}
                                                            className="footer-action-btn"
                                                        >
                                                            <AudioOutlined /> শুনুন
                                                        </button>
                                                    </div>
                                                    {msg.confidence && (
                                                        <div className="ai-confidence-badge">
                                                            <div className="ai-confidence-dot" />
                                                            <span className="ai-confidence-text">
                                                                {msg.confidence}% Confidence
                                                            </span>
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                            <div ref={messagesEndRef} />
                        </div>
                    )}
                </div>

                {/* Input Area */}
                <div className="chat-input-wrapper">
                    <div className="input-max-width-wrapper">
                        {attachedImage && (
                            <div className="image-preview-panel">
                                <div className="preview-thumb-container">
                                    <img src={attachedImage} alt="Upload preview" className="preview-thumbnail" />
                                    <div className="preview-file-details">
                                        <span className="preview-filename">{attachedImageName || 'image.png'}</span>
                                        <span className="preview-badge">সংযুক্ত ছবি প্রস্তুত</span>
                                    </div>
                                </div>
                                <button
                                    type="button"
                                    onClick={removeAttachedImage}
                                    style={{ background: 'transparent', border: 'none', color: 'rgba(255, 255, 255, 0.4)', cursor: 'pointer', fontSize: '16px' }}
                                >
                                    <CloseCircleOutlined style={{ color: 'var(--error)' }} />
                                </button>
                            </div>
                        )}
                        <form onSubmit={handleSendMessage} className="input-container-row">
                            <button
                                type="button"
                                onClick={() => setSessions(prev => prev.map(s => s.id === activeSessionId ? { ...s, messages: [] } : s))}
                                className="reset-context-btn"
                                title="চ্যাট রিসেট করুন (Reset Context)"
                            >
                                <ThunderboltOutlined style={{ fontSize: '20px' }} />
                            </button>
                            
                            <div className="main-input-capsule">
                                <input 
                                    type="file" 
                                    id="chat-image-upload" 
                                    accept="image/*" 
                                    onChange={handleImageUpload} 
                                    style={{ display: 'none' }} 
                                />
                                <Input
                                    placeholder={isRecording ? "ভয়েস রেকর্ড করা হচ্ছে... কথা বলুন..." : "নিউরাল কমান্ড টাইপ করুন..."}
                                    value={input}
                                    onChange={(e) => setInput(e.target.value)}
                                    disabled={loading}
                                    className="chat-styled-input"
                                    prefix={
                                        <div className="chat-input-actions-prefix">
                                            <Tooltip title="ছবি সংযুক্ত করুন">
                                                <button
                                                    type="button"
                                                    onClick={() => document.getElementById('chat-image-upload')?.click()}
                                                    className="prefix-action-btn"
                                                >
                                                    <PictureOutlined style={{ fontSize: '16px' }} />
                                                </button>
                                            </Tooltip>
                                            <Tooltip title={isRecording ? "রেকর্ডিং বন্ধ করুন" : "ভয়েস ইনপুট"}>
                                                <button
                                                    type="button"
                                                    onClick={toggleRecording}
                                                    className={`prefix-action-btn ${isRecording ? 'recording' : ''}`}
                                                >
                                                    {isRecording ? <LoadingOutlined style={{ fontSize: '16px' }} /> : <AudioOutlined style={{ fontSize: '16px' }} />}
                                                </button>
                                            </Tooltip>
                                        </div>
                                    }
                                />
                                <div className="input-submit-wrapper">
                                    <button
                                        type="submit"
                                        disabled={loading || (!input.trim() && !attachedImage)}
                                        className="chat-send-btn"
                                    >
                                        {loading ? <ThunderboltOutlined spin /> : <SendOutlined />}
                                        <span>{loading ? 'প্রসেস...' : 'পাঠান'}</span>
                                    </button>
                                </div>
                            </div>
                        </form>
                        
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '12px', padding: '0 8px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <div className="session-dot" style={{ background: 'var(--success)', boxShadow: '0 0 6px var(--success)', width: '5px', height: '5px' }} />
                                <span style={{ fontSize: '9px', fontWeight: 800, color: 'rgba(255, 255, 255, 0.25)', textTransform: 'uppercase', letterSpacing: '1.5px' }}>
                                    Security Level: High (Alpha-1)
                                </span>
                            </div>
                            <span style={{ fontSize: '9px', fontWeight: 800, color: 'rgba(255, 255, 255, 0.15)', textTransform: 'uppercase', letterSpacing: '1px' }}>
                                AI-Driven Autonomy System • Core v6.0 Stable
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Right Column: Knowledge Context */}
            <div className="neural-chat-knowledge-pane">
                <div className="knowledge-pane-title-row">
                    <DatabaseOutlined style={{ color: 'var(--neon-blue)', fontSize: '14px' }} />
                    <h4 style={{ fontSize: '11px', fontWeight: 800, color: '#ffffff', textTransform: 'uppercase', letterSpacing: '2px', margin: 0 }}>
                        সিস্টেম কনটেক্সট (Context)
                    </h4>
                </div>
                
                <div style={{ flex: 1, overflowY: 'auto' }} className="custom-scrollbar">
                    {knowledge.rules && knowledge.rules.length > 0 ? (
                        <div style={{ marginBottom: '24px' }}>
                            <div className="knowledge-section-header">
                                <span className="knowledge-section-title">সক্রিয় রুলস (Rules)</span>
                                <Badge count={knowledge.rules.length} style={{ backgroundColor: 'var(--neon-blue)', fontSize: '9px', fontWeight: 800, color: '#000', border: 'none' }} />
                            </div>
                            <div className="knowledge-cards-stack">
                                {knowledge.rules.map((r, i) => (
                                    <div key={i} className="knowledge-rule-card">
                                        {r.content || r.message}
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '40px 0', opacity: 0.15 }}>
                            <DatabaseOutlined style={{ fontSize: '28px', marginBottom: '12px' }} />
                            <span style={{ fontSize: '9px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '1px' }}>
                                কোন রুলস সক্রিয় নেই
                            </span>
                        </div>
                    )}
                </div>
            </div>

            {/* Rename Modal */}
            <Modal
                title={<span style={{ color: 'white', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '1px' }}>চ্যাট সেশনের নাম পরিবর্তন</span>}
                open={isRenameModalVisible}
                onOk={saveNewName}
                onCancel={() => setIsRenameModalVisible(false)}
                okText="নাম পরিবর্তন করুন"
                cancelText="বাতিল"
                centered
                className="dark-modal"
                styles={{ body: { backgroundColor: '#0a0a0a', borderBottomLeftRadius: '12px', borderBottomRightRadius: '12px' } }}
            >
                <div style={{ padding: '16px 0' }}>
                    <label style={{ display: 'block', fontSize: '9px', fontWeight: 800, color: 'rgba(255, 255, 255, 0.3)', textTransform: 'uppercase', letterSpacing: '2px', marginBottom: '8px' }}>
                        নতুন নাম লিখুন
                    </label>
                    <Input 
                        value={newName} 
                        onChange={(e) => setNewName(e.target.value)}
                        style={{
                            background: 'rgba(255, 255, 255, 0.05)',
                            border: '1px solid rgba(255, 255, 255, 0.1)',
                            color: 'white',
                            height: '46px',
                            borderRadius: '12px'
                        }}
                        placeholder="চ্যাটের নতুন নাম..."
                        onPressEnter={saveNewName}
                        autoFocus
                    />
                </div>
            </Modal>

            <style>{`
                .neural-chat-container {
                    display: flex;
                    flex: 1;
                    height: 100%;
                    overflow: hidden;
                    border-radius: var(--radius-lg);
                    background: var(--cyber-dark);
                }

                .neural-chat-sidebar {
                    width: 280px;
                    background: rgba(2, 2, 5, 0.65);
                    border-right: 1px solid rgba(0, 243, 255, 0.15);
                    display: flex;
                    flex-direction: column;
                    backdrop-filter: blur(20px);
                    flex-shrink: 0;
                }

                .sidebar-header {
                    padding: var(--space-3);
                    border-bottom: 1px solid rgba(0, 243, 255, 0.1);
                }

                .new-session-glow-btn:hover {
                    box-shadow: 0 0 25px rgba(0, 243, 255, 0.5) !important;
                    transform: translateY(-1px);
                }

                .sessions-list {
                    flex: 1;
                    overflow-y: auto;
                    padding: var(--space-2);
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                }

                .session-item {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 12px 16px;
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
                    border: 1px solid transparent;
                    position: relative;
                    overflow: hidden;
                }

                .session-item.active {
                    background: linear-gradient(90deg, rgba(0, 243, 255, 0.12), rgba(188, 19, 254, 0.03));
                    border-color: rgba(0, 243, 255, 0.3);
                    box-shadow: 0 0 15px rgba(0, 243, 255, 0.05);
                }

                .session-item:not(.active):hover {
                    background: rgba(255, 255, 255, 0.03);
                    border-color: rgba(255, 255, 255, 0.05);
                }

                .session-name-container {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    flex: 1;
                    overflow: hidden;
                }

                .session-dot {
                    width: 6px;
                    height: 6px;
                    border-radius: 50%;
                    background: rgba(255, 255, 255, 0.2);
                    transition: all 0.3s ease;
                }

                .session-item.active .session-dot {
                    background: var(--neon-blue);
                    box-shadow: 0 0 8px var(--neon-blue);
                }

                .session-name-text {
                    font-size: 13px;
                    font-weight: 600;
                    color: rgba(255, 255, 255, 0.5);
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    transition: all 0.3s ease;
                }

                .session-item.active .session-name-text {
                    color: #ffffff;
                }

                .session-actions {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    opacity: 0;
                    transition: opacity 0.25s ease;
                    padding-left: 8px;
                }

                .session-item:hover .session-actions {
                    opacity: 1;
                }

                .action-icon {
                    font-size: 13px;
                    color: rgba(255, 255, 255, 0.4);
                    transition: color 0.2s ease;
                    cursor: pointer;
                }

                .action-icon:hover {
                    color: var(--neon-blue);
                }

                .action-icon.delete:hover {
                    color: var(--error);
                }

                /* Chat Layout Content Pane */
                .neural-chat-content {
                    display: flex;
                    flex-direction: column;
                    flex: 1;
                    position: relative;
                    background: #030307;
                }

                .chat-content-header {
                    padding: 16px 24px;
                    background: rgba(2, 2, 5, 0.4);
                    border-bottom: 1px solid rgba(0, 243, 255, 0.15);
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    backdrop-filter: blur(10px);
                }

                .header-info-container {
                    display: flex;
                    align-items: center;
                    gap: 16px;
                }

                .header-icon-wrapper {
                    padding: 10px;
                    background: rgba(0, 243, 255, 0.1);
                    border-radius: 12px;
                    border: 1px solid rgba(0, 243, 255, 0.2);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .header-title-text {
                    font-size: 15px;
                    font-weight: 700;
                    color: #ffffff;
                    margin: 0 0 2px 0;
                }

                .header-subtitle-text {
                    font-size: 9px;
                    color: var(--neon-blue);
                    font-weight: 800;
                    text-transform: uppercase;
                    letter-spacing: 2px;
                }

                .custom-agent-select {
                    background: rgba(8, 8, 16, 0.8);
                    border: 1px solid rgba(0, 243, 255, 0.25);
                    color: rgba(255, 255, 255, 0.85);
                    font-size: 12px;
                    font-family: var(--font-mono);
                    padding: 8px 16px;
                    border-radius: 8px;
                    outline: none;
                    cursor: pointer;
                    transition: all 0.3s ease;
                }

                .custom-agent-select:hover {
                    border-color: rgba(0, 243, 255, 0.5);
                    box-shadow: 0 0 10px rgba(0, 243, 255, 0.15);
                }

                /* Chat Messages Display Area */
                .chat-messages-area {
                    flex: 1;
                    overflow-y: auto;
                    padding: 24px;
                }

                .messages-max-width-wrapper {
                    max-width: 800px;
                    margin: 0 auto;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    gap: 24px;
                }

                .message-row {
                    display: flex;
                    width: 100%;
                }

                .message-row.user {
                    justify-content: flex-end;
                }

                .message-row.ai {
                    justify-content: flex-start;
                }

                .message-bubble-wrapper {
                    max-width: 80%;
                    display: flex;
                    flex-direction: column;
                }

                .message-meta-header {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    margin-bottom: 6px;
                    padding: 0 4px;
                }

                .message-meta-header.user {
                    justify-content: flex-end;
                }

                .message-meta-header.ai {
                    justify-content: flex-start;
                }

                .meta-sender-name {
                    font-size: 10px;
                    font-weight: 800;
                    color: rgba(255, 255, 255, 0.35);
                    text-transform: uppercase;
                    letter-spacing: 1.5px;
                }

                .message-bubble {
                    padding: 16px 20px;
                    border-radius: 16px;
                    font-size: 14px;
                    line-height: 1.6;
                    box-shadow: 0 10px 25px -10px rgba(0, 0, 0, 0.5);
                    transition: all 0.3s ease;
                }

                .message-bubble.user {
                    background: linear-gradient(135deg, rgba(0, 243, 255, 0.15), rgba(188, 19, 254, 0.04));
                    border: 1px solid rgba(0, 243, 255, 0.25);
                    color: #ffffff;
                    border-top-right-radius: 2px;
                }

                .message-bubble.user:hover {
                    border-color: rgba(0, 243, 255, 0.4);
                    box-shadow: 0 10px 30px -10px rgba(0, 243, 255, 0.15);
                }

                .message-bubble.ai {
                    background: rgba(255, 255, 255, 0.03);
                    border: 1px solid rgba(255, 255, 255, 0.06);
                    color: rgba(255, 255, 255, 0.9);
                    border-top-left-radius: 2px;
                    backdrop-filter: blur(10px);
                }

                .message-bubble.ai:hover {
                    border-color: rgba(0, 243, 255, 0.15);
                    background: rgba(255, 255, 255, 0.04);
                }

                .message-bubble-footer {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-top: 12px;
                    padding-top: 10px;
                    border-top: 1px solid rgba(255, 255, 255, 0.05);
                }

                .bubble-footer-actions {
                    display: flex;
                    gap: 16px;
                }

                .footer-action-btn {
                    background: transparent;
                    border: none;
                    color: rgba(255, 255, 255, 0.35);
                    font-size: 10px;
                    font-weight: 700;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    transition: all 0.2s ease;
                    padding: 2px 0;
                }

                .footer-action-btn:hover {
                    color: var(--neon-blue);
                }

                .ai-confidence-badge {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    padding: 3px 8px;
                    background: rgba(0, 243, 255, 0.08);
                    border: 1px solid rgba(0, 243, 255, 0.2);
                    border-radius: 6px;
                }

                .ai-confidence-dot {
                    width: 5px;
                    height: 5px;
                    border-radius: 50%;
                    background: var(--neon-blue);
                    box-shadow: 0 0 6px var(--neon-blue);
                }

                .ai-confidence-text {
                    font-size: 9px;
                    font-weight: 800;
                    color: var(--neon-blue);
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }

                /* Chat Input Styling */
                .chat-input-wrapper {
                    padding: 24px;
                    background: linear-gradient(180deg, transparent, rgba(2, 2, 5, 0.95));
                    border-top: 1px solid rgba(255, 255, 255, 0.05);
                    position: relative;
                    z-index: 10;
                }

                .input-max-width-wrapper {
                    max-width: 800px;
                    margin: 0 auto;
                    width: 100%;
                }

                .image-preview-panel {
                    background: rgba(8, 8, 16, 0.85);
                    border: 1px solid rgba(0, 243, 255, 0.2);
                    border-radius: 12px;
                    padding: 12px 16px;
                    margin-bottom: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    backdrop-filter: blur(10px);
                }

                .preview-thumb-container {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }

                .preview-thumbnail {
                    width: 44px;
                    height: 44px;
                    border-radius: 8px;
                    object-fit: cover;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                }

                .preview-file-details {
                    display: flex;
                    flex-direction: column;
                }

                .preview-filename {
                    font-size: 12px;
                    font-weight: 700;
                    color: #ffffff;
                }

                .preview-badge {
                    font-size: 8px;
                    font-weight: 800;
                    color: var(--neon-blue);
                    letter-spacing: 1.5px;
                    margin-top: 2px;
                }

                .input-container-row {
                    display: flex;
                    gap: 12px;
                    align-items: center;
                }

                .reset-context-btn {
                    width: 56px;
                    height: 56px;
                    border-radius: 14px;
                    background: rgba(188, 19, 254, 0.1);
                    border: 1px solid rgba(188, 19, 254, 0.3);
                    color: var(--neon-purple);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    transition: all 0.3s ease;
                    flex-shrink: 0;
                }

                .reset-context-btn:hover {
                    background: rgba(188, 19, 254, 0.25);
                    border-color: var(--neon-purple);
                    box-shadow: 0 0 15px rgba(188, 19, 254, 0.2);
                    color: #ffffff;
                }

                .main-input-capsule {
                    flex: 1;
                    position: relative;
                    display: flex;
                    align-items: center;
                }

                .chat-styled-input {
                    background: rgba(2, 2, 5, 0.8) !important;
                    border: 1px solid rgba(0, 243, 255, 0.2) !important;
                    border-radius: 16px !important;
                    height: 56px !important;
                    font-size: 13px !important;
                    font-family: var(--font-mono) !important;
                    color: #ffffff !important;
                    padding-left: 100px !important;
                    padding-right: 140px !important;
                    transition: all 0.3s ease !important;
                }

                .chat-styled-input:focus, .chat-styled-input:hover {
                    border-color: rgba(0, 243, 255, 0.5) !important;
                    box-shadow: 0 0 15px rgba(0, 243, 255, 0.15) !important;
                }

                .chat-input-actions-prefix {
                    position: absolute;
                    left: 8px;
                    top: 50%;
                    transform: translateY(-50%);
                    display: flex;
                    align-items: center;
                    gap: 4px;
                    z-index: 5;
                    border-right: 1px solid rgba(0, 243, 255, 0.15);
                    padding-right: 8px;
                }

                .prefix-action-btn {
                    background: transparent;
                    border: none;
                    width: 36px;
                    height: 36px;
                    border-radius: 10px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: rgba(0, 243, 255, 0.5);
                    cursor: pointer;
                    transition: all 0.2s ease;
                }

                .prefix-action-btn:hover {
                    background: rgba(0, 243, 255, 0.1);
                    color: var(--neon-blue);
                }

                .prefix-action-btn.recording {
                    background: rgba(239, 68, 68, 0.15);
                    color: #ef4444;
                    animation: recordingPulse 1.5s infinite ease-in-out;
                }

                @keyframes recordingPulse {
                    0%, 100% { opacity: 0.8; }
                    50% { opacity: 1; box-shadow: 0 0 10px rgba(239, 68, 68, 0.2); }
                }

                .input-submit-wrapper {
                    position: absolute;
                    right: 8px;
                    top: 50%;
                    transform: translateY(-50%);
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    z-index: 5;
                }

                .chat-send-btn {
                    height: 40px;
                    padding: 0 20px;
                    background: var(--neon-blue);
                    border: none;
                    border-radius: 10px;
                    color: #020205;
                    font-weight: 800;
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 1.5px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                }

                .chat-send-btn:hover:not(:disabled) {
                    background: #ffffff;
                    box-shadow: 0 0 15px rgba(0, 243, 255, 0.4);
                    transform: translateY(-1px);
                }

                .chat-send-btn:disabled {
                    background: rgba(255, 255, 255, 0.05);
                    color: rgba(255, 255, 255, 0.15);
                    cursor: not-allowed;
                }

                /* Right Sidebar: Knowledge Context styling */
                .neural-chat-knowledge-pane {
                    width: 320px;
                    background: rgba(2, 2, 5, 0.3);
                    border-left: 1px solid rgba(0, 243, 255, 0.15);
                    display: flex;
                    flex-direction: column;
                    padding: 24px;
                    flex-shrink: 0;
                }

                .knowledge-pane-title-row {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    margin-bottom: 24px;
                }

                .knowledge-section-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 16px;
                }

                .knowledge-section-title {
                    font-size: 10px;
                    font-weight: 800;
                    color: rgba(255, 255, 255, 0.35);
                    text-transform: uppercase;
                    letter-spacing: 2px;
                }

                .knowledge-cards-stack {
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                }

                .knowledge-rule-card {
                    background: rgba(255, 255, 255, 0.02);
                    border: 1px solid rgba(255, 255, 255, 0.05);
                    border-radius: 12px;
                    padding: 14px;
                    font-size: 11.5px;
                    line-height: 1.5;
                    color: rgba(255, 255, 255, 0.65);
                    transition: all 0.25s ease;
                }

                .knowledge-rule-card:hover {
                    background: rgba(255, 255, 255, 0.04);
                    border-color: rgba(0, 243, 255, 0.15);
                    color: #ffffff;
                }

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
                    background: rgba(0, 243, 255, 0.2);
                }
                .dark-modal .ant-modal-content {
                    background-color: #05050a !important;
                    border: 1px solid rgba(255, 255, 255, 0.1) !important;
                    border-radius: 16px !important;
                    overflow: hidden !important;
                }
                .dark-modal .ant-modal-header {
                    background-color: #05050a !important;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
                }
                .dark-modal .ant-modal-title {
                    color: white !important;
                }
                .dark-modal .ant-modal-close-x {
                    color: rgba(255, 255, 255, 0.4) !important;
                }
                .dark-modal .ant-btn-primary {
                    background-color: var(--neon-blue) !important;
                    color: #000 !important;
                    font-weight: bold !important;
                    border: none !important;
                }
                .dark-modal .ant-btn-default {
                    background-color: transparent !important;
                    border: 1px solid rgba(255, 255, 255, 0.1) !important;
                    color: white !important;
                }
            `}</style>
        </div>
    );
};

export default ChatWithAI;