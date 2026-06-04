// AdminVPN.tsx - Cinematic Secure Gateway
import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
  GlobalOutlined,
  ThunderboltOutlined,
  SecurityScanOutlined,
  CloudServerOutlined,
  SafetyCertificateOutlined,
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
  InputNumber,
  Popconfirm,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { fetchWithAuth } from "../lib/authUtils";

const { Title, Text } = Typography;

interface VPNConnection {
  id?: string;
  name: string;
  host: string;
  port: number;
  status?: string;
  createdAt?: string;
}

const AdminVPN: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [connections, setConnections] = useState<VPNConnection[]>([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();

  const fetchConnections = async () => {
    setLoading(true);
    try {
      const response = await fetchWithAuth("/api/admin/vpn");
      if (response.ok) {
        const result = await response.json();
        setConnections(result.data?.connections || []);
      }
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConnections();
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
              <GlobalOutlined
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
                ENCRYPTED TUNNEL
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
              Secure <span className="text-gradient">Gateway</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Manage distributed VPN connections, proxy nodes, and secure
              network exits.
            </Text>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined spin={loading} />}
                onClick={fetchConnections}
                className="glass-action-button"
              >
                Refresh Tunnel
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setIsModalVisible(true)}
                className="cyber-button"
              >
                Establish Link
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Summary Cards */}
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
                      Active Links
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {
                        connections.filter((c) => c.status === "CONNECTED")
                          .length
                      }
                    </div>
                  </div>
                  <CloudServerOutlined
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
                      Encrypted Traffic
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      12.4 GB
                    </div>
                  </div>
                  <SecurityScanOutlined
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
                      Tunnel Health
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 20,
                        fontWeight: 800,
                      }}
                    >
                      STABLE
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
                      Latency P95
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      42ms
                    </div>
                  </div>
                  <ThunderboltOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Main Connections Table */}
        <Col span={24}>
          <div className="glass-card" style={{ minHeight: 500 }}>
            <div className="glass-card-title">
              Link Matrix Index <GlobalOutlined />
            </div>
            <Table
              dataSource={connections}
              rowKey="id"
              loading={loading}
              pagination={{ pageSize: 12 }}
              columns={[
                {
                  title: "ENDPOINT NAME",
                  dataIndex: "name",
                  key: "name",
                  render: (t: string) => (
                    <Text strong style={{ color: "#fff" }}>
                      {t}
                    </Text>
                  ),
                },
                {
                  title: "SERVER HOST",
                  dataIndex: "host",
                  key: "host",
                  render: (h: string) => (
                    <Text
                      style={{
                        fontFamily: "JetBrains Mono",
                        color: "var(--neon-blue)",
                      }}
                    >
                      {h}
                    </Text>
                  ),
                },
                { title: "PORT", dataIndex: "port", key: "port" },
                {
                  title: "STATUS",
                  dataIndex: "status",
                  key: "status",
                  render: (s: string) => (
                    <Badge
                      status={s === "CONNECTED" ? "success" : "default"}
                      text={
                        <span
                          style={{
                            color:
                              s === "CONNECTED" ? "var(--success)" : "#444",
                            fontWeight: 700,
                          }}
                        >
                          {s || "IDLE"}
                        </span>
                      }
                    />
                  ),
                },
                {
                  title: "ACTIONS",
                  key: "a",
                  render: (_, r) => (
                    <Popconfirm title="Terminate Link?">
                      <Button
                        icon={<DeleteOutlined />}
                        size="small"
                        danger
                        ghost
                      />
                    </Popconfirm>
                  ),
                },
              ]}
              className="cyber-table"
            />
          </div>
        </Col>
      </Row>

      <Modal
        title="ESTABLISH NEW TUNNEL"
        visible={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        onOk={() => form.submit()}
        className="admin-modal"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="CONNECTION ALIAS">
            <Input className="cyber-input" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={16}>
              <Form.Item name="host" label="GATEWAY HOST">
                <Input className="cyber-input" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="port" label="PORT">
                <InputNumber style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <style>{`
        .cyber-input { background: rgba(255,255,255,0.03) !important; border: 1px solid rgba(255,255,255,0.05) !important; color: #fff !important; }
      `}</style>
    </motion.div>
  );
};

export default AdminVPN;
