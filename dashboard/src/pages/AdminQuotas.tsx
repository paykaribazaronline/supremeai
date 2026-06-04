// AdminQuotas.tsx - Cinematic Quota Management
import {
  PieChartOutlined,
  ReloadOutlined,
  EditOutlined,
  UndoOutlined,
  ThunderboltOutlined,
  UserOutlined,
  BarChartOutlined,
  ControlOutlined,
  SafetyCertificateOutlined,
  SecurityScanOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Progress,
  Table,
  Statistic,
  Modal,
  Form,
  InputNumber,
  Input,
  Tag,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

interface TierQuota {
  tier: string;
  quota: number;
  maxApis: number;
  maxSimulator: number;
}

interface UserUsage {
  apiKey: string;
  userId: string;
  requestCount: number;
  status: string;
  lastUsed?: string;
}

const AdminQuotas: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [quotas, setQuotas] = useState<TierQuota[]>([]);
  const [usage, setUsage] = useState<UserUsage[]>([]);
  const [isEditModalVisible, setIsEditModalVisible] = useState(false);
  const [editingTier, setEditingTier] = useState<TierQuota | null>(null);
  const [form] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [configResp, usageResp] = await Promise.all([
        authUtils.fetchWithAuth("/api/admin/quotas/config"),
        authUtils.fetchWithAuth("/api/admin/quotas/usage"),
      ]);
      const configResult = await configResp.json();
      const usageResult = await usageResp.json();
      if (configResult.success) {
        const config = configResult.data;
        setQuotas(
          Object.keys(config.tierQuotas).map((tier) => ({
            tier,
            quota: config.tierQuotas[tier],
            maxApis: config.tierMaxApis[tier] || 0,
            maxSimulator: config.tierMaxSimulatorInstalls[tier] || 0,
          })),
        );
      }
      if (usageResult.success) setUsage(usageResult.data || []);
    } catch (err) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
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
          borderBottom: "1px solid rgba(188, 19, 254, 0.2)",
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
              <PieChartOutlined
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
                RESOURCE ALLOCATION
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
              Quota <span className="text-gradient">Management</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Control neural call limits, API key caps, and simulator instance
              quotas per tier.
            </Text>
          </Col>
          <Col>
            <Button
              icon={<ReloadOutlined spin={loading} />}
              onClick={fetchData}
              className="glass-action-button"
            >
              Refresh Limits
            </Button>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Key KPI Stats */}
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
                      API Consumers
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {usage.length}
                    </div>
                  </div>
                  <UserOutlined
                    style={{ color: "var(--neon-blue)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
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
                      Global Load
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      82.4%
                    </div>
                  </div>
                  <BarChartOutlined
                    style={{ color: "var(--neon-purple)", fontSize: 24 }}
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
                      Throttle Limit
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 20,
                        fontWeight: 800,
                      }}
                    >
                      OPTIMIZED
                    </div>
                  </div>
                  <SecurityScanOutlined
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
                      Neural Integrity
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 20, fontWeight: 800 }}
                    >
                      99.9%
                    </div>
                  </div>
                  <SafetyCertificateOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Tier Config & Usage Table */}
        <Col xs={24} lg={12}>
          <div className="glass-card" style={{ minHeight: 500 }}>
            <div className="glass-card-title">
              Tier Configuration Matrix <ControlOutlined />
            </div>
            <Table
              dataSource={quotas}
              pagination={false}
              loading={loading}
              columns={[
                {
                  title: "TIER",
                  dataIndex: "tier",
                  key: "tier",
                  render: (t: string) => (
                    <Tag color={t === "ADMIN" ? "gold" : "blue"}>{t}</Tag>
                  ),
                },
                {
                  title: "QUOTA",
                  dataIndex: "quota",
                  key: "quota",
                  render: (q: number) => (q === -1 ? "∞" : q.toLocaleString()),
                },
                { title: "MAX APIS", dataIndex: "maxApis", key: "maxApis" },
                {
                  title: "ACTION",
                  key: "action",
                  render: (_, r) => (
                    <Button
                      type="text"
                      icon={<EditOutlined />}
                      style={{ color: "var(--neon-blue)" }}
                      onClick={() => {
                        setEditingTier(r);
                        form.setFieldsValue(r);
                        setIsEditModalVisible(true);
                      }}
                    >
                      Edit
                    </Button>
                  ),
                },
              ]}
              className="cyber-table"
            />
          </div>
        </Col>

        <Col xs={24} lg={12}>
          <div className="glass-card" style={{ minHeight: 500 }}>
            <div className="glass-card-title">
              Live Consumption Tracker <ThunderboltOutlined />
            </div>
            <Table
              dataSource={usage}
              pagination={{ pageSize: 8 }}
              loading={loading}
              columns={[
                {
                  title: "CONSUMER",
                  dataIndex: "userId",
                  key: "userId",
                  render: (u: string) => (
                    <Text strong style={{ color: "#fff" }}>
                      {u}
                    </Text>
                  ),
                },
                {
                  title: "USAGE",
                  dataIndex: "requestCount",
                  key: "requestCount",
                  render: (c: number) => (
                    <Progress
                      percent={Math.min(100, c / 2)}
                      size="small"
                      strokeColor={c > 150 ? "#ef4444" : "var(--neon-blue)"}
                    />
                  ),
                },
                {
                  title: "STATUS",
                  dataIndex: "status",
                  key: "status",
                  render: (s: string) => (
                    <Badge
                      status={s === "active" ? "success" : "error"}
                      text={s.toUpperCase()}
                    />
                  ),
                },
                {
                  title: "ACTION",
                  key: "action",
                  render: (_, r) => (
                    <Button type="text" danger icon={<UndoOutlined />}>
                      Reset
                    </Button>
                  ),
                },
              ]}
              className="cyber-table"
            />
          </div>
        </Col>
      </Row>

      <Modal
        title={`MODULATE TIER: ${editingTier?.tier}`}
        visible={isEditModalVisible}
        onCancel={() => setIsEditModalVisible(false)}
        onOk={() => form.submit()}
        className="admin-modal"
      >
        <Form form={form} layout="vertical" onFinish={() => {}}>
          <Form.Item name="quota" label="MONTHLY NEURAL CALLS">
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="maxApis" label="MAX API KEYS">
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="maxSimulator" label="SIMULATOR INSTANCES">
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </motion.div>
  );
};

export default AdminQuotas;
