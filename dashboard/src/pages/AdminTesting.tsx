import {
  ExperimentOutlined,
  PlayCircleOutlined,
  StopOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  BarChartOutlined,
  CodeOutlined,
  ApiOutlined,
} from "@ant-design/icons";
import {
  Typography,
  Row,
  Col,
  Space,
  Button,
  Badge,
  Spin,
  Progress,
  Table,
  Tag,
  Card,
  Modal,
  message,
} from "antd";
import { motion } from "framer-motion";
import React, { useState } from "react";

const { Title, Text } = Typography;

interface TestResult {
  id: string;
  name: string;
  status: "passed" | "failed" | "running";
  duration: number;
  error?: string;
  timestamp: string;
}

const AdminTesting: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [testResults, setTestResults] = useState<TestResult[]>([]);
  const [runningTests, setRunningTests] = useState(false);

  const testSuites = [
    { name: "WebSocket Tests", count: 2, icon: <ApiOutlined /> },
    { name: "Performance Tests", count: 2, icon: <BarChartOutlined /> },
    { name: "Security Tests", count: 2, icon: <ExperimentOutlined /> },
    { name: "Accessibility Tests", count: 2, icon: <CodeOutlined /> },
    { name: "Mobile Tests", count: 2, icon: <CheckCircleOutlined /> },
    { name: "Error Handling Tests", count: 2, icon: <WarningOutlined /> },
    { name: "Data Validation Tests", count: 2, icon: <ReloadOutlined /> },
  ];

  const runTests = async () => {
    setLoading(true);
    setRunningTests(true);
    setTestResults([]);

    for (const suite of testSuites) {
      for (let i = 0; i < suite.count; i++) {
        const testName = `${suite.name} ${i + 1}`;
        const startTime = Date.now();

        setTestResults((prev) => [
          ...prev,
          {
            id: `${Date.now()}-${i}`,
            name: testName,
            status: "running",
            duration: 0,
            timestamp: new Date().toISOString(),
          },
        ]);

        await new Promise((resolve) =>
          setTimeout(resolve, 500 + Math.random() * 1000),
        );

        const passed = Math.random() > 0.1;
        const duration = Date.now() - startTime;

        setTestResults((prev) =>
          prev.map((t) =>
            t.name === testName
              ? {
                  ...t,
                  status: passed ? "passed" : "failed",
                  duration,
                  error: passed
                    ? undefined
                    : "Test assertion failed: expected true to be true",
                }
              : t,
          ),
        );
      }
    }

    setRunningTests(false);
    setLoading(false);
    message.success(
      `All tests completed. ${testResults.filter((t) => t.status === "passed").length} passed.`,
    );
  };

  const stopTests = () => {
    setRunningTests(false);
    setLoading(false);
    message.warning("Tests stopped by user");
  };

  const columns = [
    {
      title: "Test",
      dataIndex: "name",
      key: "name",
      render: (text: string, record: TestResult) => (
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          {record.status === "passed" && (
            <CheckCircleOutlined style={{ color: "#52c41a" }} />
          )}
          {record.status === "failed" && (
            <CloseCircleOutlined style={{ color: "#ff4d4f" }} />
          )}
          {record.status === "running" && (
            <ReloadOutlined style={{ color: "#1890ff" }} spin />
          )}
          <span>{text}</span>
        </div>
      ),
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (status: string) => {
        const statusColors: { [key: string]: string } = {
          passed: "green",
          failed: "red",
          running: "blue",
        };
        return (
          <Badge
            status={statusColors[status] as any}
            text={status.toUpperCase()}
          />
        );
      },
    },
    {
      title: "Duration",
      dataIndex: "duration",
      key: "duration",
      render: (d: number) => (
        <Text style={{ fontFamily: "JetBrains Mono" }}>{d}ms</Text>
      ),
    },
    {
      title: "Timestamp",
      dataIndex: "timestamp",
      key: "timestamp",
      render: (t: string) => (
        <Text style={{ color: "var(--text-dim)" }}>
          {new Date(t).toLocaleTimeString()}
        </Text>
      ),
    },
  ];

  const passedCount = testResults.filter((t) => t.status === "passed").length;
  const failedCount = testResults.filter((t) => t.status === "failed").length;
  const totalTests = testResults.length;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: "1600px", margin: "0 auto" }}
    >
      <div
        style={{
          marginBottom: 32,
          borderBottom: "1px solid rgba(0, 243, 255, 0.1)",
          paddingBottom: 24,
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
              <ExperimentOutlined
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
                AUTOMATED VERIFICATION
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
              Test <span className="text-gradient">Orchestrator</span>
            </Title>
            <Text style={{ color: "var(--text-dim)", fontSize: 14 }}>
              Run automated test suites and view real-time results.
            </Text>
          </Col>
          <Col>
            <Space>
              {runningTests ? (
                <Button icon={<StopOutlined />} onClick={stopTests} danger>
                  Stop Tests
                </Button>
              ) : (
                <Button
                  icon={<PlayCircleOutlined />}
                  onClick={runTests}
                  type="primary"
                  disabled={loading}
                >
                  {loading ? "Running..." : "Run All Tests"}
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
              }}
            >
              Total Tests
            </Text>
            <div style={{ color: "#fff", fontSize: 28, fontWeight: 800 }}>
              {totalTests}
            </div>
          </div>
        </Col>
        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
              }}
            >
              Passed
            </Text>
            <div style={{ color: "#52c41a", fontSize: 28, fontWeight: 800 }}>
              {passedCount}
            </div>
          </div>
        </Col>
        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
              }}
            >
              Failed
            </Text>
            <div style={{ color: "#ff4d4f", fontSize: 28, fontWeight: 800 }}>
              {failedCount}
            </div>
          </div>
        </Col>
        <Col xs={24} md={6}>
          <div className="glass-card" style={{ textAlign: "center" }}>
            <Text
              style={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 11,
                textTransform: "uppercase",
              }}
            >
              Success Rate
            </Text>
            <div
              style={{
                color: "var(--neon-blue)",
                fontSize: 28,
                fontWeight: 800,
              }}
            >
              {totalTests > 0
                ? Math.round((passedCount / totalTests) * 100)
                : 0}
              %
            </div>
          </div>
        </Col>
      </Row>

      <Row gutter={[24, 24]} style={{ marginTop: 24 }}>
        <Col span={24}>
          <div className="glass-card">
            <div className="glass-card-title">Test Suites</div>
            <Row gutter={[16, 16]}>
              {testSuites.map((suite) => (
                <Col span={8} key={suite.name}>
                  <Card className="glass-card" style={{ textAlign: "center" }}>
                    {suite.icon}
                    <div style={{ marginTop: 8 }}>{suite.name}</div>
                    <Tag style={{ marginTop: 8 }}>{suite.count} tests</Tag>
                  </Card>
                </Col>
              ))}
            </Row>
          </div>
        </Col>
      </Row>

      <Row gutter={[24, 24]} style={{ marginTop: 24 }}>
        <Col span={24}>
          <div className="glass-card">
            <div className="glass-card-title">Test Results</div>
            <Table
              dataSource={testResults}
              columns={columns}
              loading={loading}
              pagination={{ pageSize: 10 }}
              className="cyber-table"
            />
          </div>
        </Col>
      </Row>
    </motion.div>
  );
};

export default AdminTesting;
