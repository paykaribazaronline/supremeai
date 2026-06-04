import {
  EditOutlined,
  DeleteOutlined,
  RobotOutlined,
  KeyOutlined,
  InfoCircleOutlined,
  CloudServerOutlined,
  DesktopOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import {
  Table,
  Space,
  Button,
  Tag,
  Popconfirm,
  Typography,
  Collapse,
  Card,
  Empty,
  Badge,
  Tooltip,
} from "antd";
import React from "react";

import { Provider } from "./types";

const { Panel } = Collapse;
const { Text, Title } = Typography;

interface Props {
  providers: Record<string, Provider[]>;
  sortedGroupKeys: string[];
  getGroupLabel: (key: string) => string;
  loading: boolean;
  onEdit: (provider: Provider) => void;
  onDelete: (id: string) => void;
}

const ProvidersTable: React.FC<Props> = ({
  providers,
  sortedGroupKeys,
  getGroupLabel,
  loading,
  onEdit,
  onDelete,
}) => {
  const getGroupIcon = (key: string) => {
    switch (key) {
      case "api":
        return <KeyOutlined style={{ color: "#1890ff" }} />;
      case "gcloud":
        return <CloudServerOutlined style={{ color: "#722ed1" }} />;
      case "local":
      case "ollama":
        return <DesktopOutlined style={{ color: "#13c2c2" }} />;
      default:
        return <SettingOutlined style={{ color: "#8c8c8c" }} />;
    }
  };

  const columns = [
    {
      title: "Provider / Hints",
      key: "name",
      render: (_: any, record: Provider) => (
        <Space direction="vertical" size={0}>
          <Text strong>{record.name || "Unknown Provider"}</Text>
          {record.hints && (
            <Text type="secondary" style={{ fontSize: "12px" }}>
              <InfoCircleOutlined /> {record.hints}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          active: "#52c41a",
          inactive: "#faad14",
          error: "#ff4d4f",
          rotating: "#1890ff",
          dead: "#000000",
        };
        const badgeMap: Record<string, any> = {
          active: "processing",
          inactive: "warning",
          error: "error",
          rotating: "processing",
          dead: "default",
        };
        const color = colorMap[status] || "#d9d9d9";
        const label = status ? status.toUpperCase() : "UNKNOWN";
        return (
          <Badge
            status={badgeMap[status] || "default"}
            color={color}
            text={
              <Tag
                color={color}
                style={{
                  borderRadius: "12px",
                  border: "none",
                  color: "white",
                  fontWeight: 600,
                }}
              >
                {label}
              </Tag>
            }
          />
        );
      },
    },

    {
      title: "Work Roles",
      dataIndex: "assignedRoles",
      key: "assignedRoles",
      render: (roles: string[]) => (
        <div style={{ maxWidth: 200 }}>
          {roles?.map((r) => (
            <Tag
              key={r}
              color="blue"
              style={{ marginBottom: 4, borderRadius: "4px" }}
            >
              {r.replace("_", " ")}
            </Tag>
          )) || "-"}
        </div>
      ),
    },
    {
      title: "Actions",
      key: "actions",
      render: (_: any, record: Provider) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => onEdit(record)}
          >
            Edit
          </Button>
          <Popconfirm
            title="Delete this provider?"
            onConfirm={() => onDelete(record.id!)}
            okText="Delete"
            cancelText="No"
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (sortedGroupKeys.length === 0 && !loading) {
    return <Empty description="No providers found matching your search" />;
  }

  return (
    <div className="providers-grouped-list" style={{ marginTop: "20px" }}>
      <Collapse
        defaultActiveKey={sortedGroupKeys}
        ghost
        expandIconPosition="end"
      >
        {sortedGroupKeys.map((groupKey) => {
          const groupProviders = providers[groupKey] || [];
          const activeCount = groupProviders.filter(
            (p) => p.status === "active",
          ).length;
          const inactiveCount = groupProviders.length - activeCount;

          return (
            <Panel
              header={
                <Space>
                  {getGroupIcon(groupKey)}
                  <Title level={5} style={{ margin: 0 }}>
                    {getGroupLabel(groupKey)}
                  </Title>
                  <Badge
                    count={groupProviders.length}
                    style={{ backgroundColor: "#52c41a" }}
                  />
                  {activeCount > 0 && (
                    <Tag
                      color="success"
                      style={{ borderRadius: "8px", marginLeft: 4 }}
                    >
                      {activeCount} সক্রিয়
                    </Tag>
                  )}
                  {inactiveCount > 0 && (
                    <Tag color="warning" style={{ borderRadius: "8px" }}>
                      {inactiveCount} নিষ্ক্রিয়
                    </Tag>
                  )}
                </Space>
              }
              key={groupKey}
              className="glass-card cyan-bordered-panel"
              style={{
                marginBottom: 16,
                background: "rgba(13, 13, 18, 0.8)",
                border: "1px solid rgba(0, 243, 255, 0.15)",
                borderRadius: 12,
                boxShadow:
                  "0 0 clamp(10px, 1.5vw, 20px) rgba(0, 243, 255, 0.1), 0 4px 12px rgba(0, 0, 0, 0.4)",
                backdropFilter: "blur(12px)",
              }}
            >
              <Table
                columns={columns}
                dataSource={groupProviders}
                rowKey="id"
                pagination={false}
                size="middle"
                className="inner-provider-table neon-cyan-border"
                style={{
                  background: "transparent",
                  border: "1px solid rgba(0, 243, 255, 0.08)",
                  borderRadius: 8,
                }}
              />
            </Panel>
          );
        })}
      </Collapse>
    </div>
  );
};

export default ProvidersTable;
