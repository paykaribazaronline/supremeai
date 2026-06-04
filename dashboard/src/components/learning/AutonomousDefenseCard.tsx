import {
  GlobalOutlined,
  SafetyCertificateOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { Card, Row, Col, Space, Typography, Badge, Tag, Button } from "antd";
import React from "react";

const { Text } = Typography;

interface AutonomousDefenseCardProps {
  status: any;
  onCyberResearch: (topic: string) => void;
  onRunAudit: () => void;
  actionLoading: boolean;
}

const AutonomousDefenseCard: React.FC<AutonomousDefenseCardProps> = ({
  status,
  onCyberResearch,
  onRunAudit,
  actionLoading,
}) => {
  return (
    <Card
      title="System Capabilities & Autonomous Defense"
      className="glass-card"
    >
      <Row gutter={[24, 24]}>
        <Col span={12}>
          <Space direction="vertical" style={{ width: "100%" }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <Text>
                <GlobalOutlined style={{ marginRight: 8 }} /> Web Scraping
                Allowed
              </Text>
              <Badge
                status={status?.scrapingAllowed ? "processing" : "default"}
                text={status?.scrapingAllowed ? "ON" : "OFF"}
              />
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <Text>
                <SafetyCertificateOutlined style={{ marginRight: 8 }} />{" "}
                Auto-Approval (Autonomous Mode)
              </Text>
              <Badge
                status={status?.autoApprovalAllowed ? "warning" : "default"}
                text={status?.autoApprovalAllowed ? "ACTIVE" : "DISABLED"}
              />
            </div>
          </Space>
        </Col>
        <Col span={12}>
          <Card
            size="small"
            style={{
              background: "rgba(59, 130, 246, 0.05)",
              borderColor: "rgba(59, 130, 246, 0.2)",
            }}
          >
            <Space direction="vertical" style={{ width: "100%" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <Text strong style={{ color: "#60a5fa" }}>
                  <ThunderboltOutlined /> Cyber-Defense Skill
                </Text>
                <Tag color="blue">LEARNED</Tag>
              </div>
              <Text type="secondary" style={{ fontSize: 12 }}>
                System is autonomously researching exploitation patterns to
                harden infrastructure.
              </Text>
              <Space>
                <Button
                  size="small"
                  type="primary"
                  ghost
                  onClick={() => onCyberResearch("Zero-day Exploit Analysis")}
                  loading={actionLoading}
                >
                  Research Zero-days
                </Button>
                <Button
                  size="small"
                  onClick={onRunAudit}
                  loading={actionLoading}
                >
                  Run Self-Audit
                </Button>
              </Space>
            </Space>
          </Card>
        </Col>
      </Row>
    </Card>
  );
};

export default AutonomousDefenseCard;
