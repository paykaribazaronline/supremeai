import {
  BulbOutlined,
  GlobalOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import {
  Card,
  List,
  Space,
  Typography,
  Tag,
  Avatar,
  Empty,
  Button,
} from "antd";
import React from "react";

import { AlternativeSuggestion } from "./types";

const { Text, Paragraph } = Typography;

interface AlternativeSuggestionsListProps {
  suggestions?: AlternativeSuggestion[];
  onSwitch?: (url: string) => void;
}

const AlternativeSuggestionsList: React.FC<AlternativeSuggestionsListProps> = ({
  suggestions,
  onSwitch,
}) => {
  const defaultSuggestions: AlternativeSuggestion[] = [
    {
      url: "https://api.openaura.com",
      reason: "Official API detected with 90% better uptime than scraping.",
      improvement: "Reliability",
    },
    {
      url: "https://data.gov/archive",
      reason: "Public domain mirror found for this dataset.",
      improvement: "Legality",
    },
  ];

  const data =
    suggestions && suggestions.length > 0
      ? suggestions
      : suggestions
        ? []
        : defaultSuggestions;

  return (
    <Card
      className="glass-card"
      title={
        <Space>
          <BulbOutlined style={{ color: "#faad14" }} /> Smart Alternative
          Suggestions
        </Space>
      }
      style={{ height: "100%" }}
    >
      <Paragraph type="secondary">
        SupremeAI automatically identifies better or more permissive data
        sources based on your target URL.
      </Paragraph>

      <List
        itemLayout="horizontal"
        dataSource={data}
        locale={{
          emptyText: (
            <Empty
              description="No suggestions yet"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Button
                type="link"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => onSwitch?.(item.url)}
              >
                Switch To This
              </Button>,
            ]}
            className="suggestion-item"
            style={{
              background: "rgba(255,255,255,0.03)",
              padding: 12,
              borderRadius: 12,
              marginBottom: 12,
              border: "1px solid rgba(255,255,255,0.05)",
            }}
          >
            <List.Item.Meta
              avatar={
                <Avatar
                  icon={<GlobalOutlined />}
                  style={{ backgroundColor: "#1890ff" }}
                />
              }
              title={
                <Text strong ellipsis={{ tooltip: item.url }}>
                  {item.url}
                </Text>
              }
              description={
                <Space direction="vertical" size={0}>
                  <Text style={{ fontSize: 12, opacity: 0.8 }}>
                    {item.reason}
                  </Text>
                  <Tag color="green" style={{ marginTop: 4 }}>
                    +{item.improvement}
                  </Tag>
                </Space>
              }
            />
          </List.Item>
        )}
      />
    </Card>
  );
};

export default AlternativeSuggestionsList;
