import React from 'react';
import { Card, Button, Space, Typography, Badge, List, Avatar, Tooltip, Tag, Divider } from 'antd';
import { BulbOutlined, CheckCircleOutlined, CloseCircleOutlined, RocketOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { AISuggestion } from '../lib/suggestionService';

const { Text, Title, Paragraph } = Typography;

interface AISuggestionInformerProps {
  suggestions?: AISuggestion[];
  onApprove?: (id: string) => void;
  onDecline?: (id: string) => void;
  onSelect?: (id: string) => void;
  title?: string;
  context?: string;
  style?: React.CSSProperties;
}

const AISuggestionInformer: React.FC<AISuggestionInformerProps> = ({
  suggestions = [],
  onApprove,
  onDecline,
  onSelect,
  title = "AI System Optimizations",
  context = "General",
  style
}) => {
  // Use onSelect as fallback for onApprove
  const handleApprove = (id: string) => {
    if (onSelect) onSelect(id);
    else if (onApprove) onApprove(id);
  };
  
  const handleDecline = (id: string) => {
    if (onDecline) onDecline(id);
  };
  const getImpactColor = (impact: string) => {
    switch (impact) {
      case 'security': return '#ff4d4f';
      case 'performance': return '#1890ff';
      case 'cost': return '#52c41a';
      case 'capability': return '#722ed1';
      default: return '#d9d9d9';
    }
  };

  return (
    <Card 
      className="glass-card ai-suggestion-card" 
      title={<Space><BulbOutlined style={{ color: '#faad14' }} /> {title}</Space>}
      extra={<Badge status="processing" text={`${suggestions?.length || 0} Active Insights`} />}
      style={{ marginBottom: 24, borderLeft: '4px solid #faad14', ...style }}
    >
      <Paragraph type="secondary" style={{ fontSize: 13 }}>
        Based on current {context} metrics, SupremeAI suggests the following enhancements to make the system more profound.
      </Paragraph>
      
      <List
        itemLayout="vertical"
        dataSource={suggestions}
        renderItem={item => (
          <div className="suggestion-item-v2" style={{ 
            background: 'rgba(255,255,255,0.02)', 
            padding: '16px', 
            borderRadius: '12px', 
            marginBottom: '16px',
            border: '1px solid rgba(255,255,255,0.05)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <Space align="start">
                <Avatar 
                  icon={item.impact === 'security' ? <SafetyCertificateOutlined /> : <RocketOutlined />} 
                  style={{ backgroundColor: getImpactColor(item.impact) }} 
                />
                <div>
                  <Title level={5} style={{ margin: 0, color: '#fff' }}>{item.title}</Title>
                  <Space size="small" style={{ marginTop: 4 }}>
                    <Tag color={getImpactColor(item.impact)}>{item.impact.toUpperCase()}</Tag>
                    <Text type="secondary" style={{ fontSize: 11 }}>Confidence: {Math.round(item.confidence * 100)}%</Text>
                  </Space>
                </div>
              </Space>
              {item.autoExecutable && (
                <Tooltip title="This optimization can be performed autonomously upon approval">
                  <Tag color="gold" icon={<RocketOutlined />}>Auto-Ready</Tag>
                </Tooltip>
              )}
            </div>
            
            <Paragraph style={{ marginTop: 12, color: 'rgba(255,255,255,0.7)', fontSize: 13 }}>
              {item.description}
            </Paragraph>
            
            <Divider style={{ margin: '12px 0', borderColor: 'rgba(255,255,255,0.05)' }} />
            
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Space>
               <Button
                 size="small"
                 type="text"
                 icon={<CloseCircleOutlined />}
                 onClick={() => handleDecline(item.id)}
                 style={{ color: 'rgba(255,255,255,0.4)' }}
               >
                 Dismiss
               </Button>
               <Button
                 size="small"
                 type="primary"
                 icon={<CheckCircleOutlined />}
                 onClick={() => handleApprove(item.id)}
                 style={{ borderRadius: '6px' }}
               >
                 Grant Permission
               </Button>
              </Space>
            </div>
          </div>
        )}
      />
    </Card>
  );
};

export default AISuggestionInformer;
