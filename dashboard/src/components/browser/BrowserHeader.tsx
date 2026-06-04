import {
  MonitorOutlined,
  PauseCircleOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import { Row, Col, Typography, Button } from "antd";
import { motion } from "framer-motion";
import React from "react";

const { Title, Text } = Typography;

interface BrowserHeaderProps {
  isAutoMode: boolean;
  setIsAutoMode: (val: boolean) => void;
}

const BrowserHeader: React.FC<BrowserHeaderProps> = ({
  isAutoMode,
  setIsAutoMode,
}) => {
  return (
    <Col span={24}>
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <Row align="middle" justify="space-between">
          <Col>
            <Title level={2} style={{ color: "#fff", margin: 0 }}>
              <MonitorOutlined style={{ color: "#00f2fe", marginRight: 12 }} />
              Autonomous Command{" "}
              <Text style={{ color: "rgba(0, 242, 254, 0.5)" }}>Hub</Text>
            </Title>
          </Col>
          <Col>
            <Button
              type="primary"
              icon={
                isAutoMode ? <PauseCircleOutlined /> : <ThunderboltOutlined />
              }
              onClick={() => setIsAutoMode(!isAutoMode)}
              style={{
                height: 48,
                borderRadius: 14,
                padding: "0 28px",
                background: isAutoMode
                  ? "#ff4d4f"
                  : "linear-gradient(135deg, #00f2fe 0%, #4facfe 100%)",
                border: "none",
                fontWeight: 600,
              }}
            >
              {isAutoMode ? "Deactivate AI Agent" : "Engage Autonomous Agent"}
            </Button>
          </Col>
        </Row>
      </motion.div>
    </Col>
  );
};

export default BrowserHeader;
