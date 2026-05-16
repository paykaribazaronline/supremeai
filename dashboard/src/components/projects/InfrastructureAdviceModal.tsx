import React from 'react';
import { Modal, Typography, Divider, Button, Space, Skeleton, Empty, Alert } from 'antd';
import { CloudServerOutlined, RocketOutlined, ToolOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';

const { Title, Paragraph, Text } = Typography;

interface InfrastructureAdviceModalProps {
  visible: boolean;
  onCancel: () => void;
  advice: string | null;
  loading: boolean;
  projectName: string;
}

const InfrastructureAdviceModal: React.FC<InfrastructureAdviceModalProps> = ({
  visible,
  onCancel,
  advice,
  loading,
  projectName
}) => {
  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <CloudServerOutlined style={{ color: '#1890ff', fontSize: '24px' }} />
          <span>SaaS Infrastructure Concierge: {projectName}</span>
        </div>
      }
      open={visible}
      onCancel={onCancel}
      width={800}
      footer={[
        <Button key="close" onClick={onCancel}>
          Close
        </Button>,
        <Button key="deploy" type="primary" icon={<RocketOutlined />} disabled={!advice}>
          Proceed to Deployment
        </Button>
      ]}
      className="premium-modal"
    >
      <div style={{ maxHeight: '60vh', overflowY: 'auto', paddingRight: '10px' }}>
        {loading ? (
          <div style={{ padding: '20px' }}>
            <Skeleton active paragraph={{ rows: 8 }} />
            <div style={{ textAlign: 'center', marginTop: '20px' }}>
              <Paragraph strong>Analyzing project requirements and orchestrating infrastructure blueprint...</Paragraph>
            </div>
          </div>
        ) : advice ? (
          <div className="advice-content">
            <Alert 
              message="Infrastructure Strategy Generated" 
              description="Based on your requirements, SupremeAI has designed the following cloud architecture." 
              type="info" 
              showIcon 
              style={{ marginBottom: '20px' }}
            />
            <Typography>
              <ReactMarkdown>{advice}</ReactMarkdown>
            </Typography>
            
            <Divider />
            
            <Title level={5}><ToolOutlined /> Next Steps</Title>
            <Space direction="vertical">
              <Text><SafetyCertificateOutlined style={{ color: '#52c41a' }} /> Setup Google Cloud Project or Firebase Console</Text>
              <Text><SafetyCertificateOutlined style={{ color: '#52c41a' }} /> Configure environment variables in SupremeAI Dashboard</Text>
              <Text><SafetyCertificateOutlined style={{ color: '#52c41a' }} /> Initialize automated CI/CD pipeline</Text>
            </Space>
          </div>
        ) : (
          <Empty description="No advice generated yet. Please provide project details and request an infrastructure audit." />
        )}
      </div>
      
      <style>{`
        .premium-modal .ant-modal-content {
          background: rgba(20, 20, 25, 0.95);
          backdrop-filter: blur(20px);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 16px;
        }
        .advice-content {
          color: rgba(255, 255, 255, 0.85);
          line-height: 1.6;
        }
        .advice-content h1, .advice-content h2, .advice-content h3 {
          color: #fff;
          margin-top: 20px;
        }
        .advice-content ul {
          padding-left: 20px;
        }
      `}</style>
    </Modal>
  );
};

export default InfrastructureAdviceModal;
