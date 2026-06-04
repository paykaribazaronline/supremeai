import { UserOutlined, EditOutlined, SaveOutlined } from "@ant-design/icons";
import { Typography, Avatar, Switch, Select, Button, Divider } from "antd";
import { motion } from "framer-motion";
import React from "react";

import { useRole } from "../contexts/RoleContext";

const { Title, Text } = Typography;

interface UserSettingsProps {
  darkMode?: boolean;
  setDarkMode?: (val: boolean) => void;
  chatFont?: string;
  setChatFont?: (val: string) => void;
}

const UserSettings: React.FC<UserSettingsProps> = ({
  darkMode,
  setDarkMode,
  chatFont,
  setChatFont,
}) => {
  const { role } = useRole(); // Assuming useRole provides the current user context

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      style={{ maxWidth: "800px", margin: "0 auto", padding: "24px" }}
    >
      <div
        className="glass-panel"
        style={{ padding: "32px", borderRadius: "16px" }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "24px",
            marginBottom: "32px",
          }}
        >
          <Avatar
            size={80}
            icon={<UserOutlined />}
            style={{ backgroundColor: "var(--neon-blue)" }}
          />
          <div>
            <Title level={3} style={{ color: "#fff", margin: 0 }}>
              Operator Profile
            </Title>
            <Text style={{ color: "var(--text-dim)" }}>
              Access Level:{" "}
              <strong
                style={{
                  color: "var(--neon-purple)",
                  textTransform: "uppercase",
                }}
              >
                {role || "GUEST"}
              </strong>
            </Text>
          </div>
        </div>

        <Divider style={{ borderColor: "rgba(255,255,255,0.1)" }} />

        <Title
          level={5}
          style={{ color: "var(--neon-blue)", marginBottom: "24px" }}
        >
          Appearance & Preferences
        </Title>

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "24px",
          }}
        >
          <div>
            <Text style={{ color: "#fff", fontSize: "16px", display: "block" }}>
              Dark Mode
            </Text>
            <Text style={{ color: "var(--text-dim)", fontSize: "12px" }}>
              Toggle system-wide dark theme
            </Text>
          </div>
          <Switch
            checked={darkMode}
            onChange={(checked) => setDarkMode && setDarkMode(checked)}
            className="cyber-switch"
          />
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "32px",
          }}
        >
          <div>
            <Text style={{ color: "#fff", fontSize: "16px", display: "block" }}>
              Terminal Font Style
            </Text>
            <Text style={{ color: "var(--text-dim)", fontSize: "12px" }}>
              Choose the font used in the AI chat
            </Text>
          </div>
          <Select
            value={chatFont}
            onChange={(val) => setChatFont && setChatFont(val)}
            style={{ width: 150 }}
            options={[
              { value: "font-mono", label: "JetBrains Mono" },
              { value: "font-sans", label: "System Sans" },
              { value: "font-serif", label: "Classic Serif" },
            ]}
          />
        </div>

        <Divider style={{ borderColor: "rgba(255,255,255,0.1)" }} />

        <div
          style={{ display: "flex", justifyContent: "flex-end", gap: "16px" }}
        >
          <Button
            icon={<EditOutlined />}
            ghost
            style={{ borderColor: "rgba(255,255,255,0.3)" }}
          >
            Edit Profile
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            className="cyber-button"
          >
            Save Preferences
          </Button>
        </div>
      </div>
    </motion.div>
  );
};

export default UserSettings;
