import {
  EditOutlined,
  DeleteOutlined,
  ExperimentOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import {
  Table,
  Tag,
  Space,
  Button,
  Tooltip,
  Popconfirm,
  Typography,
} from "antd";
import React, { useState } from "react";

import { SavedAPIKey } from "./types";

const { Text } = Typography;

interface APIKeysTableProps {
  keys: SavedAPIKey[];
  loading: boolean;
  onEdit: (key: SavedAPIKey) => void;
  onDelete: (id: string) => void;
  onTest: (id: string) => void;
  selectedRowKeys: React.Key[];
  onSelectionChange: (selectedKeys: React.Key[]) => void;
}

const APIKeysTable: React.FC<APIKeysTableProps> = ({
  keys,
  loading,
  onEdit,
  onDelete,
  onTest,
  selectedRowKeys,
  onSelectionChange,
}) => {
  const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set());

  const toggleKeyVisibility = (id: string) => {
    setVisibleKeys((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const maskKey = (key: string) => {
    if (!key) return "";
    if (key.length <= 8) return "••••••••";
    return key.slice(0, 4) + "••••••••" + key.slice(-4);
  };

  const columns = [
    {
      title: "প্রোভাইডার",
      dataIndex: "provider",
      key: "provider",
      render: (p: string) => (
        <Tag
          color="geekblue"
          style={{ textTransform: "capitalize", fontWeight: 500 }}
        >
          {p}
        </Tag>
      ),
    },
    {
      title: "লেবেল",
      dataIndex: "label",
      key: "label",
      render: (label: string) => <Text strong>{label}</Text>,
    },
    {
      title: "এপিআই কী",
      dataIndex: "apiKey",
      key: "apiKey",
      render: (key: string, record: SavedAPIKey) => (
        <Space>
          <Text code copyable={{ text: key }} style={{ fontSize: "13px" }}>
            {visibleKeys.has(record.id) ? key : maskKey(key)}
          </Text>
          <Button
            type="text"
            size="small"
            icon={
              visibleKeys.has(record.id) ? (
                <EyeInvisibleOutlined />
              ) : (
                <EyeOutlined />
              )
            }
            onClick={() => toggleKeyVisibility(record.id)}
          />
        </Space>
      ),
    },
    {
      title: "স্ট্যাটাস",
      dataIndex: "status",
      key: "status",
      render: (s: string) => {
        let color = "default";
        let icon = <ClockCircleOutlined />;

        if (s === "active") {
          color = "green";
          icon = <CheckCircleOutlined />;
        } else if (s === "error") {
          color = "red";
          icon = <CloseCircleOutlined />;
        } else if (s === "inactive") {
          color = "orange";
          icon = <ClockCircleOutlined />;
        }

        return (
          <Tag
            icon={icon}
            color={color}
            style={{ borderRadius: "10px", padding: "0 8px" }}
          >
            {s.toUpperCase()}
          </Tag>
        );
      },
    },
    {
      title: "মডেলসমূহ",
      dataIndex: "models",
      key: "models",
      render: (models: string[]) =>
        models?.length ? (
          <div
            style={{
              maxWidth: "200px",
              display: "flex",
              flexWrap: "wrap",
              gap: "4px",
            }}
          >
            {models.map((m) => (
              <Tag key={m} style={{ fontSize: "11px", margin: 0 }}>
                {m}
              </Tag>
            ))}
          </div>
        ) : (
          <Text type="secondary" italic>
            All Models
          </Text>
        ),
    },
    {
      title: "অ্যাকশন",
      key: "actions",
      align: "right" as const,
      render: (_: any, record: SavedAPIKey) => (
        <Space>
          <Tooltip title="টেস্ট করুন">
            <Button
              size="small"
              shape="circle"
              icon={<ExperimentOutlined />}
              onClick={() => onTest(record.id)}
            />
          </Tooltip>
          <Tooltip title="এডিট">
            <Button
              size="small"
              shape="circle"
              icon={<EditOutlined />}
              onClick={() => onEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="আপনি কি নিশ্চিতভাবে এই কী মুছে ফেলতে চান?"
            onConfirm={() => onDelete(record.id)}
            okText="হ্যাঁ"
            cancelText="না"
          >
            <Button
              size="small"
              shape="circle"
              danger
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={keys}
      rowKey="id"
      loading={loading}
      rowSelection={{
        selectedRowKeys,
        onChange: onSelectionChange,
      }}
      pagination={{ pageSize: 8 }}
      className="premium-table"
      style={{ borderRadius: "12px", overflow: "hidden" }}
    />
  );
};

export default APIKeysTable;
