import {
  ReloadOutlined,
  BookOutlined,
  PlayCircleOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import {
  Card,
  Button,
  List,
  Typography,
  Space,
  Tag,
  Empty,
  Modal,
  Form,
  Input,
  message,
} from "antd";
import React, { useState } from "react";

import { authUtils } from "../../lib/authUtils";

const { Title, Text } = Typography;

interface KnowledgeDomain {
  id: string;
  name: string;
  status: string;
  keywords: string[];
  knowledgeCount: number;
}

interface KnowledgeDomainsTabProps {
  domains: KnowledgeDomain[];
  onRefresh: () => void;
  onViewKnowledge: (id: string) => void;
}

const KnowledgeDomainsTab: React.FC<KnowledgeDomainsTabProps> = ({
  domains,
  onRefresh,
  onViewKnowledge,
}) => {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleCreate = async (values: any) => {
    setLoading(true);
    try {
      const keywords = values.keywords
        ? values.keywords.split(",").map((k: string) => k.trim())
        : [];
      const resp = await authUtils.fetchWithAuth(
        "/api/admin/knowledge/domains",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: values.name,
            keywords: keywords,
          }),
        },
      );

      if (resp.ok) {
        message.success("নতুন নলেজ ডোমেইন তৈরি করা হয়েছে");
        setIsModalVisible(false);
        form.resetFields();
        onRefresh();
      } else {
        const error = await resp.json();
        message.error(error.message || "ডোমেইন তৈরি করতে সমস্যা হয়েছে");
      }
    } catch (error) {
      message.error("সার্ভার কানেকশন এরর");
    } finally {
      setLoading(false);
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
            Knowledge Domains
          </Title>
          <Text type="secondary" style={{ color: "rgba(255,255,255,0.45)" }}>
            সিস্টেমের সক্রিয় জ্ঞান ভাণ্ডারসমূহ
          </Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            Refresh
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setIsModalVisible(true)}
            style={{
              background: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
              border: "none",
            }}
          >
            Add Domain
          </Button>
        </Space>
      </div>

      <List
        dataSource={domains}
        renderItem={(domain) => (
          <List.Item
            actions={[
              <Button type="link" onClick={() => onViewKnowledge(domain.id)}>
                View Knowledge
              </Button>,
              <Button type="link" icon={<PlayCircleOutlined />}>
                Process Job
              </Button>,
            ]}
          >
            <List.Item.Meta
              avatar={
                <BookOutlined style={{ fontSize: 24, color: "#3b82f6" }} />
              }
              title={
                <span style={{ color: "#fff", fontWeight: 600 }}>
                  {domain.name}
                </span>
              }
              description={
                <Space wrap>
                  {domain.keywords.map((k) => (
                    <Tag
                      key={k}
                      style={{
                        background: "rgba(255,255,255,0.05)",
                        color: "rgba(255,255,255,0.65)",
                        border: "1px solid rgba(255,255,255,0.1)",
                      }}
                    >
                      {k}
                    </Tag>
                  ))}
                </Space>
              }
            />
            <div style={{ textAlign: "right", marginRight: 24 }}>
              <Tag
                color={domain.status === "LEARNING" ? "processing" : "success"}
              >
                {domain.status}
              </Tag>
              <br />
              <Text
                type="secondary"
                style={{ fontSize: 12, color: "rgba(255,255,255,0.45)" }}
              >
                Nodes: {domain.knowledgeCount}
              </Text>
            </div>
          </List.Item>
        )}
        locale={{
          emptyText: (
            <Empty
              description={
                <span style={{ color: "rgba(255,255,255,0.45)" }}>
                  কোনো ডোমেইন পাওয়া যায়নি
                </span>
              }
            >
              <Button type="primary" onClick={() => setIsModalVisible(true)}>
                Create First Domain
              </Button>
            </Empty>
          ),
        }}
      />

      <Modal
        title="নতুন নলেজ ডোমেইন যোগ করুন"
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        onOk={() => form.submit()}
        confirmLoading={loading}
        okText="তৈরি করুন"
        cancelText="বাতিল"
        className="modern-modal"
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            name="name"
            label="ডোমেইন নাম"
            rules={[{ required: true, message: "দয়া করে ডোমেইনের নাম লিখুন" }]}
          >
            <Input placeholder="যেমন: Java Spring Boot, Cloud Security ইত্যাদি" />
          </Form.Item>
          <Form.Item
            name="keywords"
            label="কীওয়ার্ডসমূহ (কমা দিয়ে আলাদা করুন)"
            extra="এই কীওয়ার্ডগুলো AI-কে সঠিক ডোমেইন খুঁজে পেতে সাহায্য করবে"
          >
            <Input placeholder="যেমন: spring, security, firewall" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default KnowledgeDomainsTab;
