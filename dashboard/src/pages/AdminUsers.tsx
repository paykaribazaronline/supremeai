// AdminUsers.tsx - Cinematic User Management
import {
  UserOutlined,
  ReloadOutlined,
  UserAddOutlined,
  TeamOutlined,
  IdcardOutlined,
  SafetyOutlined,
  LockOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Alert,
  message,
  Form,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect, useMemo } from "react";

import { User, UserSortField } from "../components/users/types";
import UserActionToolbar from "../components/users/UserActionToolbar";
import UserModal from "../components/users/UserModal";
import UserTable from "../components/users/UserTable";
import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

const AdminUsers: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);
  const [deletingUserId, setDeletingUserId] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [sortBy, setSortBy] = useState<UserSortField | null>(null);
  const [sortOrder, setSortOrder] = useState<"ascend" | "descend">("ascend");

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await authUtils.fetchWithAuth("/api/admin/users");
      if (response.ok) {
        const result = await response.json();
        setUsers(result.data?.users || []);
      }
    } catch (err) {
      setError("Failed to load user database");
    } finally {
      setLoading(false);
    }
  };

  const handleAddUser = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleDeactivate = async (id: string) => {
    setDeletingUserId(id);
    try {
      const res = await authUtils.fetchWithAuth(
        `/api/admin/users/${id}/deactivate`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
        },
      );
      if (res.ok) {
        message.success("User deactivated successfully");
        fetchUsers();
      } else {
        message.error("Failed to deactivate user");
      }
    } catch {
      message.error("Failed to deactivate user");
    } finally {
      setDeletingUserId(null);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const processedUsers = useMemo(() => {
    const result = users.filter((user) => {
      if (!searchTerm) return true;
      const term = searchTerm.toLowerCase();
      return (
        user.email.toLowerCase().includes(term) ||
        (user.displayName && user.displayName.toLowerCase().includes(term))
      );
    });
    return result;
  }, [users, searchTerm]);

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
              <TeamOutlined
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
                USER DIRECTORY
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
              Operator <span className="text-gradient">Management</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Control access levels, monitor usage, and authorize new system
              architects.
            </Text>
          </Col>
          <Col>
            <Space>
              <Button
                icon={<ReloadOutlined spin={loading} />}
                onClick={fetchUsers}
                className="glass-action-button"
              >
                Refresh
              </Button>
              <Button
                type="primary"
                icon={<UserAddOutlined />}
                onClick={() => {
                  setEditingUser(null);
                  form.resetFields();
                  setModalVisible(true);
                }}
                className="cyber-button"
              >
                Register Operator
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* Stats Row */}
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
                      Total Operators
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {users.length}
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
                      VIP/Enterprise
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      {
                        users.filter(
                          (u) => u.tier === "PRO" || u.tier === "ENTERPRISE",
                        ).length
                      }
                    </div>
                  </div>
                  <IdcardOutlined
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
                      Auth Integrity
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
                  <SafetyOutlined
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
                      Global Quota
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      82%
                    </div>
                  </div>
                  <LockOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Main Table Area */}
        <Col span={24}>
          <div className="glass-card" style={{ minHeight: 600 }}>
            <div className="glass-card-title">
              Operator Matrix
              <UserActionToolbar
                searchTerm={searchTerm}
                setSearchTerm={setSearchTerm}
                sortBy={sortBy}
                setSortBy={setSortBy}
                sortOrder={sortOrder}
                setSortOrder={setSortOrder}
                onAddUser={handleAddUser}
                onRefresh={fetchUsers}
                minimal={true}
              />
            </div>

            <div style={{ marginTop: 12 }}>
              {loading && !users.length ? (
                <div style={{ padding: 100, textAlign: "center" }}>
                  <Spin size="large" tip="SYNCING OPERATOR DATABASE..." />
                </div>
              ) : (
                <UserTable
                  users={processedUsers}
                  loading={loading}
                  deletingUserId={deletingUserId}
                  onEdit={(u) => {
                    setEditingUser(u);
                    setModalVisible(true);
                  }}
                  onDeactivate={handleDeactivate}
                />
              )}
            </div>
          </div>
        </Col>
      </Row>

      <UserModal
        open={modalVisible}
        editingUser={editingUser}
        onCancel={() => setModalVisible(false)}
        onFinish={async (v) => {
          setModalVisible(false);
          fetchUsers();
        }}
        loading={submitLoading}
        form={form}
      />
    </motion.div>
  );
};

export default AdminUsers;
