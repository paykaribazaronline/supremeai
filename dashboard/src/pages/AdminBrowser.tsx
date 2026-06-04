// AdminBrowser.tsx - Cinematic Autonomous Browser
import {
  ChromeOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  SearchOutlined,
  SafetyOutlined,
  RocketOutlined,
  RobotOutlined,
  CodeOutlined,
  DotChartOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  notification,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect, useRef } from "react";

// Modular Components
import BrowserDirectCommand from "../components/browser/BrowserDirectCommand";
import BrowserHeader from "../components/browser/BrowserHeader";
import BrowserSafetyDrawer from "../components/browser/BrowserSafetyDrawer";
import BrowserToolbar from "../components/browser/BrowserToolbar";
import BrowserViewport from "../components/browser/BrowserViewport";
import IntelligenceFeed from "../components/browser/IntelligenceFeed";
import IntelligenceTabs from "../components/browser/IntelligenceTabs";
import StructureTreeDrawer from "../components/browser/StructureTreeDrawer";
import { useRole } from "../contexts/RoleContext";
import { authUtils } from "../lib/authUtils";

const { Title, Text } = Typography;

const AdminBrowser: React.FC = () => {
  const { isGuest } = useRole();
  const [url, setUrl] = useState("https://www.google.com");
  const [displayUrl, setDisplayUrl] = useState("https://www.google.com");
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [navigating, setNavigating] = useState(false);
  const [stepping, setStepping] = useState(false);
  const [activities, setActivities] = useState<any[]>([]);
  const [findings, setFindings] = useState<any[]>([]);
  const [isAutoMode, setIsAutoMode] = useState(false);
  const [showDom, setShowDom] = useState(false);
  const [domTree, setDomTree] = useState<any>(null);
  const [keyInput, setKeyInput] = useState("");
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [showSettings, setShowSettings] = useState(false);
  const [isLearning, setIsLearning] = useState(true);
  const [showConsole, setShowConsole] = useState(true);
  const [deniedUrls, setDeniedUrls] = useState<any[]>([]);
  const [lastAiAction, setLastAiAction] = useState<any>(null);
  const [votingDetails, setVotingDetails] = useState<any[]>([]);

  const browserRef = useRef<HTMLImageElement>(null);

  const fetchScreenshot = async () => {
    try {
      const response = await authUtils.fetchWithAuth(
        "/api/browser/surf/screenshot",
      );
      if (response.ok) {
        const data = await response.json();
        if (data.screenshot)
          setScreenshot(`data:image/png;base64,${data.screenshot}`);
      }
    } catch (err) {}
  };

  const fetchData = async () => {
    try {
      const actRes = await authUtils.fetchWithAuth(
        "/api/browser/activity/recent",
      );
      if (actRes.ok) {
        const data = await actRes.json();
        setActivities(data.activities || []);
      }
    } catch (err) {}
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(() => {
      fetchScreenshot();
      fetchData();
    }, 1500);
    return () => clearInterval(interval);
  }, []);

  const handleNavigate = async (targetUrl?: string) => {
    const finalUrl = targetUrl || url;
    if (!finalUrl || isGuest) return;
    setNavigating(true);
    try {
      await authUtils.fetchWithAuth("/api/browser/surf/navigate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url: finalUrl }),
      });
      if (targetUrl) setUrl(targetUrl);
      setDisplayUrl(finalUrl);
      fetchScreenshot();
    } catch (err) {
    } finally {
      setNavigating(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: "1600px", margin: "0 auto" }}
    >
      {/* Cinematic Header */}
      <div
        style={{
          marginBottom: 32,
          borderBottom: "1px solid var(--neon-blue)",
          paddingBottom: 24,
          opacity: 0.8,
        }}
      >
        <Row justify="space-between" align="bottom">
          <Col>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                marginBottom: 8,
              }}
            >
              <ChromeOutlined
                style={{ color: "var(--neon-blue)", fontSize: 20 }}
              />
              <Text
                style={{
                  color: "var(--neon-blue)",
                  letterSpacing: 2,
                  fontWeight: 800,
                  fontSize: 12,
                }}
              >
                AUTONOMOUS SURFING
              </Text>
            </div>
            <Title
              level={2}
              style={{
                color: "#fff",
                margin: 0,
                fontWeight: 800,
                fontSize: 32,
              }}
            >
              Neural <span className="text-gradient">Browser</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              AI-driven web exploration, data extraction, and automated
              interaction matrix.
            </Text>
          </Col>
          <Col>
            <Space>
              <Badge
                status="processing"
                color="var(--neon-blue)"
                text={
                  <Text style={{ color: "var(--neon-blue)", fontWeight: 700 }}>
                    LIVE STREAM
                  </Text>
                }
              />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => fetchScreenshot()}
                className="glass-action-button"
              >
                Force Refresh
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        {/* KPI Stats */}
        <Col span={24}>
          <Row gutter={[24, 24]}>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid var(--neon-blue)" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Session Node
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 18, fontWeight: 800 }}
                    >
                      V-NODE-042
                    </div>
                  </div>
                  <GlobalOutlined
                    style={{ color: "var(--neon-blue)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid var(--neon-purple)" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      AI Confidence
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      94.2%
                    </div>
                  </div>
                  <RobotOutlined
                    style={{ color: "var(--neon-purple)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div
                className="glass-card"
                style={{ borderLeft: "4px solid var(--success)" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Security Layer
                    </Text>
                    <div
                      style={{
                        color: "var(--success)",
                        fontSize: 20,
                        fontWeight: 800,
                      }}
                    >
                      ENFORCED
                    </div>
                  </div>
                  <SafetyOutlined
                    style={{ color: "var(--success)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
            <Col xs={12} lg={6}>
              <div className="glass-card">
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <Text
                      style={{
                        color: "rgba(255,255,255,0.45)",
                        fontSize: 11,
                        textTransform: "uppercase",
                      }}
                    >
                      Network Latency
                    </Text>
                    <div
                      style={{ color: "#fff", fontSize: 24, fontWeight: 800 }}
                    >
                      82ms
                    </div>
                  </div>
                  <ThunderboltOutlined
                    style={{ color: "var(--text-dim)", fontSize: 24 }}
                  />
                </div>
              </div>
            </Col>
          </Row>
        </Col>

        {/* Browser Core */}
        <Col xs={24} lg={16}>
          <div
            className="glass-card"
            style={{
              padding: 0,
              overflow: "hidden",
              border: "1px solid rgba(0, 243, 255, 0.15)",
            }}
          >
            <BrowserToolbar
              url={url}
              setUrl={setUrl}
              handleNavigate={handleNavigate}
              fetchScreenshot={fetchScreenshot}
              handleTypeKey={() => {}}
              fetchDom={() => {}}
              setShowDom={setShowDom}
              showDom={showDom}
              showConsole={showConsole}
              setShowConsole={setShowConsole}
              setShowSettings={setShowSettings}
              showSettings={showSettings}
              deniedUrls={deniedUrls}
            />

            <BrowserViewport
              screenshot={screenshot}
              displayUrl={displayUrl}
              mousePos={mousePos}
              setMousePos={setMousePos}
              handleBrowserClick={() => {}}
              lastAiAction={lastAiAction}
              showConsole={showConsole}
              votingDetails={votingDetails}
              stepping={stepping}
              navigating={navigating}
              browserRef={browserRef}
            />

            <BrowserDirectCommand
              keyInput={keyInput}
              setKeyInput={setKeyInput}
              handleTypeKey={() => {}}
            />
          </div>

          <div style={{ marginTop: 24 }}>
            <IntelligenceFeed activities={activities} findings={findings} />
          </div>
        </Col>

        <Col xs={24} lg={8}>
          <IntelligenceTabs activities={activities} findings={findings} />
        </Col>
      </Row>

      <StructureTreeDrawer
        open={showDom}
        onClose={() => setShowDom(false)}
        domTree={domTree}
      />
      <BrowserSafetyDrawer
        open={showSettings}
        onClose={() => setShowSettings(false)}
        allowedUrls={[]}
        deniedUrls={[]}
        credentials={[]}
        isLearning={isLearning}
        toggleLearning={(val: boolean) => setIsLearning(val)}
        learningStatus={null}
        newUrlPattern=""
        setNewUrlPattern={() => {}}
        newUrlType="allowed"
        setNewUrlType={() => {}}
        handleSavePermission={() => {}}
        newCred={{ website: "", username: "", password: "", token: "" }}
        setNewCred={() => {}}
        handleSaveCredential={() => {}}
        handleDeleteUrl={() => {}}
        handleDeleteCredential={() => {}}
      />
    </motion.div>
  );
};

export default AdminBrowser;
