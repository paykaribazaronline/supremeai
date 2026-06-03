import React, { useState } from 'react';
import { Card, Typography, Button, Space, Tag, Tooltip, Divider, message, Modal, Progress } from 'antd';
import { CheckOutlined, CopyOutlined, DiffOutlined, ThunderboltOutlined } from '@ant-design/icons';
import type { AnalysisFix } from '../types';

const { Text, Title } = Typography;

interface FixSuggestionCardProps {
  fix: AnalysisFix;
  onApply: (fixId: string) => void;
}

const FixSuggestionCard: React.FC<FixSuggestionCardProps> = ({ fix, onApply }) => {
  const [diffVisible, setDiffVisible] = useState(false);
  const [applying, setApplying] = useState(false);

  const handleApply = async () => {
    setApplying(true);
    try {
      await onApply(fix.id);
      message.success('Fix applied successfully');
    } catch {
      message.error('Failed to apply fix');
    } finally {
      setApplying(false);
    }
  };

  const copyCode = (code: string) => {
    navigator.clipboard.writeText(code);
    message.success('Code copied to clipboard');
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.8) return '#34c759';
    if (confidence >= 0.5) return '#ffcc00';
    return '#ff3b30';
  };

  const getConfidenceLabel = (confidence: number) => {
    if (confidence >= 0.8) return 'High';
    if (confidence >= 0.5) return 'Medium';
    return 'Low';
  };

  const renderDiff = () => {
    const originalLines = (fix.originalCode || '').split('\n');
    const fixedLines = (fix.fixedCode || '').split('\n');
    const maxLines = Math.max(originalLines.length, fixedLines.length);

    return (
      <div style={{ fontFamily: 'monospace', fontSize: 12 }}>
        <div style={{ display: 'flex', gap: 16 }}>
          <div style={{ flex: 1 }}>
            <div style={{ color: '#ff3b30', fontWeight: 700, marginBottom: 8, padding: '4px 8px', background: 'rgba(255,59,48,0.1)', borderRadius: 4 }}>
              Original
            </div>
            {originalLines.slice(0, maxLines).map((line, i) => (
              <div key={i} style={{
                background: i < fixedLines.length && line !== fixedLines[i] ? 'rgba(255,59,48,0.1)' : 'transparent',
                padding: '1px 8px',
                whiteSpace: 'pre-wrap',
                color: 'var(--text-dim)'
              }}>
                {line || ' '}
              </div>
            ))}
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ color: '#34c759', fontWeight: 700, marginBottom: 8, padding: '4px 8px', background: 'rgba(52,199,89,0.1)', borderRadius: 4 }}>
              Fixed
            </div>
            {fixedLines.slice(0, maxLines).map((line, i) => (
              <div key={i} style={{
                background: i < originalLines.length && line !== originalLines[i] ? 'rgba(52,199,89,0.1)' : 'transparent',
                padding: '1px 8px',
                whiteSpace: 'pre-wrap',
                color: '#fff'
              }}>
                {line || ' '}
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  };

  return (
    <>
      <Card
        size="small"
        style={{
          marginBottom: 12,
          border: '1px solid rgba(255,255,255,0.1)',
          background: 'rgba(0,0,0,0.3)',
          borderRadius: 8
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div style={{ flex: 1 }}>
            <Space align="center" style={{ marginBottom: 8 }}>
              <ThunderboltOutlined style={{ color: '#00f3ff' }} />
              <Text strong style={{ color: '#fff' }}>{fix.file}</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>Line {fix.line}</Text>
            </Space>

            <div style={{ marginBottom: 8 }}>
              <Space>
                <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>
                  {fix.explanation}
                </Text>
              </Space>
            </div>

            <Space style={{ marginBottom: 8 }}>
              <Text style={{ fontSize: 12, color: 'var(--text-dim)' }}>Confidence:</Text>
              <Progress
                percent={Math.round(fix.confidence * 100)}
                size="small"
                strokeColor={getConfidenceColor(fix.confidence)}
                style={{ width: 80 }}
                showInfo={false}
              />
              <Tag color={getConfidenceColor(fix.confidence)} style={{ fontSize: 11 }}>
                {getConfidenceLabel(fix.confidence)} ({Math.round(fix.confidence * 100)}%)
              </Tag>
              {fix.applied && <Tag color="green">Applied</Tag>}
            </Space>

            <div style={{ maxHeight: 100, overflow: 'auto', background: 'rgba(0,0,0,0.3)', borderRadius: 4, padding: 8, marginBottom: 8 }}>
              <pre style={{ margin: 0, fontSize: 12, color: '#34c759', fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
                {fix.fixedCode}
              </pre>
            </div>
          </div>

          <Space direction="vertical" style={{ marginLeft: 12 }}>
            <Tooltip title="View diff">
              <Button
                type="text"
                icon={<DiffOutlined />}
                onClick={() => setDiffVisible(true)}
                style={{ color: 'var(--neon-blue)' }}
              />
            </Tooltip>
            <Tooltip title="Copy fixed code">
              <Button
                type="text"
                icon={<CopyOutlined />}
                onClick={() => copyCode(fix.fixedCode)}
                style={{ color: '#ffcc00' }}
              />
            </Tooltip>
            {!fix.applied && (
              <Tooltip title="Apply fix">
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={handleApply}
                  loading={applying}
                  style={{ background: '#34c759', borderColor: '#34c759' }}
                />
              </Tooltip>
            )}
          </Space>
        </div>
      </Card>

      <Modal
        title={
          <Space>
            <DiffOutlined style={{ color: '#00f3ff' }} />
            <span>Code Diff: {fix.file}</span>
          </Space>
        }
        open={diffVisible}
        onCancel={() => setDiffVisible(false)}
        footer={[
          <Button key="copy" icon={<CopyOutlined />} onClick={() => copyCode(fix.fixedCode)}>
            Copy Fixed Code
          </Button>,
          !fix.applied && (
            <Button key="apply" type="primary" icon={<CheckOutlined />} onClick={() => { handleApply(); setDiffVisible(false); }}>
              Apply Fix
            </Button>
          )
        ].filter(Boolean)}
        width={900}
        bodyStyle={{ background: 'rgba(0,0,0,0.9)', maxHeight: 500, overflow: 'auto' }}
      >
        {renderDiff()}
      </Modal>
    </>
  );
};

export default FixSuggestionCard;
