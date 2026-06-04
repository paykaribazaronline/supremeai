// AdminSecurity.tsx - Cinematic Security & Resilience
import {
  SafetyCertificateOutlined,
  SecurityScanOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  BugOutlined,
  SafetyOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  message,
  Progress,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

// Modular Components
import CyberLearningPanel from "../components/security/CyberLearningPanel";
import HealthScoreCard from "../components/security/HealthScoreCard";
import SelfHealingPanel from "../components/security/SelfHealingPanel";
import SurveillancePanel from "../components/security/SurveillancePanel";
import SystemAuditPanel from "../components/security/SystemAuditPanel";
import { fetchWithAuth } from "../lib/authUtils";

const { Title, Text } = Typography;

const AdminSecurity: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [healingStatus, setHealingStatus] = useState<any>(null);
  const [systemStats, setSystemStats] = useState<any>(null);
  const [testError, setTestError] = useState("");
  const [fixing, setFixing] = useState(false);
  const [fixResult, setFixResult] = useState<any>(null);
  const [cyberSkills, setCyberSkills] = useState<any[]>([]);
  const [protections, setProtections] = useState<any[]>([]);
  const [auditing, setAuditing] = useState(false);
  const [auditReport, setAuditReport] = useState<any>(null);
  const [learning, setLearning] = useState(false);
  const [learnTopic, setLearnTopic] = useState("");
  const [autoConfig, setAutoConfig] = useState({
    autonomousLearningEnabled: false,
    autonomousAuditEnabled: false,
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [healingRes, contractRes, skillsRes, protectRes, configRes] =
        await Promise.all([
          fetchWithAuth("/api/self-healing/status"),
          fetchWithAuth("/api/admin/dashboard/contract"),
          fetchWithAuth("/api/admin/security/cyber/skills"),
          fetchWithAuth("/api/admin/security/cyber/protections"),
          fetchWithAuth("/api/admin/security/cyber/config"),
        ]);
      if (healingRes.ok) setHealingStatus(await healingRes.json());
      if (contractRes.ok)
        setSystemStats((await contractRes.json()).data?.stats);
      if (skillsRes.ok) setCyberSkills((await skillsRes.json()).data || []);
      if (protectRes.ok) setProtections((await protectRes.json()).data || []);
      if (configRes.ok) setAutoConfig((await configRes.json()).data);
    } catch (error) {
      message.error("Failed to sync security neural net");
    } finally {
      setLoading(false);
    }
  };

  const handleTestFix = async () => {
    if (!testError.trim()) {
      message.warning("Please enter an error message to test");
      return;
    }
    setFixing(true);
    setFixResult(null);
    try {
      const res = await fetchWithAuth("/api/self-healing/detect", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ error: testError }),
      });
      if (res.ok) setFixResult(await res.json());
      else message.error("Self-healing detection failed");
    } catch {
      message.error("Failed to run self-healing test");
    } finally {
      setFixing(false);
    }
  };

  const handleWatchtower = async () => {
    try {
      const res = await fetchWithAuth("/api/admin/security/surveillance");
      if (res.ok) message.success("Watchtower surveillance active");
    } catch {
      message.error("Failed to activate Watchtower");
    }
  };

  const handleStartLearning = async () => {
    if (!learnTopic.trim()) {
      message.warning("Please enter a learning topic");
      return;
    }
    setLearning(true);
    try {
      const res = await fetchWithAuth("/api/admin/security/cyber/learn", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ topic: learnTopic }),
      });
      if (res.ok) {
        message.success("Learning cycle initiated");
        setLearnTopic("");
      } else message.error("Failed to start learning cycle");
    } catch {
      message.error("Failed to start learning cycle");
    } finally {
      setLearning(false);
    }
  };

  const handleToggleAutonomous = async (enabled: boolean) => {
    try {
      const res = await fetchWithAuth("/api/admin/security/cyber/config", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ autonomousLearningEnabled: enabled }),
      });
      if (res.ok)
        setAutoConfig((prev) => ({
          ...prev,
          autonomousLearningEnabled: enabled,
        }));
    } catch {
      message.error("Failed to toggle autonomous mode");
    }
  };

  const handleToggleAuditAutonomous = async (enabled: boolean) => {
    try {
      const res = await fetchWithAuth("/api/admin/security/cyber/config", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ autonomousAuditEnabled: enabled }),
      });
      if (res.ok)
        setAutoConfig((prev) => ({ ...prev, autonomousAuditEnabled: enabled }));
    } catch {
      message.error("Failed to toggle audit autonomous mode");
    }
  };

  const handleRunAudit = async () => {
    setAuditing(true);
    setAuditReport(null);
    try {
      const res = await fetchWithAuth("/api/admin/security/cyber/audit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });
      if (res.ok) setAuditReport(await res.json());
      else message.error("Self-audit failed");
    } catch {
      message.error("Failed to run self-audit");
    } finally {
      setAuditing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: "1600px", margin: "0 auto" }}
    >
      {/* Cinematic Header */}
      <div
        style={{
          marginBottom: 32,
          borderBottom: "1px solid var(--success)",
          paddingBottom: 24,
          opacity: 0.8,
        }}
      >
        <Row justify="space-between" align="bottom">
          <Col>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                marginBottom: 8,
              }}
            >
              <SafetyCertificateOutlined
                style={{ color: "var(--success)", fontSize: 20 }}
              />
              <Text
                style={{
                  color: "var(--success)",
                  letterSpacing: 2,
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                DEFENSIVE MATRIX ACTIVE
              </Text>
            </div>
            <Title
              level={2}
              style={{
                color: "#fff",
                margin: 0,
                fontWeight: 800,
                fontSize: 32,
              }}
            >
              Security{" "}
              <span
                style={{
                  color: "var(--success)",
                  textShadow: "0 0 10px rgba(16, 185, 129, 0.3)",
                }}
              >
                & Resilience
              </span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              AI-driven self-healing, autonomous defense, and real-time threat
              surveillance.
            </Text>
          </Col>
          <Col>
            <Space>
              <Badge dot color="var(--success)">
                <Button
                  icon={<EyeOutlined />}
                  onClick={handleWatchtower}
                  className="glass-action-button"
                >
                  Watchtower
                </Button>
              </Badge>
              <Button
                icon={<ReloadOutlined spin={loading} />}
                onClick={fetchData}
                className="glass-action-button"
              >
                Refetch Protocols
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Top Resilience Stats */}
        <Col span={24}>
          <Row gutter={[24, 24]}>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid var(--success)" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Resilience Score
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 24,
                        fontWeight: 800,
                      }}
                    >
                      {systemStats?.systemHealthScore || 98}%
                    </div>
                  </div>
                  <SafetyCertificateOutlined
                    style={{ color: "var(--success)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid var(--neon-blue)" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Threat Mitigation
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      ACTIVE
                    </div>
                  </div>
                  <SecurityScanOutlined
                    style={{ color: "var(--neon-blue)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid var(--neon-purple)" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Self-Healing
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      ENABLED
                    </div>
                  </div>
                  <ThunderboltOutlined
                    style={{ color: "var(--neon-purple)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div className="glass-card">
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Anomaly Shards
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      0
                    </div>
                  </div>
                  <BugOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Main Security Panels */}
        <Col xs={24} lg={8}>
          <HealthScoreCard
            healthScore={systemStats?.systemHealthScore || 100}
            healthStatus={systemStats?.systemHealthStatus || "healthy"}
            healthReason={systemStats?.systemHealthReason || "Operational"}
          />
        </Col>

        <Col xs={24} lg={16}>
          <SelfHealingPanel
            healingStatus={healingStatus}
            testError={testError}
            setTestError={setTestError}
            handleTestFix={handleTestFix}
            fixing={fixing}
            fixResult={fixResult}
          />
        </Col>

        <Col xs={24} lg={12}>
          <CyberLearningPanel
            learnTopic={learnTopic}
            setLearnTopic={setLearnTopic}
            onStartLearning={handleStartLearning}
            learning={learning}
            cyberSkills={cyberSkills}
            autonomousLearningEnabled={autoConfig.autonomousLearningEnabled}
            onToggleAutonomous={handleToggleAutonomous}
          />
        </Col>

        <Col xs={24} lg={12}>
          <SystemAuditPanel
            onRunAudit={handleRunAudit}
            auditing={auditing}
            auditReport={auditReport}
            protections={protections}
            autonomousAuditEnabled={autoConfig.autonomousAuditEnabled}
            onToggleAutonomous={handleToggleAuditAutonomous}
          />
        </Col>

        <Col xs={24}>
          <SurveillancePanel />
        </Col>

        {/* Footer Security Badge */}
        <Col span={24}>
          <div
            className="glass-card"
            style={{
              background: "rgba(16, 185, 129, 0.05)",
              textAlign: "center",
            }}
          >
            <Text
              style={{
                color: "var(--success)",
                fontSize: 10,
                fontWeight: 800,
                letterSpacing: 3,
                textTransform: "uppercase",
              }}
            >
              SupremeAI Security Engine // Kernel v4.2 // Advanced Threat
              Shield: Active
            </Text>
          </div>
        </Col>
      </Row>
    </motion.div>
  );
};

export default AdminSecurity;
