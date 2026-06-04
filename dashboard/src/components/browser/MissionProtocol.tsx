import {
  RocketOutlined,
  InfoCircleOutlined,
  BulbOutlined,
} from "@ant-design/icons";
import {
  Card,
  Typography,
  Tag,
  Space,
  Input,
  Button,
  Divider,
  Progress,
} from "antd";
import React from "react";

const { Title, Text, Paragraph } = Typography;

interface MissionProtocolProps {
  currentTask: any;
  goal: string;
  setGoal: (goal: string) => void;
  handleCreateTask: () => void;
  handleStep: () => void;
  stepping: boolean;
  isGuest: boolean;
}

const MissionProtocol: React.FC<MissionProtocolProps> = ({
  currentTask,
  goal,
  setGoal,
  handleCreateTask,
  handleStep,
  stepping,
  isGuest,
}) => {
  return (
    <Card className="glass-panel ai-glow" style={{ marginBottom: 24 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: 20,
        }}
      >
        <Title level={4} style={{ color: "#fff", margin: 0 }}>
          <RocketOutlined style={{ color: "#00f2fe", marginRight: 10 }} />
          Mission Protocol
        </Title>
        <Tag color="cyan">AUTONOMOUS</Tag>
      </div>

      {currentTask ? (
        <div style={{ marginBottom: 24 }}>
          <div
            style={{
              background: "rgba(0,0,0,0.4)",
              padding: 20,
              borderRadius: 16,
              border: "1px solid rgba(0, 242, 254, 0.1)",
            }}
          >
            <Text
              style={{
                color: "rgba(255,255,255,0.3)",
                fontSize: 10,
                letterSpacing: "1px",
              }}
            >
              PRIMARY OBJECTIVE
            </Text>
            <Paragraph
              style={{
                color: "#fff",
                fontSize: 16,
                marginTop: 8,
                fontWeight: 500,
                lineHeight: 1.4,
              }}
            >
              {currentTask.goal}
            </Paragraph>
            <Divider
              style={{
                margin: "16px 0",
                borderColor: "rgba(255,255,255,0.05)",
              }}
            />
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 8,
              }}
            >
              <Text style={{ color: "rgba(255,255,255,0.4)" }}>
                Mission Completion
              </Text>
              <Text style={{ color: "#00f2fe", fontWeight: 700 }}>
                {currentTask.progress}%
              </Text>
            </div>
            <Progress
              percent={currentTask.progress}
              size="small"
              strokeColor={{ "0%": "#00f2fe", "100%": "#4facfe" }}
            />
          </div>
        </div>
      ) : (
        <div
          style={{
            padding: "30px 20px",
            textAlign: "center",
            background: "rgba(255,255,255,0.02)",
            borderRadius: 16,
            border: "1px dashed rgba(255,255,255,0.1)",
            marginBottom: 20,
          }}
        >
          <InfoCircleOutlined
            style={{
              fontSize: 24,
              color: "rgba(255,255,255,0.2)",
              marginBottom: 12,
            }}
          />
          <Text style={{ color: "rgba(255,255,255,0.3)", display: "block" }}>
            No active browsing mission assigned
          </Text>
        </div>
      )}

      <Input.TextArea
        rows={4}
        placeholder="Describe what the AI should do (e.g. 'Go to Amazon, search for MacBook Pro M3, and list the prices')..."
        value={goal}
        onChange={(e) => setGoal(e.target.value)}
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.1)",
          color: "#fff",
          borderRadius: 12,
          marginBottom: 20,
          padding: 12,
        }}
      />

      <Space direction="vertical" style={{ width: "100%" }} size="middle">
        <Button
          type="primary"
          block
          size="large"
          onClick={handleCreateTask}
          disabled={!goal || isGuest}
          style={{
            height: 54,
            borderRadius: 14,
            fontWeight: 700,
            fontSize: 16,
            boxShadow: "0 8px 16px rgba(0, 242, 254, 0.2)",
          }}
        >
          INITIALIZE PROTOCOL
        </Button>
        {currentTask && (
          <Button
            block
            ghost
            style={{
              height: 48,
              borderRadius: 14,
              borderColor: "rgba(0, 242, 254, 0.3)",
              color: "#00f2fe",
            }}
            onClick={handleStep}
            loading={stepping}
            icon={<BulbOutlined />}
          >
            MANUAL INFERENCE STEP
          </Button>
        )}
      </Space>
    </Card>
  );
};

export default MissionProtocol;
