import {
  ReloadOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  AlertOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Card,
  Table,
  Tag,
  Button,
  Space,
  message,
  Badge,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

interface HealingEvent {
  id: string;
  type: string;
  status: "success" | "failed" | "in_progress";
  fix: string;
  appliedAt: string;
  duration: number;
}

export default function AdminSelfHealing() {
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState<HealingEvent[]>([]);

  useEffect(() => {
    fetchEvents();
  }, []);

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/self-healing/history");
      if (res.ok) {
        const data = await res.json();
        setEvents(Array.isArray(data) ? data : data.events || []);
      }
    } catch (error) {
      message.error("Failed to fetch healing events");
    } finally {
      setLoading(false);
    }
  };

  const triggerHealing = async () => {
    try {
      const res = await authUtils.fetchWithAuth("/api/self-healing/health", {
        method: "POST",
      });
      if (res.ok) {
        message.success("Healing triggered");
        fetchEvents();
      }
    } catch (error) {
      message.error("Failed to trigger healing");
    }
  };

  const columns = [
    {
      title: "Type",
      dataIndex: "type",
      key: "type",
      render: (text: string) => <Tag color="purple">{text}</Tag>,
    },
    {
      title: "Fix",
      dataIndex: "fix",
      key: "fix",
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (status: string) => (
        <Badge
          status={
            status === "success"
              ? "success"
              : status === "failed"
                ? "error"
                : "processing"
          }
          text={status.replace("_", " ")}
        />
      ),
    },
    {
      title: "Applied",
      dataIndex: "appliedAt",
      key: "appliedAt",
      render: (text: string) => new Date(text).toLocaleString(),
    },
    {
      title: "Duration",
      dataIndex: "duration",
      key: "duration",
      render: (dur: number) => `${dur}ms`,
    },
  ];

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
          Self-Healing
        </Title>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchEvents}
            className="glass-action-button"
          >
            Refresh
          </Button>
          <Button
            type="primary"
            icon={<SyncOutlined />}
            onClick={triggerHealing}
          >
            Trigger Healing
          </Button>
        </Space>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={24}>
          <Card className="glass-card">
            <Table
              columns={columns}
              dataSource={events}
              loading={loading}
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </Col>
      </Row>
    </motion.div>
  );
}
