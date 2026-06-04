// AdminProjects.tsx - Cinematic Project Orchestration
import {
  RocketOutlined,
  ReloadOutlined,
  SyncOutlined,
  CodeOutlined,
  BlockOutlined,
  BranchesOutlined,
  ExperimentOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Alert,
  message,
  Progress,
} from "antd";
import { motion, AnimatePresence } from "framer-motion";
import React, { useState, useEffect } from "react";

// Modular Components
import AppGenerationCard from "../components/projects/AppGenerationCard";
import InfrastructureAdviceModal from "../components/projects/InfrastructureAdviceModal";
import ProjectActionToolbar from "../components/projects/ProjectActionToolbar";
import ProjectModal from "../components/projects/ProjectModal";
import ProjectTable from "../components/projects/ProjectTable";
import {
  Project,
  GenerationForm,
  GenerationStatus,
  ProjectSortField,
} from "../components/projects/types";
import { useSystemWebSocket } from "../hooks/useSystemWebSocket";
import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

const AdminProjects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [sortBy, setSortBy] = useState<ProjectSortField | null>("createdAt");
  const [sortOrder, setSortOrder] = useState<"ascend" | "descend">("descend");

  // Generation State
  const [generationForm, setGenerationForm] = useState<GenerationForm>({
    name: "",
    description: "",
    platform: "fullstack",
    database: "PostgreSQL",
    useAI: true,
  });
  const [generationStatus, setGenerationStatus] =
    useState<GenerationStatus>("idle");
  const [generationStep, setGenerationStep] = useState(0);
  const [generationProgress, setGenerationProgress] = useState(0);
  const [generationResult, setGenerationResult] = useState<any>(null);

  const { messages } = useSystemWebSocket(["/topic/pipeline/progress"]);

  useEffect(() => {
    const pipelineMsg = messages["/topic/pipeline/progress"];
    if (pipelineMsg) {
      if (pipelineMsg.step !== undefined) setGenerationStep(pipelineMsg.step);
      if (pipelineMsg.progress !== undefined)
        setGenerationProgress(pipelineMsg.progress);
    }
  }, [messages]);

  const [adviceVisible, setAdviceVisible] = useState(false);
  const [adviceLoading, setAdviceLoading] = useState(false);
  const [infrastructureAdvice, setInfrastructureAdvice] = useState<
    string | null
  >(null);

  const processedProjects = React.useMemo(() => {
    const result = projects.filter((p) => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return (
        p.name.toLowerCase().includes(term) ||
        (p.description && p.description.toLowerCase().includes(term)) ||
        p.id.toLowerCase().includes(term)
      );
    });
    if (sortBy) {
      result.sort((a, b) => {
        const aVal = (a as any)[sortBy] ?? "";
        const bVal = (b as any)[sortBy] ?? "";
        if (sortBy === "createdAt") {
          return sortOrder === "ascend"
            ? new Date(aVal).getTime() - new Date(bVal).getTime()
            : new Date(bVal).getTime() - new Date(aVal).getTime();
        }
        return sortOrder === "ascend"
          ? String(aVal).localeCompare(String(bVal))
          : String(bVal).localeCompare(String(aVal));
      });
    }
    return result;
  }, [projects, sortBy, sortOrder, searchTerm]);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth("/api/projects");
      if (response.ok) {
        const result = await response.json();
        setProjects(result.data || []);
      }
    } catch (err) {
      setError("Failed to load project database");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleGenerateApp = async () => {
    if (!generationForm.name || !generationForm.description) {
      message.warning("Please provide target parameters");
      return;
    }
    setGenerationStatus("generating");
    try {
      const response = await authUtils.fetchWithAuth("/api/generate", {
        method: "POST",
        body: JSON.stringify({ ...generationForm, type: "project" }),
      });
      if (response.ok) {
        message.success("Synthesis engine initialized");
      }
    } catch (err) {
      setGenerationStatus("error");
    }
  };

  const handleDeleteProject = async (id: string) => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/projects/${id}`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
      });
      if (res.ok) {
        message.success("Project deleted successfully");
        fetchProjects();
      } else {
        message.error("Failed to delete project");
      }
    } catch {
      message.error("Failed to delete project");
    }
  };

  const handleUpdateStatus = async (id: string, status: string) => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/projects/${id}/status`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status }),
      });
      if (res.ok) {
        message.success(`Project status updated to ${status}`);
        fetchProjects();
      } else {
        message.error("Failed to update project status");
      }
    } catch {
      message.error("Failed to update project status");
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
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
              <RocketOutlined
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
                PROJECT VAULT
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
              System <span className="text-gradient">Orchestrator</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Manage, synthesize, and deploy neural-integrated applications.
            </Text>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined spin={loading} />}
                onClick={fetchProjects}
                className="glass-action-button"
              >
                Sync Vault
              </Button>
              <Button
                type="primary"
                icon={<ExperimentOutlined />}
                onClick={() => {
                  setEditingProject(null);
                  setModalVisible(true);
                }}
                className="cyber-button"
              >
                New Synthesis
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Statistics Row */}
        <Col span={24}>
          <Row gutter={[24, 24]}>
            {[
              {
                label: "Active Deployments",
                value: projects.length,
                icon: <BranchesOutlined />,
                color: "var(--neon-blue)",
              },
              {
                label: "Production Ready",
                value: projects.filter((p) => p.status === "production").length,
                icon: <CodeOutlined />,
                color: "var(--success)",
              },
              {
                label: "Synthesis in Progress",
                value: generationStatus === "generating" ? 1 : 0,
                icon: <SyncOutlined spin={generationStatus === "generating"} />,
                color: "var(--neon-purple)",
              },
              {
                label: "System Load",
                value: "42%",
                icon: <BlockOutlined />,
                color: "var(--text-dim)",
              },
            ].map((stat, idx) => (
              <Col xs={12} lg={6} key={idx}>
                <div className="glass-card" style={{ padding: "16px 24px" }}>
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
                          letterSpacing: 1,
                        }}
                      >
                        {stat.label}
                      </Text>
                      <div
                        style={{
                          color: "#fff",
                          fontSize: 24,
                          fontWeight: 800,
                          marginTop: 4,
                        }}
                      >
                        {stat.value}
                      </div>
                    </div>
                    <div
                      style={{ color: stat.color, fontSize: 24, opacity: 0.8 }}
                    >
                      {stat.icon}
                    </div>
                  </div>
                </div>
              </Col>
            ))}
          </Row>
        </Col>

        {/* Project List */}
        <Col xs={24} xl={16}>
          <div
            className="glass-card"
            style={{
              minHeight: "600px",
              display: "flex",
              flexDirection: "column",
            }}
          >
            <div className="glass-card-title">
              Active Project Matrix
              <Space>
                <ProjectActionToolbar
                  searchTerm={searchTerm}
                  setSearchTerm={setSearchTerm}
                  onNewProject={() => {
                    setEditingProject(null);
                    setModalVisible(true);
                  }}
                  onRefresh={fetchProjects}
                  loading={loading}
                  sortBy={sortBy}
                  setSortBy={setSortBy}
                  sortOrder={sortOrder}
                  setSortOrder={setSortOrder}
                  minimal={true}
                />
              </Space>
            </div>

            <div style={{ flex: 1, marginTop: 12 }}>
              {loading && !projects.length ? (
                <div
                  style={{
                    height: "100%",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Spin size="large" tip="SYNCING VAULT..." />
                </div>
              ) : (
                <ProjectTable
                  projects={processedProjects}
                  loading={loading}
                  onEdit={(project) => {
                    setEditingProject(project);
                    setModalVisible(true);
                  }}
                  onDelete={handleDeleteProject}
                  onUpdateStatus={handleUpdateStatus}
                />
              )}
            </div>
          </div>
        </Col>

        {/* Generation & Advice Side Panel */}
        <Col xs={24} xl={8}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            {/* App Generation Card - Modified for Sidebar */}
            <div
              className="glass-card"
              style={{ borderLeft: "4px solid var(--neon-purple)" }}
            >
              <div className="glass-card-title">
                AI Synthesis Engine
                <ExperimentOutlined style={{ color: "var(--neon-purple)" }} />
              </div>
              <AppGenerationCard
                generationForm={generationForm}
                setGenerationForm={setGenerationForm}
                generationStatus={generationStatus}
                generationStep={generationStep}
                generationProgress={generationProgress}
                generationResult={generationResult}
                onGenerate={handleGenerateApp}
                onGetAdvice={() => setAdviceVisible(true)}
                compact={true}
              />
            </div>

            {/* Infrastructure Insight */}
            <div className="glass-card">
              <div className="glass-card-title">
                Neural Infrastructure
                <BranchesOutlined style={{ color: "var(--neon-blue)" }} />
              </div>
              <div style={{ padding: "8px 0" }}>
                <Text
                  style={{
                    color: "rgba(255,255,255,0.65)",
                    fontSize: 13,
                    display: "block",
                    marginBottom: 16,
                  }}
                >
                  Our AI agents can analyze your project requirements and
                  suggest optimal deployment strategies.
                </Text>
                <Button
                  block
                  className="glass-action-button"
                  onClick={() => setAdviceVisible(true)}
                >
                  Get Infrastructure Advice
                </Button>
              </div>
            </div>

            {/* System Status */}
            <div
              className="glass-card"
              style={{ background: "rgba(16, 185, 129, 0.05)" }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
                <div
                  className="pulsing"
                  style={{
                    width: 12,
                    height: 12,
                    borderRadius: "50%",
                    background: "var(--success)",
                    boxShadow: "0 0 10px var(--success)",
                  }}
                />
                <div>
                  <Text
                    style={{
                      color: "#fff",
                      fontSize: 14,
                      fontWeight: 600,
                      display: "block",
                    }}
                  >
                    Pipeline Ready
                  </Text>
                  <Text
                    style={{ color: "rgba(255,255,255,0.45)", fontSize: 11 }}
                  >
                    Cluster capacity at 85%
                  </Text>
                </div>
              </div>
            </div>
          </Space>
        </Col>
      </Row>

      <InfrastructureAdviceModal
        visible={adviceVisible}
        onCancel={() => setAdviceVisible(false)}
        advice={infrastructureAdvice}
        loading={adviceLoading}
        projectName={generationForm.name || "New Project"}
      />

      <ProjectModal
        visible={modalVisible}
        editingProject={editingProject}
        onCancel={() => setModalVisible(false)}
        onSubmit={async (v) => {
          try {
            const isEdit = !!editingProject;
            const res = await authUtils.fetchWithAuth(
              isEdit ? `/api/projects/${editingProject.id}` : "/api/projects",
              {
                method: isEdit ? "PUT" : "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(v),
              },
            );
            if (res.ok) {
              message.success(
                isEdit
                  ? "Project updated successfully"
                  : "Project created successfully",
              );
              setModalVisible(false);
              fetchProjects();
            } else {
              message.error("Operation failed");
            }
          } catch (err) {
            message.error("Failed to save project");
          }
        }}
      />
    </motion.div>
  );
};

export default AdminProjects;
