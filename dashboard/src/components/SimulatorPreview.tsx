import {
  MobileOutlined,
  RotateRightOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  ReloadOutlined,
  CodeOutlined,
  BugOutlined,
  DashboardOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import {
  Card,
  Select,
  Button,
  Space,
  Typography,
  Tag,
  Tooltip,
  Slider,
  Empty,
  Tabs,
  List,
  Badge,
  message as antMessage,
} from "antd";
import { motion, AnimatePresence } from "framer-motion";
import React, { useState, useEffect, useRef } from "react";

const { Text, Title } = Typography;
const { Option } = Select;

interface SimulatorPreviewProps {
  appId?: string;
  initialDevice?: string;
}

const DEVICES = {
  PIXEL_6: { name: "Pixel 6", width: 412, height: 915, bezel: 12, radius: 36 },
  PIXEL_7: { name: "Pixel 7", width: 412, height: 951, bezel: 12, radius: 36 },
  SAMSUNG_S24: {
    name: "Galaxy S24",
    width: 360,
    height: 904,
    bezel: 10,
    radius: 24,
  },
  IPHONE_15: {
    name: "iPhone 15",
    width: 393,
    height: 852,
    bezel: 14,
    radius: 50,
  },
  IPHONE_15_PRO: {
    name: "iPhone 15 Pro",
    width: 393,
    height: 852,
    bezel: 12,
    radius: 55,
  },
  TABLET_10: {
    name: 'Tablet 10"',
    width: 1200,
    height: 800,
    bezel: 20,
    radius: 20,
  },
};

const SimulatorPreview: React.FC<SimulatorPreviewProps> = ({
  appId,
  initialDevice = "PIXEL_6",
}) => {
  const [deviceType, setDeviceType] = useState(initialDevice);
  const [zoom, setZoom] = useState(0.7);
  const [rotation, setRotation] = useState(0);
  const [loading, setLoading] = useState(false);
  const [key, setKey] = useState(0);
  const [activeTab, setActiveTab] = useState("preview");
  const [logs, setLogs] = useState<any[]>([]);
  const [status, setStatus] = useState<
    "DISCONNECTED" | "CONNECTING" | "CONNECTED"
  >("DISCONNECTED");
  const [sessionInfo, setSessionInfo] = useState<any>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const device = DEVICES[deviceType as keyof typeof DEVICES] || DEVICES.PIXEL_6;
  const previewUrl = appId
    ? `/api/simulator/preview/${appId}?device=${deviceType}`
    : undefined;

  // Listen for 'ready' event from iframe
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      if (event.data && event.data.source === "supremeai-simulator") {
        if (event.data.type === "ready") {
          console.log("[Dashboard] Simulator ready:", event.data.data);
          setSessionInfo(event.data.data);
          connectWebSocket(event.data.data.sessionId);
        } else if (event.data.type === "log") {
          addLog(event.data.data);
        }
      }
    };

    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, [appId]);

  const connectWebSocket = (sessionId: string) => {
    if (wsRef.current) wsRef.current.close();

    setStatus("CONNECTING");
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const wsUrl = `${protocol}//${window.location.host}/ws/simulator/dashboard/${sessionId}`;

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setStatus("CONNECTED");
      antMessage.success("সিমুলেটর কন্ট্রোল কানেক্টেড");
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === "log") {
          addLog(msg);
        } else if (msg.type === "status") {
          if (msg.data === "ready") {
            console.log("[Dashboard] Runtime agent is active");
          }
        } else if (msg.type === "error") {
          antMessage.error(msg.message);
        }
      } catch (e) {
        console.error("[Dashboard] Failed to parse message", e);
      }
    };

    ws.onclose = () => {
      setStatus("DISCONNECTED");
    };

    ws.onerror = (err) => {
      console.error("[WS] Error:", err);
      setStatus("DISCONNECTED");
    };
  };

  const addLog = (log: any) => {
    setLogs((prev) => [
      {
        ...log,
        id: Date.now() + Math.random(),
        timestamp: new Date().toLocaleTimeString(),
      },
      ...prev.slice(0, 99),
    ]);
  };

  const sendCommand = (cmd: string, data: any = {}) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      if (cmd === "screenshot") {
        handleScreenshot();
        return;
      }
      wsRef.current.send(
        JSON.stringify({
          type: "command",
          data: cmd,
          ...data,
        }),
      );
    } else {
      antMessage.warning("সিমুলেটর কানেক্টেড নেই");
    }
  };

  const handleScreenshot = async () => {
    if (!appId) return;
    try {
      const response = await fetch(
        `/api/simulator/preview/${appId}/screenshot`,
      );
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `screenshot-${appId}-${Date.now()}.png`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      antMessage.success("স্ক্রিনশট সেভ করা হয়েছে");
    } catch (e) {
      antMessage.error("স্ক্রিনশট নিতে ব্যর্থ হয়েছে");
    }
  };

  const handleReload = () => {
    setLoading(true);
    setKey((prev) => prev + 1);
    setLogs([]);
    setSessionInfo(null);
    if (wsRef.current) wsRef.current.close();
    setTimeout(() => setLoading(false), 1000);
  };

  return (
    <Card
      className="glass-panel"
      bodyStyle={{ padding: 0, overflow: "hidden" }}
    >
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        centered
        className="simulator-tabs"
        items={[
          {
            key: "preview",
            label: (
              <span>
                <DashboardOutlined /> প্রিভিউ
              </span>
            ),
            children: (
              <div
                style={{
                  background: "rgba(0,0,0,0.4)",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  padding: "40px 20px",
                  minHeight: 700,
                  position: "relative",
                }}
              >
                <div
                  style={{
                    marginBottom: 24,
                    width: "100%",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "0 20px",
                  }}
                >
                  <Space>
                    <Select
                      value={deviceType}
                      onChange={setDeviceType}
                      style={{ width: 160 }}
                      size="small"
                    >
                      {Object.entries(DEVICES).map(([key, val]) => (
                        <Option key={key} value={key}>
                          {val.name}
                        </Option>
                      ))}
                    </Select>
                    <Badge
                      status={status === "CONNECTED" ? "success" : "default"}
                      text={status}
                    />
                  </Space>
                  <Space align="center" style={{ width: 200 }}>
                    <ZoomOutOutlined
                      onClick={() => setZoom(Math.max(0.3, zoom - 0.1))}
                      style={{
                        cursor: "pointer",
                        color: "rgba(255,255,255,0.4)",
                      }}
                    />
                    <Slider
                      min={0.3}
                      max={1.0}
                      step={0.1}
                      value={zoom}
                      onChange={setZoom}
                      style={{ flex: 1, margin: "0 10px" }}
                      tooltip={{ open: false }}
                    />
                    <ZoomInOutlined
                      onClick={() => setZoom(Math.min(1.0, zoom + 0.1))}
                      style={{
                        cursor: "pointer",
                        color: "rgba(255,255,255,0.4)",
                      }}
                    />
                  </Space>
                  <Space>
                    <Tooltip title="রোটেট">
                      <Button
                        size="small"
                        icon={<RotateRightOutlined />}
                        onClick={() => setRotation((prev) => (prev + 90) % 360)}
                      />
                    </Tooltip>
                    <Tooltip title="রিফ্রেশ">
                      <Button
                        size="small"
                        icon={<ReloadOutlined />}
                        onClick={handleReload}
                        loading={loading}
                      />
                    </Tooltip>
                  </Space>
                </div>

                {!appId ? (
                  <Empty
                    description="সিমুলেট করার জন্য একটি অ্যাপ নির্বাচন করুন"
                    style={{ marginTop: 100 }}
                  />
                ) : (
                  <motion.div
                    animate={{ rotate: rotation }}
                    transition={{ type: "spring", stiffness: 300, damping: 30 }}
                    style={{
                      position: "relative",
                      width:
                        rotation % 180 === 0 ? device.width : device.height,
                      height:
                        rotation % 180 === 0 ? device.height : device.width,
                      transform: `scale(${zoom})`,
                      transformOrigin: "top center",
                      transition: "width 0.3s, height 0.3s",
                    }}
                  >
                    <div
                      style={{
                        position: "absolute",
                        top: -device.bezel,
                        left: -device.bezel,
                        right: -device.bezel,
                        bottom: -device.bezel,
                        background: "#1a1a1c",
                        borderRadius: device.radius,
                        border: "2px solid #333",
                        boxShadow:
                          "0 30px 60px rgba(0,0,0,0.8), inset 0 0 10px rgba(255,255,255,0.1)",
                        pointerEvents: "none",
                        zIndex: 1,
                      }}
                    >
                      {/* Notch based on device type */}
                    </div>

                    <div
                      style={{
                        width: "100%",
                        height: "100%",
                        background: "#fff",
                        borderRadius: device.radius - device.bezel,
                        overflow: "hidden",
                        position: "relative",
                        zIndex: 0,
                      }}
                    >
                      <iframe
                        ref={iframeRef}
                        key={key}
                        src={previewUrl}
                        style={{
                          width: "100%",
                          height: "100%",
                          border: "none",
                          background: "#fff",
                        }}
                        title="Simulator Preview"
                      />
                    </div>
                  </motion.div>
                )}
              </div>
            ),
          },
          {
            key: "logs",
            label: (
              <span>
                <BugOutlined /> Logs
              </span>
            ),
            children: (
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  height: 600,
                }}
              >
                <div
                  style={{
                    padding: "8px 16px",
                    background: "#141414",
                    borderBottom: "1px solid #333",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    লাইভ স্ট্রিমিং লগ ({logs.length})
                  </Text>
                  <Button
                    size="small"
                    type="text"
                    danger
                    onClick={() => setLogs([])}
                  >
                    ক্লিয়ার
                  </Button>
                </div>
                <div
                  style={{
                    padding: 20,
                    flex: 1,
                    overflowY: "auto",
                    background: "#000",
                  }}
                >
                  <List
                    dataSource={logs}
                    renderItem={(item) => (
                      <List.Item
                        style={{
                          borderBottom: "1px solid #1a1a1a",
                          padding: "6px 0",
                          alignItems: "flex-start",
                        }}
                      >
                        <Text
                          code
                          style={{ color: "#555", fontSize: 10, minWidth: 80 }}
                        >
                          [{item.timestamp}]
                        </Text>
                        <Tag
                          color={
                            item.level === "error"
                              ? "red"
                              : item.level === "warn"
                                ? "orange"
                                : "blue"
                          }
                          style={{
                            fontSize: 9,
                            margin: "0 8px",
                            borderRadius: 2,
                            border: "none",
                            background: "rgba(255,255,255,0.05)",
                          }}
                        >
                          {item.level.toUpperCase()}
                        </Tag>
                        <Text
                          style={{
                            color:
                              item.level === "error"
                                ? "#ff4d4f"
                                : item.level === "warn"
                                  ? "#faad14"
                                  : "#d9d9d9",
                            fontFamily: "monospace",
                            fontSize: 11,
                            whiteSpace: "pre-wrap",
                            wordBreak: "break-word",
                          }}
                        >
                          {item.message}
                        </Text>
                      </List.Item>
                    )}
                    locale={{
                      emptyText: (
                        <div style={{ padding: 40, textAlign: "center" }}>
                          <Text type="secondary">কোনো লগ নেই</Text>
                        </div>
                      ),
                    }}
                  />
                </div>
              </div>
            ),
          },
          {
            key: "controls",
            label: (
              <span>
                <ThunderboltOutlined /> কন্ট্রোল
              </span>
            ),
            children: (
              <div style={{ padding: 40, textAlign: "center" }}>
                <Title level={4}>রিমোট কন্ট্রোল প্যানেল</Title>
                <Space
                  direction="vertical"
                  size="large"
                  style={{ width: "100%" }}
                >
                  <Card
                    size="small"
                    style={{ background: "rgba(255,255,255,0.02)" }}
                  >
                    <Space size="large">
                      <Button
                        type="primary"
                        onClick={() => sendCommand("back")}
                      >
                        পেছনে
                      </Button>
                      <Button
                        type="primary"
                        onClick={() => sendCommand("home")}
                      >
                        হোম
                      </Button>
                      <Button
                        type="primary"
                        onClick={() => sendCommand("screenshot")}
                      >
                        স্ক্রিনশট
                      </Button>
                    </Space>
                  </Card>
                  <Text type="secondary">
                    মোবাইল অ্যাপ স্ক্যান করে রিমোট কন্ট্রোল সিঙ্ক্রোনাইজ করুন
                    (শীঘ্রই আসছে)
                  </Text>
                </Space>
              </div>
            ),
          },
        ]}
      />
    </Card>
  );
};

export default SimulatorPreview;
