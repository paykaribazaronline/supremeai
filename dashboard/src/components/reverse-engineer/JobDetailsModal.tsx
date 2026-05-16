import React, { useState } from 'react';
import { Modal, Space, Typography, Descriptions, Tag, Divider, Row, Col, Card, Button } from 'antd';
import { CodeOutlined, EyeOutlined } from '@ant-design/icons';
import { Job } from './types';

const { Title, Text, Paragraph } = Typography;

interface JobDetailsModalProps {
  visible: boolean;
  job: Job | null;
  onCancel: () => void;
}

const JobDetailsModal: React.FC<JobDetailsModalProps> = ({
  visible,
  job,
  onCancel
}) => {
  const [previewCode, setPreviewCode] = useState('');
  const [previewLang, setPreviewLang] = useState('');

  if (!job) return null;

  return (
    <Modal
      title={<Space><CodeOutlined /> Automation Results & Insights</Space>}
      open={visible}
      onCancel={onCancel}
      width={1000}
      footer={[<Button key="close" onClick={onCancel}>Dismiss</Button>]}
      className="glass-modal"
      destroyOnClose
    >
      <div style={{ maxHeight: '70vh', overflow: 'auto', padding: '0 12px' }}>
        <Descriptions title="Job Metadata" bordered size="small" column={2}>
          <Descriptions.Item label="Target URL">{job.url}</Descriptions.Item>
          <Descriptions.Item label="Task Type">{job.taskType}</Descriptions.Item>
          <Descriptions.Item label="Started">{new Date(job.submittedAt).toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="Status"><Tag color="green">{job.status}</Tag></Descriptions.Item>
        </Descriptions>

        <Divider orientation="left">AI Instructions Executed</Divider>
        <Paragraph className="instruction-box">
          {job.instructions || "Standard Reverse Engineering procedure."}
        </Paragraph>

        {job.results?.connectors && (
          <>
            <Divider orientation="left">Generated Logic & Connectors</Divider>
            <Row gutter={[16, 16]}>
              {Object.entries(job.results.connectors).map(([lang, connector]: [string, any]) => (
                <Col span={12} key={lang}>
                  <Card size="small" title={lang.toUpperCase()} className="code-card">
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <Text code>{connector.filename}</Text>
                      <Button 
                        block 
                        icon={<EyeOutlined />} 
                        onClick={() => {
                          setPreviewCode(connector.code);
                          setPreviewLang(lang);
                        }}
                      >
                        View Implementation
                      </Button>
                    </Space>
                  </Card>
                </Col>
              ))}
            </Row>
          </>
        )}

        {previewCode && (
          <div style={{ marginTop: 24 }}>
            <Title level={5}>Code Output ({previewLang})</Title>
            <pre className="code-preview"><code>{previewCode}</code></pre>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default JobDetailsModal;
