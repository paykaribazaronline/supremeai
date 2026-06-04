import { LockOutlined, SecurityScanOutlined } from "@ant-design/icons";
import { Typography, Space, Button } from "antd";
import { motion } from "framer-motion";
import React from "react";

const { Title, Text } = Typography;

interface RestrictedDemoProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
}

export const RestrictedDemo: React.FC<RestrictedDemoProps> = ({
  title,
  description,
  icon,
}) => (
  <motion.div
    initial={{ opacity: 0, scale: 0.9 }}
    animate={{ opacity: 1, scale: 1 }}
    className="glass-panel"
    style={{
      padding: "80px 40px",
      textAlign: "center",
      marginTop: "40px",
      maxWidth: "800px",
      margin: "40px auto",
      border: "1px solid rgba(255, 152, 0, 0.3)",
      boxShadow: "0 0 40px rgba(255, 152, 0, 0.1)",
    }}
  >
    <div className="pulsing" style={{ marginBottom: 32 }}>
      {icon || (
        <LockOutlined
          style={{ fontSize: 80, color: "#f59e0b", opacity: 0.8 }}
        />
      )}
    </div>
    <Title
      level={2}
      style={{
        color: "var(--warning)",
        marginBottom: 16,
        fontWeight: 800,
        textTransform: "uppercase",
        letterSpacing: "0.2em",
      }}
    >
      অ্যাক্সেস প্রত্যাখ্যান করা হয়েছে
    </Title>
    <p
      style={{
        color: "var(--text-dim)",
        maxWidth: 600,
        margin: "0 auto 40px",
        fontSize: 18,
        lineHeight: 1.8,
      }}
    >
      নিরাপত্তা প্রোটোকল{" "}
      <Text code style={{ color: "var(--warning)" }}>
        LVL-4
      </Text>{" "}
      সক্রিয়। মডিউল{" "}
      <Text strong style={{ color: "var(--text-main)" }}>
        {title}
      </Text>{" "}
      শুধুমাত্র প্রশাসকদের জন্য এনক্রিপ্ট করা হয়েছে।
      <br />
      <span style={{ fontSize: "0.9rem", opacity: 0.7 }}>{description}</span>
    </p>
    <Space size="large">
      <Button
        className="cyber-button"
        icon={<SecurityScanOutlined />}
        onClick={() => (window.location.href = "/login")}
        style={{ height: "auto", padding: "12px 30px" }}
      >
        অ্যাডমিন যাচাই করুন
      </Button>
      <Button
        ghost
        onClick={() => (window.location.href = "/")}
        style={{
          height: "auto",
          padding: "12px 30px",
          borderRadius: 4,
          borderColor: "rgba(255,255,255,0.3)",
        }}
      >
        পিছনে যান
      </Button>
    </Space>
  </motion.div>
);
