import {
  ExclamationCircleOutlined,
  LockOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from "@ant-design/icons";
import {
  Layout,
  theme,
  Typography,
  ConfigProvider,
  message,
  Modal,
  Spin,
  Button,
} from "antd";
import React, { useState, useEffect, Suspense, lazy } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";

import { useRole } from "../contexts/RoleContext";
import { authUtils } from "../lib/authUtils";

import {
  allMenuItems,
  getBreadcrumbs,
  getMenuPath,
} from "./dashboard/DashboardConfigs";
import DashboardHeader from "./dashboard/DashboardHeader";
import DashboardMobileDrawer from "./dashboard/DashboardMobileDrawer";
import DashboardSidebar from "./dashboard/DashboardSidebar";
import { RestrictedDemo } from "./dashboard/RestrictedDemo";

const { Content } = Layout;
const { Text } = Typography;

const AdminRouteLayout: React.FC = () => {
  const { isAdmin, isAuthenticated, isGuest, user, refreshUser } = useRole();
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [autoHide, setAutoHide] = useState(() => {
    return localStorage.getItem("sidebar_autohide") === "true";
  });
  const [darkMode, setDarkMode] = useState(() => {
    return localStorage.getItem("darkMode") !== "false";
  });
  const [chatFont, setChatFont] = useState(
    localStorage.getItem("chatFont") || "font-mono",
  );
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (!isAuthenticated && !isGuest) {
      navigate("/login");
      return;
    }
    if (!isAdmin) {
      message.error("Access denied. Admin privileges required.");
      navigate("/user/dashboard");
    }
  }, [isAdmin, isAuthenticated, isGuest, navigate]);

  const handleLogout = () => {
    const isGuestUser = !isAuthenticated;

    Modal.confirm({
      title: (
        <Text
          style={{ color: "var(--text-main)", fontSize: 18, fontWeight: 700 }}
        >
          {isGuestUser ? "লগইন পেজে যান" : "লগআউট নিশ্চিত করুন"}
        </Text>
      ),
      icon: (
        <ExclamationCircleOutlined
          style={{ color: "var(--warning)", fontSize: 24 }}
        />
      ),
      content: (
        <div style={{ marginTop: 12 }}>
          <Text style={{ color: "var(--text-dim)" }}>
            {isGuestUser
              ? "আপনি কি গেস্ট সেশন শেষ করে মূল লগইন পেজে ফিরে যেতে চান?"
              : "আপনি কি নিশ্চিতভাবে আপনার বর্তমান সেশনটি শেষ করতে চান? সকল সেভ না করা পরিবর্তন হারিয়ে যেতে পারে।"}
          </Text>
        </div>
      ),
      okText: isGuestUser ? "লগইন করুন" : "লগআউট",
      cancelText: "ফিরে যান",
      centered: true,
      okButtonProps: {
        className: "cyber-button",
        style: { background: "var(--warning)", border: "none", color: "#000" },
      },
      cancelButtonProps: {
        style: {
          background: "rgba(255,255,255,0.05)",
          color: "#fff",
          border: "1px solid rgba(255,255,255,0.1)",
        },
      },
      onOk: async () => {
        try {
          await authUtils.logout();
          refreshUser();
          message.success("নিরাপদে লগআউট করা হয়েছে।");
        } catch (error) {
          message.error("লগআউট করতে সমস্যা হয়েছে।");
        }
      },
    });
  };

  const currentRole = isAdmin ? "admin" : isAuthenticated ? "user" : "guest";
  
  const filterMenuItems = (items: any[]): any[] => {
    return items
      .filter((item) => Array.isArray(item.roles) && item.roles.includes(currentRole))
      .map((item) => {
        if (item.children) {
          return { ...item, children: filterMenuItems(item.children) };
        }
        return item;
      });
  };

  const menuItems = filterMenuItems(allMenuItems);

  const getCurrentPageKey = () => {
    const path = location.pathname
      .replace(/^\/admin\/?/, "")
      .replace(/\/$/, "");
    return path || "dashboard";
  };

  const activeKey = getCurrentPageKey();

  const handleNavigate = (key: string) => {
    navigate(getMenuPath(key));
  };

  const renderContent = () => {
    let activeItem = allMenuItems.find((item) => item.key === activeKey);
    if (!activeItem) {
      for (const item of allMenuItems) {
        if (item.children) {
          const child = item.children.find((c) => c.key === activeKey);
          if (child) {
            activeItem = child;
            break;
          }
        }
      }
    }
    
    const hasAccess = activeItem?.roles.includes(currentRole);

    if (!hasAccess && activeKey !== "dashboard") {
      return (
        <RestrictedDemo
          title={activeItem?.label || "Unknown"}
          description="Security clearance insufficient."
          icon={<LockOutlined />}
        />
      );
    }

    return (
      <div
        className="admin-scroll-area"
        style={{
          height: "calc(100vh - 80px)",
          overflowY: "auto",
          overflowX: "hidden",
        }}
      >
        <div
          className="admin-content-container"
          style={{
            padding: "24px",
            maxWidth: "1800px",
            margin: "0 auto",
            width: "100%",
          }}
        >
          <Suspense
            fallback={
              <div
                style={{
                  height: "100%",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <div
                  className="loading-fallback"
                  style={{ background: "transparent", position: "relative" }}
                >
                  <Spin size="large" />
                  <div className="loading-text">SYNCING NEURAL LINK...</div>
                </div>
              </div>
            }
          >
            <Outlet
              context={{ darkMode, setDarkMode, chatFont, setChatFont }}
            />
          </Suspense>
        </div>
      </div>
    );
  };

  if (!mounted) return null;

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: "#00f3ff",
          colorBgBase: "#020205",
          colorTextBase: "#ffffff",
          borderRadius: 8,
          colorLink: "#00f3ff",
        },
        components: {
          Layout: {
            headerBg: "rgba(0,0,0,0.6)",
            bodyBg: "transparent",
            triggerBg: "#00f3ff",
          },
          Menu: {
            itemColor: "#ffffff",
            itemSelectedColor: "#00f3ff",
            itemBg: "transparent",
            itemSelectedBg: "rgba(0, 243, 255, 0.1)",
          },
          Progress: {
            colorSuccess: "#00f3ff",
            colorInfo: "#00f3ff",
            size: 8,
          },
          Table: {
            colorBgContainer: "rgba(13, 13, 18, 0.5)",
            colorTextHeading: "#00f3ff",
          },
        },
      }}
    >
      <Layout
        className="animated-bg"
        style={{ minHeight: "100vh", position: "relative" }}
      >
        <div className="bg-grid" />
        <div className="hex-grid" />
        <div className="scanline" />

        <DashboardSidebar
          collapsed={collapsed}
          setCollapsed={setCollapsed}
          activeKey={activeKey}
          setActiveKey={handleNavigate}
          menuItems={menuItems}
          isAdmin={isAdmin}
          isAuthenticated={isAuthenticated}
          autoHide={autoHide}
          setAutoHide={setAutoHide}
        />

        {autoHide && collapsed && (
          <div
            className="sidebar-hover-trigger"
            onMouseEnter={() => setCollapsed(false)}
          />
        )}

        <DashboardMobileDrawer
          open={mobileDrawerOpen}
          onClose={() => setMobileDrawerOpen(false)}
          activeKey={activeKey}
          setActiveKey={handleNavigate}
          menuItems={menuItems}
          isAdmin={isAdmin}
          isAuthenticated={isAuthenticated}
        />

        <Layout
          className="responsive-layout"
          style={{
            background: "transparent",
            ["--sidebar-margin" as any]: collapsed ? "0px" : "276px",
          }}
        >
          <DashboardHeader
            collapsed={collapsed}
            setCollapsed={setCollapsed}
            getBreadcrumbs={() => getBreadcrumbs(activeKey)}
            isAuthenticated={isAuthenticated}
            isAdmin={isAdmin}
            user={user}
            handleLogout={handleLogout}
          />

          <Content style={{ overflow: "auto", position: "relative" }}>
            {renderContent()}
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
};

export default AdminRouteLayout;
