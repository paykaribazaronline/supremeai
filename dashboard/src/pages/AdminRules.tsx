// AdminRules.tsx - Cinematic System Rules & Command Center
import {
  AuditOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  CodeOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  SecurityScanOutlined,
  ControlOutlined,
  ReloadOutlined,
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
  message,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { useRole } from "../contexts/RoleContext";
import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

interface RuleItem {
  key: string;
  id: string;
  type: string;
  description: string;
  active: boolean;
}

const AdminRules: React.FC = () => {
  const { isGuest } = useRole();
  const [data, setData] = useState<RuleItem[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchRules = async () => {
    if (isGuest) return;
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/admin/rules");
      if (res.ok) {
        const result = await res.json();
        setData(result.data || []);
      }
    } catch (err) {
      console.error("Failed to fetch rules:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleToggle = async (record: RuleItem) => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(
        `/api/admin/rules/${record.id}/toggle`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ active: !record.active }),
        },
      );
      if (res.ok) {
        message.success(
          `Rule ${record.id} ${!record.active ? "activated" : "deactivated"}`,
        );
        fetchRules();
      }
    } catch (err) {
      message.error("Failed to toggle rule");
    }
  };

  const handleDelete = async (id: string) => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(`/api/admin/rules/${id}`, {
        method: "DELETE",
      });
      if (res.ok) {
        message.success("Rule deleted");
        fetchRules();
      }
    } catch (err) {
      message.error("Failed to delete rule");
    }
  };

  const columns = [
    {
      title: "ID",
      dataIndex: "id",
      key: "id",
      render: (t: string) => (
        <Text style={{ color: "var(--neon-blue)", fontFamily: "monospace" }}>
          {t}
        </Text>
      ),
    },
    {
      title: "Type",
      dataIndex: "type",
      key: "type",
      render: (t: string) => <Tag color="purple">{t}</Tag>,
    },
    {
      title: "Description",
      dataIndex: "description",
      key: "description",
      render: (t: string) => <Text style={{ color: "#fff" }}>{t}</Text>,
    },
    {
      title: "Status",
      dataIndex: "active",
      key: "active",
      render: (active: boolean) => (
        <Tag color={active ? "green" : "red"}>
          {active ? "ACTIVE" : "INACTIVE"}
        </Tag>
      ),
    },
    {
      title: "Actions",
      key: "actions",
      render: (_: any, record: RuleItem) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => message.info("Edit modal would open here")}
          >
            Edit
          </Button>
          <Button
            size="small"
            icon={<ControlOutlined />}
            onClick={() => handleToggle(record)}
          >
            {record.active ? "Disable" : "Enable"}
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ maxWidth: "1400px", margin: "0 auto" }}
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
              <AuditOutlined
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
                SYSTEM PROTOCOLS
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
              Rules <span className="text-gradient">& Commands</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Define autonomous behaviors, operational constraints, and neural
              execution plans.
            </Text>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ControlOutlined />}
                className="glass-action-button"
              >
                Audit Logs
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                className="cyber-button"
              >
                Forge Rule
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
                      Active Rules
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      124
                    </div>
                  </div>
                  <FileTextOutlined
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
                      Command Chains
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      42
                    </div>
                  </div>
                  <CodeOutlined
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
                      Sync Status
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
                  <GlobalOutlined
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
                      Audit Integrity
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 20, fontWeight: 800 }}
                    >
                      SECURED
                    </div>
                  </div>
                  <SecurityScanOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Main Table */}
        <Col span={24}>
          <div className="glass-card" style={{ minHeight: 500 }}>
            <div className="glass-card-title">
              Rule Matrix Engine <ThunderboltOutlined />
            </div>
            <Table
              dataSource={data}
              pagination={false}
              columns={[
                {
                  title: "IDENTIFIER",
                  dataIndex: "id",
                  key: "id",
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
                  title: "TYPE",
                  dataIndex: "type",
                  key: "type",
                  render: (t: string) => (
                    <Tag
                      color="black"
                      style={{
                        border: "1px solid var(--neon-purple)",
                        color: "var(--neon-purple)",
                        fontWeight: 800,
                        fontSize: 10,
                      }}
                    >
                      {t}
                    </Tag>
                  ),
                },
                {
                  title: "PROTOCOL DESCRIPTION",
                  dataIndex: "description",
                  key: "description",
                  render: (t: string) => (
                    <Text style={{ color: "#fff" }}>{t}</Text>
                  ),
                },
                {
                  title: "STATUS",
                  dataIndex: "active",
                  key: "active",
                  render: (a: boolean) => (
                    <Badge
                      status={a ? "success" : "default"}
                      text={
                        <span
                          style={{
                            color: a ? "var(--success)" : "#444",
                            fontWeight: 700,
                          }}
                        >
                          {a ? "ACTIVE" : "DORMANT"}
                        </span>
                      }
                    />
                  ),
                },
                {
                  title: "OPERATIONS",
                  key: "ops",
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
    </motion.div>
  );
};

export default AdminRules;
