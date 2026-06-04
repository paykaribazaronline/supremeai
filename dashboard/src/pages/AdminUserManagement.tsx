// AdminUserManagement.tsx - User Management
import {
  UserOutlined,
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Card,
  Table,
  Button,
  Space,
  Input,
  message,
  Modal,
  Form,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;
const { Search } = Input;

interface User {
  id: string;
  email: string;
  displayName: string;
  role: "admin" | "user" | "guest";
  createdAt: string;
}

export default function AdminUserManagement() {
  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState<User[]>([]);
  const [searchText, setSearchText] = useState("");
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/auth/users");
      if (res.ok) {
        const data = await res.json();
        setUsers(Array.isArray(data) ? data : data.users || []);
      }
    } catch (error) {
      message.error("Failed to fetch users");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async () => {
    try {
      const values = await form.validateFields();
      const res = await authUtils.fetchWithAuth("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      if (res.ok) {
        message.success("User created successfully");
        setCreateModalVisible(false);
        form.resetFields();
        fetchUsers();
      } else {
        message.error("Failed to create user");
      }
    } catch (error) {
      message.error("Failed to create user");
    }
  };

  const columns = [
    {
      title: "User",
      dataIndex: "email",
      key: "email",
      render: (email: string, record: User) => (
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <div
            className="pulsing"
            style={{
              width: 32,
              height: 32,
              borderRadius: "50%",
              background: "var(--neon-blue)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <UserOutlined style={{ color: "#fff" }} />
          </div>
          <div>
            <Text style={{ color: "#fff", fontWeight: 500, display: "block" }}>
              {record.displayName || email}
            </Text>
            <Text style={{ color: "var(--text-dim)", fontSize: 11 }}>
              {email}
            </Text>
          </div>
        </div>
      ),
    },
    {
      title: "Role",
      dataIndex: "role",
      key: "role",
      render: (role: string) => (
        <Text
          style={{
            color:
              role === "admin"
                ? "var(--neon-blue)"
                : role === "user"
                  ? "var(--neon-purple)"
                  : "var(--text-dim)",
            fontWeight: 600,
            textTransform: "uppercase",
            fontSize: 12,
          }}
        >
          {role}
        </Text>
      ),
    },
    {
      title: "Created",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (createdAt: string) => (
        <Text style={{ color: "var(--text-dim)", fontSize: 12 }}>
          {new Date(createdAt).toLocaleDateString()}
        </Text>
      ),
    },
  ];

  const filteredUsers = users.filter(
    (user) =>
      user.email.toLowerCase().includes(searchText.toLowerCase()) ||
      (user.displayName &&
        user.displayName.toLowerCase().includes(searchText.toLowerCase())),
  );

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      style={{ padding: "24px" }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 24,
        }}
      >
        <Title level={4} style={{ color: "#fff", margin: 0 }}>
          User Management
        </Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalVisible(true)}
        >
          Add User
        </Button>
      </div>

      <Card className="glass-card" style={{ marginBottom: 24 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Text style={{ color: "var(--text-dim)" }}>
            Total Users: {users.length}
          </Text>
          <Search
            placeholder="Search users..."
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
          />
        </div>
      </Card>

      <Card className="glass-card">
        <Table
          dataSource={filteredUsers}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 10 }}
          rowKey="id"
          style={{ background: "transparent" }}
        />
      </Card>

      <Modal
        title="Create New User"
        open={createModalVisible}
        onOk={handleCreateUser}
        onCancel={() => setCreateModalVisible(false)}
        okText="Create"
        cancelText="Cancel"
      >
        <Form form={form} layout="vertical">
          <Form.Item label="Email" name="email" rules={[{ required: true }]}>
            <Input placeholder="user@example.com" />
          </Form.Item>
          <Form.Item label="Display Name" name="displayName">
            <Input placeholder="John Doe" />
          </Form.Item>
          <Form.Item label="Role" name="role" initialValue="user">
            <select
              style={{
                width: "100%",
                padding: "8px",
                borderRadius: "6px",
                border: "1px solid rgba(255,255,255,0.2)",
                background: "#000",
              }}
            >
              <option value="user">User</option>
              <option value="admin">Admin</option>
              <option value="guest">Guest</option>
            </select>
          </Form.Item>
        </Form>
      </Modal>
    </motion.div>
  );
}
