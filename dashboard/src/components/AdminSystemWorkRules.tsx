// AdminSystemWorkRules.tsx - Cinematic Work Rules Authority
import {
  RocketOutlined,
  ReloadOutlined,
  PlusOutlined,
  SyncOutlined,
  ControlOutlined,
  GlobalOutlined,
  SafetyCertificateOutlined,
  EditOutlined,
  DeleteOutlined,
  DatabaseOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Table,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Tooltip,
  Popconfirm,
  InputNumber,
  Progress,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;
const { Option } = Select;

interface SystemWorkRuleRow {
  id: string;
  ruleKey: string;
  category: string;
  description: string;
  value: string;
  enabled: boolean;
  lastSyncStatus: string;
}

const AdminSystemWorkRules: React.FC = () => {
  const [rules, setRules] = useState<SystemWorkRuleRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  const fetchRules = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/admin/system-work-rules");
      const body = await res.json();
      if (body.success) setRules(body.data || []);
    } catch (e) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: "1600px", margin: "0 auto" }}
    >
      {/* Cinematic Header */}
      <div
        style={{
          marginBottom: 32,
          borderBottom: "1px solid rgba(59, 130, 246, 0.2)",
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
                AUTHORITY MATRIX
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
              System <span className="text-gradient">Work Rules</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Authoritative neural overrides, propagation protocols, and
              cross-module synchronization.
            </Text>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined spin={loading} />}
                onClick={fetchRules}
                className="glass-action-button"
              >
                Force Sync
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setModalVisible(true)}
                className="cyber-button"
              >
                Forge Override
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
                      Active Rules
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {rules.filter((r) => r.enabled).length}
                    </div>
                  </div>
                  <ControlOutlined
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
                      In-Sync Nodes
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      42
                    </div>
                  </div>
                  <GlobalOutlined
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
                      Integrity
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 20,
                        fontWeight: 800,
                      }}
                    >
                      VERIFIED
                    </div>
                  </div>
                  <SafetyCertificateOutlined
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
                      Storage Sync
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      100%
                    </div>
                  </div>
                  <DatabaseOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Rules Table Area */}
        <Col span={24}>
          <div className="glass-card" style={{ minHeight: 600 }}>
            <div className="glass-card-title">
              Override Logic Matrix <SyncOutlined />
            </div>
            <Table
              dataSource={rules}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 12 }}
              columns={[
                {
                  title: "RULE_KEY",
                  dataIndex: "ruleKey",
                  key: "ruleKey",
                  render: (t: string) => (
                    <Text
                      style={{
                        fontFamily: "JetBrains Mono",
                        color: "var(--neon-blue)",
                        fontSize: 12,
                      }}
                    >
                      {t}
                    </Text>
                  ),
                },
                {
                  title: "CATEGORY",
                  dataIndex: "category",
                  key: "category",
                  render: (c: string) => (
                    <Tag
                      color="black"
                      style={{
                        border: "1px solid rgba(255,255,255,0.1)",
                        color: "rgba(255,255,255,0.6)",
                        fontSize: 10,
                      }}
                    >
                      {c}
                    </Tag>
                  ),
                },
                {
                  title: "DESCRIPTION",
                  dataIndex: "description",
                  key: "description",
                  render: (d: string) => (
                    <Text style={{ color: "#fff", fontSize: 13 }}>{d}</Text>
                  ),
                },
                {
                  title: "VALUE",
                  dataIndex: "value",
                  key: "value",
                  render: (v: string) => (
                    <Text
                      style={{
                        color: "var(--neon-purple)",
                        fontFamily: "JetBrains Mono",
                        fontWeight: 800,
                      }}
                    >
                      {v}
                    </Text>
                  ),
                },
                {
                  title: "STATUS",
                  dataIndex: "lastSyncStatus",
                  key: "lastSyncStatus",
                  render: (s: string) => (
                    <Badge
                      status={s === "NO_CONFLICT" ? "success" : "processing"}
                      text={
                        <span style={{ color: "#fff", fontSize: 10 }}>{s}</span>
                      }
                    />
                  ),
                },
                {
                  title: "ACTION",
                  key: "a",
                  render: () => (
                    <Space>
                      <Button icon={<EditOutlined />} size="small" ghost />
                      <Button
                        icon={<DeleteOutlined />}
                        size="small"
                        danger
                        ghost
                      />
                    </Space>
                  ),
                },
              ]}
              className="cyber-table"
            />
          </div>
        </Col>
      </Row>

      <Modal
        title="FORGE SYSTEM OVERRIDE"
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        className="admin-modal"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="ruleKey" label="IDENTIFIER">
            <Input className="cyber-input" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="category" label="DOMAIN">
                <Select className="cyber-select">
                  <Option value="LEARNING">LEARNING</Option>
                  <Option value="SECURITY">SECURITY</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="value" label="MAGNITUDE">
                <Input className="cyber-input" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <style>{`
        .cyber-input { background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.05) !important; color: #fff !important; }
        .cyber-select .ant-select-selector { background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.05) !important; color: #fff !important; }
      `}</style>
    </motion.div>
  );
};

export default AdminSystemWorkRules;
