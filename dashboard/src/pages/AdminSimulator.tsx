// AdminSimulator.tsx - Cinematic App Simulation Center
import {
  MobileOutlined,
  RocketOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  ExperimentOutlined,
  ControlOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Drawer,
  Alert,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import DeploymentHistoryTable from "../components/simulator/DeploymentHistoryTable";
import QuotaAdminCard from "../components/simulator/QuotaAdminCard";
import SimulationControlCard from "../components/simulator/SimulationControlCard";
import SimulatorStats from "../components/simulator/SimulatorStats";
import {
  DeploymentRecord,
  Project,
  SimulatorStatsData,
} from "../components/simulator/types";
import SimulatorPreview from "../components/SimulatorPreview";
import { useRole } from "../contexts/RoleContext";
import { fetchWithAuth } from "../lib/authUtils";

const { Title, Text } = Typography;

const AdminSimulator: React.FC = () => {
  const { isAdmin, isGuest } = useRole();
  const [loading, setLoading] = useState(false);
  const [deployments, setDeployments] = useState<DeploymentRecord[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedAppId, setSelectedAppId] = useState<string | undefined>();
  const [previewOpen, setPreviewOpen] = useState(false);
  const [stats, setStats] = useState<SimulatorStatsData>({
    totalDeployments: 0,
    activeSessions: 0,
  });

  const fetchData = async () => {
    if (isGuest) return;
    setLoading(true);
    try {
      const endpoint = isAdmin
        ? "/api/simulator/admin/usage"
        : "/api/simulator/installed";
      const usageRes = await fetchWithAuth(endpoint);
      if (usageRes.ok) {
        const data = await usageRes.json();
        setDeployments(data.deployments || []);
        setStats({
          totalDeployments: data.totalDeployments || 0,
          activeSessions: (data.deployments || []).filter(
            (d: any) => d.status === "RUNNING",
          ).length,
        });
      }
      const projectsRes = await fetchWithAuth("/api/projects");
      if (projectsRes.ok) {
        const data = await projectsRes.json();
        setProjects(data.projects || []);
      }
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [isAdmin, isGuest]);

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
          borderBottom: "1px solid var(--neon-purple)",
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
              <MobileOutlined
                style={{ color: "var(--neon-purple)", fontSize: 20 }}
              />
              <Text
                style={{
                  color: "var(--neon-purple)",
                  letterSpacing: 2,
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                NEURAL RUNTIME
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
              App <span className="text-gradient">Simulator</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Virtualize and test neural-integrated applications across
              distributed device clusters.
            </Text>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined spin={loading} />}
                onClick={fetchData}
                className="glass-action-button"
              >
                Refresh Instances
              </Button>
              <Button
                type="primary"
                icon={<RocketOutlined />}
                className="cyber-button"
              >
                Launch Emulator
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Statistics Row */}
        <Col span={24}>
          <Row gutter={[24, 24]}>
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
                      Active Sessions
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {stats.activeSessions}
                    </div>
                  </div>
                  <ExperimentOutlined
                    style={{ color: "var(--neon-purple)", fontSize: 24 }}
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
                      Available Nodes
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      12
                    </div>
                  </div>
                  <GlobalOutlined
                    style={{ color: "var(--neon-blue)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
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
                      Cluster Health
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 24,
                        fontWeight: 800,
                      }}
                    >
                      99.8%
                    </div>
                  </div>
                  <ThunderboltOutlined
                    style={{ color: "var(--success)", fontSize: 24 }}
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
                      Total Deployments
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {stats.totalDeployments}
                    </div>
                  </div>
                  <ControlOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Main Interface */}
        <Col xs={24} lg={16}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <div className="glass-card">
              <div className="glass-card-title">Instance Control Matrix</div>
              <SimulationControlCard
                projects={projects}
                selectedAppId={selectedAppId}
                onSelectAppId={setSelectedAppId}
                onOpenSimulator={() => setPreviewOpen(true)}
              />
            </div>
            <div className="glass-card">
              <div className="glass-card-title">Runtime Log Index</div>
              <DeploymentHistoryTable
                deployments={deployments}
                loading={loading}
                onRefresh={fetchData}
                onSimulate={(id) => {
                  setSelectedAppId(id);
                  setPreviewOpen(true);
                }}
              />
            </div>
          </Space>
        </Col>

        <Col xs={24} lg={8}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <div
              className="glass-card"
              style={{ padding: 0, overflow: "hidden", height: 400 }}
            >
              <div
                style={{
                  padding: 20,
                  borderBottom: "1px solid rgba(255,255,255,0.05)",
                }}
              >
                <Text strong style={{ color: "#fff" }}>
                  Neural Viewport
                </Text>
              </div>
              <div style={{ background: "#000", height: "100%" }}>
                <SimulatorPreview appId={selectedAppId} />
              </div>
            </div>
            {isAdmin && (
              <div className="glass-card">
                <div className="glass-card-title">Resource Allocation</div>
                <QuotaAdminCard onSetQuota={() => {}} />
              </div>
            )}
          </Space>
        </Col>
      </Row>

      <Drawer
        title={
          <span style={{ color: "#fff" }}>VIRTUALIZED UI: {selectedAppId}</span>
        }
        placement="right"
        width="80%"
        onClose={() => setPreviewOpen(false)}
        open={previewOpen}
        bodyStyle={{ background: "#000", padding: 0 }}
        headerStyle={{
          background: "#080810",
          borderBottom: "1px solid rgba(0, 243, 255, 0.2)",
        }}
      >
        <div
          style={{
            height: "100%",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            background: "#000",
          }}
        >
          <SimulatorPreview appId={selectedAppId} />
        </div>
      </Drawer>
    </motion.div>
  );
};

export default AdminSimulator;
