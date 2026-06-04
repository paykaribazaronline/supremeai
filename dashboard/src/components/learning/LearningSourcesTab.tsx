import {
  PlusOutlined,
  LinkOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import {
  Card,
  Button,
  List,
  Typography,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  InputNumber,
  Empty,
  message,
  Spin,
  Switch,
  Popconfirm,
} from "antd";
import React, { useState } from "react";

import { authUtils } from "../../lib/authUtils";

const { Title, Text } = Typography;

interface LearningSourceItem {
  id: string;
  url: string;
  domain: string;
  detectedFocus: string;
  manualFocus: string | null;
  effectiveFocus: string;
  enabled: boolean;
  priority: number;
  successCount: number;
  failureCount: number;
  lastScrapedAt: string | null;
  createdAt: string | null;
  notes: string;
}

interface LearningSourcesTabProps {
  sources: LearningSourceItem[];
  onRefresh: () => void;
}

const LearningSourcesTab: React.FC<LearningSourcesTabProps> = ({
  sources,
  onRefresh,
}) => {
  const [detecting, setDetecting] = useState(false);
  const [previewFocus, setPreviewFocus] = useState<string>("");
  const [previewDomain, setPreviewDomain] = useState<string>("");
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();

  const handleUrlBlur = async () => {
    const url = form.getFieldValue("url");
    if (!url) {
      setPreviewFocus("");
      return;
    }
    setDetecting(true);
    try {
      const resp = await authUtils.fetchWithAuth(
        "/api/admin/learning/sources/detect-focus",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ url }),
        },
      );
      if (resp.ok) {
        const data = await resp.json();
        setPreviewDomain(data.domain || "");
        setPreviewFocus(data.detectedFocus || "general");
        form.setFieldValue("manualFocus", data.detectedFocus || "general");
      }
    } catch {
      /* silently ignore — fallback manually */
    } finally {
      setDetecting(false);
    }
  };

  const handleAdd = async (values: any) => {
    try {
      const resp = await authUtils.fetchWithAuth(
        "/api/admin/learning/sources",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            url: values.url,
            manualFocus: values.manualFocus || null,
            priority: values.priority ?? 5,
            notes: values.notes || null,
          }),
        },
      );
      if (resp.ok) {
        const data = await resp.json();
        message.success(data.message || "সোর্স যোগ করা হয়েছে");
        setModalOpen(false);
        form.resetFields();
        setPreviewFocus("");
        setPreviewDomain("");
        onRefresh();
      } else {
        const err = await resp.json();
        message.error(err.message || "সোর্স যোগ ব্যর্থ");
      }
    } catch {
      message.error("সার্ভার কানেকশন এরর");
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const resp = await authUtils.fetchWithAuth(
        `/api/admin/learning/sources/${id}`,
        { method: "DELETE" },
      );
      if (resp.ok) {
        message.success("সোর্স মুছে ফেলা হয়েছে");
        onRefresh();
      }
    } catch {
      message.error("মুছে ফেলা যায়নি");
    }
  };

  const handleToggle = async (id: string) => {
    try {
      const resp = await authUtils.fetchWithAuth(
        `/api/admin/learning/sources/${id}/toggle`,
        { method: "POST" },
      );
      if (resp.ok) {
        const data = await resp.json();
        message.success(
          data.enabled ? "সোর্স সক্রিয় করা হয়েছে" : "সোর্স নিষ্ক্রিয় করা হয়েছে",
        );
        onRefresh();
      }
    } catch {
      message.error("টগল ব্যর্থ");
    }
  };

  return (
    <Card bordered={false} className="glass-card">
      <div
        style={{
          marginBottom: 16,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <div>
          <Title level={4} style={{ margin: 0, color: "#fff" }}>
            Learning Sources
          </Title>
          <Text type="secondary" style={{ color: "rgba(255,255,255,0.45)" }}>
            ওয়েবসাইট যোগ করুন — সিস্টেম নিজে টপিক ভাগ করে পাবে
          </Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            Refresh
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              setPreviewFocus("");
              setPreviewDomain("");
              setModalOpen(true);
            }}
            style={{
              background: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
              border: "none",
            }}
          >
            Add URL
          </Button>
        </Space>
      </div>

      <List
        dataSource={sources}
        renderItem={(item) => (
          <List.Item
            style={{
              background: "rgba(255,255,255,0.02)",
              border: "1px solid rgba(255,255,255,0.05)",
              borderRadius: 8,
              marginBottom: 8,
              padding: "12px 20px",
            }}
            actions={[
              <Popconfirm
                title="এই সোর্সটি মুছে ফেলবেন?"
                onConfirm={() => handleDelete(item.id)}
              >
                <Button type="text" danger icon={<DeleteOutlined />} />
              </Popconfirm>,
            ]}
            extra={
              <Space>
                <Switch
                  checked={item.enabled}
                  onChange={() => handleToggle(item.id)}
                  checkedChildren="ON"
                  unCheckedChildren="OFF"
                />
                <Tag
                  color={item.effectiveFocus !== "general" ? "blue" : "default"}
                  icon={<ThunderboltOutlined />}
                >
                  {item.manualFocus || item.detectedFocus}
                </Tag>
              </Space>
            }
          >
            <List.Item.Meta
              avatar={
                <LinkOutlined style={{ fontSize: 22, color: "#3b82f6" }} />
              }
              title={
                <span style={{ color: "#fff", fontWeight: 600 }}>
                  {item.domain}
                </span>
              }
              description={
                <Space direction="vertical" size={2} style={{ width: "100%" }}>
                  <Text
                    copyable
                    style={{ color: "rgba(255,255,255,0.45)", fontSize: 12 }}
                  >
                    {item.url}
                  </Text>
                  <Space wrap size={4}>
                    <Tag
                      color={item.enabled ? "processing" : "default"}
                      style={{ fontSize: 11 }}
                    >
                      {item.enabled ? "Active" : "Paused"}
                    </Tag>
                    {item.manualFocus && (
                      <Tag color="gold" style={{ fontSize: 11 }}>
                        Manual: {item.manualFocus}
                      </Tag>
                    )}
                    <Tag style={{ fontSize: 11 }}>
                      Priority: {item.priority}
                    </Tag>
                    {item.successCount > 0 && (
                      <Tag color="green" style={{ fontSize: 11 }}>
                        OK: {item.successCount}
                      </Tag>
                    )}
                    {item.failureCount > 0 && (
                      <Tag color="red" style={{ fontSize: 11 }}>
                        Fail: {item.failureCount}
                      </Tag>
                    )}
                    {item.lastScrapedAt && (
                      <Tag style={{ fontSize: 11 }}>
                        Last:{" "}
                        {new Date(item.lastScrapedAt).toLocaleDateString()}
                      </Tag>
                    )}
                  </Space>
                  {item.notes && (
                    <Text
                      type="secondary"
                      style={{ color: "rgba(255,255,255,0.3)", fontSize: 12 }}
                    >
                      {item.notes}
                    </Text>
                  )}
                </Space>
              }
            />
          </List.Item>
        )}
        locale={{
          emptyText: (
            <Empty
              description={
                <span style={{ color: "rgba(255,255,255,0.45)" }}>
                  কোনো লার্নিং সোর্স যোগ করা হয়নি
                </span>
              }
            >
              <Button
                type="primary"
                onClick={() => {
                  form.resetFields();
                  setPreviewFocus("");
                  setModalOpen(true);
                }}
              >
                প্রথম সোর্স যোগ করুন
              </Button>
            </Empty>
          ),
        }}
      />

      {/* Add Source Modal */}
      <Modal
        title="নতুন লার্নিং সোর্স যোগ করুন"
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          setPreviewFocus("");
          setPreviewDomain("");
        }}
        onOk={() => form.submit()}
        okText="যোগ করুন"
        cancelText="বাতিল"
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleAdd}>
          <Form.Item
            name="url"
            label="ওয়েবসাইট URL"
            rules={[{ required: true, message: "দয়া করে URL দিন" }]}
            style={{ marginBottom: 0 }}
          >
            <Input
              placeholder="যেমন: https://growthhackers.com"
              prefix={<LinkOutlined />}
              onBlur={handleUrlBlur}
              suffix={detecting ? <Spin size="small" /> : null}
            />
          </Form.Item>

          {previewFocus && (
            <div style={{ marginTop: 8, marginBottom: 16 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                📡 সিস্টেম ইঙ্গিত:{" "}
                <Tag color="blue" style={{ margin: 0 }}>
                  {previewDomain}
                </Tag>{" "}
                — <strong>{previewFocus}</strong> টপিক থেকে শিখবে
              </Text>
            </div>
          )}

          <Form.Item
            name="manualFocus"
            label="লার্নিং এরিয়া (ঐচ্ছিক)"
            help="যদি স্বয়ংক্রিয় ডিটেকশন ভুল থাকে তবে সরাসরি টপিক দিন।"
          >
            <Input placeholder="যেমন: marketing, security, ai_research" />
          </Form.Item>

          <Form.Item
            name="notes"
            label="নোট (ঐচ্ছিক)"
            style={{ marginBottom: 12 }}
          >
            <Input.TextArea rows={2} placeholder="এই সাইটের ব্যাখ্যা..." />
          </Form.Item>

          <Form.Item
            name="priority"
            label="প্রাথমিকতা (1-10)"
            initialValue={5}
            style={{ marginBottom: 0 }}
          >
            <InputNumber min={1} max={10} style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default LearningSourcesTab;
