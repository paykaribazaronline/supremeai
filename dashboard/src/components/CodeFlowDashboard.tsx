import React, { useState, useEffect, useCallback } from 'react';
import { Layout, Card, Row, Col, Statistic, Progress, Button, Spin, Alert, Tabs, Badge, Tag, Tooltip, Modal, List, Typography, Space } from 'antd';
import {
  BugOutlined,
  SecurityScanOutlined,
  EyeOutlined,
  DownloadOutlined,
  ReloadOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  NodeIndexOutlined,
  BranchesOutlined,
  FileTextOutlined,
  CodeOutlined,
  RocketOutlined,
  AimOutlined
} from '@ant-design/icons';
// @ts-ignore
import * as d3 from 'd3';
import { useNavigate } from 'react-router-dom';
import { authUtils } from '../lib/authUtils';

const { Header, Content } = Layout;

const { Text, Title, Paragraph } = Typography;

interface CodeFlowDashboardProps {
  onClose?: () => void;
}

interface RepositoryAnalysis {
  id: string;
  name: string;
  fullName: string;
  description?: string;
  analysisStatus: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED' | 'PARTIAL';
  lastAnalyzedAt?: string;
  healthScore?: number;
  healthGrade?: string;
  totalFiles?: number;
  totalLinesOfCode?: number;
  languageStats?: Record<string, number>;
  securityIssues?: SecurityIssue[];
  detectedPatterns?: PatternDetection[];
  circularDependencies?: CircularDependency[];
  deadCode?: DeadCode[];
  dependencyGraph?: DependencyGraph;
  totalFunctions?: number;
  aiSuggestions?: AISuggestion[];
  errorAnalyses?: ErrorAnalysis[];
}

interface SecurityIssue {
  type: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  file: string;
  line: number;
  codeSnippet?: string;
  remediation: string;
  cweId?: string;
  owaspCategory?: string;
}

interface PatternDetection {
  patternType: string;
  description: string;
  file: string;
  line: number;
  confidence: number;
}

interface CircularDependency {
  files: string[];
  description: string;
  severity: number;
  suggestion: string;
}

interface DeadCode {
  type: string;
  file: string;
  line: number;
  name: string;
}

interface DependencyGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
  centralityScores?: Record<string, number>;
  criticalPath?: string[];
  blastRadius?: number;
}

interface GraphNode {
  id: string;
  file: string;
  type: string;
  linesOfCode: number;
  complexity: number;
  fanIn: number;
  fanOut: number;
  centralityScore: number;
}

interface GraphEdge {
  source: string;
  target: string;
  type: string;
  weight: number;
  line: number;
}

interface AISuggestion {
  type: string;
  description: string;
  file: string;
  line: number;
  suggestion: string;
  codeBefore?: string;
  codeAfter?: string;
  provider: string;
  confidence: number;
}

interface ErrorAnalysis {
  errorType: string;
  stackTrace?: string;
  rootCause?: string;
  file: string;
  line: number;
  affectedNodes: string[];
  suggestedFix: string;
  provider: string;
  confidence: number;
}

interface AnalysisRequest {
  repoUrl: string;
  sourceType: string;
  ownerId: string;
}

const CodeFlowDashboard: React.FC<CodeFlowDashboardProps> = ({ onClose }) => {
  const [loading, setLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [repositories, setRepositories] = useState<RepositoryAnalysis[]>([]);
  const [selectedRepo, setSelectedRepo] = useState<RepositoryAnalysis | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [repoUrl, setRepoUrl] = useState('');
  const [sourceType, setSourceType] = useState('GITHUB');
  const [ownerId, setOwnerId] = useState('');
  const [activeTab, setActiveTab] = useState('1');
  const [viewMode, setViewMode] = useState<'list' | 'graph'>('list');
  const [, setSvgRef] = useState<SVGSVGElement | null>(null);
  const [graphModalVisible, setGraphModalVisible] = useState(false);
  const [selectedGraphRepo, setSelectedGraphRepo] = useState<RepositoryAnalysis | null>(null);
  const navigate = useNavigate();

  // Check authentication
  useEffect(() => {
    if (!authUtils.isAuthenticated()) {
      navigate('/login');
    }
  }, [navigate]);

  // Fetch existing analyses
  const fetchAnalyses = useCallback(async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('supremeai_token');
      const response = await fetch('/api/codeflow/owner/current', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch analyses');
      }
      
      const data = await response.json();
      if (data.success) {
        setRepositories(data.data || []);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch analyses');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnalyses();
  }, [fetchAnalyses]);

  // Analyze repository
  const handleAnalyze = async () => {
    if (!repoUrl.trim()) {
      setError('Please enter a repository URL');
      return;
    }

    try {
      setAnalyzing(true);
      setError(null);
      
      const token = localStorage.getItem('supremeai_token');
      const response = await fetch('/api/codeflow/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          repoUrl,
          sourceType,
          ownerId: ownerId || 'current-user',
        } as AnalysisRequest),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Analysis failed');
      }

      setSuccess('Analysis started successfully!');
      setRepoUrl('');
      
      // Poll for completion
      const pollInterval = setInterval(async () => {
        try {
          const statusResponse = await fetch(`/api/codeflow/analysis/${data.repositoryId}`, {
            headers: {
              'Authorization': `Bearer ${token}`,
            },
          });
          
          if (statusResponse.ok) {
            const statusData = await statusResponse.json();
            if (statusData.data?.analysisStatus === 'COMPLETED') {
              clearInterval(pollInterval);
              fetchAnalyses();
              setSuccess('Analysis completed successfully!');
            } else if (statusData.data?.analysisStatus === 'FAILED') {
              clearInterval(pollInterval);
              setError('Analysis failed. Please try again.');
            }
          }
        } catch (err) {
          clearInterval(pollInterval);
        }
      }, 5000);

      setTimeout(() => clearInterval(pollInterval), 300000); // Stop after 5 minutes

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Analysis failed');
    } finally {
      setAnalyzing(false);
    }
  };

  // Re-analyze repository
  const handleReanalyze = async (repositoryId: string) => {
    try {
      setAnalyzing(true);
      const token = localStorage.getItem('supremeai_token');
      const response = await fetch(`/api/codeflow/reanalyze/${repositoryId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Re-analysis failed');
      }

      setSuccess('Re-analysis started!');
      fetchAnalyses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Re-analysis failed');
    } finally {
      setAnalyzing(false);
    }
  };

  // Export analysis
  const handleExport = async (repositoryId: string, format: 'json') => {
    try {
      const token = localStorage.getItem('supremeai_token');
      const response = await fetch(`/api/codeflow/export/${format}/${repositoryId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Export failed');
      }

      const data = await response.json();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `codeflow-analysis-${repositoryId}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
    }
  };

  // Render dependency graph with D3
  const renderDependencyGraph = useCallback((graph: DependencyGraph, containerId: string) => {
    if (!graph || !graph.nodes || !graph.edges) return;

    const container = document.getElementById(containerId);
    if (!container) return;

    const width = container.clientWidth;
    const height = Math.max(400, graph.nodes.length * 60);

    // Clear previous graph
    d3.select(`#${containerId}`).selectAll('*').remove();

    const svg = d3.select(`#${containerId}`)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .attr('viewBox', [0, 0, width, height]);

    // Create force simulation
    const simulation = d3.forceSimulation(graph.nodes as any)
      .force('link', d3.forceLink(graph.edges)
        .id((d: any) => d.id)
        .distance(100))
      .force('charge', d3.forceManyBody().strength(-300))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .force('collision', d3.forceCollide().radius(40));

    // Create links
    const link = svg.append('g')
      .selectAll('line')
      .data(graph.edges)
      .join('line')
      .attr('stroke', '#999')
      .attr('stroke-opacity', 0.6)
      .attr('stroke-width', (d: any) => Math.sqrt(d.weight || 1));

    // Create nodes
    const node = svg.append('g')
      .selectAll('circle')
      .data(graph.nodes)
      .join('circle')
      .attr('r', (d: any) => Math.max(8, Math.min(20, Math.sqrt(d.linesOfCode || 1) * 2)))
      .attr('fill', (d: any) => {
        const centrality = d.centralityScore || 0;
        if (centrality > 0.7) return '#ff4d4f';
        if (centrality > 0.4) return '#faad14';
        return '#52c41a';
      })
      .attr('stroke', '#fff')
      .attr('stroke-width', 2)
      .call(d3.drag<any, any>()
        .on('start', (event: any, d: any) => {
          if (!event.active) simulation.alphaTarget(0.3).restart();
          d.fx = d.x;
          d.fy = d.y;
        })
        .on('drag', (event: any, d: any) => {
          d.fx = event.x;
          d.fy = event.y;
        })
        .on('end', (event: any, d: any) => {
          if (!event.active) simulation.alphaTarget(0);
          d.fx = null;
          d.fy = null;
        })
      );

    // Add labels
    const label = svg.append('g')
      .selectAll('text')
      .data(graph.nodes)
      .join('text')
      .text((d: any) => {
        const parts = d.file.split('/');
        return parts[parts.length - 1];
      })
      .attr('font-size', 10)
      .attr('dx', 12)
      .attr('dy', 4)
      .attr('fill', '#666');

    // Update positions on tick
    simulation.on('tick', () => {
      link
        .attr('x1', (d: any) => d.source.x)
        .attr('y1', (d: any) => d.source.y)
        .attr('x2', (d: any) => d.target.x)
        .attr('y2', (d: any) => d.target.y);

      node
        .attr('cx', (d: any) => d.x)
        .attr('cy', (d: any) => d.y);

      label
        .attr('x', (d: any) => d.x)
        .attr('y', (d: any) => d.y);
    });

    // Add tooltip
    node.append('title')
      .text((d: any) => `${d.file}\nLines: ${d.linesOfCode}\nComplexity: ${d.complexity}\nCentrality: ${(d.centralityScore || 0).toFixed(2)}`);
  }, []);

  useEffect(() => {
    if (selectedGraphRepo?.dependencyGraph && graphModalVisible) {
      setTimeout(() => {
        if (selectedGraphRepo.dependencyGraph) {
        renderDependencyGraph(selectedGraphRepo.dependencyGraph, 'dependency-graph-container');
      }
      }, 100);
    }
  }, [selectedGraphRepo, graphModalVisible, renderDependencyGraph]);

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return '#ff4d4f';
      case 'HIGH': return '#ff7875';
      case 'MEDIUM': return '#faad14';
      case 'LOW': return '#52c41a';
      default: return '#d9d9d9';
    }
  };

  const getGradeColor = (grade: string) => {
    switch (grade) {
      case 'A': return '#52c41a';
      case 'B': return '#1890ff';
      case 'C': return '#faad14';
      case 'D': return '#ff7875';
      case 'F': return '#ff4d4f';
      default: return '#d9d9d9';
    }
  };

  return (
    <Layout style={{ minHeight: '100vh', background: '#0a0a0c' }}>
      <Header style={{ 
        background: '#0a0a0c', 
        borderBottom: '1px solid #262626',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <NodeIndexOutlined style={{ fontSize: 24, color: '#00ff9d' }} />
          <Title level={3} style={{ color: '#f0f0f2', margin: 0 }}>
            SupremeAI CodeFlow
          </Title>
        </div>
        <Space>
          <Button 
            type="primary" 
            icon={<RocketOutlined />}
            onClick={() => setViewMode(viewMode === 'list' ? 'graph' : 'list')}
            style={{ 
              background: '#00ff9d',
              borderColor: '#00ff9d',
              color: '#0a0a0c'
            }}
          >
            {viewMode === 'list' ? 'Graph View' : 'List View'}
          </Button>
          {onClose && (
            <Button onClick={onClose} style={{ color: '#f0f0f2' }}>
              Close
            </Button>
          )}
        </Space>
      </Header>

      <Content style={{ padding: 24, background: '#0a0a0c' }}>
        {/* Analysis Input Card */}
        <Card 
          style={{ 
            background: '#16161a', 
            border: '1px solid #262626',
            marginBottom: 24
          }} 
          bodyStyle={{ padding: 24 }}
        >
          <Title level={4} style={{ color: '#f0f0f2', marginBottom: 16 }}>
            <AimOutlined style={{ color: '#00ff9d', marginRight: 8 }} />
            Analyze Repository
          </Title>
          
          <Row gutter={16}>
            <Col span={10}>
              <div style={{ marginBottom: 16 }}>
                <Text style={{ color: '#a3a3a3', display: 'block', marginBottom: 8 }}>
                  Repository URL
                </Text>
                <input
                  value={repoUrl}
                  onChange={(e) => setRepoUrl(e.target.value)}
                  placeholder="https://github.com/owner/repo"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    background: '#0a0a0c',
                    border: '1px solid #262626',
                    borderRadius: 6,
                    color: '#f0f0f2',
                    fontSize: 14
                  }}
                />
              </div>
            </Col>
            <Col span={4}>
              <div style={{ marginBottom: 16 }}>
                <Text style={{ color: '#a3a3a3', display: 'block', marginBottom: 8 }}>
                  Source Type
                </Text>
                <select
                  value={sourceType}
                  onChange={(e) => setSourceType(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    background: '#0a0a0c',
                    border: '1px solid #262626',
                    borderRadius: 6,
                    color: '#f0f0f2',
                    fontSize: 14
                  }}
                >
                  <option value="GITHUB">GitHub</option>
                  <option value="GITLAB">GitLab</option>
                  <option value="BITBUCKET">Bitbucket</option>
                  <option value="LOCAL">Local</option>
                </select>
              </div>
            </Col>
            <Col span={6}>
              <div style={{ marginBottom: 16 }}>
                <Text style={{ color: '#a3a3a3', display: 'block', marginBottom: 8 }}>
                  Owner ID (optional)
                </Text>
                <input
                  value={ownerId}
                  onChange={(e) => setOwnerId(e.target.value)}
                  placeholder="user-123"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    background: '#0a0a0c',
                    border: '1px solid #262626',
                    borderRadius: 6,
                    color: '#f0f0f2',
                    fontSize: 14
                  }}
                />
              </div>
            </Col>
            <Col span={4}>
              <Button
                type="primary"
                size="large"
                loading={analyzing}
                onClick={handleAnalyze}
                disabled={!repoUrl.trim()}
                style={{
                  width: '100%',
                  height: 40,
                  background: '#00ff9d',
                  borderColor: '#00ff9d',
                  color: '#0a0a0c',
                  fontWeight: 600
                }}
              >
                {analyzing ? 'Analyzing...' : 'Analyze'}
              </Button>
            </Col>
          </Row>

          {error && (
            <Alert
              message="Error"
              description={error}
              type="error"
              showIcon
              style={{ marginTop: 16 }}
              onClose={() => setError(null)}
            />
          )}

          {success && (
            <Alert
              message="Success"
              description={success}
              type="success"
              showIcon
              style={{ marginTop: 16 }}
              onClose={() => setSuccess(null)}
            />
          )}
        </Card>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <Spin size="large" />
          </div>
        ) : viewMode === 'list' ? (
          /* List View */
          <Row gutter={[16, 16]}>
            {repositories.map((repo) => (
              <Col xs={24} lg={12} xl={8} key={repo.id}>
                <Card
                  style={{
                    background: '#16161a',
                    border: '1px solid #262626',
                    cursor: 'pointer',
                    transition: 'all 0.3s',
                  }}
                  hoverable
                  bodyStyle={{ padding: 20 }}
                  onClick={() => setSelectedRepo(repo)}
                  actions={[
                    <Tooltip title="View Details">
                      <EyeOutlined key="view" onClick={(e) => {
                        e.stopPropagation();
                        setSelectedRepo(repo);
                      }}
                      />
                    </Tooltip>,
                    <Tooltip title="View Graph">
                      <BranchesOutlined key="graph" onClick={(e) => {
                        e.stopPropagation();
                        setSelectedGraphRepo(repo);
                        setGraphModalVisible(true);
                      }}
                      />
                    </Tooltip>,
                    <Tooltip title="Re-analyze">
                      <ReloadOutlined key="reload" onClick={(e) => {
                        e.stopPropagation();
                        handleReanalyze(repo.id);
                      }}
                      />
                    </Tooltip>,
                    <Tooltip title="Export">
                      <DownloadOutlined key="export" onClick={(e) => {
                        e.stopPropagation();
                        handleExport(repo.id, 'json');
                      }}
                      />
                    </Tooltip>,
                  ]}
                >
                  <div style={{ marginBottom: 12 }}>
                    <Title level={5} style={{ color: '#f0f0f2', margin: 0 }}>
                      {repo.name}
                    </Title>
                    <Text style={{ color: '#a3a3a3', fontSize: 12 }}>
                      {repo.fullName}
                    </Text>
                  </div>

                  {repo.healthScore !== undefined && (
                    <div style={{ marginBottom: 12 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                        <div
                          style={{
                            width: 24,
                            height: 24,
                            borderRadius: '50%',
                            background: getGradeColor(repo.healthGrade || 'F'),
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#fff',
                            fontWeight: 'bold',
                            fontSize: 12
                          }}
                        >
                          {repo.healthGrade}
                        </div>
                        <Text style={{ color: '#f0f0f2', fontWeight: 600 }}>
                          {repo.healthScore}/100
                        </Text>
                      </div>
                      <Progress
                        percent={repo.healthScore}
                        strokeColor={getGradeColor(repo.healthGrade || 'F')}
                        trailColor="#262626"
                        size="small"
                      />
                    </div>
                  )}

                  <Row gutter={16} style={{ marginBottom: 12 }}>
                    <Col span={12}>
                      <Text style={{ color: '#a3a3a3', fontSize: 11 }}>Files</Text>
                      <div style={{ color: '#f0f0f2', fontWeight: 600 }}>
                        {repo.totalFiles || 0}
                      </div>
                    </Col>
                    <Col span={12}>
                      <Text style={{ color: '#a3a3a3', fontSize: 11 }}>Lines</Text>
                      <div style={{ color: '#f0f0f2', fontWeight: 600 }}>
                        {repo.totalLinesOfCode?.toLocaleString() || 0}
                      </div>
                    </Col>
                  </Row>

                  <div style={{ marginBottom: 12 }}>
                    <Text style={{ color: '#a3a3a3', fontSize: 11, display: 'block', marginBottom: 4 }}>
                      Security Issues
                    </Text>
                    <Space wrap>
                      {(repo.securityIssues?.filter(s => s.severity === 'CRITICAL').length || 0) > 0 && (
                        <Badge count={repo.securityIssues?.filter(s => s.severity === 'CRITICAL').length}>
                          <Tag color="#ff4d4f">Critical</Tag>
                        </Badge>
                      )}
                      {(repo.securityIssues?.filter(s => s.severity === 'HIGH').length || 0) > 0 && (
                        <Badge count={repo.securityIssues?.filter(s => s.severity === 'HIGH').length}>
                          <Tag color="#ff7875">High</Tag>
                        </Badge>
                      )}
                      {(repo.securityIssues?.filter(s => s.severity === 'MEDIUM').length || 0) > 0 && (
                        <Badge count={repo.securityIssues?.filter(s => s.severity === 'MEDIUM').length}>
                          <Tag color="#faad14">Medium</Tag>
                        </Badge>
                      )}
                      {(!repo.securityIssues || repo.securityIssues.length === 0) && (
                        <Tag color="#52c41a">Clean</Tag>
                      )}
                    </Space>
                  </div>

                  <div style={{ marginBottom: 12 }}>
                    <Text style={{ color: '#a3a3a3', fontSize: 11, display: 'block', marginBottom: 4 }}>
                      Patterns Detected
                    </Text>
                    <Space wrap>
                      {repo.detectedPatterns?.slice(0, 3).map((p, i) => (
                        <Tag key={i} color="blue">{p.patternType}</Tag>
                      ))}
                      {repo.detectedPatterns && repo.detectedPatterns.length > 3 && (
                        <Tag>+{repo.detectedPatterns.length - 3} more</Tag>
                      )}
                    </Space>
                  </div>

                  {repo.circularDependencies && repo.circularDependencies.length > 0 && (
                    <Alert
                      message={`${repo.circularDependencies.length} Circular Dependency`}
                      type="warning"
                      showIcon
                      style={{ marginBottom: 0 }}
                    />
                  )}

                  <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Badge
                      status={repo.analysisStatus === 'COMPLETED' ? 'success' : 
                              repo.analysisStatus === 'ANALYZING' ? 'processing' : 'default'}
                    />
                    <Text style={{ color: '#a3a3a3', fontSize: 11 }}>
                      {repo.analysisStatus}
                    </Text>
                    {repo.lastAnalyzedAt && (
                      <Text style={{ color: '#666', fontSize: 11 }}>
                        {new Date(repo.lastAnalyzedAt).toLocaleDateString()}
                      </Text>
                    )}
                  </div>
                </Card>
              </Col>
            ))}
          </Row>
        ) : (
          /* Graph View - Overview */
          <Card 
            style={{ 
              background: '#16161a', 
              border: '1px solid #262626',
              minHeight: 600
            }} 
            bodyStyle={{ padding: 24 }}
          >
            <Title level={4} style={{ color: '#f0f0f2', marginBottom: 24 }}>
              <BranchesOutlined style={{ color: '#00ff9d', marginRight: 8 }} />
              Repository Dependency Overview
            </Title>
            <div id="overview-graph-container" style={{ width: '100%', height: 500 }}>
              {repositories.length > 0 ? (
                <svg ref={setSvgRef} width="100%" height="100%" />
              ) : (
                <div style={{ textAlign: 'center', padding: 60, color: '#666' }}>
                  No repositories to display
                </div>
              )}
            </div>
          </Card>
        )}

        {/* Repository Detail Modal */}
        <Modal
          title={null}
          open={!!selectedRepo}
          onCancel={() => setSelectedRepo(null)}
          footer={null}
          width="90%"
          style={{ top: 20 }}
          bodyStyle={{ padding: 0 }}
        >
          {selectedRepo && (
            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              items={[
                {
                  key: '1',
                  label: (
                    <span>
                      <FileTextOutlined />
                      Overview
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 24 }}>
                      <Row gutter={24}>
                        <Col span={16}>
                          <Title level={4} style={{ color: '#f0f0f2' }}>
                            {selectedRepo.name}
                          </Title>
                          <Paragraph style={{ color: '#a3a3a3' }}>
                            {selectedRepo.description || 'No description'}
                          </Paragraph>
                          
                          <Row gutter={16} style={{ marginTop: 24 }}>
                            <Col span={6}>
                              <Statistic
                                title="Health Score"
                                value={selectedRepo.healthScore || 0}
                                valueStyle={{ color: getGradeColor(selectedRepo.healthGrade || 'F') }}
                                suffix={`/100 (${selectedRepo.healthGrade})`}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Files"
                                value={selectedRepo.totalFiles || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Lines"
                                value={selectedRepo.totalLinesOfCode?.toLocaleString() || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Functions"
                                value={selectedRepo.totalFunctions || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                          </Row>

                          {selectedRepo.languageStats && (
                            <div style={{ marginTop: 24 }}>
                              <Text style={{ color: '#a3a3a3', display: 'block', marginBottom: 8 }}>
                                Languages
                              </Text>
                              <Space wrap>
                                {Object.entries(selectedRepo.languageStats).map(([lang, count]) => (
                                  <Tag key={lang} color="blue">
                                    {lang}: {count}
                                  </Tag>
                                ))}
                              </Space>
                            </div>
                          )}
                        </Col>
                        <Col span={8}>
                          <Card 
                            size="small" 
                            title="Security Summary"
                            style={{ background: '#0a0a0c', border: '1px solid #262626' }}
                            bodyStyle={{ padding: 16 }}
                          >
                            {selectedRepo.securityIssues && selectedRepo.securityIssues.length > 0 ? (
                              <Space direction="vertical" style={{ width: '100%' }}>
                                {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map(severity => {
                                  const count = selectedRepo.securityIssues?.filter(
                                    s => s.severity === severity
                                  ).length || 0;
                                  if (count === 0) return null;
                                  return (
                                    <div key={severity} style={{ display: 'flex', justifyContent: 'space-between' }}>
                                      <Text style={{ color: getSeverityColor(severity) }}>
                                        {severity}
                                      </Text>
                                      <Badge count={count} style={{ background: getSeverityColor(severity) }} />
                                    </div>
                                  );
                                })}
                              </Space>
                            ) : (
                              <Text style={{ color: '#52c41a' }}>No security issues found</Text>
                            )}
                          </Card>
                        </Col>
                      </Row>

                      {selectedRepo.aiSuggestions && selectedRepo.aiSuggestions.length > 0 && (
                        <div style={{ marginTop: 24 }}>
                          <Title level={5} style={{ color: '#f0f0f2' }}>
                            <RocketOutlined style={{ color: '#00ff9d', marginRight: 8 }} />
                            AI Suggestions
                          </Title>
                          <List
                            size="small"
                            dataSource={selectedRepo.aiSuggestions.slice(0, 5)}
                            renderItem={(item) => (
                              <List.Item>
                                <List.Item.Meta
                                  title={<Text style={{ color: '#f0f0f2' }}>{item.type}</Text>}
                                  description={
                                    <div>
                                      <Text style={{ color: '#a3a3a3' }}>{item.description}</Text>
                                      <br />
                                      <Text style={{ color: '#666', fontSize: 11 }}>
                                        {item.file}:{item.line} | Confidence: {item.confidence}%
                                      </Text>
                                    </div>
                                  }
                                />
                              </List.Item>
                            )}
                          />
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  key: '2',
                  label: (
                    <span>
                      <SecurityScanOutlined />
                      Security
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 24, maxHeight: 500, overflowY: 'auto' }}>
                      {selectedRepo.securityIssues && selectedRepo.securityIssues.length > 0 ? (
                        <List
                          dataSource={selectedRepo.securityIssues}
                          renderItem={(item) => (
                            <List.Item>
                              <Card
                                size="small"
                                style={{
                                  width: '100%',
                                  background: '#0a0a0c',
                                  border: `1px solid ${getSeverityColor(item.severity)}`,
                                }}
                              >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                  <div>
                                    <Tag color={getSeverityColor(item.severity)}>
                                      {item.severity}
                                    </Tag>
                                    <Tag>{item.type}</Tag>
                                    <div style={{ marginTop: 8 }}>
                                      <Text style={{ color: '#f0f0f2' }}>
                                        {item.description}
                                      </Text>
                                      <br />
                                      <Text style={{ color: '#666', fontSize: 11 }}>
                                        {item.file}:{item.line}
                                      </Text>
                                      {item.cweId && (
                                        <Text style={{ color: '#666', fontSize: 11 }}>
                                          | CWE-{item.cweId}
                                        </Text>
                                      )}
                                    </div>
                                  </div>
                                  <Tooltip title={item.remediation}>
                                    <InfoCircleOutlined style={{ color: '#00ff9d', fontSize: 16 }} />
                                  </Tooltip>
                                </div>
                              </Card>
                            </List.Item>
                          )}
                        />
                      ) : (
                        <div style={{ textAlign: 'center', padding: 40 }}>
                          <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
                          <div style={{ marginTop: 16, color: '#52c41a' }}>
                            No security issues found
                          </div>
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  key: '3',
                  label: (
                    <span>
                      <BranchesOutlined />
                      Dependencies
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 24 }}>
                      {selectedRepo.dependencyGraph && (
                        <div>
                          <Row gutter={16} style={{ marginBottom: 16 }}>
                            <Col span={6}>
                              <Statistic
                                title="Nodes"
                                value={selectedRepo.dependencyGraph.nodes?.length || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Edges"
                                value={selectedRepo.dependencyGraph.edges?.length || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Blast Radius"
                                value={selectedRepo.dependencyGraph.blastRadius || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Critical Path"
                                value={selectedRepo.dependencyGraph.criticalPath?.length || 0}
                                valueStyle={{ color: '#f0f0f2' }}
                              />
                            </Col>
                          </Row>
                          
                          <Button
                            onClick={() => {
                              setSelectedGraphRepo(selectedRepo);
                              setGraphModalVisible(true);
                            }}
                            style={{
                              background: '#00ff9d',
                              borderColor: '#00ff9d',
                              color: '#0a0a0c'
                            }}
                          >
                            View Interactive Graph
                          </Button>
                        </div>
                      )}

                      {selectedRepo.circularDependencies && selectedRepo.circularDependencies.length > 0 && (
                        <div style={{ marginTop: 24 }}>
                          <Title level={5} style={{ color: '#ff7875' }}>
                            <WarningOutlined style={{ marginRight: 8 }} />
                            Circular Dependencies
                          </Title>
                          <List
                            size="small"
                            dataSource={selectedRepo.circularDependencies}
                            renderItem={(item) => (
                              <List.Item>
                                <Alert
                                  message={`${item.files.length} files involved`}
                                  description={item.suggestion}
                                  type="warning"
                                  showIcon
                                  style={{ width: '100%' }}
                                />
                              </List.Item>
                            )}
                          />
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  key: '4',
                  label: (
                    <span>
                      <CodeOutlined />
                      Patterns
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 24, maxHeight: 500, overflowY: 'auto' }}>
                      {selectedRepo.detectedPatterns && selectedRepo.detectedPatterns.length > 0 ? (
                        <List
                          grid={{ gutter: 16, xs: 1, sm: 2, md: 2, lg: 2, xl: 3, xxl: 3 }}
                          dataSource={selectedRepo.detectedPatterns}
                          renderItem={(item) => (
                            <List.Item>
                              <Card
                                size="small"
                                style={{ background: '#0a0a0c', border: '1px solid #262626' }}
                              >
                                <Card.Meta
                                  title={<Text style={{ color: '#f0f0f2' }}>{item.patternType}</Text>}
                                  description={
                                    <div>
                                      <Text style={{ color: '#a3a3a3', fontSize: 12 }}>
                                        {item.description}
                                      </Text>
                                      <br />
                                      <Text style={{ color: '#666', fontSize: 11 }}>
                                        {item.file}:{item.line} | Confidence: {item.confidence}%
                                      </Text>
                                    </div>
                                  }
                                />
                              </Card>
                            </List.Item>
                          )}
                        />
                      ) : (
                        <div style={{ textAlign: 'center', padding: 40 }}>
                          <InfoCircleOutlined style={{ fontSize: 48, color: '#666' }} />
                          <div style={{ marginTop: 16, color: '#666' }}>
                            No patterns detected
                          </div>
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  key: '5',
                  label: (
                    <span>
                      <BugOutlined />
                      Dead Code
                    </span>
                  ),
                  children: (
                    <div style={{ padding: 24, maxHeight: 500, overflowY: 'auto' }}>
                      {selectedRepo.deadCode && selectedRepo.deadCode.length > 0 ? (
                        <List
                          dataSource={selectedRepo.deadCode}
                          renderItem={(item) => (
                            <List.Item>
                              <Card
                                size="small"
                                style={{ width: '100%', background: '#0a0a0c', border: '1px solid #262626' }}
                              >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                  <div>
                                    <Tag color="orange">{item.type}</Tag>
                                    <Text style={{ color: '#f0f0f2', marginLeft: 8 }}>
                                      {item.name}
                                    </Text>
                                    <br />
                                    <Text style={{ color: '#666', fontSize: 11 }}>
                                      {item.file}:{item.line}
                                    </Text>
                                  </div>
                                </div>
                              </Card>
                            </List.Item>
                          )}
                        />
                      ) : (
                        <div style={{ textAlign: 'center', padding: 40 }}>
                          <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
                          <div style={{ marginTop: 16, color: '#52c41a' }}>
                            No dead code found
                          </div>
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
            />
          )}
        </Modal>

        {/* Graph Modal */}
          <Modal
            title="Dependency Graph"
            open={graphModalVisible}
            onCancel={() => setGraphModalVisible(false)}
            footer={null}
            width="90%"
            style={{ top: 20 }}
            bodyStyle={{ padding: 0 }}
          >
            <div id="dependency-graph-container" style={{ width: '100%', height: 600, background: '#0a0a0c' }} />
          </Modal>
        </Content>
      </Layout>
  );
};

export default CodeFlowDashboard;