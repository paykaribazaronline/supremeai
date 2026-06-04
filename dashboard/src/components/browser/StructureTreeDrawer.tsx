import { CodeOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { Drawer, Alert, Tag, Typography, Spin } from "antd";
import React from "react";

const { Text, Paragraph } = Typography;

interface StructureTreeDrawerProps {
  open: boolean;
  onClose: () => void;
  domTree: any;
}

const StructureTreeDrawer: React.FC<StructureTreeDrawerProps> = ({
  open,
  onClose,
  domTree,
}) => {
  const renderDomNode = (node: any, depth = 0) => {
    if (!node) return null;
    return (
      <div
        key={node.name + Math.random()}
        style={{
          marginLeft: depth * 12,
          borderLeft: "1px solid rgba(255,255,255,0.05)",
          paddingLeft: 8,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            margin: "4px 0",
          }}
        >
          <Tag color="blue" style={{ fontSize: 10 }}>
            {node.role}
          </Tag>
          <Text style={{ color: "#fff", fontSize: 12 }}>
            {node.name || "(unnamed)"}
          </Text>
          {node.value && (
            <Tag color="green" style={{ fontSize: 10 }}>
              {node.value}
            </Tag>
          )}
        </div>
        {node.children &&
          node.children.map((child: any) => renderDomNode(child, depth + 1))}
      </div>
    );
  };

  return (
    <Drawer
      title={
        <span style={{ color: "#fff" }}>
          <CodeOutlined style={{ marginRight: 10 }} /> Structural Intelligence
          Tree
        </span>
      }
      placement="right"
      width={550}
      onClose={onClose}
      open={open}
      headerStyle={{
        background: "#1a1b1e",
        borderBottom: "1px solid rgba(255,255,255,0.1)",
      }}
      bodyStyle={{ background: "#08090a", padding: 24 }}
      closeIcon={<span style={{ color: "rgba(255,255,255,0.5)" }}>×</span>}
    >
      <Alert
        message="DOM Analysis"
        description="This structure is currently being used by the AI Agent to identify interactive elements and layout patterns."
        type="info"
        showIcon
        style={{
          marginBottom: 24,
          background: "rgba(0,242,254,0.05)",
          border: "1px solid rgba(0,242,254,0.2)",
          color: "rgba(255,255,255,0.8)",
        }}
      />
      {domTree ? (
        <div
          style={{
            background: "rgba(255,255,255,0.02)",
            padding: 16,
            borderRadius: 12,
            border: "1px solid rgba(255,255,255,0.05)",
          }}
        >
          {renderDomNode(domTree)}
        </div>
      ) : (
        <div style={{ textAlign: "center", marginTop: 100 }}>
          <Spin
            indicator={
              <ThunderboltOutlined
                spin
                style={{ fontSize: 32, color: "#00f2fe" }}
              />
            }
          />
          <Paragraph
            style={{
              color: "rgba(255,255,255,0.3)",
              marginTop: 24,
              fontSize: 16,
            }}
          >
            Reconstructing page accessibility tree...
          </Paragraph>
        </div>
      )}
    </Drawer>
  );
};

export default StructureTreeDrawer;
