import { Card, Typography } from "antd";
import React from "react";

import { ProviderHealthStats as StatsType } from "./types";

const { Text } = Typography;

interface Props {
  stats: StatsType | null;
}

const ProviderHealthStats: React.FC<Props> = ({ stats }) => {
  if (!stats) return null;

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
        gap: 16,
        marginBottom: 24,
      }}
    >
      <Card
        size="small"
        style={{
          borderRadius: 12,
          background: "rgba(39, 201, 63, 0.05)",
          border: "1px solid rgba(39, 201, 63, 0.1)",
        }}
      >
        <Text type="secondary" style={{ fontSize: 12 }}>
          Active Keys
        </Text>
        <div style={{ fontSize: 24, fontWeight: 700, color: "#52c41a" }}>
          {stats.active}
        </div>
      </Card>
      <Card
        size="small"
        style={{
          borderRadius: 12,
          background: "rgba(250, 173, 20, 0.05)",
          border: "1px solid rgba(250, 173, 20, 0.1)",
        }}
      >
        <Text type="secondary" style={{ fontSize: 12 }}>
          Error Streak
        </Text>
        <div style={{ fontSize: 24, fontWeight: 700, color: "#faad14" }}>
          {stats.error}
        </div>
      </Card>
      <Card
        size="small"
        style={{
          borderRadius: 12,
          background: "rgba(245, 34, 45, 0.05)",
          border: "1px solid rgba(245, 34, 45, 0.1)",
        }}
      >
        <Text type="secondary" style={{ fontSize: 12 }}>
          Dead Keys
        </Text>
        <div style={{ fontSize: 24, fontWeight: 700, color: "#f5222d" }}>
          {stats.dead}
        </div>
      </Card>
      <Card
        size="small"
        style={{
          borderRadius: 12,
          background: "rgba(24, 144, 255, 0.05)",
          border: "1px solid rgba(24, 144, 255, 0.1)",
        }}
      >
        <Text type="secondary" style={{ fontSize: 12 }}>
          Health Score
        </Text>
        <div style={{ fontSize: 24, fontWeight: 700, color: "#1890ff" }}>
          {stats.healthScore}%
        </div>
      </Card>
    </div>
  );
};

export default ProviderHealthStats;
