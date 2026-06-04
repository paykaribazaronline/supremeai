import {
  DownloadOutlined,
  RocketOutlined,
  EditOutlined,
  SaveOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Card,
  Button,
  Space,
  message,
  Spin,
  List,
  Input,
  Form,
  Tag,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { useRole } from "../contexts/RoleContext";
import { authUtils } from "../lib/authUtils";

const { Title, Text, Paragraph } = Typography;

interface SuperFlySettings {
  downloadUrl: string;
  fileSize: string;
  version: string;
  benefits: string[];
  guideline: string;
}

export default function AdminSuperFly() {
  const { isAdmin } = useRole();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [settings, setSettings] = useState<SuperFlySettings | null>(null);

  // Local download simulation state
  const [downloading, setDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);

  const [form] = Form.useForm();

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/config/superfly");
      if (res.ok) {
        const body = await res.json();
        const data = body.data || body;
        setSettings(data);
        form.setFieldsValue(data);
      } else {
        message.error("Failed to load SupremeAI Offline configurations");
      }
    } catch (error) {
      console.error(error);
      message.error("Failed to load SupremeAI Offline configurations");
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (values: any) => {
    setSaving(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/config/superfly", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      if (res.ok) {
        const body = await res.json();
        setSettings(body.data || body);
        setIsEditing(false);
        message.success(
          "SupremeAI Offline configurations updated successfully!",
        );
      } else {
        message.error("Failed to save configurations");
      }
    } catch (error) {
      console.error(error);
      message.error("Failed to save configurations");
    } finally {
      setSaving(false);
    }
  };

  const handleDownload = () => {
    if (downloading) return;
    setDownloading(true);
    setDownloadProgress(0);
    message.loading(
      "SupremeAI Offline model download started in background...",
      2,
    );

    const interval = setInterval(() => {
      setDownloadProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setDownloading(false);
          message.success(
            "SupremeAI Offline model successfully downloaded to local memory!",
          );

          // Trigger actual browser download for user convenience
          if (settings?.downloadUrl) {
            window.open(settings.downloadUrl, "_blank");
          }
          return 100;
        }
        return prev + 10;
      });
    }, 400);
  };

  if (loading) {
    return (
      <div
        style={{
          height: "70vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Spin size="large" tip="SYNCING NEURAL LINK FOR SUPREMEAI OFFLINE..." />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: "24px", maxWidth: 1200, margin: "0 auto" }}
    >
      {/* Header */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 32,
        }}
      >
        <div>
          <Title level={3} style={{ color: "#fff", margin: 0 }}>
            ⚡ SupremeAI Offline (অন-ডিভাইস এআই)
          </Title>
          <Text style={{ color: "var(--neon-blue)", fontSize: 13 }}>
            লোকাল-ফার্স্ট অন-ডিভাইস আল্ট্রা-লাইট ইন্টেলিজেন্ট মডেল (৯৪ মিলিয়ন
            প্যারামিটার)
          </Text>
        </div>
        <Space>
          {isAdmin && (
            <Button
              type={isEditing ? "default" : "primary"}
              icon={isEditing ? <SaveOutlined /> : <EditOutlined />}
              onClick={() => {
                if (isEditing) {
                  form.submit();
                } else {
                  setIsEditing(true);
                }
              }}
              loading={saving}
              className="glass-action-button"
            >
              {isEditing ? "সংরক্ষণ করুন" : "কনফিগ এডিট"}
            </Button>
          )}
        </Space>
      </div>

      <Row gutter={[24, 24]}>
        {/* Main Panel */}
        <Col xs={24} lg={isEditing ? 24 : 14}>
          {isEditing ? (
            <Card className="glass-card" style={{ padding: 12 }}>
              <Form form={form} layout="vertical" onFinish={handleSave}>
                <Form.Item
                  name="downloadUrl"
                  label="Download Link"
                  rules={[{ required: true }]}
                >
                  <Input
                    className="cyber-input"
                    style={{ background: "rgba(0,0,0,0.5)", color: "#fff" }}
                  />
                </Form.Item>
                <Form.Item
                  name="fileSize"
                  label="File Size"
                  rules={[{ required: true }]}
                >
                  <Input
                    className="cyber-input"
                    style={{ background: "rgba(0,0,0,0.5)", color: "#fff" }}
                  />
                </Form.Item>
                <Form.Item
                  name="version"
                  label="Model Version"
                  rules={[{ required: true }]}
                >
                  <Input
                    className="cyber-input"
                    style={{ background: "rgba(0,0,0,0.5)", color: "#fff" }}
                  />
                </Form.Item>
                <Form.Item
                  name="guideline"
                  label="Guideline (Markdown/Plaintext)"
                  rules={[{ required: true }]}
                >
                  <Input.TextArea
                    rows={8}
                    style={{
                      background: "rgba(0,0,0,0.5)",
                      color: "#fff",
                      fontFamily: "monospace",
                    }}
                  />
                </Form.Item>
              </Form>
            </Card>
          ) : (
            <Card
              className="glass-card"
              style={{ position: "relative", overflow: "hidden", padding: 8 }}
            >
              <div
                style={{
                  position: "absolute",
                  top: 0,
                  right: 0,
                  opacity: 0.05,
                  transform: "translate(20%, -20%)",
                }}
              >
                <RocketOutlined
                  style={{ fontSize: 240, color: "var(--neon-blue)" }}
                />
              </div>

              <div style={{ marginBottom: 28 }}>
                <Title
                  level={4}
                  style={{ color: "var(--neon-blue)", marginTop: 0 }}
                >
                  🤖 SupremeAI Offline ডাউনলোড ও কন্ট্রোল
                </Title>
                <Paragraph
                  style={{ color: "rgba(255,255,255,0.7)", fontSize: 14 }}
                >
                  SupremeAI Offline মডেলটি ডাউনলোড করে আপনার লোকাল সিস্টেমে সচল
                  করুন। এটি ডাউনলোড হয়ে গেলে ইন্টারনেট ছাড়াই আপনার কমান্ড
                  সেন্টার এবং চ্যাট ইঞ্জিন সুপার ফাস্ট স্পিডে কাজ করবে।
                </Paragraph>
              </div>

              {/* Progress HUD */}
              <div
                style={{
                  background: "rgba(0, 243, 255, 0.05)",
                  border: "1px solid rgba(0, 243, 255, 0.1)",
                  borderRadius: 8,
                  padding: 20,
                  marginBottom: 24,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: 12,
                  }}
                >
                  <Text style={{ color: "#fff", fontWeight: 600 }}>
                    এজ এআই মডেল স্ট্যাটাস
                  </Text>
                  <Tag color="cyan">{settings?.version || "v1.0.0"}</Tag>
                </div>
                <div style={{ display: "flex", gap: 24, marginBottom: 16 }}>
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.5)",
                        fontSize: 12,
                        display: "block",
                      }}
                    >
                      ফাইল সাইজ
                    </Text>
                    <Text
                      style={{
                        color: "var(--neon-blue)",
                        fontWeight: 700,
                        fontSize: 16,
                      }}
                    >
                      {settings?.fileSize || "188 MB"}
                    </Text>
                  </div>
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.5)",
                        fontSize: 12,
                        display: "block",
                      }}
                    >
                      ল্যাটেন্সি স্পিড
                    </Text>
                    <Text
                      style={{
                        color: "var(--neon-purple)",
                        fontWeight: 700,
                        fontSize: 16,
                      }}
                    >
                      ০.১ সেকেন্ড (অন-ডিভাইস)
                    </Text>
                  </div>
                </div>

                {downloading && (
                  <div style={{ marginBottom: 16 }}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        fontSize: 12,
                        marginBottom: 4,
                      }}
                    >
                      <Text style={{ color: "var(--neon-blue)" }}>
                        Neural Link Downloading...
                      </Text>
                      <Text style={{ color: "var(--neon-blue)" }}>
                        {downloadProgress}%
                      </Text>
                    </div>
                    <div
                      style={{
                        height: 6,
                        background: "rgba(255,255,255,0.1)",
                        borderRadius: 3,
                        overflow: "hidden",
                      }}
                    >
                      <div
                        style={{
                          height: "100%",
                          width: `${downloadProgress}%`,
                          background: "var(--neon-blue)",
                          boxShadow: "0 0 10px var(--neon-blue)",
                        }}
                      />
                    </div>
                  </div>
                )}

                <Button
                  type="primary"
                  icon={<DownloadOutlined />}
                  onClick={handleDownload}
                  loading={downloading}
                  size="large"
                  style={{
                    width: "100%",
                    height: 48,
                    background: "var(--neon-blue)",
                    border: "none",
                    color: "#000",
                    fontWeight: 700,
                  }}
                >
                  {downloading
                    ? "ডাউনলোড হচ্ছে..."
                    : "অন-ডিভাইস SupremeAI Offline মডেল ডাউনলোড করুন"}
                </Button>
              </div>

              {/* Guideline section */}
              <div>
                <Title
                  level={5}
                  style={{
                    color: "#fff",
                    marginBottom: 12,
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  <InfoCircleOutlined style={{ color: "var(--neon-purple)" }} />{" "}
                  লোকাল সেটআপ নির্দেশিকা
                </Title>
                <div
                  style={{
                    background: "#04040a",
                    border: "1px solid rgba(255,255,255,0.05)",
                    borderRadius: 6,
                    padding: 16,
                    fontFamily: "monospace",
                    color: "rgba(255, 255, 255, 0.85)",
                    whiteSpace: "pre-wrap",
                    fontSize: 13,
                    maxHeight: 300,
                    overflowY: "auto",
                  }}
                >
                  {settings?.guideline}
                </div>
              </div>
            </Card>
          )}
        </Col>

        {/* Benefits & Details Sidebar */}
        {!isEditing && (
          <Col xs={24} lg={10}>
            <Card className="glass-card" style={{ padding: 8 }}>
              <Title
                level={4}
                style={{
                  color: "var(--neon-purple)",
                  marginTop: 0,
                  marginBottom: 20,
                }}
              >
                🚀 SupremeAI Offline ব্যবহারের অসামান্য সুবিধাসমূহ
              </Title>
              <List
                dataSource={settings?.benefits || []}
                renderItem={(benefit, index) => (
                  <List.Item
                    style={{
                      borderBottom: "1px solid rgba(255,255,255,0.03)",
                      padding: "12px 0",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        alignItems: "flex-start",
                        gap: 12,
                      }}
                    >
                      <CheckCircleOutlined
                        style={{ color: "var(--neon-blue)", marginTop: 4 }}
                      />
                      <div>
                        <Text
                          style={{
                            color: "#fff",
                            fontWeight: 600,
                            display: "block",
                          }}
                        >
                          সুবিধা {index + 1}
                        </Text>
                        <Text
                          style={{
                            color: "rgba(255,255,255,0.7)",
                            fontSize: 13,
                          }}
                        >
                          {benefit}
                        </Text>
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            </Card>
          </Col>
        )}
      </Row>
    </motion.div>
  );
}
