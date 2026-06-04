import {
  GitlabOutlined,
  SyncOutlined,
  PlayCircleOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Card,
  Input,
  Button,
  Space,
  message,
  List,
  Avatar,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;
const { TextArea } = Input;

interface GitStatus {
  branch: string;
  ahead: number;
  behind: number;
  modified: number;
  untracked: number;
}

interface GitProject {
  id: string;
  name: string;
  path: string;
  status: GitStatus;
  lastCommit: {
    message: string;
    author: string;
    date: string;
  };
}

export default function AdminGitProjects() {
  const [loading, setLoading] = useState(true);
  const [projects, setProjects] = useState<GitProject[]>([]);
  const [selectedProject, setSelectedProject] = useState<string | null>(null);
  const [commitMessage, setCommitMessage] = useState("");

  useEffect(() => {
    fetchProjects();
  }, []);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/git/status");
      if (res.ok) {
        const data = await res.json();
        setProjects(Array.isArray(data) ? data : data.projects || []);
      }
    } catch (error) {
      message.error("Failed to fetch git projects");
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!selectedProject || !commitMessage) return;
    try {
      const res = await authUtils.fetchWithAuth("/api/git/commit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          projectId: selectedProject,
          message: commitMessage,
        }),
      });
      if (res.ok) {
        message.success("Commit created");
        setCommitMessage("");
        fetchProjects();
      }
    } catch (error) {
      message.error("Failed to commit");
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: "24px" }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 24,
        }}
      >
        <Title level={4} style={{ color: "#fff", margin: 0 }}>
          Git Projects
        </Title>
        <Button
          icon={<SyncOutlined />}
          onClick={fetchProjects}
          className="glass-action-button"
        >
          Refresh
        </Button>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={16}>
          <Card className="glass-card" title="Projects">
            <List
              dataSource={projects}
              loading={loading}
              renderItem={(project) => (
                <List.Item
                  key={project.id}
                  style={{ cursor: "pointer", padding: "12px" }}
                  onClick={() => setSelectedProject(project.id)}
                  extra={<Avatar icon={<GitlabOutlined />} />}
                >
                  <List.Item.Meta
                    title={
                      <Text style={{ color: "#fff" }}>{project.name}</Text>
                    }
                    description={
                      <div style={{ color: "var(--text-dim)" }}>
                        Branch: {project.status.branch} • Modified:{" "}
                        {project.status.modified} •
                        {project.status.ahead > 0 &&
                          ` Ahead: ${project.status.ahead}`}
                      </div>
                    }
                  />
                </List.Item>
              )}
              locale={{ emptyText: "No git projects found" }}
            />
          </Card>
        </Col>

        <Col span={8}>
          <Card className="glass-card" title="Quick Actions">
            <Space direction="vertical" style={{ width: "100%" }}>
              <TextArea
                placeholder="Commit message..."
                value={commitMessage}
                onChange={(e) => setCommitMessage(e.target.value)}
                rows={3}
              />
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleCommit}
                disabled={!selectedProject || !commitMessage}
              >
                Commit Changes
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </motion.div>
  );
}
