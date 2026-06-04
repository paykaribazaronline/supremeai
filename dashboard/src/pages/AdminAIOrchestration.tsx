// AdminAIOrchestration.tsx - AI Learning Orchestration
import {
  RadarChartOutlined,
  BulbOutlined,
  TeamOutlined,
  TrophyOutlined,
  SyncOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Card,
  Statistic,
  Progress,
  List,
  Button,
  Space,
  message,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

interface AgentStats {
  id: string;
  name: string;
  specialty: string;
  accuracy: number;
  requests: number;
  lastActive: string;
}

export default function AdminAIOrchestration() {
  const [loading, setLoading] = useState(true);
  const [agents, setAgents] = useState<AgentStats[]>([]);
  const [orchestratorStatus, setOrchestratorStatus] = useState<any>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [agentsRes, statusRes] = await Promise.all([
        authUtils.fetchWithAuth("/api/orchestration/leaderboard"),
        authUtils.fetchWithAuth("/api/orchestration/status"),
      ]);

      if (agentsRes.ok) {
        const data = await agentsRes.json();
        setAgents(Array.isArray(data) ? data : data.agents || []);
      }

      if (statusRes.ok) {
        const data = await statusRes.json();
        setOrchestratorStatus(data);
      }
    } catch (error) {
      message.error("Failed to fetch orchestration data");
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: "24px" }}
    >
      <Title level={4} style={{ color: "#fff", marginBottom: 24 }}>
        AI Learning Orchestration
      </Title>

      <Row gutter={[24, 24]}>
        {/* Orchestrator Status */}
        <Col xs={24} lg={12}>
          <Card className="glass-card" style={{ textAlign: "center" }}>
            <RadarChartOutlined
              style={{
                fontSize: 32,
                color: "var(--neon-blue)",
                marginBottom: 16,
              }}
            />
            <Title level={5} style={{ color: "#fff", marginBottom: 16 }}>
              {orchestratorStatus?.name || "Adaptive Orchestrator"}
            </Title>
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title={
                    <Text style={{ color: "var(--text-dim)", fontSize: 11 }}>
                      Active Agents
                    </Text>
                  }
                  value={agents.length}
                  valueStyle={{ color: "var(--neon-blue)", fontWeight: 700 }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title={
                    <Text style={{ color: "var(--text-dim)", fontSize: 11 }}>
                      Total Requests
                    </Text>
                  }
                  value={agents.reduce((sum, a) => sum + (a.requests || 0), 0)}
                  valueStyle={{ color: "var(--neon-purple)", fontWeight: 700 }}
                />
              </Col>
            </Row>
          </Card>
        </Col>

        {/* MoE Routing */}
        <Col xs={24} lg={12}>
          <Card className="glass-card">
            <Title level={5} style={{ color: "#fff", marginBottom: 16 }}>
              Mixture of Experts Routing
            </Title>
            <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
              <div style={{ flex: 1 }}>
                <Text
                  style={{
                    color: "var(--text-dim)",
                    fontSize: 12,
                    display: "block",
                    marginBottom: 8,
                  }}
                >
                  Routing Efficiency
                </Text>
                <Progress
                  percent={orchestratorStatus?.efficiency || 85}
                  strokeColor="var(--neon-blue)"
                  trailColor="rgba(255,255,255,0.05)"
                  showInfo={false}
                />
              </div>
              <BulbOutlined
                style={{ fontSize: 24, color: "var(--neon-purple)" }}
              />
            </div>
          </Card>
        </Col>

        {/* Agent Leaderboard */}
        <Col xs={24}>
          <Card className="glass-card">
            <Title level={5} style={{ color: "#fff", marginBottom: 16 }}>
              Agent Leaderboard
            </Title>
            <List
              dataSource={agents}
              renderItem={(agent, index) => (
                <List.Item
                  style={{
                    padding: "12px 0",
                    borderBottom: "1px solid rgba(255,255,255,0.05)",
                  }}
                >
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 16 }}
                  >
                    <div
                      style={{
                        width: 40,
                        height: 40,
                        borderRadius: "50%",
                        background: "var(--neon-blue)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontWeight: 700,
                        color: "#fff",
                      }}
                    >
                      {index + 1}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                        }}
                      >
                        <Text style={{ color: "#fff", fontWeight: 500 }}>
                          {agent.name}
                        </Text>
                        <Text
                          style={{ color: "var(--text-dim)", fontSize: 12 }}
                        >
                          {agent.specialty}
                        </Text>
                      </div>
                      <div style={{ display: "flex", gap: 16, marginTop: 4 }}>
                        <Text
                          style={{ color: "var(--text-dim)", fontSize: 11 }}
                        >
                          Accuracy: {agent.accuracy}%
                        </Text>
                        <Text
                          style={{ color: "var(--text-dim)", fontSize: 11 }}
                        >
                          Requests: {agent.requests}
                        </Text>
                      </div>
                    </div>
                    <TrophyOutlined
                      style={{ color: "#faad14", fontSize: 20 }}
                    />
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </motion.div>
  );
}
