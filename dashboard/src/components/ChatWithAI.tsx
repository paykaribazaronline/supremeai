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
  LoadingOutlined,
  LockOutlined,
} from "@ant-design/icons";
import { Input, Button, message, Tooltip, Modal, Badge } from "antd";
import React, { useState, useRef, useEffect } from "react";
import ReactMarkdown from "react-markdown";

import { authUtils } from "../lib/authUtils";
import "./ChatWithAI.css";

interface ChatMessage {
  id: string;
  sender: "user" | "ai";
  agent: string;
  content: string;
  timestamp: string;
  confidence?: number;
  intent?: string;
  status?: "pending" | "completed" | "error";
  image?: string;
  options?: string[];
  type?: string;
}

interface ChatSession {
  id: string;
  name: string;
  messages: ChatMessage[];
  createdAt: string;
}

interface Agent {
  id: string;
  name: string;
  status: string;
  type: string;
}

interface Rule {
  content?: string;
  message?: string;
}

interface Plan {
  id?: string;
  name?: string;
}

interface Action {
  id?: string;
  name?: string;
}

interface SpeechRecognitionEvent extends Event {
  resultIndex: number;
  results: {
    [index: number]: {
      [index: number]: {
        transcript: string;
      };
      isFinal: boolean;
    };
    length: number;
  };
}

interface SpeechRecognitionErrorEvent extends Event {
  error: string;
}

interface ISpeechRecognition {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onresult: (event: SpeechRecognitionEvent) => void;
  onerror: (event: SpeechRecognitionErrorEvent) => void;
  onend: () => void;
  start: () => void;
  stop: () => void;
}

interface ChatWithAIProps {
  chatFont?: string;
}

const ChatWithAI: React.FC<ChatWithAIProps> = ({ chatFont = "font-mono" }) => {
  // Session State
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [isRenameModalVisible, setIsRenameModalVisible] = useState(false);
  const [sessionToRename, setSessionToRename] = useState<ChatSession | null>(
    null,
  );
  const [newName, setNewName] = useState("");

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [selectedAgent, setSelectedAgent] = useState("all");
  const [agents, setAgents] = useState<Agent[]>([]);
  const [knowledge, setKnowledge] = useState<{
    rules: Rule[];
    plans: Plan[];
    actions: Action[];
  }>({ rules: [], plans: [], actions: [] });
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Voice & Image States
  const [isRecording, setIsRecording] = useState(false);
  const [recognition, setRecognition] = useState<ISpeechRecognition | null>(
    null,
  );
  const [attachedImage, setAttachedImage] = useState<string | null>(null);
  const [attachedImageName, setAttachedImageName] = useState<string>("");

  // Speech Recognition setup on mount
  useEffect(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition ||
      (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recog = new SpeechRecognition();
      recog.continuous = true;
      recog.interimResults = true;
      recog.lang = "bn-BD"; // Support Bengali by default

      recog.onresult = (event: SpeechRecognitionEvent) => {
        let interimTranscript = "";
        let finalTranscript = "";
        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTranscript += event.results[i][0].transcript;
          } else {
            interimTranscript += event.results[i][0].transcript;
          }
        }
        const transcription = finalTranscript || interimTranscript;
        if (transcription.trim()) {
          setInput((prev) => {
            if (prev.endsWith(" ") || prev === "") {
              return prev + transcription;
            } else {
              return prev + " " + transcription;
            }
          });
        }
      };

      recog.onerror = (event: SpeechRecognitionErrorEvent) => {
        console.error("Speech recognition error:", event.error);
        if (event.error === "not-allowed") {
          message.error("মাইক্রোফোন অ্যাক্সেসের অনুমতি নেই।");
        } else {
          message.error("ভয়েস সনাক্তকরণে সমস্যা হয়েছে: " + event.error);
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
      message.warning(
        "আপনার ব্রাউজার ভয়েস সনাক্তকরণ সমর্থন করে না। Google Chrome ব্যবহার করার চেষ্টা করুন।",
      );
      return;
    }

    if (isRecording) {
      recognition.stop();
      setIsRecording(false);
      message.info("ভয়েস ইনপুট বন্ধ করা হয়েছে।");
    } else {
      try {
        recognition.start();
        setIsRecording(true);
        message.success("ভয়েস ইনপুট সক্রিয় হয়েছে... কথা বলুন।");
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
      message.error("ফাইলের সাইজ ৪ মেগাবাইটের কম হতে হবে।");
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === "string") {
        setAttachedImage(reader.result);
        setAttachedImageName(file.name);
        message.success("ছবি সংযুক্ত করা হয়েছে।");
      }
    };
    reader.onerror = () => {
      message.error("ছবি প্রসেস করতে ত্রুটি হয়েছে।");
    };
    reader.readAsDataURL(file);
  };

  const removeAttachedImage = () => {
    setAttachedImage(null);
    setAttachedImageName("");
    message.info("সংযুক্ত ছবি মুছে ফেলা হয়েছে।");
  };

  // Encryption helpers
  const getCryptoKey = async (): Promise<CryptoKey | null> => {
    const raw = sessionStorage.getItem("supremeai_crypto_key");
    if (!raw) return null;
    try {
      return await crypto.subtle.importKey(
        "jwk",
        JSON.parse(raw),
        { name: "AES-GCM", length: 256 },
        true,
        ["encrypt", "decrypt"],
      );
    } catch {
      return null;
    }
  };

  const storeCryptoKey = async (key: CryptoKey) => {
    const jwk = await crypto.subtle.exportKey("jwk", key);
    sessionStorage.setItem("supremeai_crypto_key", JSON.stringify(jwk));
  };

  const encryptPayload = async (payload: unknown): Promise<string> => {
    try {
      let key = await getCryptoKey();
      if (!key) {
        key = await crypto.subtle.generateKey(
          { name: "AES-GCM", length: 256 },
          true,
          ["encrypt", "decrypt"],
        );
        await storeCryptoKey(key);
      }
      const iv = crypto.getRandomValues(new Uint8Array(12));
      const encoded = new TextEncoder().encode(JSON.stringify(payload));
      const cipher = await crypto.subtle.encrypt(
        { name: "AES-GCM", iv },
        key,
        encoded,
      );
      const combined = new Uint8Array(iv.length + cipher.byteLength);
      combined.set(iv, 0);
      combined.set(new Uint8Array(cipher), iv.length);
      return btoa(String.fromCharCode(...combined));
    } catch {
      return btoa(encodeURIComponent(JSON.stringify(payload)));
    }
  };

  const decryptPayload = async (token: string): Promise<unknown> => {
    try {
      const combined = Uint8Array.from(atob(token), (c) => c.charCodeAt(0));
      const key = await getCryptoKey();
      if (!key) throw new Error("missing_key");
      const iv = combined.slice(0, 12);
      const cipher = combined.slice(12);
      const plain = await crypto.subtle.decrypt(
        { name: "AES-GCM", iv },
        key,
        cipher,
      );
      return JSON.parse(new TextDecoder().decode(plain));
    } catch {
      return JSON.parse(decodeURIComponent(atob(token)));
    }
  };

  const sessionStorageKey = "supremeai_chat_sessions_v1";

  const loadSessions = async (): Promise<ChatSession[]> => {
    try {
      const raw = sessionStorage.getItem(sessionStorageKey);
      if (!raw) return [];
      const data = (await decryptPayload(raw)) as ChatSession[];
      if (Array.isArray(data)) return data;
    } catch (e) {
      console.error("Failed to load sessions:", e);
    }
    return [];
  };

  const saveSessions = async (sessions: ChatSession[]) => {
    try {
      const encrypted = await encryptPayload(sessions);
      sessionStorage.setItem(sessionStorageKey, encrypted);
    } catch (e) {
      console.error("Failed to save sessions:", e);
    }
  };

  // Load sessions from sessionStorage on mount
  useEffect(() => {
    let cancelled = false;
    const bootstrap = async () => {
      const saved = await loadSessions();
      if (cancelled) return;
      setSessions(saved);
      if (saved.length > 0) {
        setActiveSessionId(saved[0].id);
      } else {
        createNewSession();
      }
      fetchAgents();
      fetchKnowledge();
    };
    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  // Save sessions to sessionStorage whenever they change
  useEffect(() => {
    if (sessions.length >= 0) {
      saveSessions(sessions);
    }
  }, [sessions]);

  const activeSession = sessions.find((s) => s.id === activeSessionId);
  const messages = activeSession?.messages || [];

  const createNewSession = () => {
    const newSession: ChatSession = {
      id: Date.now().toString(),
      name: "নতুন চ্যাট সেশন",
      messages: [],
      createdAt: new Date().toISOString(),
    };
    setSessions((prev) => [newSession, ...prev]);
    setActiveSessionId(newSession.id);
  };

  const deleteSession = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const filtered = sessions.filter((s) => s.id !== id);
    setSessions(filtered);
    if (activeSessionId === id) {
      setActiveSessionId(filtered.length > 0 ? filtered[0].id : null);
      if (filtered.length === 0) {
        setTimeout(() => {
          if (filtered.length === 0) createNewSession();
        }, 0);
      }
    }
    message.success("চ্যাট সেশন মুছে ফেলা হয়েছে");
  };

  const handleRename = (session: ChatSession, e: React.MouseEvent) => {
    e.stopPropagation();
    setSessionToRename(session);
    setNewName(session.name);
    setIsRenameModalVisible(true);
  };

  const saveNewName = () => {
    if (sessionToRename && newName.trim()) {
      setSessions((prev) =>
        prev.map((s) =>
          s.id === sessionToRename.id ? { ...s, name: newName.trim() } : s,
        ),
      );
      setIsRenameModalVisible(false);
      message.success("চ্যাটের নাম পরিবর্তন করা হয়েছে");
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
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

      const response = await authUtils.fetchWithAuth(
        "/api/admin/providers/configured",
      );
      if (response.ok) {
        const result = await response.json();
        const rawData =
          result.data?.providers ||
          (Array.isArray(result.data) ? result.data : []);

        const mappedAgents = rawData.map(
          (p: {
            id: string;
            name: string;
            status?: string;
            type?: string;
          }) => ({
            id: p.id,
            name: p.name,
            status: p.status || "online",
            type: p.type || "llm",
          }),
        );

        setAgents(mappedAgents);
      }
    } catch (error) {
      console.error("Failed to fetch agents");
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
        authUtils.fetchWithAuth("/api/admin/rules").catch(() => null),
        authUtils.fetchWithAuth("/api/admin/plans").catch(() => null),
        authUtils
          .fetchWithAuth("/api/admin/chat/actions/pending")
          .catch(() => null),
      ]);
      const rules = rulesRes?.ok ? await rulesRes.json() : [];
      const plans = plansRes?.ok ? await plansRes.json() : [];
      const actions = actionsRes?.ok ? await actionsRes.json() : [];
      setKnowledge({
        rules: Array.isArray(rules) ? rules.slice(0, 5) : [],
        plans: Array.isArray(plans) ? plans.slice(0, 5) : [],
        actions: Array.isArray(actions) ? actions.slice(0, 5) : [],
      });
    } catch (error) {
      console.error("Failed to fetch knowledge context");
    }
  };

  const sendMessageToServer = async (messageText: string) => {
    if ((!messageText.trim() && !attachedImage) || loading || !activeSessionId)
      return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      sender: "user",
      agent: "You",
      content: messageText,
      image: attachedImage || undefined,
      timestamp: new Date().toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      }),
      status: "completed",
    };

    // Update session messages
    const currentSessions = [...sessions];
    const sessionIndex = currentSessions.findIndex(
      (s) => s.id === activeSessionId,
    );
    if (sessionIndex !== -1) {
      currentSessions[sessionIndex].messages.push(userMessage);

      // Smart Name: If first message or default name, generate name
      if (
        currentSessions[sessionIndex].messages.length === 1 ||
        currentSessions[sessionIndex].name === "New Chat" ||
        currentSessions[sessionIndex].name === "নতুন চ্যাট সেশন"
      ) {
        const words = messageText.trim()
          ? messageText.split(" ")
          : ["সংযুক্ত ছবি"];
        currentSessions[sessionIndex].name =
          words.slice(0, 4).join(" ") + (words.length > 4 ? "..." : "");
      }

      setSessions(currentSessions);
    }

    const currentImage = attachedImage;
    const currentImageName = attachedImageName;

    setInput("");
    setAttachedImage(null);
    setAttachedImageName("");
    setLoading(true);

    try {
      const currentSession = sessions.find((s) => s.id === activeSessionId);
      const history = currentSession ? currentSession.messages : [];

      const messageBody = currentImage
        ? `${messageText}\n\n[সংযুক্ত ছবি: ${currentImageName}]\n![${currentImageName}](${currentImage})`
        : messageText;

      // 🧠 Intent Classifier: Route between Tiny Hybrid and GODMODE 3
      const criticalKeywords = [
        "code",
        "analyze",
        "hack",
        "critical",
        "error",
        "debug",
        "explain",
        "complex",
        "why",
        "কীভাবে",
        "কেন",
        "কোড",
        "বিশ্লেষণ",
        "সমস্যা",
      ];
      const isCritical = criticalKeywords.some((kw) =>
        messageText.toLowerCase().includes(kw),
      );

      // If critical, we route to GODMODE (represented by 'all' models or a special tag)
      const routingAgentId = isCritical
        ? "all"
        : selectedAgent === "all"
          ? null
          : selectedAgent;

      const response = await authUtils.fetchWithAuth("/api/chat/send", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: isCritical
            ? `[GODMODE 3 CRITICAL REQUEST]\n${messageBody}`
            : messageBody,
          agentId: routingAgentId,
          sessionId: activeSessionId,
          messages: history,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        const aiMessage: ChatMessage = {
          id: (Date.now() + 1).toString(),
          sender: "ai",
          agent: isCritical
            ? `🔥 GODMODE 3 (${data.agent_name || "Multi-Model"})`
            : `Tiny Hybrid (${data.agent_name || "Core"})`,
          content: data.message || "Processing optimized.",
          timestamp: new Date().toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          }),
          confidence: data.confidence
            ? Math.round(data.confidence * 100)
            : isCritical
              ? 99
              : 88,
          intent: isCritical ? "CRITICAL_ROUTING" : "NORMAL_ROUTING",
          status: "completed",
          type: data.type,
          options: data.options || [],
        };

        setSessions((prev) =>
          prev.map((s) =>
            s.id === activeSessionId
              ? { ...s, messages: [...s.messages, aiMessage] }
              : s,
          ),
        );
      } else {
        // API failed - provide local fallback response
        let errorMsg = `সিস্টেম প্রক্রিয়াকরণে সমস্যা হয়েছে (Error ${response.status}: ${response.statusText})। অনুগ্রহ করে আবার চেষ্টা করুন বা ব্যাকএন্ড লগ পরীক্ষা করুন।`;
        try {
          const data = await response.json();
          if (data && data.message)
            errorMsg = `Error ${response.status}: ${data.message}`;
          else if (data && data.error)
            errorMsg = `Error ${response.status}: ${data.error}`;
        } catch (e) {
          console.error("Failed to parse API error response JSON:", e);
        }

        const fallbackMessage: ChatMessage = {
          id: (Date.now() + 1).toString(),
          sender: "ai",
          agent: "সিস্টেম রেসপন্স (Error)",
          content: errorMsg,
          timestamp: new Date().toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          }),
          status: "error",
        };

        setSessions((prev) =>
          prev.map((s) =>
            s.id === activeSessionId
              ? { ...s, messages: [...s.messages, fallbackMessage] }
              : s,
          ),
        );
      }
    } catch (error: any) {
      // Network error - provide local fallback response
      const fallbackMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: "ai",
        agent: "লোকাল নেটওয়ার্ক",
        content: `সার্ভারের সাথে সংযোগ স্থাপন করা যাচ্ছে না। ত্রুটি: ${error.message || "Unknown network error"}`,
        timestamp: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
        status: "completed",
      };

      setSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionId
            ? { ...s, messages: [...s.messages, fallbackMessage] }
            : s,
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    await sendMessageToServer(input);
  };

  const playVoice = async (text: string) => {
    try {
      const response = await authUtils.fetchWithAuth("/api/voicebox/speak", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text, profile: "default" }),
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
          utterance.lang = "bn-BD";
          window.speechSynthesis.speak(utterance);
        }
      } else {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = "bn-BD";
        window.speechSynthesis.speak(utterance);
      }
    } catch (err) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "bn-BD";
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
              background:
                "linear-gradient(135deg, var(--neon-blue), var(--neon-purple))",
              border: "none",
              height: "46px",
              borderRadius: "12px",
              fontWeight: 800,
              fontSize: "11px",
              letterSpacing: "1px",
              textTransform: "uppercase",
              color: "#ffffff",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: "8px",
              boxShadow: "0 0 15px rgba(0, 243, 255, 0.25)",
            }}
          >
            নতুন নিউরাল চ্যাট
          </Button>
        </div>
        <div className="sessions-list custom-scrollbar">
          {sessions.map((s) => (
            <div
              key={s.id}
              onClick={() => setActiveSessionId(s.id)}
              className={`session-item ${activeSessionId === s.id ? "active" : ""}`}
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
              <RobotOutlined
                style={{ color: "var(--neon-blue)", fontSize: "20px" }}
              />
            </div>
            <div>
              <h3 className="header-title-text">
                {activeSession?.name || "নিউরাল চ্যাট (Neural Chat)"}
              </h3>
              <span className="header-subtitle-text">
                SUPREMEAI NEURAL CORE SYSTEM
              </span>
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
            <span
              style={{
                fontSize: "11px",
                color: "rgba(255, 255, 255, 0.4)",
                fontWeight: 600,
                textTransform: "uppercase",
                letterSpacing: "1px",
              }}
            >
              মডেল সিলেক্ট করুন:
            </span>
            <select
              value={selectedAgent}
              onChange={(e) => setSelectedAgent(e.target.value)}
              className="custom-agent-select"
            >
              <option value="all">Dynamic Routing (All Models)</option>
              {agents.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Chat Messages Area */}
        <div className="chat-messages-area custom-scrollbar">
          {messages.length === 0 ? (
            <div
              style={{
                height: "60vh",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                color: "rgba(255,255,255,0.15)",
              }}
            >
              <div
                style={{
                  width: "80px",
                  height: "80px",
                  background: "rgba(0, 243, 255, 0.05)",
                  borderRadius: "24px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  border: "1px solid rgba(0, 243, 255, 0.15)",
                  boxShadow: "0 0 30px rgba(0, 243, 255, 0.05)",
                  marginBottom: "20px",
                  position: "relative",
                }}
              >
                <MessageOutlined
                  style={{ fontSize: "32px", color: "var(--neon-blue)" }}
                />
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    border: "1.5px solid var(--neon-blue)",
                    borderRadius: "24px",
                    opacity: 0.3,
                    animation: "pulse 2s infinite ease-in-out",
                  }}
                />
              </div>
              <span
                style={{
                  fontSize: "10px",
                  fontWeight: 800,
                  letterSpacing: "4px",
                  textTransform: "uppercase",
                  color: "var(--neon-blue)",
                  opacity: 0.6,
                }}
              >
                নিউরাল সিঙ্ক সেশন চালু হয়েছে
              </span>
            </div>
          ) : (
            <div className="messages-max-width-wrapper">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`message-row ${msg.sender === "user" ? "user" : "ai"}`}
                >
                  <div className="message-bubble-wrapper">
                    <div
                      className={`message-meta-header ${msg.sender === "user" ? "user" : "ai"}`}
                    >
                      <span className="meta-sender-name">
                        {msg.sender === "ai"
                          ? msg.agent
                          : "অনুমোদিত অপারেটর (Operator)"}{" "}
                        • {msg.timestamp}
                      </span>
                    </div>
                    <div
                      className={`message-bubble ${msg.sender === "user" ? "user" : "ai"}`}
                    >
                      {msg.image && (
                        <div
                          style={{
                            marginBottom: "12px",
                            borderRadius: "12px",
                            overflow: "hidden",
                            border: "1px solid rgba(255, 255, 255, 0.1)",
                            maxWidth: "320px",
                            cursor: "pointer",
                          }}
                        >
                          <img
                            src={msg.image}
                            alt="Attached file"
                            style={{
                              width: "100%",
                              height: "auto",
                              display: "block",
                              transition: "transform 0.3s",
                            }}
                            onClick={() => {
                              Modal.info({
                                title: (
                                  <span
                                    style={{
                                      color: "white",
                                      fontWeight: 700,
                                      textTransform: "uppercase",
                                      letterSpacing: "1px",
                                    }}
                                  >
                                    সংযুক্ত ছবি প্রিভিউ
                                  </span>
                                ),
                                icon: null,
                                width: 800,
                                centered: true,
                                content: (
                                  <div
                                    style={{
                                      display: "flex",
                                      justifyContent: "center",
                                      padding: "12px",
                                      background: "#08080f",
                                      borderRadius: "12px",
                                      border:
                                        "1px solid rgba(255, 255, 255, 0.1)",
                                      marginTop: "16px",
                                    }}
                                  >
                                    <img
                                      src={msg.image}
                                      alt="Preview"
                                      style={{
                                        maxWidth: "100%",
                                        maxHeight: "70vh",
                                        borderRadius: "8px",
                                        objectFit: "contain",
                                      }}
                                    />
                                  </div>
                                ),
                                okText: "বন্ধ করুন",
                                okButtonProps: {
                                  style: {
                                    background: "var(--neon-blue)",
                                    border: "none",
                                    color: "#000",
                                    fontWeight: "bold",
                                  },
                                },
                                styles: {
                                  body: {
                                    backgroundColor: "#05050a",
                                    color: "white",
                                  },
                                },
                                className: "dark-modal",
                              });
                            }}
                          />
                        </div>
                      )}
                      <div style={{ wordBreak: "break-word" }}>
                        <ReactMarkdown>{msg.content}</ReactMarkdown>
                      </div>

                      {msg.type === "CLARIFICATION_REQUIRED" &&
                        msg.options &&
                        msg.options.length > 0 && (
                          <div
                            style={{
                              marginTop: "16px",
                              display: "flex",
                              flexDirection: "column",
                              gap: "10px",
                            }}
                          >
                            <span
                              style={{
                                fontSize: "12px",
                                fontWeight: 600,
                                color: "rgba(255,255,255,0.6)",
                              }}
                            >
                              সম্ভাব্য অপশনগুলো থেকে বেছে নিন:
                            </span>
                            <div
                              style={{
                                display: "flex",
                                flexWrap: "wrap",
                                gap: "8px",
                              }}
                            >
                              {msg.options.map((opt, idx) => (
                                <button
                                  key={idx}
                                  onClick={() => sendMessageToServer(opt)}
                                  style={{
                                    background: "rgba(0, 243, 255, 0.05)",
                                    border: "1px solid rgba(0, 243, 255, 0.2)",
                                    color: "var(--neon-blue)",
                                    padding: "8px 14px",
                                    borderRadius: "8px",
                                    fontSize: "13px",
                                    cursor: "pointer",
                                    transition: "all 0.2s",
                                    fontWeight: 500,
                                  }}
                                  onMouseEnter={(e) => {
                                    e.currentTarget.style.background =
                                      "rgba(0, 243, 255, 0.15)";
                                    e.currentTarget.style.borderColor =
                                      "var(--neon-blue)";
                                    e.currentTarget.style.boxShadow =
                                      "0 0 10px rgba(0, 243, 255, 0.3)";
                                  }}
                                  onMouseLeave={(e) => {
                                    e.currentTarget.style.background =
                                      "rgba(0, 243, 255, 0.05)";
                                    e.currentTarget.style.borderColor =
                                      "rgba(0, 243, 255, 0.2)";
                                    e.currentTarget.style.boxShadow = "none";
                                  }}
                                >
                                  {opt}
                                </button>
                              ))}
                            </div>
                            <span
                              style={{
                                fontSize: "11px",
                                color: "rgba(255,255,255,0.4)",
                                fontStyle: "italic",
                                marginTop: "4px",
                              }}
                            >
                              *অথবা নিচের ইনপুট বক্সে আপনার নিজের মতো করে কাস্টম
                              উত্তর টাইপ করুন।
                            </span>
                          </div>
                        )}

                      {msg.sender === "ai" && (
                        <div className="message-bubble-footer">
                          <div className="bubble-footer-actions">
                            <button
                              onClick={() => {
                                navigator.clipboard.writeText(msg.content);
                                message.success("টেক্সট কপি করা হয়েছে");
                              }}
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
                  <img
                    src={attachedImage}
                    alt="Upload preview"
                    className="preview-thumbnail"
                  />
                  <div className="preview-file-details">
                    <span className="preview-filename">
                      {attachedImageName || "image.png"}
                    </span>
                    <span className="preview-badge">সংযুক্ত ছবি প্রস্তুত</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={removeAttachedImage}
                  style={{
                    background: "transparent",
                    border: "none",
                    color: "rgba(255, 255, 255, 0.4)",
                    cursor: "pointer",
                    fontSize: "16px",
                  }}
                >
                  <CloseCircleOutlined style={{ color: "var(--error)" }} />
                </button>
              </div>
            )}
            <form onSubmit={handleSendMessage} className="input-container-row">
              <button
                type="button"
                onClick={() =>
                  setSessions((prev) =>
                    prev.map((s) =>
                      s.id === activeSessionId ? { ...s, messages: [] } : s,
                    ),
                  )
                }
                className="reset-context-btn"
                title="চ্যাট রিসেট করুন (Reset Context)"
              >
                <ThunderboltOutlined style={{ fontSize: "20px" }} />
              </button>

              <div className="main-input-capsule">
                <input
                  type="file"
                  id="chat-image-upload"
                  accept="image/*"
                  onChange={handleImageUpload}
                  style={{ display: "none" }}
                />
                <Input
                  placeholder={
                    isRecording
                      ? "ভয়েস রেকর্ড করা হচ্ছে... কথা বলুন..."
                      : "নিউরাল কমান্ড টাইপ করুন..."
                  }
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  disabled={loading}
                  className="chat-styled-input"
                  prefix={
                    <div className="chat-input-actions-prefix">
                      <Tooltip title="ছবি সংযুক্ত করুন">
                        <button
                          type="button"
                          onClick={() =>
                            document
                              .getElementById("chat-image-upload")
                              ?.click()
                          }
                          className="prefix-action-btn"
                        >
                          <PictureOutlined style={{ fontSize: "16px" }} />
                        </button>
                      </Tooltip>
                      <Tooltip
                        title={
                          isRecording ? "রেকর্ডিং বন্ধ করুন" : "ভয়েস ইনপুট"
                        }
                      >
                        <button
                          type="button"
                          onClick={toggleRecording}
                          className={`prefix-action-btn ${isRecording ? "recording" : ""}`}
                        >
                          {isRecording ? (
                            <LoadingOutlined style={{ fontSize: "16px" }} />
                          ) : (
                            <AudioOutlined style={{ fontSize: "16px" }} />
                          )}
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
                    <span>{loading ? "প্রসেস..." : "পাঠান"}</span>
                  </button>
                </div>
              </div>
            </form>

            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                marginTop: "12px",
                padding: "0 8px",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "8px" }}
              >
                <div
                  className="session-dot"
                  style={{
                    background: "var(--success)",
                    boxShadow: "0 0 6px var(--success)",
                    width: "5px",
                    height: "5px",
                  }}
                />
                <span
                  style={{
                    fontSize: "9px",
                    fontWeight: 800,
                    color: "rgba(255, 255, 255, 0.25)",
                    textTransform: "uppercase",
                    letterSpacing: "1.5px",
                  }}
                >
                  Security Level: High (Alpha-1)
                </span>
              </div>
              <span
                style={{
                  fontSize: "9px",
                  fontWeight: 800,
                  color: "rgba(255, 255, 255, 0.15)",
                  textTransform: "uppercase",
                  letterSpacing: "1px",
                }}
              >
                AI-Driven Autonomy System • Core v6.0 Stable
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Right Column: Knowledge Context */}
      <div className="neural-chat-knowledge-pane">
        <div className="knowledge-pane-title-row">
          <DatabaseOutlined
            style={{ color: "var(--neon-blue)", fontSize: "14px" }}
          />
          <h4
            style={{
              fontSize: "11px",
              fontWeight: 800,
              color: "#ffffff",
              textTransform: "uppercase",
              letterSpacing: "2px",
              margin: 0,
            }}
          >
            সিস্টেম কনটেক্সট (Context)
          </h4>
        </div>

        <div
          style={{ flex: 1, overflowY: "auto" }}
          className="custom-scrollbar"
        >
          {knowledge.rules && knowledge.rules.length > 0 ? (
            <div style={{ marginBottom: "24px" }}>
              <div className="knowledge-section-header">
                <span className="knowledge-section-title">
                  সক্রিয় রুলস (Rules)
                </span>
                <Badge
                  count={knowledge.rules.length}
                  style={{
                    backgroundColor: "var(--neon-blue)",
                    fontSize: "9px",
                    fontWeight: 800,
                    color: "#000",
                    border: "none",
                  }}
                />
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
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                padding: "40px 0",
                opacity: 0.15,
              }}
            >
              <DatabaseOutlined
                style={{ fontSize: "28px", marginBottom: "12px" }}
              />
              <span
                style={{
                  fontSize: "9px",
                  fontWeight: 700,
                  textTransform: "uppercase",
                  letterSpacing: "1px",
                }}
              >
                কোন রুলস সক্রিয় নেই
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Rename Modal */}
      <Modal
        title={
          <span
            style={{
              color: "white",
              fontWeight: 800,
              textTransform: "uppercase",
              letterSpacing: "1px",
            }}
          >
            চ্যাট সেশনের নাম পরিবর্তন
          </span>
        }
        open={isRenameModalVisible}
        onOk={saveNewName}
        onCancel={() => setIsRenameModalVisible(false)}
        okText="নাম পরিবর্তন করুন"
        cancelText="বাতিল"
        centered
        className="dark-modal"
        styles={{
          body: {
            backgroundColor: "#0a0a0a",
            borderBottomLeftRadius: "12px",
            borderBottomRightRadius: "12px",
          },
        }}
      >
        <div style={{ padding: "16px 0" }}>
          <label
            style={{
              display: "block",
              fontSize: "9px",
              fontWeight: 800,
              color: "rgba(255, 255, 255, 0.3)",
              textTransform: "uppercase",
              letterSpacing: "2px",
              marginBottom: "8px",
            }}
          >
            নতুন নাম লিখুন
          </label>
          <Input
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            style={{
              background: "rgba(255, 255, 255, 0.05)",
              border: "1px solid rgba(255, 255, 255, 0.1)",
              color: "white",
              height: "46px",
              borderRadius: "12px",
            }}
            placeholder="চ্যাটের নতুন নাম..."
            onPressEnter={saveNewName}
            autoFocus
          />
        </div>
      </Modal>
    </div>
  );
};

export default ChatWithAI;
