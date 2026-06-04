import {
  ChromeOutlined,
  RobotOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { Spin, Badge, Typography, Progress, Divider } from "antd";
import { motion, AnimatePresence } from "framer-motion";
import React from "react";

const { Title, Text } = Typography;

interface BrowserViewportProps {
  screenshot: string | null;
  displayUrl: string;
  mousePos: { x: number; y: number };
  setMousePos: (pos: { x: number; y: number }) => void;
  handleBrowserClick: (e: React.MouseEvent<HTMLImageElement>) => void;
  lastAiAction: any;
  showConsole: boolean;
  votingDetails: any[];
  stepping: boolean;
  navigating: boolean;
  browserRef: React.RefObject<HTMLImageElement>;
}

const BrowserViewport: React.FC<BrowserViewportProps> = ({
  screenshot,
  displayUrl,
  mousePos,
  setMousePos,
  handleBrowserClick,
  lastAiAction,
  showConsole,
  votingDetails,
  stepping,
  navigating,
  browserRef,
}) => {
  return (
    <div className="browser-viewport">
      <AnimatePresence mode="wait">
        {screenshot ? (
          <div
            style={{
              position: "relative",
              overflow: "hidden",
              borderRadius: "0 0 12px 12px",
            }}
          >
            <motion.img
              key={screenshot}
              initial={{ opacity: 0, scale: 1.02 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.3 }}
              src={screenshot}
              alt="Remote Browser View"
              style={{
                width: "100%",
                height: "auto",
                display: "block",
                imageRendering: "auto",
                boxShadow: "inset 0 0 100px rgba(0,0,0,0.5)",
              }}
              onClick={handleBrowserClick}
              onMouseMove={(e) => {
                const rect = e.currentTarget.getBoundingClientRect();
                setMousePos({
                  x: Math.round((e.clientX - rect.left) * (1280 / rect.width)),
                  y: Math.round((e.clientY - rect.top) * (720 / rect.height)),
                });
              }}
              ref={browserRef}
            />

            {/* Floating Navigation Overlay */}
            <div
              style={{
                position: "absolute",
                top: 20,
                left: "50%",
                transform: "translateX(-50%)",
                background: "rgba(10, 11, 14, 0.7)",
                backdropFilter: "blur(10px)",
                padding: "8px 16px",
                borderRadius: "30px",
                border: "1px solid rgba(255, 255, 255, 0.1)",
                display: "flex",
                alignItems: "center",
                gap: 12,
                zIndex: 5,
              }}
            >
              <Badge status="processing" color="#27c93f" />
              <Text style={{ color: "#fff", fontSize: 12, fontWeight: 500 }}>
                {displayUrl.replace(/^https?:\/\//, "")}
              </Text>
            </div>

            {/* AI Cursor Tracker */}
            {lastAiAction && lastAiAction.action === "click" && (
              <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: [1, 2, 1], opacity: [0.8, 0.2, 0] }}
                transition={{ duration: 1.5, repeat: Infinity }}
                style={{
                  position: "absolute",
                  top: "50%",
                  left: "50%",
                  width: 60,
                  height: 60,
                  background:
                    "radial-gradient(circle, rgba(0, 242, 254, 0.4) 0%, transparent 70%)",
                  borderRadius: "50%",
                  pointerEvents: "none",
                  transform: "translate(-50%, -50%)",
                  zIndex: 10,
                  border: "1px solid rgba(0, 242, 254, 0.3)",
                }}
              />
            )}

            {/* Coordinates & Status */}
            <div
              style={{
                position: "absolute",
                bottom: 16,
                left: 16,
                display: "flex",
                gap: 8,
                zIndex: 5,
              }}
            >
              <div
                style={{
                  background: "rgba(0,0,0,0.7)",
                  padding: "4px 12px",
                  borderRadius: 6,
                  fontSize: 10,
                  color: "#00f2fe",
                  border: "1px solid rgba(0, 242, 254, 0.2)",
                }}
              >
                X: {mousePos.x} Y: {mousePos.y}
              </div>
              <div
                style={{
                  background: "rgba(0,0,0,0.7)",
                  padding: "4px 12px",
                  borderRadius: 6,
                  fontSize: 10,
                  color: "#27c93f",
                  border: "1px solid rgba(39, 201, 63, 0.2)",
                }}
              >
                LIVE STREAM ACTIVE
              </div>
            </div>

            {/* AI Thought Stream Console Overlay */}
            {showConsole && (
              <motion.div
                initial={{ x: 300, opacity: 0 }}
                animate={{ x: 0, opacity: 1 }}
                exit={{ x: 300, opacity: 0 }}
                style={{
                  position: "absolute",
                  top: 80,
                  right: 20,
                  width: 280,
                  maxHeight: "calc(100% - 120px)",
                  background: "rgba(10, 11, 14, 0.85)",
                  backdropFilter: "blur(15px)",
                  borderRadius: 16,
                  border: "1px solid rgba(0, 242, 254, 0.2)",
                  padding: 16,
                  zIndex: 20,
                  display: "flex",
                  flexDirection: "column",
                  boxShadow: "0 20px 40px rgba(0,0,0,0.5)",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: 12,
                  }}
                >
                  <Text
                    style={{
                      color: "#00f2fe",
                      fontSize: 10,
                      fontWeight: 700,
                      letterSpacing: "1px",
                    }}
                  >
                    AI THOUGHT STREAM
                  </Text>
                  <Badge status="processing" color="#00f2fe" />
                </div>

                <div
                  style={{
                    flex: 1,
                    overflowY: "auto",
                    display: "flex",
                    flexDirection: "column",
                    gap: 10,
                  }}
                >
                  {votingDetails.map((v, i) => (
                    <div
                      key={i}
                      style={{
                        background: "rgba(255,255,255,0.03)",
                        padding: 10,
                        borderRadius: 8,
                        borderLeft: `3px solid ${v.confidence > 0.8 ? "#27c93f" : "#faad14"}`,
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          marginBottom: 4,
                        }}
                      >
                        <Text
                          style={{
                            color: "#fff",
                            fontSize: 11,
                            fontWeight: 600,
                          }}
                        >
                          {v.model}
                        </Text>
                        <Text
                          style={{
                            color: v.confidence > 0.8 ? "#27c93f" : "#faad14",
                            fontSize: 10,
                          }}
                        >
                          {(v.confidence * 100).toFixed(0)}%
                        </Text>
                      </div>
                      <Text
                        style={{
                          color: "rgba(255,255,255,0.5)",
                          fontSize: 10,
                          fontStyle: "italic",
                        }}
                      >
                        {v.reasoning}
                      </Text>
                    </div>
                  ))}

                  <Divider
                    style={{
                      margin: "8px 0",
                      borderColor: "rgba(255,255,255,0.05)",
                    }}
                  />

                  <div
                    style={{
                      background: "rgba(0, 242, 254, 0.05)",
                      padding: 10,
                      borderRadius: 8,
                    }}
                  >
                    <Text
                      style={{
                        color: "#00f2fe",
                        fontSize: 10,
                        fontWeight: 700,
                        display: "block",
                        marginBottom: 4,
                      }}
                    >
                      CONSENSUS ACTION
                    </Text>
                    <Text style={{ color: "#fff", fontSize: 11 }}>
                      {stepping
                        ? "Processing inference..."
                        : "Waiting for objective..."}
                    </Text>
                  </div>
                </div>

                <div style={{ marginTop: 12, textAlign: "center" }}>
                  <Text style={{ color: "rgba(255,255,255,0.2)", fontSize: 9 }}>
                    সিস্টেম আপনার নির্দেশ অনুযায়ী সাইটটি বিশ্লেষণ করছে
                  </Text>
                </div>
              </motion.div>
            )}
          </div>
        ) : (
          <div
            style={{
              height: 600,
              display: "flex",
              flexDirection: "column",
              justifyContent: "center",
              alignItems: "center",
              background: "#0a0a0b",
            }}
          >
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
              style={{ marginBottom: 24 }}
            >
              <ChromeOutlined
                style={{ fontSize: 48, color: "rgba(255,255,255,0.1)" }}
              />
            </motion.div>
            <Spin
              size="large"
              tip={
                <span style={{ color: "rgba(255,255,255,0.3)", marginTop: 12 }}>
                  Initializing Neural Bridge...
                </span>
              }
            />
          </div>
        )}
      </AnimatePresence>

      {(navigating || stepping) && (
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(8, 9, 10, 0.9)",
            backdropFilter: "blur(12px)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 100,
          }}
        >
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: [1, 1.1, 1], opacity: 1 }}
            transition={{ repeat: Infinity, duration: 2 }}
            className="ai-glow"
            style={{
              padding: 40,
              borderRadius: "50%",
              background: "rgba(0,242,254,0.05)",
              marginBottom: 32,
            }}
          >
            <RobotOutlined style={{ fontSize: 64, color: "#00f2fe" }} />
          </motion.div>
          <Title
            level={3}
            style={{
              color: "#fff",
              margin: 0,
              fontWeight: 700,
              letterSpacing: "-0.5px",
            }}
          >
            {stepping
              ? "PROCESSING STRATEGIC INFERENCE"
              : "SYNCHRONIZING CONTEXT"}
          </Title>
          <Text
            style={{
              color: "rgba(255,255,255,0.4)",
              marginTop: 12,
              fontSize: 14,
              letterSpacing: "1px",
            }}
          >
            {stepping
              ? "SYSTEM IS ANALYZING DOM TOPOLOGY & VISUAL CUES"
              : "ESTABLISHING SECURE HANDSHAKE WITH REMOTE INSTANCE"}
          </Text>
          <div style={{ width: 350, marginTop: 40 }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 12,
              }}
            >
              <Text
                style={{
                  color: "rgba(0,242,254,0.8)",
                  fontSize: 10,
                  fontWeight: 600,
                }}
              >
                NEURAL COMPUTATION
              </Text>
              <Text style={{ color: "rgba(255,255,255,0.3)", fontSize: 10 }}>
                {stepping ? "88%" : "42%"}
              </Text>
            </div>
            <Progress
              percent={stepping ? 88 : 42}
              status="active"
              strokeColor="#00f2fe"
              showInfo={false}
              strokeWidth={4}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default BrowserViewport;
