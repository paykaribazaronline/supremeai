import {
  UploadOutlined,
  GithubOutlined,
  FileZipOutlined,
  SearchOutlined,
  SecurityScanOutlined,
  BugOutlined,
  ApiOutlined,
  ApartmentOutlined,
  AimOutlined,
  ThunderboltOutlined,
  DatabaseOutlined,
} from "@ant-design/icons";
import {
  Form,
  Input,
  Select,
  Button,
  Upload,
  Space,
  Typography,
  message,
  Radio,
  Checkbox,
  Card,
  Divider,
  Switch,
  Tooltip,
} from "antd";
import type { UploadFile } from "antd/es/upload/interface";
import React, { useState } from "react";

import type { AgentConfig } from "../types";

const { Title, Text } = Typography;
const { Option } = Select;

interface ProjectUploadFormProps {
  onSubmit: (values: any) => void;
  loading: boolean;
}

const ProjectUploadForm: React.FC<ProjectUploadFormProps> = ({
  onSubmit,
  loading,
}) => {
  const [form] = Form.useForm();
  const [file, setFile] = useState<UploadFile | null>(null);
  const [sourceType, setSourceType] = useState<"git" | "zip">("git");

  const availableAgents: AgentConfig[] = [
    {
      key: "SECURITY",
      name: "Security Scanner",
      description:
        "Hardcoded secrets, SQL injection, XSS, path traversal, vulnerable dependencies",
      enabled: true,
      category: "SECURITY",
    },
    {
      key: "QUALITY",
      name: "Code Quality",
      description:
        "Complexity analysis, code smells, naming conventions, best practices",
      enabled: true,
      category: "QUALITY",
    },
    {
      key: "DEPENDENCIES",
      name: "Dependency Analysis",
      description:
        "Outdated packages, security vulnerabilities, license compliance",
      enabled: true,
      category: "DEPENDENCIES",
    },
    {
      key: "ARCHITECTURE",
      name: "Architecture Review",
      description:
        "Design patterns, coupling/cohesion, SOLID principles, layering violations",
      enabled: true,
      category: "ARCHITECTURE",
    },
  ];

  const onFinish = (values: any) => {
    if (sourceType === "git" && !values.gitUrl) {
      message.error("Please provide a Git repository URL");
      return;
    }
    if (sourceType === "zip" && !file) {
      message.error("Please select a ZIP file");
      return;
    }

    // Convert agent checkboxes to agent map
    const agents: Record<string, boolean> = {};
    availableAgents.forEach((agent) => {
      agents[agent.key] = values.agents?.includes(agent.key) ?? agent.enabled;
    });

    const payload: any = {
      ...values,
      agents,
      sourceType,
    };
    if (sourceType === "zip" && file?.originFileObj) {
      payload.zipFile = file.originFileObj;
    }

    onSubmit(payload);
  };

  const handleUploadChange = (info: any) => {
    if (info.file.status === "done" || info.file.originFileObj)
      setFile(info.file);
  };

  const beforeUpload = (file: File) => {
    const isZip = file.type === "application/zip" || file.name.endsWith(".zip");
    if (!isZip) {
      message.error("Only ZIP files allowed");
      return false;
    }
    if (file.size / 1024 / 1024 >= 100) {
      message.error("Max 100MB");
      return false;
    }
    setFile({ name: file.name, originFileObj: file } as UploadFile);
    return false;
  };

  const getAgentIcon = (category: string) => {
    switch (category) {
      case "SECURITY":
        return <SecurityScanOutlined style={{ color: "#ff3b30" }} />;
      case "QUALITY":
        return <BugOutlined style={{ color: "#ff9500" }} />;
      case "DEPENDENCIES":
        return <ApiOutlined style={{ color: "#007aff" }} />;
      case "ARCHITECTURE":
        return <ApartmentOutlined style={{ color: "#34c759" }} />;
      default:
        return <SearchOutlined />;
    }
  };

  return (
    <Form
      form={form}
      layout="vertical"
      onFinish={onFinish}
      initialValues={{
        projectType: "generic",
        branch: "main",
        agents: availableAgents.filter((a) => a.enabled).map((a) => a.key),
        ragEnabled: false,
        incrementalEnabled: false,
        fixesEnabled: true,
      }}
    >
      <Form.Item label="Project Type" name="projectType">
        <Select style={{ width: "100%" }}>
          <Option value="generic">Generic (Multi-language)</Option>
          <Option value="java">Java / Spring Boot</Option>
          <Option value="javascript">JavaScript / TypeScript</Option>
          <Option value="python">Python</Option>
          <Option value="go">Go</Option>
          <Option value="php">PHP</Option>
          <Option value="dotnet">C# / .NET</Option>
        </Select>
      </Form.Item>

      <Form.Item label="Source" required>
        <Space direction="vertical" style={{ width: "100%" }}>
          <Radio.Group
            value={sourceType}
            onChange={(e) => setSourceType(e.target.value)}
            style={{ marginBottom: 12 }}
          >
            <Radio.Button value="git">
              <Space>
                <GithubOutlined /> Git Repository
              </Space>
            </Radio.Button>
            <Radio.Button value="zip">
              <Space>
                <FileZipOutlined /> ZIP Upload
              </Space>
            </Radio.Button>
          </Radio.Group>

          {sourceType === "git" && (
            <>
              <Form.Item name="gitUrl" noStyle>
                <Input
                  placeholder="Enter Git repository URL"
                  prefix={<GithubOutlined />}
                  style={{ marginBottom: 8 }}
                />
              </Form.Item>

              <Form.Item name="branch" noStyle>
                <Input
                  placeholder="Branch (default: main)"
                  style={{ marginBottom: 8, width: 200 }}
                />
              </Form.Item>
            </>
          )}

          {sourceType === "zip" && (
            <Upload
              customRequest={({ onSuccess }) =>
                setTimeout(() => onSuccess?.({}), 0)
              }
              onChange={handleUploadChange}
              beforeUpload={beforeUpload}
              showUploadList={true}
              maxCount={1}
              accept=".zip"
            >
              <Button icon={<UploadOutlined />}>Select ZIP (max 100MB)</Button>
            </Upload>
          )}
        </Space>
      </Form.Item>

      <Divider />

      <Form.Item label="Analysis Agents" name="agents">
        <div style={{ display: "grid", gap: 12 }}>
          {availableAgents.map((agent) => (
            <Card
              key={agent.key}
              size="small"
              style={{
                border: "1px solid rgba(255,255,255,0.1)",
                background: "rgba(0,0,0,0.2)",
              }}
            >
              <Space align="start">
                <Checkbox value={agent.key} defaultChecked={agent.enabled}>
                  <Space>
                    {getAgentIcon(agent.category)}
                    <div>
                      <Text strong style={{ color: "#fff" }}>
                        {agent.name}
                      </Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {agent.description}
                      </Text>
                    </div>
                  </Space>
                </Checkbox>
              </Space>
            </Card>
          ))}
        </div>
        <Text type="secondary" style={{ fontSize: 12, marginTop: 8 }}>
          Select which analysis agents to run. Multiple agents run in parallel
          for faster results.
        </Text>
      </Form.Item>

      <Divider />

      <Form.Item label="Phase 3 Features">
        <div style={{ display: "grid", gap: 12 }}>
          <Card
            size="small"
            style={{
              border: "1px solid rgba(0,243,255,0.2)",
              background: "rgba(0,243,255,0.05)",
            }}
          >
            <Space align="start">
              <Form.Item name="ragEnabled" valuePropName="checked" noStyle>
                <Switch
                  checkedChildren={<AimOutlined />}
                  unCheckedChildren={<AimOutlined />}
                />
              </Form.Item>
              <div>
                <Space>
                  <Text strong style={{ color: "#fff" }}>
                    RAG Context Selection
                  </Text>
                  <Tooltip title="Use vector embeddings to find relevant files instead of scanning all files. Reduces analysis time for large projects.">
                    <AimOutlined style={{ color: "var(--neon-blue)" }} />
                  </Tooltip>
                </Space>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Use AI-powered vector search to analyze only relevant code
                  chunks
                </Text>
              </div>
            </Space>
          </Card>

          <Card
            size="small"
            style={{
              border: "1px solid rgba(52,199,89,0.2)",
              background: "rgba(52,199,89,0.05)",
            }}
          >
            <Space align="start">
              <Form.Item
                name="incrementalEnabled"
                valuePropName="checked"
                noStyle
              >
                <Switch
                  checkedChildren={<DatabaseOutlined />}
                  unCheckedChildren={<DatabaseOutlined />}
                />
              </Form.Item>
              <div>
                <Space>
                  <Text strong style={{ color: "#fff" }}>
                    Incremental Analysis
                  </Text>
                  <Tooltip title="Only analyze changed files and their dependencies. Requires Git repository.">
                    <DatabaseOutlined style={{ color: "#34c759" }} />
                  </Tooltip>
                </Space>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Only analyze changed files + impacted dependencies (requires
                  Git)
                </Text>
              </div>
            </Space>
          </Card>

          <Card
            size="small"
            style={{
              border: "1px solid rgba(255,204,0,0.2)",
              background: "rgba(255,204,0,0.05)",
            }}
          >
            <Space align="start">
              <Form.Item name="fixesEnabled" valuePropName="checked" noStyle>
                <Switch
                  checkedChildren={<ThunderboltOutlined />}
                  unCheckedChildren={<ThunderboltOutlined />}
                  defaultChecked
                />
              </Form.Item>
              <div>
                <Space>
                  <Text strong style={{ color: "#fff" }}>
                    LLM Fix Suggestions
                  </Text>
                  <Tooltip title="Generate AI-powered fix suggestions for HIGH and CRITICAL findings.">
                    <ThunderboltOutlined style={{ color: "#ffcc00" }} />
                  </Tooltip>
                </Space>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Auto-generate code fixes for HIGH/CRITICAL issues using AI
                </Text>
              </div>
            </Space>
          </Card>
        </div>
      </Form.Item>

      <Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          loading={loading}
          icon={<SearchOutlined />}
          style={{
            background: "var(--neon-blue)",
            borderColor: "var(--neon-blue)",
          }}
        >
          Start Multi-Agent Analysis
        </Button>
      </Form.Item>
    </Form>
  );
};

export default ProjectUploadForm;
