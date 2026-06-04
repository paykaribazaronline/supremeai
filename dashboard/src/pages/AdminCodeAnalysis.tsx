// AdminCodeAnalysis.tsx - Cinematic Code Intelligence
import {
  CodeOutlined,
  SearchOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  BugOutlined,
  AimOutlined,
  DatabaseOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  GlobalOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Tag,
  message,
  Progress,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import AdminLayout from "../components/AdminLayout";
import AnalysisResultsTable from "../components/AnalysisResultsTable";
import ProjectUploadForm from "../components/ProjectUploadForm";
import { authUtils } from "../lib/authUtils";
import type { AnalysisJob } from "../types";

const { Title, Text } = Typography;

const AdminCodeAnalysis: React.FC = () => {
  const [jobs, setJobs] = useState<AnalysisJob[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth("/api/analysis/jobs");
      if (response.ok) {
        const data = await response.json();
        setJobs(data.data || []);
      }
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
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
          borderBottom: "1px solid var(--neon-blue)",
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
              <AimOutlined
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
                NEURAL CODE AUDIT
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
              Code <span className="text-gradient">Intelligence</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              AI-driven vulnerability detection, structural analysis, and
              autonomous patch generation.
            </Text>
          </Col>
          <Col>
            <Button
              icon={<ReloadOutlined spin={loading} />}
              onClick={fetchJobs}
              className="glass-action-button"
            >
              Refresh Database
            </Button>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* KPI Cards */}
        <Col span={24}>
          <Row gutter={[24, 24]}>
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
                      Analyzed Repos
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {jobs.length}
                    </div>
                  </div>
                  <CodeOutlined
                    style={{ color: "var(--neon-blue)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid #ef4444" }}
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
                      Critical Findings
                    </Text>
                    <div
                      style={{
                        color: "#ef4444",
                        fontSize: 24,
                        fontWeight: 800,
                      }}
                    >
                      {jobs.reduce((sum, j) => sum + (j.totalFindings || 0), 0)}
                    </div>
                  </div>
                  <BugOutlined style={{ color: "#ef4444", fontSize: 24 }} />
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
                      Resolved Shards
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 24,
                        fontWeight: 800,
                      }}
                    >
                      84%
                    </div>
                  </div>
                  <CheckCircleOutlined
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
                      Global Context
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 20, fontWeight: 800 }}
                    >
                      SYNCHRONIZED
                    </div>
                  </div>
                  <GlobalOutlined
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
              <div className="glass-card-title">
                Initiate Neural Scan <SearchOutlined />
              </div>
              <ProjectUploadForm onSubmit={() => {}} loading={submitLoading} />
            </div>
            <div className="glass-card">
              <div className="glass-card-title">
                Analysis History Index <DatabaseOutlined />
              </div>
              <AnalysisResultsTable
                jobs={jobs}
                onDelete={() => {}}
                onView={() => {}}
                onViewFixes={() => {}}
                onClearCache={() => {}}
                getSeverityColor={() => "blue"}
                getStatusColor={() => "default"}
              />
            </div>
          </Space>
        </Col>

        {/* Sidebar Info */}
        <Col xs={24} lg={8}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <div
              className="glass-card"
              style={{ background: "rgba(0, 243, 255, 0.05)" }}
            >
              <div className="glass-card-title">
                RAG Engine Active <AimOutlined />
              </div>
              <Text style={{ color: "var(--text-dim)", fontSize: 12 }}>
                Phase 3 features enabled: Incremental Scanning and LLM-assisted
                patches. Target processing speed: 1000 files in &lt;10s.
              </Text>
              <div style={{ marginTop: 16 }}>
                <Tag color="cyan">RAG CONTEXT</Tag>
                <Tag color="green">INCREMENTAL</Tag>
                <Tag color="gold">LLM FIXES</Tag>
              </div>
            </div>

            <div className="glass-card">
              <div className="glass-card-title">Intelligence Insight</div>
              <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
                <RobotOutlined
                  style={{ fontSize: 24, color: "var(--neon-purple)" }}
                />
                <Text style={{ color: "rgba(255,255,255,0.7)", fontSize: 13 }}>
                  "Current code structural patterns show a 12% improvement in
                  security resilience across the cluster."
                </Text>
              </div>
            </div>
          </Space>
        </Col>
      </Row>
    </motion.div>
  );
};

export default AdminCodeAnalysis;
