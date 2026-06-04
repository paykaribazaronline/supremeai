import { Layout, Menu, Typography, Switch } from "antd";
import { motion } from "framer-motion";
import React, { useRef, useEffect } from "react";

const { Sider } = Layout;
const { Text } = Typography;

interface DashboardSidebarProps {
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  activeKey: string;
  setActiveKey: (key: string) => void;
  menuItems: any[];
  isAdmin: boolean;
  isAuthenticated: boolean;
  autoHide: boolean;
  setAutoHide: (autoHide: boolean) => void;
}

const DashboardSidebar: React.FC<DashboardSidebarProps> = ({
  collapsed,
  setCollapsed,
  activeKey,
  setActiveKey,
  menuItems,
  isAdmin,
  isAuthenticated,
  autoHide,
  setAutoHide,
}) => {
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleMouseEnter = () => {
    if (!autoHide) return;
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    if (collapsed) {
      setCollapsed(false);
    }
  };

  const handleMouseLeave = () => {
    if (!autoHide) return;
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    timeoutRef.current = setTimeout(() => {
      setCollapsed(true);
    }, 400);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return (
    <Sider
      collapsed={collapsed}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      theme="dark"
      className="glass-panel responsive-sidebar"
      width={260}
      collapsedWidth={0}
      style={{
        position: "fixed",
        top: "8px",
        left: "8px",
        bottom: "8px",
        borderRadius: "var(--radius-lg)",
        border: "1px solid rgba(255,255,255,0.05)",
        background: "rgba(0,0,0,0.6)",
        zIndex: 10,
      }}
    >
      <div
        style={{
          height: "var(--header-height, 80px)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "0 var(--space-3)",
          borderBottom: "1px solid rgba(255,255,255,0.05)",
          background: "rgba(0,0,0,0.4)",
        }}
      >
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "12px",
          }}
        >
          <div
            style={{
              width: "32px",
              height: "32px",
              borderRadius: "8px",
              background: "rgba(0, 243, 255, 0.1)",
              border: "1px solid var(--neon-blue)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              boxShadow: "0 0 10px rgba(0, 243, 255, 0.4)",
            }}
          >
            <span style={{ color: "var(--neon-blue)", fontWeight: "bold" }}>
              N
            </span>
          </div>
          {!collapsed && (
            <span
              style={{
                color: "var(--neon-blue)",
                fontWeight: 800,
                fontSize: "18px",
                letterSpacing: "3px",
                fontFamily: "'Outfit', sans-serif",
              }}
            >
              NEUROLYNX
            </span>
          )}
        </motion.div>
      </div>

      <Menu
        mode="inline"
        selectedKeys={[activeKey]}
        onClick={(e) => setActiveKey(e.key)}
        items={menuItems}
        theme="dark"
        style={{
          background: "transparent",
          borderRight: "none",
          marginTop: 24,
        }}
      />

      {!collapsed && (
        <div
          style={{
            position: "absolute",
            bottom: "var(--space-5)",
            left: "var(--space-3)",
            right: "var(--space-3)",
          }}
        >
          <div
            style={{
              padding: "var(--space-3)",
              background: "rgba(255,255,255,0.03)",
              borderRadius: "var(--radius-lg)",
              border: "1px solid rgba(255,255,255,0.05)",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: "var(--space-3)",
                paddingBottom: "var(--space-2)",
                borderBottom: "1px solid rgba(255,255,255,0.05)",
              }}
            >
              <Text
                style={{
                  fontSize: "var(--text-xs)",
                  color: "rgba(255,255,255,0.6)",
                  letterSpacing: 0.5,
                }}
              >
                সাইডবার অটো-হাইড
              </Text>
              <Switch
                checked={autoHide}
                onChange={(checked) => {
                  setAutoHide(checked);
                  localStorage.setItem(
                    "sidebar_autohide",
                    checked ? "true" : "false",
                  );
                  if (checked) {
                    setCollapsed(true);
                  }
                }}
                size="small"
                className="cyber-switch"
              />
            </div>

            <Text
              style={{
                fontSize: "var(--text-xs)",
                color: "rgba(255,255,255,0.4)",
                textTransform: "uppercase",
                letterSpacing: 1,
              }}
            >
              অথোরাইজেশন ট্রেস
            </Text>
            <div
              style={{
                display: "flex",
                gap: "var(--space-1)",
                marginTop: "var(--space-3)",
              }}
            >
              {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                <div
                  key={i}
                  style={{
                    height: "clamp(4px, 1vw, 8px)",
                    flex: 1,
                    background:
                      i <= (isAdmin ? 8 : isAuthenticated ? 4 : 1)
                        ? "var(--neon-blue)"
                        : "rgba(255,255,255,0.05)",
                    boxShadow:
                      i <= (isAdmin ? 8 : isAuthenticated ? 4 : 1)
                        ? "0 0 clamp(4px, 1vw, 8px) var(--neon-blue)"
                        : "none",
                    borderRadius: "clamp(1px, 0.25vw, 3px)",
                  }}
                />
              ))}
            </div>
          </div>
        </div>
      )}
    </Sider>
  );
};

export default DashboardSidebar;
