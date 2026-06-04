// AnalysisStatsCard.tsx - Summary statistics card
import {
  CodeOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import { Card, Row, Col, Statistic, Typography, Progress, Space } from "antd";
import React from "react";

const { Title, Text } = Typography;

interface AnalysisStatsCardProps {
  job: {
    projectName: string;
    status: string;
    filesAnalyzed: number;
    totalFindings: number;
    findingsBySeverity: Record<string, number>;
    durationMs?: number;
  };
}

const AnalysisStatsCard: React.FC<AnalysisStatsCardProps> = ({ job }) => {
  const severityOrder = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"];
  const maxFindings = Math.max(
    ...Object.values(job.findingsBySeverity || {}).map(Number),
    1,
  );

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case "CRITICAL":
        return "#ff3b30";
      case "HIGH":
        return "#ff9500";
      case "MEDIUM":
        return "#ffcc00";
      case "LOW":
        return "#34c759";
      default:
        return "#007aff";
    }
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return "-";
    const seconds = ms / 1000;
    return `${seconds.toFixed(1)}s`;
  };

  return (
    <Card
      className="glass-card"
      title={
        <Space>
          <CodeOutlined style={{ color: "var(--neon-blue)" }} />
          <span style={{ color: "#fff", fontWeight: 700 }}>
            {job.projectName}
          </span>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {job.status}
          </Text>
        </Space>
      }
      style={{ marginBottom: 24 }}
    >
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Statistic
            title={<span style={{ color: "var(--text-dim)" }}>Files</span>}
            value={job.filesAnalyzed}
            prefix={<FileTextOutlined />}
            valueStyle={{ color: "#fff" }}
          />
        </Col>
        <Col span={6}>
          <Statistic
            title={<span style={{ color: "var(--text-dim)" }}>Findings</span>}
            value={job.totalFindings}
            prefix={<CodeOutlined />}
            valueStyle={{ color: "#ff3b30" }}
          />
        </Col>
        <Col span={6}>
          <Statistic
            title={<span style={{ color: "var(--text-dim)" }}>Duration</span>}
            value={formatDuration(job.durationMs)}
            prefix={<ClockCircleOutlined />}
            valueStyle={{ color: "var(--neon-blue)" }}
          />
        </Col>
        <Col span={6}>
          <Statistic
            title={<span style={{ color: "var(--text-dim)" }}>Status</span>}
            value={job.status}
            prefix={<CheckCircleOutlined />}
            valueStyle={{
              color:
                job.status === "COMPLETED"
                  ? "#34c759"
                  : job.status === "FAILED"
                    ? "#ff3b30"
                    : "#ffcc00",
            }}
          />
        </Col>
      </Row>

      {/* Severity breakdown */}
      <div style={{ marginTop: 24 }}>
        <Title level={5} style={{ color: "#fff", marginBottom: 12 }}>
          Severity Breakdown
        </Title>
        <Row gutter={[8, 8]}>
          {severityOrder.map((severity) => {
            const count = job.findingsBySeverity?.[severity] || 0;
            const percentage = (count / maxFindings) * 100;
            return (
              <Col span={24} key={severity}>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <Text
                    style={{ width: 80, color: getSeverityColor(severity) }}
                  >
                    {severity}
                  </Text>
                  <Progress
                    percent={percentage}
                    size="small"
                    strokeColor={getSeverityColor(severity)}
                    showInfo={false}
                    style={{ flex: 1 }}
                  />
                  <Text style={{ width: 30, color: "#fff" }}>{count}</Text>
                </div>
              </Col>
            );
          })}
        </Row>
      </div>
    </Card>
  );
};

export default AnalysisStatsCard;
