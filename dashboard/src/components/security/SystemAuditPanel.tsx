import React from 'react';
import { Card, Button, Typography, Alert, Tag, Switch, Space } from 'antd';
import { AuditOutlined, SecurityScanOutlined, SafetyOutlined, RobotOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Title, Text } = Typography;

interface AuditReport {
  resilienceScore: number;
  summary: string;
  vulnerabilitiesFound: number;
  auditId: string;
}

interface Protection {
  targetId: string;
  protectionType: string;
}

interface SystemAuditPanelProps {
  onRunAudit: () => void;
  auditing: boolean;
  auditReport: AuditReport | null;
  protections: Protection[];
  autonomousAuditEnabled?: boolean;
  onToggleAutonomous?: (enabled: boolean) => void;
}

const SystemAuditPanel: React.FC<SystemAuditPanelProps> = ({
  onRunAudit,
  auditing,
  auditReport,
  protections,
  autonomousAuditEnabled,
  onToggleAutonomous,
}) => {
  return (
    <Card 
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <span style={{ color: '#fff' }}><AuditOutlined /> System Self-Audit (Red Team)</span>
          <Space>
            <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: '12px' }}>
              <RobotOutlined /> Autonomous Mode
            </Text>
            <Switch 
              size="small" 
              checked={autonomousAuditEnabled} 
              onChange={onToggleAutonomous}
            />
          </Space>
        </div>
      }
      bordered={false}
      className="glass-card"
      style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.1)' }}
    >
      <div style={{ textAlign: 'center', padding: '20px 0' }}>
        <Button 
          size="large" 
          danger 
          icon={<SecurityScanOutlined />} 
          onClick={onRunAudit}
          loading={auditing}
          style={{ height: 60, fontSize: 18, padding: '0 40px', borderRadius: 30 }}
        >
          Run System Self-Audit
        </Button>
        <p style={{ marginTop: 16, color: 'rgba(255,255,255,0.45)' }}>সিস্টেম নিজের ওপর একটি নিয়ন্ত্রিত সাইবার অ্যাটাক সিমুলেট করবে রেজিলিয়েন্স পরীক্ষার জন্য।</p>
      </div>

      {auditReport && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <Alert
            message="Self-Audit Result"
            description={
              <div style={{ marginTop: 8 }}>
                <Text strong style={{ color: '#fff' }}>Score: {(auditReport.resilienceScore * 100).toFixed(1)}%</Text>
                <br />
                <Text style={{ color: 'rgba(255,255,255,0.8)' }}>{auditReport.summary}</Text>
                <div style={{ marginTop: 12 }}>
                  <Tag color="green">Vulnerabilities: {auditReport.vulnerabilitiesFound}</Tag>
                  <Tag color="blue">Audit ID: {auditReport.auditId.substring(0,8)}</Tag>
                </div>
              </div>
            }
            type="success"
            showIcon
            style={{ background: 'rgba(82, 196, 26, 0.1)', border: '1px solid rgba(82, 196, 26, 0.2)' }}
          />
        </motion.div>
      )}

      <div style={{ marginTop: 24 }}>
        <Title level={5} style={{ color: '#fff' }}>Active Dynamic Protections</Title>
        {protections.length === 0 ? <Text type="secondary">No active shields generated yet.</Text> : (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
             {protections.map(p => (
               <Tag key={p.targetId} color="processing" icon={<SafetyOutlined />}>
                 {p.protectionType} (ID: {p.targetId.substring(0,5)})
               </Tag>
             ))}
          </div>
        )}
      </div>
    </Card>
  );
};

export default SystemAuditPanel;
