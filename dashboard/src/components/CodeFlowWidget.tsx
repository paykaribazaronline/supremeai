import {
  EyeOutlined,
  BugOutlined,
  SecurityScanOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  NodeIndexOutlined,
  BranchesOutlined,
  FileTextOutlined,
  CodeOutlined,
  RocketOutlined,
  ReloadOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import {
  Card,
  Statistic,
  Progress,
  Badge,
  Tooltip,
  Button,
  Spin,
  Alert,
  List,
  Tag,
  Typography,
  Row,
  Col,
  Divider,
} from "antd";
import React, { useState, useEffect } from "react";
import "./CodeFlowWidget.css";

const { Text, Title } = Typography;

interface CodeFlowWidgetProps {
  repositoryId: string;
  repositoryName: string;
  onViewDetails?: () => void;
  compact?: boolean;
  autoRefresh?: boolean;
  refreshInterval?: number;
}

interface HealthData {
  score: number;
  grade: string;
  issues: any[];
}

interface SecurityData {
  issues: any[];
}

interface AnalysisData {
  id: string;
  name: string;
  analysisStatus: string;
  healthScore?: number;
  healthGrade?: string;
  totalFiles?: number;
  totalLinesOfCode?: number;
  securityIssues?: any[];
  detectedPatterns?: any[];
  circularDependencies?: any[];
  deadCode?: any[];
  aiSuggestions?: any[];
}

const CodeFlowWidget: React.FC<CodeFlowWidgetProps> = ({
  repositoryId,
  repositoryName,
  onViewDetails,
  compact = false,
  autoRefresh = true,
  refreshInterval = 30000,
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<AnalysisData | null>(null);
  const [healthData, setHealthData] = useState<HealthData | null>(null);
  const [securityData, setSecurityData] = useState<SecurityData | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  // Fetch analysis data
  const fetchAnalysis = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem("supremeai_token");

      const response = await fetch(`/api/codeflow/analysis/${repositoryId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error("Failed to fetch analysis");
      }

      const data = await response.json();
      if (data.success) {
        setAnalysis(data.data);
        setLastUpdated(new Date());
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch analysis");
    } finally {
      setLoading(false);
    }
  };

  // Fetch health score
  const fetchHealth = async () => {
    try {
      const token = localStorage.getItem("supremeai_token");

      const response = await fetch(`/api/codeflow/health/${repositoryId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setHealthData({
            score: data.score,
            grade: data.grade,
            issues: data.issues || [],
          });
        }
      }
    } catch (err) {
      // Silently fail - health data is optional
    }
  };

  // Fetch security issues
  const fetchSecurity = async () => {
    try {
      const token = localStorage.getItem("supremeai_token");

      const response = await fetch(`/api/codeflow/security/${repositoryId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setSecurityData({
            issues: data.issues || [],
          });
        }
      }
    } catch (err) {
      // Silently fail - security data is optional
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchAnalysis();
    fetchHealth();
    fetchSecurity();
  }, [repositoryId]);

  // Auto-refresh
  useEffect(() => {
    if (!autoRefresh) return;

    const interval = setInterval(() => {
      fetchAnalysis();
      fetchHealth();
      fetchSecurity();
    }, refreshInterval);

    return () => clearInterval(interval);
  }, [autoRefresh, refreshInterval, repositoryId]);

  // Get severity color
  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case "CRITICAL":
        return "#ff4d4f";
      case "HIGH":
        return "#ff7875";
      case "MEDIUM":
        return "#faad14";
      case "LOW":
        return "#52c41a";
      default:
        return "#d9d9d9";
    }
  };

  // Get grade color
  const getGradeColor = (grade: string) => {
    switch (grade) {
      case "A":
        return "#52c41a";
      case "B":
        return "#1890ff";
      case "C":
        return "#faad14";
      case "D":
        return "#ff7875";
      case "F":
        return "#ff4d4f";
      default:
        return "#d9d9d9";
    }
  };

  // Render loading state
  if (loading && !analysis) {
    return (
      <Card
        className="codeflow-widget"
        style={{ background: "#16161a", border: "1px solid #262626" }}
      >
        <div style={{ textAlign: "center", padding: compact ? 20 : 40 }}>
          <Spin size="large" />
          <div style={{ marginTop: 16, color: "#a3a3a3" }}>
            Analyzing {repositoryName}...
          </div>
        </div>
      </Card>
    );
  }

  // Render error state
  if (error && !analysis) {
    return (
      <Card
        className="codeflow-widget"
        style={{ background: "#16161a", border: "1px solid #262626" }}
      >
        <Alert
          message="Error"
          description={error}
          type="error"
          showIcon
          action={
            <Button size="small" danger onClick={fetchAnalysis}>
              Retry
            </Button>
          }
        />
      </Card>
    );
  }

  if (!analysis) {
    return null;
  }

  // Compact view
  if (compact) {
    return (
      <Card
        className="codeflow-widget codeflow-widget-compact"
        style={{
          background: "#16161a",
          border: "1px solid #262626",
          cursor: onViewDetails ? "pointer" : "default",
        }}
        onClick={onViewDetails}
        bodyStyle={{ padding: 12 }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              flex: 1,
              minWidth: 0,
            }}
          >
            <NodeIndexOutlined style={{ color: "#00ff9d", fontSize: 16 }} />
            <Text
              style={{ color: "#f0f0f2", fontSize: 14, fontWeight: 500 }}
              ellipsis
            >
              {repositoryName}
            </Text>
          </div>

          {analysis.healthScore !== undefined && (
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <div
                style={{
                  width: 20,
                  height: 20,
                  borderRadius: "50%",
                  background: getGradeColor(analysis.healthGrade || "F"),
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#fff",
                  fontSize: 10,
                  fontWeight: "bold",
                }}
              >
                {analysis.healthGrade}
              </div>
              <Text style={{ color: "#f0f0f2", fontSize: 14, fontWeight: 600 }}>
                {analysis.healthScore}
              </Text>
            </div>
          )}

          {analysis.securityIssues && analysis.securityIssues.length > 0 && (
            <Badge
              count={analysis.securityIssues.length}
              style={{
                backgroundColor: "#ff4d4f",
                boxShadow: "0 0 0 1px #16161a",
              }}
            />
          )}

          <Badge
            status={
              analysis.analysisStatus === "COMPLETED"
                ? "success"
                : analysis.analysisStatus === "ANALYZING"
                  ? "processing"
                  : "default"
            }
          />
        </div>
      </Card>
    );
  }

  // Full view
  return (
    <Card
      className="codeflow-widget codeflow-widget-full"
      style={{
        background: "#16161a",
        border: "1px solid #262626",
      }}
      title={
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <NodeIndexOutlined style={{ color: "#00ff9d", fontSize: 20 }} />
          <Title level={4} style={{ color: "#f0f0f2", margin: 0 }}>
            {repositoryName}
          </Title>
          {onViewDetails && (
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={onViewDetails}
              style={{ color: "#00ff9d" }}
            >
              View Details
            </Button>
          )}
        </div>
      }
      extra={
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            fetchAnalysis();
            fetchHealth();
            fetchSecurity();
          }}
          style={{ color: "#a3a3a3" }}
        >
          Refresh
        </Button>
      }
    >
      {error && (
        <Alert
          message="Error"
          description={error}
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          onClose={() => setError(null)}
        />
      )}

      <Row gutter={[16, 16]}>
        {/* Health Score */}
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              background: "#0a0a0c",
              border: "1px solid #262626",
              height: "100%",
            }}
          >
            <Statistic
              title="Health Score"
              value={healthData?.score || analysis.healthScore || 0}
              valueStyle={{
                color: getGradeColor(
                  healthData?.grade || analysis.healthGrade || "F",
                ),
                fontSize: 24,
              }}
              suffix={`/100 (${healthData?.grade || analysis.healthGrade || "?"})`}
            />
            {healthData?.score !== undefined && (
              <Progress
                percent={healthData.score}
                strokeColor={getGradeColor(healthData.grade)}
                trailColor="#262626"
                size="small"
                style={{ marginTop: 8 }}
              />
            )}
          </Card>
        </Col>

        {/* Security Status */}
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              background: "#0a0a0c",
              border: "1px solid #262626",
              height: "100%",
            }}
          >
            <div style={{ marginBottom: 8 }}>
              <SecurityScanOutlined
                style={{ color: "#ff7875", marginRight: 8 }}
              />
              <Text style={{ color: "#a3a3a3" }}>Security Issues</Text>
            </div>
            {securityData?.issues && securityData.issues.length > 0 ? (
              <div>
                <div
                  style={{
                    display: "flex",
                    gap: 4,
                    flexWrap: "wrap",
                    marginBottom: 8,
                  }}
                >
                  {["CRITICAL", "HIGH", "MEDIUM", "LOW"].map((severity) => {
                    const count = securityData.issues.filter(
                      (i: any) => i.severity === severity,
                    ).length;
                    if (count === 0) return null;
                    return (
                      <Badge
                        key={severity}
                        count={count}
                        style={{
                          backgroundColor: getSeverityColor(severity),
                          boxShadow: "0 0 0 1px #0a0a0c",
                        }}
                      />
                    );
                  })}
                </div>
                <Text style={{ color: "#ff7875", fontSize: 12 }}>
                  {securityData.issues.length} total issues
                </Text>
              </div>
            ) : (
              <div style={{ textAlign: "center", padding: "8px 0" }}>
                <CheckCircleOutlined
                  style={{ color: "#52c41a", fontSize: 16 }}
                />
                <div style={{ color: "#52c41a", fontSize: 12, marginTop: 4 }}>
                  No issues
                </div>
              </div>
            )}
          </Card>
        </Col>

        {/* File Stats */}
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              background: "#0a0a0c",
              border: "1px solid #262626",
              height: "100%",
            }}
          >
            <div style={{ marginBottom: 8 }}>
              <FileTextOutlined style={{ color: "#1890ff", marginRight: 8 }} />
              <Text style={{ color: "#a3a3a3" }}>Code Stats</Text>
            </div>
            <Row gutter={8}>
              <Col span={12}>
                <div style={{ textAlign: "center" }}>
                  <div
                    style={{ color: "#f0f0f2", fontSize: 18, fontWeight: 600 }}
                  >
                    {analysis.totalFiles || 0}
                  </div>
                  <div style={{ color: "#666", fontSize: 10 }}>Files</div>
                </div>
              </Col>
              <Col span={12}>
                <div style={{ textAlign: "center" }}>
                  <div
                    style={{ color: "#f0f0f2", fontSize: 18, fontWeight: 600 }}
                  >
                    {(analysis.totalLinesOfCode || 0).toLocaleString()}
                  </div>
                  <div style={{ color: "#666", fontSize: 10 }}>Lines</div>
                </div>
              </Col>
            </Row>
          </Card>
        </Col>

        {/* Analysis Status */}
        <Col xs={24} sm={12} md={6}>
          <Card
            size="small"
            style={{
              background: "#0a0a0c",
              border: "1px solid #262626",
              height: "100%",
            }}
          >
            <div style={{ marginBottom: 8 }}>
              <ThunderboltOutlined
                style={{ color: "#00ff9d", marginRight: 8 }}
              />
              <Text style={{ color: "#a3a3a3" }}>Analysis Status</Text>
            </div>
            <div style={{ textAlign: "center", padding: "8px 0" }}>
              <Badge
                status={
                  analysis.analysisStatus === "COMPLETED"
                    ? "success"
                    : analysis.analysisStatus === "ANALYZING"
                      ? "processing"
                      : "default"
                }
                style={{ fontSize: 12 }}
              />
              <div
                style={{
                  color: "#f0f0f2",
                  fontSize: 14,
                  fontWeight: 600,
                  marginTop: 4,
                  textTransform: "capitalize",
                }}
              >
                {analysis.analysisStatus}
              </div>
              {lastUpdated && (
                <div style={{ color: "#666", fontSize: 10, marginTop: 4 }}>
                  Updated {lastUpdated.toLocaleTimeString()}
                </div>
              )}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Quick Stats Row */}
      {((analysis.detectedPatterns?.length || 0) > 0 ||
        (analysis.circularDependencies?.length || 0) > 0 ||
        (analysis.deadCode?.length || 0) > 0) && (
        <Row gutter={16} style={{ marginTop: 16 }}>
          {analysis.detectedPatterns &&
            analysis.detectedPatterns.length > 0 && (
              <Col xs={24} sm={8}>
                <Card
                  size="small"
                  style={{ background: "#0a0a0c", border: "1px solid #262626" }}
                >
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 8 }}
                  >
                    <CodeOutlined style={{ color: "#1890ff" }} />
                    <Text style={{ color: "#a3a3a3" }}>Patterns</Text>
                    <Badge
                      count={analysis.detectedPatterns.length}
                      style={{ marginLeft: "auto" }}
                    />
                  </div>
                  <div
                    style={{
                      marginTop: 8,
                      display: "flex",
                      flexWrap: "wrap",
                      gap: 4,
                    }}
                  >
                    {analysis.detectedPatterns
                      .slice(0, 3)
                      .map((p: any, i: number) => (
                        <Tag key={i} color="blue" style={{ fontSize: 10 }}>
                          {p.patternType}
                        </Tag>
                      ))}
                    {analysis.detectedPatterns.length > 3 && (
                      <Tag style={{ fontSize: 10 }}>
                        +{analysis.detectedPatterns.length - 3} more
                      </Tag>
                    )}
                  </div>
                </Card>
              </Col>
            )}

          {analysis.circularDependencies &&
            analysis.circularDependencies.length > 0 && (
              <Col xs={24} sm={8}>
                <Card
                  size="small"
                  style={{ background: "#0a0a0c", border: "1px solid #262626" }}
                >
                  <div
                    style={{ display: "flex", alignItems: "center", gap: 8 }}
                  >
                    <BranchesOutlined style={{ color: "#ff7875" }} />
                    <Text style={{ color: "#a3a3a3" }}>Circular Deps</Text>
                    <Badge
                      count={analysis.circularDependencies.length}
                      style={{ marginLeft: "auto", background: "#ff7875" }}
                    />
                  </div>
                  <div style={{ marginTop: 8 }}>
                    <Text style={{ color: "#ff7875", fontSize: 11 }}>
                      {analysis.circularDependencies[0].files.length} files
                      involved
                    </Text>
                  </div>
                </Card>
              </Col>
            )}

          {analysis.deadCode && analysis.deadCode.length > 0 && (
            <Col xs={24} sm={8}>
              <Card
                size="small"
                style={{ background: "#0a0a0c", border: "1px solid #262626" }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <BugOutlined style={{ color: "#faad14" }} />
                  <Text style={{ color: "#a3a3a3" }}>Dead Code</Text>
                  <Badge
                    count={analysis.deadCode.length}
                    style={{ marginLeft: "auto", background: "#faad14" }}
                  />
                </div>
                <div style={{ marginTop: 8 }}>
                  <Text style={{ color: "#faad14", fontSize: 11 }}>
                    {
                      analysis.deadCode.filter(
                        (d: any) => d.type === "UNUSED_FUNCTION",
                      ).length
                    }{" "}
                    unused functions
                  </Text>
                </div>
              </Card>
            </Col>
          )}
        </Row>
      )}

      {/* AI Suggestions Preview */}
      {analysis.aiSuggestions && analysis.aiSuggestions.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <Divider style={{ borderColor: "#262626" }}>
            <Text style={{ color: "#00ff9d" }}>
              <RocketOutlined style={{ marginRight: 8 }} />
              AI Suggestions
            </Text>
          </Divider>
          <List
            size="small"
            dataSource={analysis.aiSuggestions.slice(0, 3)}
            renderItem={(item: any) => (
              <List.Item
                style={{
                  padding: "8px 0",
                  borderBottom: "1px solid #262626",
                }}
              >
                <List.Item.Meta
                  avatar={<RocketOutlined style={{ color: "#00ff9d" }} />}
                  title={
                    <Text style={{ color: "#f0f0f2", fontSize: 12 }}>
                      {item.type}
                    </Text>
                  }
                  description={
                    <Text style={{ color: "#a3a3a3", fontSize: 11 }}>
                      {item.description}
                    </Text>
                  }
                />
                <Badge
                  count={item.confidence}
                  style={{
                    backgroundColor: "#00ff9d",
                    fontSize: 10,
                    height: 16,
                    minWidth: 16,
                    lineHeight: "16px",
                  }}
                />
              </List.Item>
            )}
          />
          {analysis.aiSuggestions.length > 3 && (
            <div style={{ textAlign: "center", marginTop: 8 }}>
              <Text style={{ color: "#666", fontSize: 11 }}>
                +{analysis.aiSuggestions.length - 3} more suggestions
              </Text>
            </div>
          )}
        </div>
      )}

      {/* Health Issues Preview */}
      {healthData?.issues && healthData.issues.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <Divider style={{ borderColor: "#262626" }}>
            <Text style={{ color: "#ff7875" }}>
              <WarningOutlined style={{ marginRight: 8 }} />
              Health Issues
            </Text>
          </Divider>
          <List
            size="small"
            dataSource={healthData.issues.slice(0, 3)}
            renderItem={(item: any) => (
              <List.Item
                style={{
                  padding: "6px 0",
                  borderBottom: "1px solid #262626",
                }}
              >
                <Text style={{ color: "#ff7875", fontSize: 11 }}>
                  {item.type}: {item.description}
                </Text>
              </List.Item>
            )}
          />
        </div>
      )}
    </Card>
  );
};

export default CodeFlowWidget;
