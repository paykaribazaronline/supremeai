import { CheckCircleOutlined, CloseCircleOutlined } from "@ant-design/icons";
import {
  Card,
  Typography,
  List,
  Tag,
  Space,
  Popconfirm,
  Button,
  Empty,
} from "antd";
import React from "react";

const { Title, Text, Paragraph } = Typography;

interface Recommendation {
  id: string;
  title: string;
  description: string;
  confidence: number;
  suggestedKeywords: string[];
  createdAt: string;
}

interface EvolutionProposalsTabProps {
  recommendations: Recommendation[];
  onApprove: (id: string) => void;
  onDecline: (id: string) => void;
}

const EvolutionProposalsTab: React.FC<EvolutionProposalsTabProps> = ({
  recommendations,
  onApprove,
  onDecline,
}) => {
  return (
    <Card bordered={false} className="glass-card">
      <div style={{ marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0, color: "#fff" }}>
          Evolution Proposals
        </Title>
        <Text type="secondary">
          সিস্টেম লার্নিং থেকে আসা নতুন রিকমেন্ডেশনসমূহ
        </Text>
      </div>
      <List
        dataSource={recommendations}
        renderItem={(rec) => (
          <Card
            size="small"
            style={{
              marginBottom: 12,
              background: "rgba(255,255,255,0.02)",
              border: "1px solid rgba(255,255,255,0.05)",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <div>
                <Text strong style={{ color: "#fff", fontSize: 16 }}>
                  {rec.title}
                </Text>
                <Tag color="blue" style={{ marginLeft: 8 }}>
                  {Math.round(rec.confidence * 100)}% Confidence
                </Tag>
                <div style={{ marginTop: 8 }}>
                  <Paragraph style={{ color: "rgba(255,255,255,0.6)" }}>
                    {rec.description}
                  </Paragraph>
                </div>
                <Space wrap>
                  {rec.suggestedKeywords.map((k) => (
                    <Tag key={k}>{k}</Tag>
                  ))}
                </Space>
              </div>
              <Space direction="vertical" align="end">
                <Text type="secondary">
                  {new Date(rec.createdAt).toLocaleDateString()}
                </Text>
                <Space>
                  <Popconfirm
                    title="এই রিকমেন্ডেশনটি অ্যাপ্রুভ করবেন?"
                    onConfirm={() => onApprove(rec.id)}
                  >
                    <Button
                      type="primary"
                      size="small"
                      icon={<CheckCircleOutlined />}
                    >
                      Approve
                    </Button>
                  </Popconfirm>
                  <Button
                    size="small"
                    icon={<CloseCircleOutlined />}
                    danger
                    onClick={() => onDecline(rec.id)}
                  >
                    Decline
                  </Button>
                </Space>
              </Space>
            </div>
          </Card>
        )}
        locale={{
          emptyText: <Empty description="কোনো নতুন রিকমেন্ডেশন নেই" />,
        }}
      />
    </Card>
  );
};

export default EvolutionProposalsTab;
