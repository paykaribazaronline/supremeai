import React, { useState } from 'react';
import { Upload, message, Card, Typography, List, Tag, Button, Spin, Empty, Space } from 'antd';
import { InboxOutlined, FileImageOutlined, CheckCircleOutlined, SyncOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Dragger } = Upload;
const { Text, Title, Paragraph } = Typography;

interface OCRResult {
    id: string;
    fileName: string;
    text: string;
    confidence: number;
    status: 'processing' | 'completed' | 'error';
}

const AdminOCRCard: React.FC = () => {
    const [results, setResults] = useState<OCRResult[]>([]);
    const [processing, setProcessing] = useState(false);

    const uploadProps = {
        name: 'file',
        multiple: true,
        showUploadList: false,
        customRequest: async (options: any) => {
            const { file, onSuccess, onError } = options;
            setProcessing(true);

            const newResult: OCRResult = {
                id: Math.random().toString(36).substr(2, 9),
                fileName: file.name,
                text: '',
                confidence: 0,
                status: 'processing'
            };

            setResults(prev => [newResult, ...prev]);

            try {
                const token = authUtils.getToken();
                const formData = new FormData();
                formData.append('image', file);

                // Mocking OCR API call
                // In reality: await fetch('/api/ocr/process', { method: 'POST', ... })

                setTimeout(() => {
                    setResults(prev => prev.map(r => r.id === newResult.id ? {
                        ...r,
                        text: 'extracted text from ' + file.name + ' (Bengali text would appear here)',
                        confidence: 0.95,
                        status: 'completed'
                    } : r));
                    setProcessing(false);
                    onSuccess("ok");
                    message.success(`${file.name} processed successfully.`);
                }, 2000);

            } catch (err) {
                console.error('OCR processing failed:', err);
                onError(err);
                setResults(prev => prev.map(r => r.id === newResult.id ? { ...r, status: 'error' } : r));
                setProcessing(false);
                message.error(`${file.name} processing failed.`);
            }
        }
    };

    return (
        <div>
            <Dragger {...uploadProps} style={{ padding: '20px', background: '#F8FAFC', borderRadius: '12px' }}>
                <p className="ant-upload-drag-icon">
                    <InboxOutlined style={{ color: '#7C3AED' }} />
                </p>
                <p className="ant-upload-text">Click or drag image to this area to process OCR</p>
                <p className="ant-upload-hint">
                    Support for single or bulk upload. Automatically detects Bengali text and converts to structured data.
                </p>
            </Dragger>

            {results.length > 0 && (
                <div style={{ marginTop: '24px' }}>
                    <Title level={5}>Recent OCR Results</Title>
                    <List
                        dataSource={results}
                        renderItem={item => (
                            <List.Item
                                extra={
                                    <Space>
                                        {item.status === 'processing' ? <SyncOutlined spin /> : <CheckCircleOutlined style={{ color: '#52c41a' }} />}
                                        <Tag color={item.status === 'completed' ? 'success' : item.status === 'processing' ? 'processing' : 'error'}>
                                            {item.status.toUpperCase()}
                                        </Tag>
                                    </Space>
                                }
                            >
                                <List.Item.Meta
                                    avatar={<FileImageOutlined style={{ fontSize: '24px', color: '#7C3AED' }} />}
                                    title={item.fileName}
                                    description={
                                        <div>
                                            {item.status === 'completed' ? (
                                                <Paragraph ellipsis={{ rows: 2, expandable: true }}>{item.text}</Paragraph>
                                            ) : (
                                                <Text type="secondary">Processing image...</Text>
                                            )}
                                            {item.confidence > 0 && (
                                                <Text type="secondary" style={{ fontSize: '12px' }}>
                                                    Confidence: {Math.round(item.confidence * 100)}%
                                                </Text>
                                            )}
                                        </div>
                                    }
                                />
                            </List.Item>
                        )}
                    />
                </div>
            )}
        </div>
    );
};

export default AdminOCRCard;
