import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  HomeOutlined,
  LogoutOutlined,
  LoginOutlined,
  BellOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { Layout, Button, Breadcrumb, Typography, Badge, Tooltip } from "antd";
import { motion } from "framer-motion";
import React from "react";

import { ConnectionIndicator } from "../FeedbackSystem";

const { Header } = Layout;
const { Text } = Typography;

interface DashboardHeaderProps {
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  getBreadcrumbs: () => any[];
  isAuthenticated: boolean;
  isAdmin: boolean;
  user: any;
  handleLogout: () => void;
}

const DashboardHeader: React.FC<DashboardHeaderProps> = ({
  collapsed,
  setCollapsed,
  getBreadcrumbs,
  isAuthenticated,
  isAdmin,
  user,
  handleLogout,
}) => {
  return (
    <Header
      className="responsive-header"
      style={{
        padding: "0 24px",
        background: "rgba(2, 2, 5, 0.7)",
        backdropFilter: "blur(20px) saturate(180%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        borderBottom: "1px solid rgba(0, 243, 255, 0.15)",
        height: "80px",
        zIndex: 1000,
        boxShadow: "0 4px 30px rgba(0, 0, 0, 0.5)",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
        <motion.div whileHover={{ scale: 1.1 }} whileTap={{ scale: 0.9 }}>
          {React.createElement(
            collapsed ? MenuUnfoldOutlined : MenuFoldOutlined,
            {
              className: "trigger",
              onClick: () => setCollapsed(!collapsed),
              style: {
                fontSize: "20px",
                color: "var(--neon-blue)",
                cursor: "pointer",
              },
            },
          )}
        </motion.div>

        <Breadcrumb
          separator={<span style={{ color: "rgba(255,255,255,0.2)" }}>/</span>}
          style={{ display: "flex", alignItems: "center" }}
        >
          <Breadcrumb.Item href="">
            <HomeOutlined style={{ color: "rgba(255,255,255,0.45)" }} />
          </Breadcrumb.Item>
          {getBreadcrumbs().map((bc: any, idx: number) => (
            <Breadcrumb.Item key={idx}>
              <span
                style={{
                  color:
                    idx === getBreadcrumbs().length - 1
                      ? "var(--neon-blue)"
                      : "rgba(255,255,255,0.6)",
                  fontWeight: idx === getBreadcrumbs().length - 1 ? 700 : 400,
                  fontSize: "13px",
                  letterSpacing: "0.5px",
                }}
              >
                {bc.title}
              </span>
            </Breadcrumb.Item>
          ))}
        </Breadcrumb>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "16px",
            borderRight: "1px solid rgba(255,255,255,0.1)",
            paddingRight: "24px",
          }}
        >
          <Tooltip title="Neural Link Active">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <ConnectionIndicator />
              <Text
                style={{
                  color: "var(--success)",
                  fontSize: 11,
                  fontWeight: 700,
                  letterSpacing: 1,
                }}
              >
                CONNECTED
              </Text>
            </div>
          </Tooltip>

          <Badge dot color="var(--neon-blue)">
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: "rgba(255,255,255,0.05)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
              }}
            >
              <BellOutlined
                style={{ color: "rgba(255,255,255,0.85)", fontSize: 16 }}
              />
            </div>
          </Badge>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ textAlign: "right" }}>
            <div
              style={{
                color: "var(--neon-blue)",
                fontSize: "11px",
                fontWeight: 800,
                letterSpacing: "1.5px",
                textTransform: "uppercase",
                lineHeight: 1,
              }}
            >
              {isAdmin ? "System Architect" : "Operator"}
            </div>
            <div
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: "12px",
                fontFamily: "JetBrains Mono",
              }}
            >
              {user?.email || "Guest User"}
            </div>
          </div>

          <Tooltip title="Security Clearance Alpha">
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 10,
                background:
                  "linear-gradient(135deg, rgba(0, 243, 255, 0.2), rgba(188, 19, 254, 0.2))",
                border: "1px solid rgba(0, 243, 255, 0.3)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                position: "relative",
                overflow: "hidden",
              }}
            >
              <ThunderboltOutlined
                style={{ color: "#fff", fontSize: 18, zIndex: 1 }}
              />
              <div
                style={{
                  position: "absolute",
                  inset: 0,
                  background: "rgba(0,0,0,0.2)",
                }}
              />
            </div>
          </Tooltip>

          <Button
            onClick={handleLogout}
            className="glass-action-button"
            icon={
              isAuthenticated ? (
                <LogoutOutlined style={{ color: "var(--warning)" }} />
              ) : (
                <LoginOutlined style={{ color: "var(--success)" }} />
              )
            }
            style={{
              height: "38px",
              minHeight: "38px",
              padding: "0 16px",
              borderRadius: 10,
              borderColor: isAuthenticated
                ? "rgba(255, 152, 0, 0.2)"
                : "rgba(16, 185, 129, 0.2)",
              color: isAuthenticated ? "var(--warning)" : "var(--success)",
            }}
          >
            {isAuthenticated ? "LOGOUT" : "LOGIN"}
          </Button>
        </div>
      </div>
    </Header>
  );
};

export default DashboardHeader;
