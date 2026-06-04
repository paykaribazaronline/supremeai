import {
  ThunderboltOutlined,
  SyncOutlined,
  ClearOutlined,
} from "@ant-design/icons";
import { Typography, Card, List, Space, Button, Tag } from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";

import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

interface Activity {
  id: string;
  action: string;
  source: string;
  timestamp: string;
  details: string;
}

export default function AdminLiveActivity() {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState<Activity[]>([]);

  useEffect(() => {
    fetchActivities();
    const interval = setInterval(fetchActivities, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchActivities = async () => {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth("/api/activity/live");
      if (res.ok) {
        const data = await res.json();
        setActivities(Array.isArray(data) ? data : data.activities || []);
      }
    } catch (error) {
      // silent fail for demo
    } finally {
      setLoading(false);
    }
  };

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
          Live Activity Feed
        </Title>
        <Space>
          <Button
            icon={<SyncOutlined />}
            onClick={fetchActivities}
            className="glass-action-button"
          >
            Refresh
          </Button>
          <Button
            icon={<ClearOutlined />}
            onClick={() => setActivities([])}
            className="glass-action-button"
          >
            Clear
          </Button>
        </Space>
      </div>

      <Card className="glass-card">
        <List
          dataSource={activities}
          loading={loading}
          renderItem={(activity) => (
            <List.Item key={activity.id} style={{ padding: "12px" }}>
              <List.Item.Meta
                avatar={<ThunderboltOutlined style={{ color: "#faad14" }} />}
                title={
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 8 }}
                  >
                    <Text style={{ color: "#fff" }}>{activity.action}</Text>
                    <Tag color="blue">{activity.source}</Tag>
                  </div>
                }
                description={
                  <Text style={{ color: "rgba(255,255,255,0.7)" }}>
                    {activity.details}
                  </Text>
                }
              />
              <Text style={{ color: "var(--text-dim)", fontSize: 11 }}>
                {new Date(activity.timestamp).toLocaleTimeString()}
              </Text>
            </List.Item>
          )}
          locale={{ emptyText: "No recent activity" }}
        />
      </Card>
    </motion.div>
  );
}
