import { MobileOutlined, ReloadOutlined, EyeOutlined } from "@ant-design/icons";
import { Table, Space, Typography, Tag, Button } from "antd";
import React from "react";

import { DeploymentRecord } from "./types";

const { Text } = Typography;

interface DeploymentHistoryTableProps {
  deployments: DeploymentRecord[];
  loading: boolean;
  onRefresh: () => void;
  onSimulate: (appId: string) => void;
}

const DeploymentHistoryTable: React.FC<DeploymentHistoryTableProps> = ({
  deployments,
  loading,
  onRefresh,
  onSimulate,
}) => {
  const columns = [
    {
      title: "অ্যাপ আইডি",
      dataIndex: "appId",
      key: "appId",
      render: (id: string) => (
        <Text
          code
          style={{ color: "#60a5fa", background: "rgba(96, 165, 250, 0.1)" }}
        >
          {id}
        </Text>
      ),
    },
    {
      title: "ডিভাইস টাইপ",
      dataIndex: "deviceType",
      key: "deviceType",
      render: (type: string) => (
        <Tag color="blue" style={{ borderRadius: 4 }}>
          {type}
        </Tag>
      ),
    },
    {
      title: "স্ট্যাটাস",
      dataIndex: "status",
      key: "status",
      render: (status: string) => (
        <Tag
          color={
            status === "RUNNING"
              ? "green"
              : status === "FAILED"
                ? "red"
                : "orange"
          }
          style={{ borderRadius: 4 }}
        >
          {status}
        </Tag>
      ),
    },
    {
      title: "ডিপ্লয়মেন্ট সময়",
      dataIndex: "deployedAt",
      key: "deployedAt",
      render: (date: string) => (
        <Text style={{ color: "rgba(255,255,255,0.65)" }}>
          {new Date(date).toLocaleString()}
        </Text>
      ),
    },
    {
      title: "অ্যাকশন",
      key: "actions",
      render: (_: any, record: DeploymentRecord) => (
        <Space>
          <Button
            size="small"
            type="primary"
            icon={<EyeOutlined />}
            onClick={() => onSimulate(record.appId)}
            style={{ borderRadius: 4 }}
          >
            লাইভ প্রিভিউ
          </Button>
          <Button
            size="small"
            href={record.previewUrl}
            target="_blank"
            disabled={!record.previewUrl}
            style={{
              borderRadius: 4,
              background: "rgba(255,255,255,0.05)",
              border: "none",
              color: "#fff",
            }}
          >
            এক্সটার্নাল
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="admin-table-dark">
      <Table
        columns={columns}
        dataSource={deployments}
        rowKey="appId"
        loading={loading}
        pagination={{ pageSize: 5 }}
        size="middle"
        style={{ background: "transparent" }}
      />
    </div>
  );
};

export default DeploymentHistoryTable;
