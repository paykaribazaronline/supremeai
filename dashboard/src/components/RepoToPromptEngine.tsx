import {
  DownloadOutlined,
  CopyOutlined,
  NodeIndexOutlined,
  RocketOutlined,
  AimOutlined,
} from "@ant-design/icons";
import {
  Card,
  Button,
  Input,
  Select,
  Row,
  Col,
  Alert,
  Typography,
  Space,
  Tag,
  Progress,
  Tabs,
  Divider,
  message,
  Statistic,
} from "antd";
import React, { useState } from "react";

import { parseWithTreeSitter, parseWithRegex } from "../services/code-parser";
import promptGenerator from "../services/prompt-generator";
import RepoIngestor from "../services/repo-ingestor";
import {
  filterFiles,
  detectLanguage,
  getSmartFilter,
  prioritizeFiles,
  estimateComplexity,
} from "../utils/repo-filter";

import CodeFlowWidget from "./CodeFlowWidget";
import "./RepoToPromptEngine.css";

const { TextArea } = Input;
const { Title, Text } = Typography;
const { TabPane } = Tabs;

interface RepoToPromptEngineProps {
  onClose?: () => void;
  defaultRepo?: string;
}

interface AnalysisState {
  metadata: any;
  fileTree: any[];
  filteredFiles: any[];
  parsedFiles: any[];
  analysis: any;
  prompts: any;
  isAnalyzing: boolean;
  error: string | null;
}

const RepoToPromptEngine: React.FC<RepoToPromptEngineProps> = ({
  defaultRepo = "",
}) => {
  const [repoUrl, setRepoUrl] = useState(defaultRepo);
  const [githubToken, setGithubToken] = useState("");
  const [customFocus, setCustomFocus] = useState("");
  const [language, setLanguage] = useState("auto");
  const [analysisState, setAnalysisState] = useState<AnalysisState>({
    metadata: null,
    fileTree: [],
    filteredFiles: [],
    parsedFiles: [],
    analysis: {},
    prompts: {},
    isAnalyzing: false,
    error: null,
  });
  const [showPreview, setShowPreview] = useState(false);
  const [activeTab, setActiveTab] = useState("1");
  const [ingestor] = useState(new RepoIngestor());

  /**
   * Parse repository URL
   */
  const parseRepoUrl = (url: string) => {
    const patterns = [
      /github\.com[/:]([^/]+)\/([^/.]+)(?:\.git)?/,
      /gitlab\.com[/:]([^/]+)\/([^/.]+)(?:\.git)?/,
      /bitbucket\.org[/:]([^/]+)\/([^/.]+)(?:\.git)?/,
    ];

    for (const pattern of patterns) {
      const match = url.match(pattern);
      if (match) {
        return { owner: match[1], repo: match[2] };
      }
    }

    return null;
  };

  /**
   * Run complete analysis pipeline
   */
  const runAnalysis = async () => {
    if (!repoUrl.trim()) {
      message.error("Please enter a repository URL");
      return;
    }

    const repoInfo = parseRepoUrl(repoUrl);
    if (!repoInfo) {
      message.error(
        "Invalid repository URL. Please use GitHub, GitLab, or Bitbucket URL.",
      );
      return;
    }

    setAnalysisState((prev) => ({ ...prev, isAnalyzing: true, error: null }));
    message.loading("Analyzing repository...", 0);

    try {
      // Step 1: Fetch metadata
      message.info("Fetching repository metadata...", 2);
      const metadata = await ingestor.fetchRepoMetadata(
        repoInfo.owner,
        repoInfo.repo,
        githubToken || undefined,
      );

      // Step 2: Fetch file tree
      message.info("Fetching file structure...", 2);
      const fileTree = await ingestor.fetchFileTree(
        repoInfo.owner,
        repoInfo.repo,
        "",
        3,
        githubToken || undefined,
      );

      // Step 3: Detect language and apply filters
      const detectedLang =
        language === "auto" ? detectLanguage(fileTree) : language;
      const smartFilter = getSmartFilter(detectedLang);

      const filteredFiles = filterFiles(fileTree, smartFilter, customFocus);
      const prioritizedFiles = prioritizeFiles(filteredFiles);

      // Limit files for analysis
      const filesToAnalyze = prioritizedFiles.slice(0, 30);

      // Step 4: Parse files
      message.info("Parsing code structure...", 2);
      const parsedFiles = [];

      for (const file of filesToAnalyze) {
        try {
          const fileContent = (await ingestor.fetchFileContent(
            repoInfo.owner,
            repoInfo.repo,
            file.path,
            githubToken || undefined,
          )) as { content: string };

          let parseResult;
          if (detectedLang === "javascript" || detectedLang === "typescript") {
            // Try Tree-sitter first
            try {
              parseResult = await parseWithTreeSitter(
                fileContent.content,
                detectedLang,
              );
            } catch (e) {
              // Fallback to regex
              parseResult = parseWithRegex(fileContent.content, detectedLang);
            }
          } else {
            // Use regex for other languages
            parseResult = parseWithRegex(fileContent.content, detectedLang);
          }

          parsedFiles.push({
            ...file,
            ...parseResult,
            language: detectedLang,
          });
        } catch (error) {
          console.warn(`Failed to parse ${file.path}:`, error);
        }
      }

      // Step 5: Generate analysis
      message.info("Generating analysis...", 2);
      const analysis = generateAnalysis(parsedFiles, metadata);

      // Step 6: Generate prompts
      message.info("Generating AI prompts...", 2);
      const prompts = promptGenerator.generate(
        metadata,
        fileTree,
        parsedFiles,
        analysis,
      );

      setAnalysisState({
        metadata,
        fileTree,
        filteredFiles: prioritizedFiles,
        parsedFiles,
        analysis,
        prompts,
        isAnalyzing: false,
        error: null,
      });

      message.destroy();
      message.success("Analysis complete!", 3);
      setActiveTab("1");
      setShowPreview(true);
    } catch (error) {
      message.destroy();
      const errorMsg =
        error instanceof Error ? error.message : "Analysis failed";
      setAnalysisState((prev) => ({
        ...prev,
        isAnalyzing: false,
        error: errorMsg,
      }));
      message.error(errorMsg, 5);
    }
  };

  /**
   * Generate analysis summary
   */
  const generateAnalysis = (parsedFiles: any[], metadata: any) => {
    const totalFunctions = parsedFiles.reduce(
      (sum, f) => sum + (f.functions?.length || 0),
      0,
    );
    const totalClasses = parsedFiles.reduce(
      (sum, f) => sum + (f.classes?.length || 0),
      0,
    );
    const totalImports = parsedFiles.reduce(
      (sum, f) => sum + (f.imports?.length || 0),
      0,
    );

    const complexity = estimateComplexity(parsedFiles);
    const patterns = detectPatterns(parsedFiles);
    const securityIssues = scanSecurity(parsedFiles);
    const deadCode = detectDeadCode(parsedFiles);
    const circularDeps = detectCircularDependencies(parsedFiles);

    // Calculate health score
    const healthScore = calculateHealthScore({
      patterns,
      securityIssues,
      deadCode,
      circularDeps,
      complexity,
    });

    const healthGrade = getHealthGrade(healthScore);

    return {
      totalFunctions,
      totalClasses,
      totalImports,
      complexity,
      patterns,
      securityIssues,
      deadCode,
      circularDeps,
      healthScore,
      healthGrade,
      language: metadata.language,
    };
  };

  /**
   * Detect design patterns
   */
  const detectPatterns = (files: any[]) => {
    const patterns: {
      patternType: string;
      description: string;
      file: string;
      line: number;
      confidence: number;
    }[] = [];

    files.forEach((file) => {
      file.classes?.forEach((cls: any) => {
        // Singleton pattern
        if (
          cls.name.toLowerCase().includes("singleton") ||
          (Array.isArray(file.functions) &&
            file.functions.some((f: any) => f.name === cls.name))
        ) {
          patterns.push({
            patternType: "SINGLETON",
            description: "Singleton pattern detected",
            file: file.path,
            line: cls.line,
            confidence: 80,
          });
        }

        // Factory pattern
        if (
          cls.name.toLowerCase().includes("factory") ||
          (Array.isArray(cls.methods) &&
            cls.methods.some((m: any) => m.name.includes("create")))
        ) {
          patterns.push({
            patternType: "FACTORY",
            description: "Factory pattern detected",
            file: file.path,
            line: cls.line,
            confidence: 75,
          });
        }

        // Repository pattern
        if (
          cls.name.toLowerCase().includes("repository") ||
          cls.name.toLowerCase().includes("dao")
        ) {
          patterns.push({
            patternType: "REPOSITORY",
            description: "Repository pattern detected",
            file: file.path,
            line: cls.line,
            confidence: 85,
          });
        }
      });

      // React hooks
      if (file.language === "javascript" || file.language === "typescript") {
        const content = file.content || "";
        if (content.includes("useState") || content.includes("useEffect")) {
          patterns.push({
            patternType: "REACT_HOOKS",
            description: "React hooks usage detected",
            file: file.path,
            line: 1,
            confidence: 90,
          });
        }
      }
    });

    return patterns;
  };

  /**
   * Scan for security issues
   */
  const scanSecurity = (files: any[]) => {
    const issues: {
      type: string;
      severity: string;
      description: string;
      file: string;
      line: number;
      remediation: string;
    }[] = [];

    files.forEach((file) => {
      const content = file.content || "";

      // Hardcoded secrets
      const secretPatterns = [
        /password\s*=\s*['"][^'"]{8,}['"]/gi,
        /api[_-]?key\s*=\s*['"][^'"]{20,}['"]/gi,
        /secret\s*=\s*['"][^'"]{10,}['"]/gi,
      ];

      secretPatterns.forEach((pattern) => {
        if (pattern.test(content)) {
          issues.push({
            type: "HARDCODED_SECRET",
            severity: "HIGH",
            description: "Potential hardcoded secret detected",
            file: file.path,
            line: 1,
            remediation: "Use environment variables or secure vault",
          });
        }
      });

      // eval usage
      if (content.includes("eval(")) {
        issues.push({
          type: "CODE_INJECTION",
          severity: "CRITICAL",
          description: "Use of eval() detected",
          file: file.path,
          line: 1,
          remediation: "Avoid eval(), use safer alternatives",
        });
      }
    });

    return issues;
  };

  /**
   * Detect dead code
   */
  const detectDeadCode = (files: any[]) => {
    const deadCode: any[] = [];

    files.forEach((file) => {
      // Check for unused imports
      file.imports?.forEach((imp: any) => {
        if (!imp.isUsed) {
          deadCode.push({
            type: "UNUSED_IMPORT",
            file: file.path,
            line: imp.line,
            name: imp.module,
          });
        }
      });
    });

    return deadCode;
  };

  /**
   * Detect circular dependencies
   */
  const detectCircularDependencies = (files: any[]) => {
    // Simplified detection
    const circularDeps: any[] = [];

    files.forEach((file) => {
      file.imports?.forEach((imp: any) => {
        const importedFile = files.find((f) => f.path === imp.module);
        if (importedFile) {
          const hasReverseImport =
            Array.isArray(importedFile.imports) &&
            importedFile.imports.some(
              (revImp: any) => revImp.module === file.path,
            );

          if (hasReverseImport) {
            circularDeps.push({
              files: [file.path, importedFile.path],
              description: "Circular dependency detected",
              severity: 5,
              suggestion: "Refactor to remove circular dependency",
            });
          }
        }
      });
    });

    return circularDeps;
  };

  /**
   * Calculate health score
   */
  const calculateHealthScore = (metrics: any) => {
    let score = 100;

    // Deduct for security issues
    metrics.securityIssues?.forEach((issue: any) => {
      if (issue.severity === "CRITICAL") score -= 15;
      else if (issue.severity === "HIGH") score -= 8;
      else if (issue.severity === "MEDIUM") score -= 4;
    });

    // Deduct for dead code
    score -= metrics.deadCode?.length * 2 || 0;

    // Deduct for circular dependencies
    score -= metrics.circularDeps?.length * 5 || 0;

    // Deduct for complexity
    if (metrics.complexity.complexity === "high") score -= 10;
    else if (metrics.complexity.complexity === "medium") score -= 5;

    return Math.max(0, Math.min(100, score));
  };

  /**
   * Get health grade
   */
  const getHealthGrade = (score: number) => {
    if (score >= 90) return "A";
    if (score >= 80) return "B";
    if (score >= 70) return "C";
    if (score >= 60) return "D";
    return "F";
  };

  /**
   * Copy prompt to clipboard
   */
  const copyPrompt = async (prompt: string, type: string) => {
    try {
      await navigator.clipboard.writeText(prompt);
      message.success(`${type} prompt copied to clipboard`);
    } catch (error) {
      message.error("Failed to copy to clipboard");
    }
  };

  /**
   * Export analysis
   */
  const exportAnalysis = () => {
    const data = {
      metadata: analysisState.metadata,
      analysis: analysisState.analysis,
      prompts: analysisState.prompts,
      timestamp: new Date().toISOString(),
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `codeflow-analysis-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="repo-to-prompt-engine">
      <Card
        style={{
          background: "#16161a",
          border: "1px solid #262626",
          marginBottom: 24,
        }}
        bodyStyle={{ padding: 24 }}
      >
        <Title level={3} style={{ color: "#f0f0f2", marginBottom: 24 }}>
          <RocketOutlined style={{ color: "#00ff9d", marginRight: 12 }} />
          Repo-to-Prompt Engine
        </Title>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={16}>
            <Input
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
              placeholder="https://github.com/owner/repository"
              size="large"
              style={{
                background: "#0a0a0c",
                border: "1px solid #262626",
                color: "#f0f0f2",
                fontSize: 16,
              }}
              prefix={<NodeIndexOutlined style={{ color: "#666" }} />}
            />
          </Col>
          <Col xs={24} lg={8}>
            <Select
              value={language}
              onChange={setLanguage}
              style={{ width: "100%" }}
              size="large"
              options={[
                { value: "auto", label: "Auto Detect" },
                { value: "javascript", label: "JavaScript/TypeScript" },
                { value: "python", label: "Python" },
                { value: "go", label: "Go" },
                { value: "rust", label: "Rust" },
                { value: "java", label: "Java" },
                { value: "ruby", label: "Ruby" },
                { value: "cpp", label: "C++" },
              ]}
            />
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} sm={12} md={8}>
            <Input
              value={customFocus}
              onChange={(e) => setCustomFocus(e.target.value)}
              placeholder="Focus path (optional, e.g., src/api/)"
              style={{
                background: "#0a0a0c",
                border: "1px solid #262626",
                color: "#f0f0f2",
              }}
            />
          </Col>
          <Col xs={24} sm={12} md={8}>
            <Input.Password
              value={githubToken}
              onChange={(e) => setGithubToken(e.target.value)}
              placeholder="GitHub Token (optional)"
              style={{
                background: "#0a0a0c",
                border: "1px solid #262626",
                color: "#f0f0f2",
              }}
            />
          </Col>
          <Col xs={24} md={8}>
            <Button
              type="primary"
              size="large"
              loading={analysisState.isAnalyzing}
              onClick={runAnalysis}
              disabled={!repoUrl.trim()}
              style={{
                width: "100%",
                height: 48,
                background: "#00ff9d",
                borderColor: "#00ff9d",
                color: "#0a0a0c",
                fontWeight: 600,
                fontSize: 16,
              }}
            >
              {analysisState.isAnalyzing
                ? "Analyzing..."
                : "Analyze Repository"}
            </Button>
          </Col>
        </Row>

        {analysisState.error && (
          <Alert
            message="Analysis Failed"
            description={analysisState.error}
            type="error"
            showIcon
            style={{ marginTop: 16 }}
            onClose={() =>
              setAnalysisState((prev) => ({ ...prev, error: null }))
            }
          />
        )}
      </Card>

      {showPreview && analysisState.metadata && (
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={8}>
            <CodeFlowWidget
              repositoryId={analysisState.metadata.full_name}
              repositoryName={analysisState.metadata.full_name}
              compact={false}
              autoRefresh={false}
            />
          </Col>
          <Col xs={24} lg={16}>
            <Card
              style={{
                background: "#16161a",
                border: "1px solid #262626",
              }}
              title={
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <AimOutlined style={{ color: "#00ff9d" }} />
                  <Text style={{ color: "#f0f0f2" }}>Analysis Results</Text>
                  <Space>
                    <Button
                      icon={<DownloadOutlined />}
                      onClick={exportAnalysis}
                      style={{ color: "#00ff9d" }}
                    >
                      Export
                    </Button>
                    <Button
                      icon={<CopyOutlined />}
                      onClick={() =>
                        copyPrompt(
                          JSON.stringify(analysisState.prompts, null, 2),
                          "All prompts",
                        )
                      }
                      style={{ color: "#00ff9d" }}
                    >
                      Copy All
                    </Button>
                  </Space>
                </div>
              }
            >
              <Tabs activeKey={activeTab} onChange={setActiveTab}>
                <TabPane tab="Overview" key="1">
                  <Row gutter={[16, 16]}>
                    <Col span={8}>
                      <Statistic
                        title="Health Score"
                        value={analysisState.analysis.healthScore || 0}
                        valueStyle={{
                          color:
                            getHealthGrade(
                              analysisState.analysis.healthScore || 0,
                            ) === "A"
                              ? "#52c41a"
                              : getHealthGrade(
                                    analysisState.analysis.healthScore || 0,
                                  ) === "B"
                                ? "#1890ff"
                                : getHealthGrade(
                                      analysisState.analysis.healthScore || 0,
                                    ) === "C"
                                  ? "#faad14"
                                  : getHealthGrade(
                                        analysisState.analysis.healthScore || 0,
                                      ) === "D"
                                    ? "#ff7875"
                                    : "#ff4d4f",
                        }}
                        suffix={`/100 (${analysisState.analysis.healthGrade})`}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic
                        title="Files"
                        value={analysisState.filteredFiles.length}
                        valueStyle={{ color: "#f0f0f2" }}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic
                        title="Functions"
                        value={analysisState.analysis.totalFunctions || 0}
                        valueStyle={{ color: "#f0f0f2" }}
                      />
                    </Col>
                  </Row>

                  <Divider style={{ borderColor: "#262626" }} />

                  <div style={{ marginBottom: 16 }}>
                    <Text
                      style={{
                        color: "#a3a3a3",
                        display: "block",
                        marginBottom: 8,
                      }}
                    >
                      Complexity
                    </Text>
                    <Progress
                      percent={
                        analysisState.analysis.complexity?.estimatedTime
                          ? analysisState.analysis.complexity.estimatedTime.includes(
                              "high",
                            )
                            ? 75
                            : analysisState.analysis.complexity.estimatedTime.includes(
                                  "medium",
                                )
                              ? 45
                              : 20
                          : 0
                      }
                      strokeColor={
                        analysisState.analysis.complexity?.estimatedTime?.includes(
                          "high",
                        )
                          ? "#ff4d4f"
                          : analysisState.analysis.complexity?.estimatedTime?.includes(
                                "medium",
                              )
                            ? "#faad14"
                            : "#52c41a"
                      }
                      trailColor="#262626"
                    />
                    <Text style={{ color: "#666", fontSize: 12 }}>
                      {analysisState.analysis.complexity?.estimatedTime ||
                        "Unknown"}
                    </Text>
                  </div>

                  {analysisState.analysis.securityIssues?.length > 0 && (
                    <div style={{ marginBottom: 16 }}>
                      <Text
                        style={{
                          color: "#a3a3a3",
                          display: "block",
                          marginBottom: 8,
                        }}
                      >
                        Security Issues
                      </Text>
                      <Space wrap>
                        {analysisState.analysis.securityIssues.map(
                          (issue: any, i: number) => (
                            <Tag
                              key={i}
                              color={
                                issue.severity === "CRITICAL"
                                  ? "#ff4d4f"
                                  : issue.severity === "HIGH"
                                    ? "#ff7875"
                                    : issue.severity === "MEDIUM"
                                      ? "#faad14"
                                      : "#52c41a"
                              }
                            >
                              {issue.type}
                            </Tag>
                          ),
                        )}
                      </Space>
                    </div>
                  )}

                  {analysisState.analysis.patterns?.length > 0 && (
                    <div>
                      <Text
                        style={{
                          color: "#a3a3a3",
                          display: "block",
                          marginBottom: 8,
                        }}
                      >
                        Design Patterns
                      </Text>
                      <Space wrap>
                        {analysisState.analysis.patterns.map(
                          (pattern: any, i: number) => (
                            <Tag key={i} color="blue">
                              {pattern.patternType}
                            </Tag>
                          ),
                        )}
                      </Space>
                    </div>
                  )}
                </TabPane>

                <TabPane tab="Build Prompt" key="2">
                  <div style={{ marginBottom: 16 }}>
                    <Button
                      icon={<CopyOutlined />}
                      onClick={() =>
                        copyPrompt(analysisState.prompts.build || "", "Build")
                      }
                      style={{ marginBottom: 12 }}
                    >
                      Copy Prompt
                    </Button>
                    <TextArea
                      value={analysisState.prompts.build || ""}
                      readOnly
                      rows={20}
                      style={{
                        background: "#0a0a0c",
                        border: "1px solid #262626",
                        color: "#f0f0f2",
                        fontFamily: "monospace",
                        fontSize: 12,
                      }}
                    />
                  </div>
                </TabPane>

                <TabPane tab="Analyze Prompt" key="3">
                  <div style={{ marginBottom: 16 }}>
                    <Button
                      icon={<CopyOutlined />}
                      onClick={() =>
                        copyPrompt(
                          analysisState.prompts.analyze || "",
                          "Analyze",
                        )
                      }
                      style={{ marginBottom: 12 }}
                    >
                      Copy Prompt
                    </Button>
                    <TextArea
                      value={analysisState.prompts.analyze || ""}
                      readOnly
                      rows={20}
                      style={{
                        background: "#0a0a0c",
                        border: "1px solid #262626",
                        color: "#f0f0f2",
                        fontFamily: "monospace",
                        fontSize: 12,
                      }}
                    />
                  </div>
                </TabPane>

                <TabPane tab="Ask Prompt" key="4">
                  <div style={{ marginBottom: 16 }}>
                    <Button
                      icon={<CopyOutlined />}
                      onClick={() =>
                        copyPrompt(analysisState.prompts.ask || "", "Ask")
                      }
                      style={{ marginBottom: 12 }}
                    >
                      Copy Prompt
                    </Button>
                    <TextArea
                      value={analysisState.prompts.ask || ""}
                      readOnly
                      rows={20}
                      style={{
                        background: "#0a0a0c",
                        border: "1px solid #262626",
                        color: "#f0f0f2",
                        fontFamily: "monospace",
                        fontSize: 12,
                      }}
                    />
                  </div>
                </TabPane>
              </Tabs>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
};

export default RepoToPromptEngine;
