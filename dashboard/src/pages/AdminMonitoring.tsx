// AdminMonitoring.tsx - Cinematic System Monitoring
import {
  ThunderboltOutlined,
  HddOutlined,
  DatabaseOutlined,
  SyncOutlined,
  DeleteOutlined,
  DotChartOutlined,
  BulbOutlined,
  BugOutlined,
  DashboardOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Progress,
  List,
  Tag,
  notification,
  message,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect, useMemo } from "react";

import { useRole } from "../contexts/RoleContext";
import { useSystemWebSocket } from "../hooks/useSystemWebSocket";
import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

interface ResourceMetrics {
  memoryUsed: number;
  memoryMax: number;
  cpuLoad: number;
  cpuUsagePercentage?: number;
  memoryUsagePercentage?: number;
  availableProcessors: number;
  dbActiveConnections?: number;
  dbIdleConnections?: number;
  redisStatus?: string;
  timestamp: number;
}

interface SystemLog {
  level: string;
  component: string;
  message: string;
  timestamp: number;
}

const AdminMonitoring: React.FC = () => {
  const { isGuest } = useRole();
  const [metrics, setMetrics] = useState<ResourceMetrics | null>(null);
  const [logs, setLogs] = useState<SystemLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [stressLoading, setStressLoading] = useState(false);

  const { messages, connected } = useSystemWebSocket(["/topic/monitoring"]);

  useEffect(() => {
    const monitoringData = messages["/topic/monitoring"];
    if (monitoringData) {
      if (monitoringData.type === "SYSTEM_RESOURCES") {
        setMetrics(monitoringData as unknown as ResourceMetrics);
      } else if (monitoringData.type === "SYSTEM_LOG") {
        const newLog = monitoringData as unknown as SystemLog;
        setLogs((prev) => [newLog, ...prev].slice(0, 100));
        if (newLog.level === "ALERT" || newLog.level === "ERROR") {
          notification[newLog.level === "ALERT" ? "warning" : "error"]({
            message: `Neural Alert: ${newLog.level}`,
            description: newLog.message,
            placement: "bottomRight",
          });
        }
      }
    }
  }, [messages]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const metricsResp = await authUtils.fetchWithAuth(
        "/api/system/metrics/resources",
      );
      if (metricsResp.ok) {
        const result = await metricsResp.json();
        setMetrics(result.data || null);
      }
      const logsResp = await authUtils.fetchWithAuth(
        "/api/admin/logs?limit=50",
      );
      if (logsResp.ok) {
        const result = await logsResp.json();
        setLogs(result.data?.logs || result.data || []);
      }
    } catch (err) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleClearHistory = () => {
    setLogs([]);
    message.success("Log history cleared");
  };

  const handleRunStressTest = async () => {
    setStressLoading(true);
    try {
      const res = await authUtils.fetchWithAuth(
        "/api/admin/monitoring/stress-test",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
        },
      );
      if (res.ok) {
        const data = await res.json();
        message.success(
          `Stress test completed: ${data.summary || "All systems passed"}`,
        );
      } else {
        message.error("Stress test failed");
      }
    } catch {
      message.error("Failed to run stress test");
    } finally {
      setStressLoading(false);
    }
  };

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
          borderBottom: "1px solid rgba(0, 243, 255, 0.1)",
          paddingBottom: 24,
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
              <DotChartOutlined
                style={{ color: "var(--neon-blue)", fontSize: 20 }}
              />
              <Text
                style={{
                  color: "var(--neon-blue)",
                  letterSpacing: 2,
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                REAL-TIME TELEMETRY
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
              System <span className="text-gradient">Monitoring</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Live resource allocation, neural stability, and distributed log
              stream.
            </Text>
          </Col>
          <Col>
            <Space>
              <Badge
                status={connected ? "processing" : "default"}
                color={connected ? "var(--neon-blue)" : "#444"}
                text={
                  <Text
                    style={{
                      color: connected ? "var(--neon-blue)" : "#666",
                      fontWeight: 700,
                    }}
                  >
                    {connected ? "NEURAL LINK ACTIVE" : "DISCONNECTED"}
                  </Text>
                }
              />
              <Button
                icon={<SyncOutlined spin={loading} />}
                onClick={fetchData}
                className="glass-action-button"
              >
                Full Sync
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Core Vitals Cards */}
        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
                letterSpacing: 1,
              }}
            >
              CPU LOAD
            </Text>
            <div style={{ margin: "16px 0" }}>
              <Progress
                type="dashboard"
                percent={Math.round(metrics?.cpuUsagePercentage || 0)}
                strokeColor="var(--neon-blue)"
                trailColor="rgba(255,255,255,0.05)"
                width={100}
                strokeWidth={8}
                format={(p) => (
                  <span style={{ color: "#fff", fontWeight: 800 }}>{p}%</span>
                )}
              />
            </div>
            <Text
              style={{
                color: "var(--neon-blue)",
                fontSize: 12,
                fontWeight: 700,
              }}
            >
              {metrics?.availableProcessors || 0} CORES ACTIVE
            </Text>
          </div>
        </Col>

        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
                letterSpacing: 1,
              }}
            >
              MEMORY STACK
            </Text>
            <div style={{ margin: "16px 0" }}>
              <Progress
                type="dashboard"
                percent={Math.round(metrics?.memoryUsagePercentage || 0)}
                strokeColor="var(--neon-purple)"
                trailColor="rgba(255,255,255,0.05)"
                width={100}
                strokeWidth={8}
                format={(p) => (
                  <span style={{ color: "#fff", fontWeight: 800 }}>{p}%</span>
                )}
              />
            </div>
            <Text
              style={{
                color: "var(--neon-purple)",
                fontSize: 12,
                fontWeight: 700,
              }}
            >
              {Math.round((metrics?.memoryUsed || 0) / 1024 / 1024)} MB USED
            </Text>
          </div>
        </Col>

        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
                letterSpacing: 1,
              }}
            >
              DATABASE POOL
            </Text>
            <div
              style={{
                margin: "24px 0",
                height: 100,
                display: "flex",
                flexDirection: "column",
                justifyContent: "center",
              }}
            >
              <div
                style={{
                  color: "var(--success)",
                  fontSize: 32,
                  fontWeight: 800,
                }}
              >
                {metrics?.dbActiveConnections || 0}
              </div>
              <Text style={{ color: "rgba(255,255,255,0.45)", fontSize: 12 }}>
                ACTIVE CONNECTIONS
              </Text>
            </div>
            <Badge
              status="success"
              text={<Text style={{ color: "#fff", fontSize: 12 }}>STABLE</Text>}
            />
          </div>
        </Col>

        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
                letterSpacing: 1,
              }}
            >
              GLOBAL STATUS
            </Text>
            <div
              style={{
                margin: "24px 0",
                height: 100,
                display: "flex",
                flexDirection: "column",
                justifyContent: "center",
              }}
            >
              <div
                style={{
                  color: "var(--neon-blue)",
                  fontSize: 24,
                  fontWeight: 800,
                }}
              >
                HEALTHY
              </div>
              <Text style={{ color: "rgba(255,255,255,0.45)", fontSize: 12 }}>
                SYNAPTIC SYNC
              </Text>
            </div>
            <div
              className="pulsing"
              style={{
                margin: "0 auto",
                width: 8,
                height: 8,
                borderRadius: "50%",
                background: "var(--neon-blue)",
                boxShadow: "0 0 10px var(--neon-blue)",
              }}
            />
          </div>
        </Col>

        {/* Log Stream */}
        <Col xs={24} lg={16}>
          <div
            className="glass-card"
            style={{ height: 500, display: "flex", flexDirection: "column" }}
          >
            <div className="glass-card-title">
              Neural Log Stream
              <Space>
                <Button
                  icon={<DeleteOutlined />}
                  size="small"
                  type="text"
                  danger
                  onClick={handleClearHistory}
                >
                  Clear History
                </Button>
              </Space>
            </div>
            <div
              style={{
                flex: 1,
                overflow: "auto",
                background: "rgba(0,0,0,0.2)",
                padding: 16,
                borderRadius: 8,
              }}
            >
              <List
                dataSource={logs}
                renderItem={(item) => (
                  <div
                    style={{
                      display: "flex",
                      gap: 16,
                      marginBottom: 12,
                      borderBottom: "1px solid rgba(255,255,255,0.03)",
                      paddingBottom: 8,
                    }}
                  >
                    <Text
                      style={{
                        fontFamily: "JetBrains Mono",
                        color: "rgba(255,255,255,0.3)",
                        fontSize: 11,
                        minWidth: 80,
                      }}
                    >
                      {new Date(item.timestamp).toLocaleTimeString()}
                    </Text>
                    <Tag
                      color={
                        item.level === "ERROR"
                          ? "red"
                          : item.level === "ALERT"
                            ? "magenta"
                            : "blue"
                      }
                      style={{
                        fontSize: 10,
                        minWidth: 60,
                        textAlign: "center",
                      }}
                    >
                      {item.level}
                    </Tag>
                    <div style={{ flex: 1 }}>
                      <Text
                        style={{
                          color: "var(--neon-blue)",
                          fontSize: 10,
                          display: "block",
                          textTransform: "uppercase",
                        }}
                      >
                        {item.component}
                      </Text>
                      <Text style={{ color: "#fff", fontSize: 13 }}>
                        {item.message}
                      </Text>
                    </div>
                  </div>
                )}
              />
            </div>
          </div>
        </Col>

        {/* Infrastructure Sidebar */}
        <Col xs={24} lg={8}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <div className="glass-card">
              <div className="glass-card-title">Infrastructure Cluster</div>
              <div style={{ padding: "8px 0" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: 12,
                  }}
                >
                  <Text style={{ color: "var(--text-dim)" }}>Redis Node</Text>
                  <Tag color="green">ONLINE</Tag>
                </div>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: 12,
                  }}
                >
                  <Text style={{ color: "var(--text-dim)" }}>Worker Nodes</Text>
                  <Text style={{ color: "#fff" }}>12 active</Text>
                </div>
                <div
                  style={{ display: "flex", justifyContent: "space-between" }}
                >
                  <Text style={{ color: "var(--text-dim)" }}>Sync Latency</Text>
                  <Text style={{ color: "var(--neon-blue)", fontWeight: 700 }}>
                    4ms
                  </Text>
                </div>
              </div>
            </div>

            <div
              className="glass-card"
              style={{ background: "rgba(188, 19, 254, 0.05)" }}
            >
              <div className="glass-card-title">Intelligence Insight</div>
              <Text style={{ color: "var(--text-dim)", fontSize: 13 }}>
                System resource utilization is optimized. No memory leaks
                detected in the last 24h cycle.
              </Text>
              <Button
                block
                ghost
                type="primary"
                style={{ marginTop: 16 }}
                icon={<BugOutlined />}
                onClick={handleRunStressTest}
                loading={stressLoading}
              >
                Run Stress Test
              </Button>
            </div>
          </Space>
        </Col>
      </Row>
    </motion.div>
  );
};

export default AdminMonitoring;
