// AnalysisResultsTable.tsx - Table displaying analysis findings (Phase 3)
import React from 'react';
import { Table, Tag, Space, Button, Typography, Tooltip, Empty, Popconfirm, message } from 'antd';
import {
  FileTextOutlined,
  BugOutlined,
  EyeOutlined,
  DeleteOutlined,
  CopyOutlined,
  ThunderboltOutlined,
  AimOutlined,
  DatabaseOutlined,
  ClearOutlined
} from '@ant-design/icons';
import type { AnalysisJob } from '../types';
import type { AnalysisFinding } from '../types';

const { Text, Link, Title } = Typography;

interface AnalysisResultsTableProps {
  jobs: AnalysisJob[];
  onDelete: (jobId: string) => void;
  onView: (jobId: string) => void;
  onViewFixes?: (job: AnalysisJob) => void;
  onClearCache?: (projectId: string) => void;
  getSeverityColor: (severity: string) => string;
  getStatusColor: (status: string) => string;
}

const AnalysisResultsTable: React.FC<AnalysisResultsTableProps> = ({
  jobs,
  onDelete,
  onView,
  onViewFixes,
  onClearCache,
  getSeverityColor,
  getStatusColor
}) => {
  // Get all findings from all jobs
  const allFindings = jobs.flatMap(job =>
    (job.findings || []).map(finding => ({
      ...finding,
      jobId: job.id,
      projectName: job.projectName,
      jobStatus: job.status,
      jobTime: job.startTime
    }))
  );

  const getCategoryIcon = (category: string) => {
    switch (category?.toUpperCase()) {
      case 'SECRETS': return '🔑';
      case 'SQL_INJECTION': return '💉';
      case 'XSS': return '🌐';
      case 'PATH_TRAVERSAL': return '📁';
      case 'INSECURE_RANDOM': return '🎲';
      case 'WEAK_CRYPTO': return '🔐';
      case 'COMMAND_INJECTION': return '⚡';
      default: return <BugOutlined />;
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('Copied to clipboard');
  };

  const columns = [
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: (severity: string) => (
        <Tag color={getSeverityColor(severity)} style={{ fontWeight: 700 }}>
          {severity}
        </Tag>
      )
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      width: 130,
      render: (category: string) => (
        <Space>
          {getCategoryIcon(category)}
          <Text style={{ color: 'var(--text-dim)' }}>{category}</Text>
        </Space>
      )
    },
    {
      title: 'File',
      dataIndex: 'file',
      key: 'file',
      render: (file: string, record: any) => (
        <Tooltip title={file}>
          <Link
            onClick={() => copyToClipboard(file)}
            style={{ color: 'var(--neon-blue)', maxWidth: 200, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis' }}
          >
            {file}
          </Link>
        </Tooltip>
      )
    },
    {
      title: 'Line',
      dataIndex: 'line',
      key: 'line',
      width: 60,
      render: (line: number) => (
        <Text strong style={{ color: '#fff' }}>{line}</Text>
      )
    },
    {
      title: 'Message',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
      render: (message: string) => (
        <Tooltip title={message}>
          <Text style={{ color: 'var(--text-main)' }}>{message}</Text>
        </Tooltip>
      )
    },
    {
      title: 'Suggestion',
      dataIndex: 'suggestion',
      key: 'suggestion',
      ellipsis: true,
      render: (suggestion: string) => (
        <Tooltip title={suggestion}>
          <Text type="secondary" style={{ fontSize: 12 }}>{suggestion}</Text>
        </Tooltip>
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 80,
      render: (_: any, record: any) => (
        <Space>
          <Tooltip title="View Code Snippet">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => copyToClipboard(record.codeSnippet || record.message)}
              style={{ color: 'var(--neon-blue)' }}
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  if (jobs.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={
          <Text type="secondary">
            No analysis jobs yet. Submit a project to analyze.
          </Text>
        }
      />
    );
  }

  const jobColumns = [
    ...columns.slice(0, 4), // severity, category, file, line
    ...columns.slice(5), // skip message (use project-specific)
  ];

  // Combine completed jobs with findings
  const completedJobs = jobs.filter(j => j.status === 'COMPLETED' && j.findings && j.findings.length > 0);

  if (completedJobs.length === 0 && jobs.length > 0) {
    return (
      <div>
        <Title level={4} style={{ color: '#fff', marginBottom: 16 }}>
          Analysis Jobs ({jobs.length})
        </Title>
        <Table
          dataSource={jobs}
          rowKey="id"
          pagination={false}
          size="small"
          columns={[
            {
              title: 'Project',
              dataIndex: 'projectName',
              key: 'projectName',
              render: (name: string, record: any) => (
                <Space>
                  <Text style={{ color: '#fff' }}>{name}</Text>
                  {record.ragUsed && (
                    <Tooltip title="RAG Context Selection">
                      <Tag color="cyan" style={{ fontSize: 10, margin: 0 }}><AimOutlined /></Tag>
                    </Tooltip>
                  )}
                  {record.incrementalUsed && (
                    <Tooltip title={`Incremental: ${record.changedFiles || 0} changed files`}>
                      <Tag color="green" style={{ fontSize: 10, margin: 0 }}><DatabaseOutlined /></Tag>
                    </Tooltip>
                  )}
                </Space>
              )
            },
            {
              title: 'Status',
              dataIndex: 'status',
              key: 'status',
              render: (status: string) => (
                <Tag color={getStatusColor(status)}>{status}</Tag>
              )
            },
            {
              title: 'Files',
              dataIndex: 'filesAnalyzed',
              key: 'filesAnalyzed',
              sorter: (a: any, b: any) => a.filesAnalyzed - b.filesAnalyzed
            },
            {
              title: 'Findings',
              dataIndex: 'totalFindings',
              key: 'totalFindings',
              sorter: (a: any, b: any) => a.totalFindings - b.totalFindings,
              render: (count: number) => (
                <Tag color={count > 0 ? 'red' : 'green'}>{count}</Tag>
              )
            },
            {
              title: 'Started',
              dataIndex: 'startTime',
              key: 'startTime',
              render: (time: string) => new Date(time).toLocaleString()
            },
            {
              title: 'Actions',
              key: 'actions',
              width: 200,
              render: (_: any, record: AnalysisJob) => (
                <Space>
                  <Button
                    type="text"
                    icon={<EyeOutlined />}
                    onClick={() => onView(record.id)}
                    disabled={record.status === 'RUNNING'}
                  />
                  {onViewFixes && record.totalFindings > 0 && (
                    <Tooltip title="View Fix Suggestions">
                      <Button
                        type="text"
                        icon={<ThunderboltOutlined />}
                        onClick={() => onViewFixes(record)}
                        style={{ color: '#ffcc00' }}
                      />
                    </Tooltip>
                  )}
                  {onClearCache && (
                    <Tooltip title="Clear RAG Cache">
                      <Button
                        type="text"
                        icon={<ClearOutlined />}
                        onClick={() => onClearCache(record.projectName)}
                        style={{ color: 'var(--neon-blue)' }}
                      />
                    </Tooltip>
                  )}
                  <Popconfirm
                    title="Delete this analysis job?"
                    onConfirm={() => onDelete(record.id)}
                    okText="Yes"
                    cancelText="No"
                  >
                    <Button
                      type="text"
                      icon={<DeleteOutlined />}
                      danger
                      disabled={record.status === 'RUNNING'}
                    />
                  </Popconfirm>
                </Space>
              )
            }
          ]}
        />
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          Security Findings ({allFindings.length})
        </Title>
        <Space>
          <Button
            icon={<CopyOutlined />}
            onClick={() => {
              const csv = convertToCSV(allFindings);
              copyToClipboard(csv);
              message.success('Findings exported to CSV');
            }}
          >
            Export CSV
          </Button>
        </Space>
      </div>

      <Table
        dataSource={allFindings}
        rowKey="id"
        pagination={{ pageSize: 25, showSizeChanger: true, pageSizeOptions: ['10', '25', '50', '100'] }}
        size="small"
        scroll={{ x: 1000 }}
        columns={columns}
      />
    </div>
  );
};

function convertToCSV(findings: any[]): string {
  const headers = ['Severity', 'Category', 'File', 'Line', 'Message', 'Suggestion', 'Code Snippet'];
  const rows = findings.map(f => [
    f.severity,
    f.category,
    `"${f.file}"`,
    f.line,
    `"${f.message}"`,
    `"${f.suggestion}"`,
    `"${f.codeSnippet}"`
  ]);
  return [headers, ...rows].map(row => row.join(',')).join('\n');
}

export default AnalysisResultsTable;
