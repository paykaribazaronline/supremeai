// AdminOCR.tsx - Cinematic Bengali OCR Tool
import React, { useState, useRef, useEffect } from 'react';
import { Typography, Row, Col, Space, Button, Badge, Card, Alert, Table, Tag, message, Progress, Spin } from 'antd';
import {
  EyeOutlined,
  UploadOutlined,
  FileTextOutlined,
  ScanOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  TranslationOutlined,
  DownloadOutlined,
  ReloadOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { authUtils } from '../lib/authUtils';
import { useRole } from '../contexts/RoleContext';

const { Title, Text } = Typography;

interface OCRResult {
  key: string;
  id: string;
  filename: string;
  language: string;
  confidence: number;
  status: string;
  createdAt: string;
  textPreview: string;
}

const AdminOCR: React.FC = () => {
  const { isGuest } = useRole();
  const [results, setResults] = useState<OCRResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [scanProgress, setScanProgress] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchHistory = async () => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth('/api/ocr/history');
      if (res.ok) {
        const data = await res.json();
        setResults(data.results || []);
      }
    } catch (err) {
      console.error('Failed to fetch OCR history:', err);
    }
  };

  useEffect(() => { fetchHistory(); }, []);

  const handleScan = async (file?: File) => {
    if (isGuest) return;
    const targetFile = file || (fileInputRef.current?.files?.[0]);
    if (!targetFile) {
      message.warning('Please select a file first');
      return;
    }

    setScanning(true);
    setScanProgress(0);
    const progressInterval = setInterval(() => {
      setScanProgress(prev => Math.min(prev + 10, 90));
    }, 200);

    try {
      const formData = new FormData();
      formData.append('file', targetFile);
      formData.append('language', 'ben');

      const res = await authUtils.fetchWithAuth('/api/ocr/process', {
        method: 'POST',
        body: formData,
      });

      clearInterval(progressInterval);
      setScanProgress(100);

      if (res.ok) {
        const data = await res.json();
        message.success(`OCR completed — ${data.confidence || 0}% confidence`);
        fetchHistory();
      } else {
        message.error('OCR processing failed');
      }
    } catch (err) {
      message.error('Failed to process OCR');
    } finally {
      setScanning(false);
      setScanProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDelete = async (id: string) => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(`/api/ocr/history/${id}`, { method: 'DELETE' });
      if (res.ok) {
        message.success('Result deleted');
        fetchHistory();
      }
    } catch (err) {
      message.error('Failed to delete result');
    }
  };

  const handleExport = async (id: string, format: 'json' | 'xlsx') => {
    if (isGuest) return;
    try {
      const res = await authUtils.fetchWithAuth(`/api/ocr/history/${id}/export?format=${format}`);
      if (res.ok) {
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ocr-result-${id}.${format}`;
        a.click();
        URL.revokeObjectURL(url);
        message.success(`Exported as ${format.toUpperCase()}`);
      }
    } catch (err) {
      message.error('Failed to export');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', render: (t: string) => <Text style={{ color: 'var(--neon-blue)', fontFamily: 'monospace', fontSize: 12 }}>{t}</Text> },
    { title: 'File', dataIndex: 'filename', key: 'filename', render: (t: string) => <Text style={{ color: '#fff' }}>{t}</Text> },
    { title: 'Language', dataIndex: 'language', key: 'language', render: (t: string) => <Tag color="magenta">{t}</Tag> },
    { title: 'Confidence', dataIndex: 'confidence', key: 'confidence', render: (v: number) => <Progress percent={v} size="small" strokeColor="#eb2f96" style={{ width: 100 }} /> },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'COMPLETED' ? 'green' : s === 'PROCESSING' ? 'blue' : 'red'}>{s}</Tag> },
    { title: 'Date', dataIndex: 'createdAt', key: 'createdAt', render: (t: string) => <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>{t}</Text> },
    {
      title: 'Actions', key: 'actions', render: (_: any, record: OCRResult) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => message.info(`Preview: ${record.textPreview?.substring(0, 100)}...`)}>View</Button>
          <Button size="small" icon={<DownloadOutlined />} onClick={() => handleExport(record.id, 'json')}>JSON</Button>
          <Button size="small" icon={<DownloadOutlined />} onClick={() => handleExport(record.id, 'xlsx')}>XLSX</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      style={{ maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid rgba(235, 47, 150, 0.2)', paddingBottom: 24 }}>
        <Row justify="space-between" align="bottom">
          <Col>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <TranslationOutlined style={{ color: '#eb2f96', fontSize: 20 }} />
              <Text style={{ color: '#eb2f96', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>NEURAL VISION</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32 }}>
              Bengali <span style={{ color: '#eb2f96', textShadow: '0 0 10px rgba(235, 47, 150, 0.3)' }}>OCR Engine</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Neural character recognition for complex script analysis and structured data extraction.</Text>
          </Col>
          <Col>
            <Space>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*,.pdf"
                style={{ display: 'none' }}
                onChange={(e) => e.target.files?.[0] && handleScan(e.target.files[0])}
              />
              <Button
                type="primary"
                icon={<UploadOutlined />}
                className="cyber-button"
                style={{ background: '#eb2f96', border: 'none', color: '#fff' }}
                onClick={() => fileInputRef.current?.click()}
                loading={scanning}
              >
                {scanning ? 'Scanning...' : 'Initialize Scan'}
              </Button>
              <Button icon={<ReloadOutlined />} onClick={fetchHistory}>Refresh</Button>
            </Space>
          </Col>
        </Row>
      </div>

      {scanning && (
        <Card className="glass-card" style={{ marginBottom: 24, background: 'rgba(235, 47, 150, 0.05)' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Text style={{ color: '#eb2f96', fontWeight: 700 }}>Processing document...</Text>
            <Progress percent={scanProgress} strokeColor="#eb2f96" />
          </Space>
        </Card>
      )}

      {/* Stats Row */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" style={{ textAlign: 'center' }}>
            <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase', fontWeight: 700 }}>Total Scans</Text>
            <div style={{ color: '#fff', fontSize: 28, fontWeight: 800 }}>{results.length}</div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" style={{ textAlign: 'center' }}>
            <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase', fontWeight: 700 }}>Completed</Text>
            <div style={{ color: '#52c41a', fontSize: 28, fontWeight: 800 }}>{results.filter(r => r.status === 'COMPLETED').length}</div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" style={{ textAlign: 'center' }}>
            <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase', fontWeight: 700 }}>Avg Confidence</Text>
            <div style={{ color: '#eb2f96', fontSize: 28, fontWeight: 800 }}>
              {results.length > 0 ? Math.round(results.reduce((a, b) => a + b.confidence, 0) / results.length) : 0}%
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="glass-card" style={{ textAlign: 'center' }}>
            <Text style={{ color: 'var(--text-dim)', fontSize: 11, textTransform: 'uppercase', fontWeight: 700 }}>Engine</Text>
            <div style={{ color: 'var(--neon-blue)', fontSize: 20, fontWeight: 800 }}>TESSERACT 5+</div>
          </Card>
        </Col>
      </Row>

      {/* Results Table */}
      <Card className="glass-card" title={<><FileTextOutlined /> OCR Scan History</>} extra={<Badge count={results.length} style={{ background: '#eb2f96' }} />}>
        <Table
          columns={columns}
          dataSource={results}
          pagination={{ pageSize: 10 }}
          loading={loading}
          locale={{ emptyText: 'No OCR scans yet. Upload an image or PDF to begin.' }}
        />
      </Card>

      <div className="glass-card" style={{ marginTop: 24, background: 'rgba(0, 243, 255, 0.05)' }}>
         <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
           <ThunderboltOutlined style={{ color: 'var(--neon-blue)' }} />
           <Text style={{ color: 'var(--neon-blue)', fontSize: 11, fontWeight: 800, letterSpacing: 1, textTransform: 'uppercase' }}>
              Quantum Vision Ready. Structured export to .XLSX and .JSON supported upon activation.
           </Text>
         </div>
      </div>
    </motion.div>
  );
};

export default AdminOCR;
