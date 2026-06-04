import {
  RocketOutlined,
  CodeOutlined,
  CloudServerOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Row,
  Col,
  Typography,
  Steps,
  Progress,
  Alert,
  Space,
} from "antd";
import React, { useState } from "react";

import { GenerationForm, GenerationStatus } from "./types";

const { Option } = Select;
const { Title, Paragraph, Text } = Typography;

interface AppGenerationCardProps {
  generationForm: GenerationForm;
  setGenerationForm: (form: GenerationForm) => void;
  generationStatus: GenerationStatus;
  generationStep: number;
  generationProgress: number;
  generationResult: any;
  onGenerate: () => void;
  onGetAdvice?: () => void;
  compact?: boolean;
}

const AppGenerationCard: React.FC<AppGenerationCardProps> = ({
  generationForm,
  setGenerationForm,
  generationStatus,
  generationStep,
  generationProgress,
  generationResult,
  onGenerate,
  onGetAdvice = () => {},
}) => {
  const [experienceLevel, setExperienceLevel] = useState<
    "beginner" | "advanced"
  >("beginner");

  return (
    <Card
      title={
        <>
          <RocketOutlined
            style={{
              marginRight: 8,
              color: "var(--neon-blue)",
              textShadow: "0 0 10px var(--neon-blue)",
            }}
          />{" "}
          <span
            className="glow-text-cyan"
            style={{
              fontSize: "18px",
              textTransform: "uppercase",
              letterSpacing: "1px",
            }}
          >
            NEURAL APP GENERATOR
          </span>
        </>
      }
      style={{ marginTop: 24, border: "1px solid rgba(0, 243, 255, 0.2)" }}
      className="glass-card"
    >
      <Row gutter={24}>
        <Col xs={24} lg={12}>
          <Title level={4} style={{ color: "var(--neon-blue)" }}>
            System Requirements
          </Title>
          <Paragraph style={{ color: "var(--text-dim)" }}>
            Define the architecture parameters. SupremeAI will synthesize a
            robust application structure.
          </Paragraph>
          <Form layout="vertical" onFinish={onGenerate}>
            <div
              style={{
                marginBottom: 20,
                textAlign: "center",
                padding: "10px",
                background: "rgba(0, 243, 255, 0.05)",
                borderRadius: "8px",
                border: "1px solid rgba(0, 243, 255, 0.1)",
              }}
            >
              <Text
                strong
                style={{ marginRight: 15, color: "var(--text-main)" }}
              >
                Orchestration Level:
              </Text>
              <Select
                value={experienceLevel}
                style={{ width: 200 }}
                onChange={(val) => {
                  setExperienceLevel(val as "beginner" | "advanced");
                  if (val === "beginner") {
                    setGenerationForm({
                      ...generationForm,
                      platform: "fullstack",
                    });
                  }
                }}
              >
                <Option value="beginner">
                  I want SupremeAI to decide (Beginner)
                </Option>
                <Option value="advanced">I want full control (Advanced)</Option>
              </Select>
            </div>

            <Form.Item label="App Name" required>
              <Input
                placeholder="e.g., My Personal Budget Tracker"
                value={generationForm.name}
                onChange={(e) =>
                  setGenerationForm({ ...generationForm, name: e.target.value })
                }
              />
            </Form.Item>

            <Form.Item label="What should this app do?" required>
              <Input.TextArea
                rows={4}
                placeholder="Example: I need an app for my small grocery shop to track daily sales and stock."
                value={generationForm.description}
                onChange={(e) =>
                  setGenerationForm({
                    ...generationForm,
                    description: e.target.value,
                  })
                }
              />
            </Form.Item>

            {generationForm.platform !== "fullstack" ||
            experienceLevel === "advanced" ? (
              <>
                <Form.Item label="Target Platform">
                  <Select
                    placeholder="Select target platform"
                    value={generationForm.platform}
                    onChange={(val) =>
                      setGenerationForm({ ...generationForm, platform: val })
                    }
                  >
                    <Option value="web">Web Application (React)</Option>
                    <Option value="android">Android App (Kotlin)</Option>
                    <Option value="ios">iOS App (SwiftUI)</Option>
                    <Option value="desktop">Desktop App (JavaFX)</Option>
                    <Option value="fullstack">
                      Full-Stack Web App (Backend + Frontend)
                    </Option>
                  </Select>
                </Form.Item>

                <Form.Item label="Database Preference">
                  <Select
                    placeholder="Select database engine"
                    value={generationForm.database}
                    onChange={(val) =>
                      setGenerationForm({ ...generationForm, database: val })
                    }
                  >
                    <Option value="PostgreSQL">PostgreSQL (Relational)</Option>
                    <Option value="MySQL">MySQL (Relational)</Option>
                    <Option value="MongoDB">MongoDB (NoSQL)</Option>
                    <Option value="Firebase">Firebase Firestore</Option>
                  </Select>
                </Form.Item>
              </>
            ) : null}

            <Form.Item style={{ marginTop: 24 }}>
              <Space
                direction="vertical"
                style={{ width: "100%" }}
                size="middle"
              >
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<RocketOutlined />}
                  loading={generationStatus === "generating"}
                  size="large"
                  block
                  className="cyber-button pulse-button"
                  style={{
                    height: "50px",
                    fontSize: "18px",
                    background: "transparent",
                    color: "var(--neon-blue)",
                    border: "1px solid var(--neon-blue)",
                    boxShadow: "0 0 15px rgba(0, 243, 255, 0.3)",
                    textTransform: "uppercase",
                    letterSpacing: "1px",
                  }}
                >
                  {generationStatus === "generating"
                    ? "Synthesizing Architecture..."
                    : "Initialize Pipeline"}
                </Button>

                <Button
                  icon={<CloudServerOutlined />}
                  onClick={onGetAdvice}
                  block
                  className="cyber-button"
                  style={{
                    background: "rgba(139, 92, 246, 0.1)",
                    borderColor: "rgba(139, 92, 246, 0.5)",
                    color: "var(--neon-purple)",
                    height: "40px",
                    boxShadow: "0 0 10px rgba(139, 92, 246, 0.2)",
                  }}
                >
                  Infrastructure Concierge (AI Audit)
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Col>

        <Col xs={24} lg={12}>
          {generationStatus !== "idle" ? (
            <div
              style={{
                padding: "20px",
                background: "rgba(0,0,0,0.4)",
                borderRadius: "12px",
                minHeight: "300px",
                border: "1px solid rgba(255, 255, 255, 0.05)",
                backdropFilter: "blur(10px)",
              }}
            >
              <Title level={4} style={{ color: "var(--neon-purple)" }}>
                Synthesis Pipeline
              </Title>

              <Steps
                current={generationStep}
                direction="vertical"
                size="small"
                items={[
                  {
                    title: "Requirements Analysis",
                    subTitle: "Analyzing intent",
                    icon: <CodeOutlined />,
                    status: generationStep > 0 ? "finish" : "process",
                  },
                  {
                    title: "Architectural Blueprint",
                    subTitle: "Designing services",
                    icon: <CloudServerOutlined />,
                    status:
                      generationStep > 1
                        ? "finish"
                        : generationStep === 1
                          ? "process"
                          : "wait",
                  },
                  {
                    title: "Synthesizing Components",
                    subTitle: "Writing code",
                    icon: <RocketOutlined />,
                    status:
                      generationStep > 2
                        ? "finish"
                        : generationStep === 2
                          ? "process"
                          : "wait",
                  },
                  {
                    title: "Finalizing Deployment",
                    subTitle: "Packaging app",
                    icon: <CheckCircleOutlined />,
                    status:
                      generationStep > 3
                        ? "finish"
                        : generationStep === 3
                          ? "process"
                          : "wait",
                  },
                ]}
              />

              <div style={{ marginTop: 32 }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: 8,
                  }}
                >
                  <span style={{ color: "var(--text-dim)" }}>
                    Overall Synthesis
                  </span>
                  <span className="glow-text-cyan">{generationProgress}%</span>
                </div>
                <Progress
                  percent={generationProgress}
                  status={generationStatus === "error" ? "exception" : "active"}
                  strokeColor={{
                    "0%": "var(--neon-blue)",
                    "100%": "var(--neon-purple)",
                  }}
                  trailColor="rgba(255, 255, 255, 0.05)"
                  showInfo={false}
                />
              </div>

              {generationStatus === "success" && generationResult && (
                <div style={{ marginTop: 24 }}>
                  <Alert
                    message="System Synthesis Complete!"
                    description={`Your ${generationForm.platform} application has been successfully generated.`}
                    type="success"
                    showIcon
                    style={{ marginBottom: 16, border: "1px solid #52c41a" }}
                  />

                  <Space direction="vertical" style={{ width: "100%" }}>
                    <div style={{ display: "flex", gap: "10px" }}>
                      <Button
                        type="primary"
                        icon={<RocketOutlined />}
                        href={generationResult.previewUrl || "#"}
                        target="_blank"
                        style={{ flex: 1 }}
                      >
                        Live Preview
                      </Button>
                      <Button
                        icon={<CodeOutlined />}
                        href={generationResult.repoUrl || "#"}
                        target="_blank"
                        style={{ flex: 1 }}
                      >
                        Source Code
                      </Button>
                    </div>
                    <Button
                      block
                      icon={<CloudServerOutlined />}
                      onClick={onGetAdvice}
                      className="cyber-button"
                      style={{
                        background: "rgba(0, 243, 255, 0.1)",
                        color: "var(--neon-blue)",
                        borderColor: "rgba(0, 243, 255, 0.3)",
                      }}
                    >
                      View Optimized Hosting Plan
                    </Button>
                  </Space>
                </div>
              )}

              {generationStatus === "error" && (
                <Alert
                  message="Pipeline Disrupted"
                  description="An error occurred during the generation process. Please check logs and try again."
                  type="error"
                  showIcon
                  style={{
                    marginTop: 24,
                    border: "1px solid var(--neon-red)",
                    background: "rgba(255, 77, 79, 0.1)",
                  }}
                />
              )}
            </div>
          ) : (
            <div
              style={{
                height: "100%",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                background: "rgba(0,0,0,0.3)",
                borderRadius: "12px",
                padding: "40px",
                border: "1px dashed rgba(255,255,255,0.1)",
              }}
            >
              <div style={{ textAlign: "center" }}>
                <RocketOutlined
                  style={{
                    fontSize: "48px",
                    color: "var(--neon-blue)",
                    opacity: 0.5,
                    marginBottom: "16px",
                    filter: "drop-shadow(0 0 10px var(--neon-blue))",
                  }}
                />
                <Paragraph style={{ color: "var(--text-dim)" }}>
                  Configure your requirements on the left to start the automated
                  application generation pipeline.
                </Paragraph>
              </div>
            </div>
          )}
        </Col>
      </Row>
    </Card>
  );
};

export default AppGenerationCard;
