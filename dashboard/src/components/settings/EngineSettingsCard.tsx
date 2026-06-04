import { Card, Table, Input, message } from "antd";
import React from "react";

import { SystemConfig } from "./types";

interface EngineSettingsCardProps {
  config: SystemConfig;
  onUpdateValue: (field: keyof SystemConfig, key: string, value: any) => void;
}

const EngineSettingsCard: React.FC<EngineSettingsCardProps> = ({
  config,
  onUpdateValue,
}) => {
  const renderMapEditor = (field: keyof SystemConfig, title: string) => {
    const data = Object.entries(config[field] || {}).map(([key, value]) => ({
      key,
      value,
    }));

    const columns = [
      {
        title: "Parameter Key",
        dataIndex: "key",
        key: "key",
        width: "40%",
        render: (text: string) => (
          <code style={{ color: "#eb2f96" }}>{text}</code>
        ),
      },
      {
        title: "Configured Value",
        dataIndex: "value",
        key: "value",
        render: (text: any, record: any) => (
          <Input
            defaultValue={text}
            onBlur={(e) => {
              const val = e.target.value;
              const parsed =
                isNaN(Number(val)) || val === "" ? val : Number(val);
              onUpdateValue(field, record.key, parsed);
            }}
            onPressEnter={(e) => {
              const val = (e.target as any).value;
              const parsed =
                isNaN(Number(val)) || val === "" ? val : Number(val);
              onUpdateValue(field, record.key, parsed);
            }}
            style={{ borderRadius: "6px" }}
          />
        ),
      },
    ];

    return (
      <Card
        title={title}
        size="small"
        style={{ marginBottom: 24, borderRadius: "12px" }}
        className="glass-card"
      >
        <Table
          dataSource={data}
          columns={columns}
          pagination={false}
          size="small"
          rowKey="key"
          className="glass-table"
        />
      </Card>
    );
  };

  return (
    <div style={{ marginTop: 16 }}>
      {renderMapEditor("timeouts", "Internal System Timeouts (ms)")}
      {renderMapEditor("thresholds", "Decision Logic Thresholds (0.0 - 1.0)")}
      {renderMapEditor("settings", "Advanced System Parameters")}
      {renderMapEditor("collections", "Persistent Database Mappings")}
    </div>
  );
};

export default EngineSettingsCard;
