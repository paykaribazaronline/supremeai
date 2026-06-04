import { OrbitControls, Stars, PerspectiveCamera } from "@react-three/drei";
import { Canvas } from "@react-three/fiber";
import { Spin } from "antd";
import { motion } from "framer-motion";
import {
  Cpu,
  Shield,
  Zap,
  Terminal as TerminalIcon,
  Globe,
} from "lucide-react";
import React, { useState, useEffect, Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { CoreEngine } from "./components/CoreEngine";
import { allMenuItems } from "./components/dashboard/DashboardConfigs";
import ErrorBoundary from "./components/ErrorBoundary";
import FeedbackSystem from "./components/FeedbackSystem";

// Lazy load pages for performance optimization
const ModernAdminDashboard = lazy(() => import("./pages/ModernAdminDashboard"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const AdminRouteLayout = lazy(() => import("./components/AdminRouteLayout"));

// Lazy load admin pages
const DashboardHome = lazy(
  () => import("./components/dashboard/DashboardHome"),
);
const ChatWithAI = lazy(() => import("./components/ChatWithAI"));
const AdminProjects = lazy(() => import("./pages/AdminProjects"));
const AdminProviders = lazy(() => import("./pages/AdminProviders"));
const AdminUsers = lazy(() => import("./pages/AdminUsers"));
const AdminMonitoring = lazy(() => import("./pages/AdminMonitoring"));
const AdminLearning = lazy(() => import("./pages/AdminLearning"));
const AdminSecurity = lazy(() => import("./pages/AdminSecurity"));
const AdminSystemWorkRules = lazy(
  () => import("./components/AdminSystemWorkRules"),
);
const AdminAnalytics = lazy(() => import("./pages/AdminAnalytics"));
const AdminVPN = lazy(() => import("./pages/AdminVPN"));
const AdminBrowser = lazy(() => import("./pages/AdminBrowser"));
const AutoBrowser = lazy(() => import("./pages/AutoBrowser"));
const AdminQuotas = lazy(() => import("./pages/AdminQuotas"));
const AdminNotifications = lazy(() => import("./pages/AdminNotifications"));
const AdminPerformance = lazy(() => import("./pages/AdminPerformance"));
const AdminBackup = lazy(() => import("./pages/AdminBackup"));
const AdminOCR = lazy(() => import("./pages/AdminOCR"));
const AdminReverseEngineer = lazy(() => import("./pages/AdminReverseEngineer"));
const AdminReports = lazy(() => import("./pages/AdminReports"));
const AdminApprovals = lazy(() => import("./components/AdminApprovals"));
const AdminInfrastructure = lazy(() => import("./pages/AdminInfrastructure"));
const AdminCodeAnalysis = lazy(() => import("./pages/AdminCodeAnalysis"));
const AdminSettings = lazy(() => import("./pages/AdminSettings"));
const AdminLogs = lazy(() => import("./pages/AdminLogs"));
const AdminSimulator = lazy(() => import("./pages/AdminSimulator"));
const AdminRules = lazy(() => import("./pages/AdminRules"));
const AdminTesting = lazy(() => import("./pages/AdminTesting"));
const AdminSuperFly = lazy(() => import("./pages/AdminSuperFly"));
const AdminCloudDBHub = lazy(() => import("./pages/AdminCloudDBHub"));

interface ModelStatus {
  id: string;
  name: string;
  status: "online" | "offline" | "loading";
  latency: number;
  memory: string;
  type: string;
}

const models_list: ModelStatus[] = [
  {
    id: "1",
    name: "Supreme-Qwen-Coder-32B",
    status: "online",
    latency: 45,
    memory: "24GB",
    type: "LLM",
  },
  {
    id: "2",
    name: "Supreme-Llama-3-70B",
    status: "online",
    latency: 82,
    memory: "48GB",
    type: "LLM",
  },
  {
    id: "3",
    name: "Supreme-DeepSeek-R1",
    status: "online",
    latency: 120,
    memory: "80GB",
    type: "LLM",
  },
];

const LoadingFallback = () => (
  <div className="loading-fallback">
    <div className="loading-spinner">
      <Spin size="large" />
    </div>
    <div className="loading-text">
      সিস্টেম_লোড_হচ্ছে... (INITIALIZING_SYSTEM)
    </div>
  </div>
);

const MainVisualizer = () => {
  const [models, setModels] = useState<ModelStatus[]>(models_list);
  const [activeModel, setActiveModel] = useState<string | null>(null);
  const [logs, setLogs] = useState<string[]>([]);

  const fetchHealth = async (signal?: AbortSignal) => {
    try {
      const API_BASE = import.meta.env.VITE_API_URL || "";
      const response = await fetch(`${API_BASE}/telemetry/health`, { signal });
      const data = await response.json();
      if (data.models) {
        setModels(data.models);
      }
    } catch (error) {
      if ((error as Error).name !== "AbortError") {
        console.error("Failed to fetch health metrics:", error);
      }
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    fetchHealth(controller.signal);
    const metricsInterval = setInterval(
      () => fetchHealth(controller.signal),
      15000,
    );
    const logInterval = setInterval(() => {
      const messages = [
        "📡 System Heartbeat: OK",
        "⚙️ Optimizing GCP Cluster us-central1",
        "🛡️ Resilience Policy: Autonomous Failover Active",
        "💎 Model Weights Loaded: Verified",
        "🌐 Scaling Qwen Coder... Instance Count: 4",
      ];
      setLogs((prev) => [
        messages[Math.floor(Math.random() * messages.length)],
        ...prev.slice(0, 5),
      ]);
    }, 10000);
    return () => {
      controller.abort();
      clearInterval(metricsInterval);
      clearInterval(logInterval);
    };
  }, []);

  return (
    <div
      className="app-container"
      style={{ position: "relative", overflow: "hidden" }}
    >
      <div className="bg-grid"></div>
      <div className="scanline"></div>

      {/* 3D Visualizer */}
      <div
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          zIndex: 0,
        }}
      >
        <Canvas
          shadows
          dpr={[1, 2]}
          onError={(e) => console.error("Canvas Error:", e)}
        >
          <Suspense fallback={null}>
            <PerspectiveCamera makeDefault position={[0, 0, 8]} />
            <ambientLight intensity={0.2} />
            <spotLight position={[10, 10, 10]} angle={0.15} penumbra={1} />
            <CoreEngine />
            <Stars
              radius={100}
              depth={50}
              count={5000}
              factor={4}
              saturation={0}
              fade
              speed={1}
            />
            <OrbitControls
              enableZoom={false}
              autoRotate
              autoRotateSpeed={0.5}
            />
          </Suspense>
        </Canvas>
      </div>

      {/* UI Overlay */}
      <header className="header-overlay">
        <div className="header-panel">
          <div className="pulsing status-indicator"></div>
          <h1
            className="neon-text"
            style={{
              fontWeight: 800,
              letterSpacing: "clamp(1px, 0.3vw, 3px)",
              fontSize: "var(--text-base)",
            }}
          >
            SUPREME AI COMMAND CENTER
          </h1>
        </div>
      </header>

      {/* Model Cards */}
      <main className="model-cards-container">
        {models.map((model) => (
          <motion.div
            key={model.id}
            whileHover={{ y: -10, borderColor: "var(--neon-blue)" }}
            onClick={() => setActiveModel(model.id)}
            className={`glass-panel model-card ${activeModel === model.id ? "active" : ""}`}
          >
            <div className="model-card-header">
              <Cpu
                size={20}
                color={model.status === "online" ? "var(--neon-blue)" : "#555"}
              />
              <span style={{ fontSize: "var(--text-xs)", color: "#888" }}>
                {model.type}
              </span>
            </div>
            <h3 className="model-card-title">{model.name}</h3>
            <div className="model-card-stats">
              <div className="model-stat-row">
                <span style={{ color: "#888" }}>Latency</span>
                <span className="neon-text">{model.latency}ms</span>
              </div>
              <div className="model-stat-row">
                <span style={{ color: "#888" }}>RAM</span>
                <span>{model.memory}</span>
              </div>
            </div>
            <div className="model-progress-bar">
              <motion.div
                animate={{ width: model.status === "online" ? "100%" : "30%" }}
                className={`model-progress-fill ${model.status === "online" ? "glow-blue" : "glow-purple"}`}
              />
            </div>
          </motion.div>
        ))}
      </main>

      {/* Terminal / Logs */}
      <aside className="terminal-panel">
        <div className="glass-panel terminal-panel-content">
          <div className="terminal-panel-header">
            <TerminalIcon size={18} color="var(--neon-blue)" />
            <span className="terminal-panel-title">LIVE TELEMETRY</span>
          </div>
          <div className="terminal-logs">
            {logs.map((log, i) => (
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1 - i * 0.15, x: 0 }}
                key={i}
                className="terminal-log-item"
              >
                {log}
              </motion.div>
            ))}
          </div>
        </div>
      </aside>

      {/* Global Status HUD */}
      <div className="hud-metrics">
        <HUDMetric
          icon={<Shield size={18} />}
          label="Security"
          value="MIL-SPEC"
          color="var(--neon-blue)"
        />
        <HUDMetric
          icon={<Zap size={18} />}
          label="Response"
          value="ULTRALOW"
          color="var(--neon-purple)"
        />
        <HUDMetric
          icon={<Globe size={18} />}
          label="Region"
          value="GCP-US"
          color="var(--neon-pink)"
        />
      </div>
    </div>
  );
};

const HUDMetric = ({
  icon,
  label,
  value,
  color,
}: {
  icon: any;
  label: string;
  value: string;
  color: string;
}) => (
  <div className="hud-metric">
    <div className="hud-metric-icon" style={{ color }}>
      {icon}
    </div>
    <div className="hud-metric-content">
      <span className="hud-metric-label">{label}</span>
      <span className="hud-metric-value" style={{ color }}>
        {value}
      </span>
    </div>
  </div>
);

function App() {
  const [darkMode, setDarkMode] = useState(true);
  const [chatFont, setChatFont] = useState("font-mono");

  return (
    <div className={darkMode ? "dark-mode" : "light-mode"}>
      <ErrorBoundary>
        <BrowserRouter>
          <FeedbackSystem />
          <Suspense fallback={<LoadingFallback />}>
            <Routes>
              {/* Authentication */}
              <Route path="/login" element={<LoginPage />} />

              {/* Admin routes under single URL path */}
              <Route path="/admin" element={<AdminRouteLayout />}>
                <Route
                  index
                  element={<Navigate to="/admin/dashboard" replace />}
                />

                {/* Statically defined routes guarantee stability during Suspense/Lazy loads */}
                <Route
                  path="dashboard"
                  element={
                    <DashboardHome isAdmin={true} setActiveKey={() => {}} />
                  }
                />
                <Route path="ai" element={<ChatWithAI chatFont={chatFont} />} />
                <Route path="projects" element={<AdminProjects />} />
                <Route path="providers" element={<AdminProviders />} />
                <Route path="users" element={<AdminUsers />} />
                <Route path="monitoring" element={<AdminMonitoring />} />
                <Route path="learning" element={<AdminLearning />} />
                <Route path="security" element={<AdminSecurity />} />
                <Route
                  path="system-work-rules"
                  element={<AdminSystemWorkRules />}
                />
                <Route path="rules" element={<AdminRules />} />
                <Route path="analytics" element={<AdminAnalytics />} />
                <Route path="logs" element={<AdminLogs />} />
                <Route path="vpn" element={<AdminVPN />} />
                <Route path="browser" element={<AdminBrowser />} />
                <Route path="auto-browser" element={<AutoBrowser />} />
                <Route path="quotas" element={<AdminQuotas />} />
                <Route path="simulator" element={<AdminSimulator />} />
                <Route path="reverse" element={<AdminReverseEngineer />} />
                <Route path="notifications" element={<AdminNotifications />} />
                <Route path="reports" element={<AdminReports />} />
                <Route path="performance" element={<AdminPerformance />} />
                <Route path="backup" element={<AdminBackup />} />
                <Route path="ocr" element={<AdminOCR />} />
                <Route
                  path="infrastructure"
                  element={<AdminInfrastructure />}
                />
                <Route path="code-analysis" element={<AdminCodeAnalysis />} />
                <Route
                  path="settings"
                  element={
                    <AdminSettings
                      darkMode={darkMode}
                      setDarkMode={setDarkMode}
                      chatFont={chatFont}
                      setChatFont={setChatFont}
                    />
                  }
                />
                <Route path="approvals" element={<AdminApprovals />} />
                <Route path="superfly" element={<AdminSuperFly />} />
                <Route path="cloud-db-hub" element={<AdminCloudDBHub />} />

                {/* Hidden Developer/Testing Routes not in sidebar */}
                <Route path="testing" element={<AdminTesting />} />
              </Route>

              {/* Legacy routes redirect to admin */}
              <Route
                path="/"
                element={<Navigate to="/admin/dashboard" replace />}
              />
              <Route path="/visualizer" element={<MainVisualizer />} />
              {/* Catch-all */}
              <Route
                path="*"
                element={<Navigate to="/admin/dashboard" replace />}
              />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </ErrorBoundary>
    </div>
  );
}

export default App;
