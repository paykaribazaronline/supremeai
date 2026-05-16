import React, { useState } from 'react';
import { Card, Input, Button, Progress, List, Tag, Typography, Space, Divider, message, Modal, Drawer, Tabs } from 'antd';
import { RocketOutlined, CodeOutlined, RobotOutlined, CheckCircleOutlined, FileTextOutlined, EyeOutlined } from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { TextArea } = Input;
const { Title, Text, Paragraph } = Typography;

const ProjectGenerator: React.FC = () => {
    const [requirement, setRequirement] = useState('');
    const [isGenerating, setIsGenerating] = useState(false);
    const [progress, setProgress] = useState(0);
    const [steps, setSteps] = useState<any[]>([]);
    const [result, setResult] = useState<any>(null);
    const [selectedFile, setSelectedFile] = useState<string | null>(null);
    const [fileContent, setFileContent] = useState<string | null>(null);
    const [isDrawerVisible, setIsDrawerVisible] = useState(false);

    const generateProject = async () => {
        if (!requirement.trim()) {
            message.warning('দয়া করে আপনার প্রোজেক্টের বিবরণ দিন');
            return;
        }

        setIsGenerating(true);
        setProgress(10);
        setSteps([
            { title: 'Analysis (বিশ্লেষণ)', status: 'processing', icon: <RobotOutlined /> },
        ]);

        try {
            const token = authUtils.getToken();
            const response = await fetch('/api/orchestrate/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
                body: JSON.stringify({ requirement }),
            });

            if (!response.ok) throw new Error('Generation failed');

            const data = await response.json();
            
            setProgress(100);
            setSteps([
                { title: 'Analysis (বিশ্লেষণ)', status: 'finish', icon: <CheckCircleOutlined /> },
                { title: 'Extraction (এনটিটি শনাক্তকরণ)', status: 'finish', icon: <CheckCircleOutlined /> },
                { title: 'Code Generation (কোড জেনারেশন)', status: 'finish', icon: <CheckCircleOutlined /> },
                { title: 'Done (সম্পন্ন)', status: 'finish', icon: <CheckCircleOutlined /> }
            ]);

            setResult(data);
            setIsGenerating(false);
            message.success('আপনার প্রোজেক্ট সফলভাবে জেনারেট হয়েছে!');

        } catch (err: any) {
            message.error(err.message);
            setIsGenerating(false);
        }
    };

    const viewFile = (fileName: string, content: string) => {
        setSelectedFile(fileName);
        setFileContent(content);
        setIsDrawerVisible(true);
    };

    return (
        <Card 
            title={
                <Space>
                    <RocketOutlined style={{ color: '#4285F4' }} />
                    <span>SupremeAI Real Generation Engine</span>
                </Space>
            }
            bordered={false}
            style={{ 
                borderRadius: '20px', 
                boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
                background: 'linear-gradient(to bottom, #ffffff, #f9fafb)'
            }}
        >
            <div style={{ marginBottom: '24px' }}>
                <Paragraph strong style={{ fontSize: '16px' }}>
                    আপনি কী ধরণের অ্যাপ্লিকেশন তৈরি করতে চান?
                </Paragraph>
                <TextArea
                    rows={5}
                    placeholder="উদাঃ একটি ই-কমার্স অ্যাপ তৈরি করুন যেখানে ইউজাররা পণ্য কিনতে পারবে এবং অ্যাডমিন ইনভেন্টরি ম্যানেজ করবে।"
                    value={requirement}
                    onChange={(e) => setRequirement(e.target.value)}
                    disabled={isGenerating}
                    style={{ borderRadius: '14px', border: '2px solid #e5e7eb', padding: '12px' }}
                />
                <Button 
                    type="primary" 
                    icon={<RocketOutlined />} 
                    size="large"
                    onClick={generateProject}
                    loading={isGenerating}
                    style={{ 
                        marginTop: '20px', 
                        width: '100%', 
                        borderRadius: '12px', 
                        height: '56px', 
                        fontSize: '18px',
                        fontWeight: 'bold',
                        background: 'linear-gradient(90deg, #4285F4 0%, #34A853 100%)',
                        border: 'none'
                    }}
                >
                    {isGenerating ? 'AI এজেন্টরা আপনার অ্যাপ তৈরি করছে...' : 'জেনারেট করুন (Real App)'}
                </Button>
            </div>

            {isGenerating && (
                <div style={{ marginTop: '32px', textAlign: 'center' }}>
                    <Progress 
                        type="circle"
                        percent={progress} 
                        strokeColor={{ '0%': '#4285F4', '100%': '#34A853' }} 
                    />
                    <List
                        size="small"
                        dataSource={steps}
                        renderItem={item => (
                            <List.Item style={{ border: 'none', justifyContent: 'center' }}>
                                <Space>
                                    {item.icon}
                                    <Text style={{ fontSize: '14px' }}>{item.title}</Text>
                                    {item.status === 'finish' && <CheckCircleOutlined style={{ color: '#34A853' }} />}
                                </Space>
                            </List.Item>
                        )}
                        style={{ marginTop: '24px' }}
                    />
                </div>
            )}

            {result && !isGenerating && (
                <div style={{ marginTop: '32px' }}>
                    <Divider><Tag color="green" style={{ padding: '4px 12px', fontSize: '14px' }}>প্রোজেক্ট রেডি!</Tag></Divider>
                    
                    <Tabs defaultActiveKey="1" items={[
                        {
                            key: '1',
                            label: (<span><RobotOutlined /> Architecture</span>),
                            children: (
                                <Card size="small" style={{ borderRadius: '12px', background: '#f8fafc' }}>
                                    <Title level={5}>Decisions & Stack:</Title>
                                    <Space wrap>
                                        {result.decisions ? Object.entries(result.decisions).map(([key, value]: any) => (
                                            <Tag key={key} color="blue" style={{ borderRadius: '6px' }}>{key.toUpperCase()}: {value}</Tag>
                                        )) : <Tag color="blue">Standard Stack</Tag>}
                                    </Space>
                                    
                                    <Divider style={{ margin: '16px 0' }} />
                                    
                                    <Title level={5}>Identified Entities (এনটিটি):</Title>
                                    <Space wrap>
                                        {result.entities ? result.entities.map((ent: any, idx: number) => (
                                            <Tag key={idx} color="purple" style={{ padding: '4px 12px', borderRadius: '8px' }}>
                                                <strong>{ent.name}</strong>: {ent.fields?.length || 0} Fields
                                            </Tag>
                                        )) : <Text type="secondary">Product (Fallback)</Text>}
                                    </Space>
                                </Card>
                            )
                        },
                        {
                            key: '2',
                            label: (<span><CodeOutlined /> Files (কোড)</span>),
                            children: (
                                <div style={{ 
                                    maxHeight: '400px', 
                                    overflowY: 'auto', 
                                    background: '#1e293b', 
                                    padding: '16px', 
                                    borderRadius: '12px' 
                                }}>
                                    {result.generatedApp?.files ? Object.entries(result.generatedApp.files).map(([fileName, content]: any, i) => (
                                        <div 
                                            key={i} 
                                            onClick={() => viewFile(fileName, content)}
                                            style={{ 
                                                fontSize: '13px', 
                                                fontFamily: 'JetBrains Mono, monospace', 
                                                color: '#e2e8f0',
                                                padding: '8px',
                                                cursor: 'pointer',
                                                display: 'flex',
                                                alignItems: 'center',
                                                borderBottom: '1px solid #334155'
                                            }}
                                            className="file-item-hover"
                                        >
                                            <FileTextOutlined style={{ marginRight: '10px', color: '#94a3b8' }} />
                                            {fileName}
                                            <EyeOutlined style={{ marginLeft: 'auto', color: '#4285F4' }} />
                                        </div>
                                    )) : <Text style={{ color: '#fff' }}>No files found.</Text>}
                                </div>
                            )
                        }
                    ]} />

                    <div style={{ marginTop: '24px', display: 'flex', flexWrap: 'wrap', gap: '16px' }}>
                        {result.github?.previewUrl && (
                            <Button 
                                type="primary" 
                                icon={<RocketOutlined />}
                                size="large"
                                onClick={() => window.open(result.github.previewUrl, '_blank')}
                                style={{ borderRadius: '12px', height: '50px', flex: 1, background: '#34A853', borderColor: '#34A853' }}
                            >
                                লাইভ ওয়েবসাইট দেখুন (Preview)
                            </Button>
                        )}
                        
                        {result.github?.repoUrl && (
                            <Button 
                                type="default" 
                                icon={<CodeOutlined />}
                                size="large"
                                onClick={() => window.open(result.github.repoUrl, '_blank')}
                                style={{ borderRadius: '12px', height: '50px', flex: 1 }}
                            >
                                গিটহাব রিপোজিটরি (Source)
                            </Button>
                        )}

                        {!result.github?.previewUrl && (
                            <Button 
                                type="primary" 
                                block
                                icon={<RocketOutlined />}
                                size="large"
                                onClick={() => window.open(`/api/simulator/runtime/${result.generatedApp?.appId || result.appId}`, '_blank')}
                                style={{ borderRadius: '12px', height: '50px' }}
                            >
                                লোকাল সিমুলেটর দেখুন
                            </Button>
                        )}
                    </div>
                </div>
            )}

            <Drawer
                title={`File: ${selectedFile}`}
                placement="right"
                width={800}
                onClose={() => setIsDrawerVisible(false)}
                open={isDrawerVisible}
                bodyStyle={{ background: '#0f172a', color: '#f8fafc', padding: 0 }}
            >
                <pre style={{ 
                    padding: '24px', 
                    margin: 0, 
                    whiteSpace: 'pre-wrap', 
                    fontFamily: 'JetBrains Mono, monospace',
                    fontSize: '14px',
                    lineHeight: '1.6'
                }}>
                    {fileContent}
                </pre>
            </Drawer>

            <style>{`
                .file-item-hover:hover {
                    background: #334155;
                    border-radius: 6px;
                }
            `}</style>
        </Card>
    );
};

export default ProjectGenerator;
