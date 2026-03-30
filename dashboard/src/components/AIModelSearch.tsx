// AIModelSearch.tsx - Search and Manage AI Models

import React, { useState } from 'react';
import { Card, Button, Input, Empty, List, Tag, Row, Col, Spin, message, Modal, Form, Select } from 'antd';
import { SearchOutlined, PlusOutlined, StarOutlined, StarFilled } from '@ant-design/icons';

interface AIModel {
    id: string;
    name: string;
    provider: string;
    capabilities: string[];
    rank: number;
    performance: number;
    accuracy: number;
    costPerRequest: number;
}

const AIModelSearch: React.FC = () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [models, setModels] = useState<AIModel[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedModels, setSelectedModels] = useState<string[]>([]);
    const [isAddModalVisible, setIsAddModalVisible] = useState(false);
    const [form] = Form.useForm();

    const handleSearch = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`/api/models/search?query=${encodeURIComponent(searchQuery)}`, {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setModels(data);
            }
        } catch (error) {
            message.error('Failed to search models');
        } finally {
            setLoading(false);
        }
    };

    const handleAddModel = async (values: any) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch('/api/models/add', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    modelName: values.modelName,
                    providerId: values.providerId,
                    ...values,
                }),
            });

            if (response.ok) {
                message.success('Model added successfully!');
                setIsAddModalVisible(false);
                form.resetFields();
            }
        } catch (error) {
            message.error('Failed to add model');
        }
    };

    return (
        <div>
            <Card title="AI Model Search & Discovery">
                <Row gutter={16} style={{ marginBottom: '24px' }}>
                    <Col xs={24} sm={18}>
                        <Input
                            placeholder="Search models (e.g., 'GPT-4', 'Claude', 'Vision model')"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            onPressEnter={handleSearch}
                            size="large"
                        />
                    </Col>
                    <Col xs={24} sm={6}>
                        <Button type="primary" block size="large" icon={<SearchOutlined />} loading={loading} onClick={handleSearch}>
                            Search
                        </Button>
                    </Col>
                </Row>

                <Spin spinning={loading}>
                    {models.length === 0 && !loading ? (
                        <Empty description="Search to discover latest AI models" />
                    ) : (
                        <List
                            dataSource={models}
                            renderItem={(model) => (
                                <List.Item key={model.id}>
                                    <List.Item.Meta
                                        title={<strong>{model.name}</strong>}
                                        description={`Provider: ${model.provider} | Rank: #${model.rank}`}
                                    />
                                    <div style={{ marginRight: '16px' }}>
                                        {model.capabilities.map((cap) => (
                                            <Tag key={cap} color="blue">
                                                {cap}
                                            </Tag>
                                        ))}
                                    </div>
                                    <div style={{ marginRight: '16px', minWidth: '200px', textAlign: 'right' }}>
                                        <div>Performance: {model.performance}%</div>
                                        <div>Accuracy: {model.accuracy}%</div>
                                        <div>${model.costPerRequest}/req</div>
                                    </div>
                                    <Button
                                        type="primary"
                                        icon={<PlusOutlined />}
                                        onClick={() => {
                                            setSelectedModels([...selectedModels, model.id]);
                                            message.success(`${model.name} added to selection!`);
                                        }}
                                    >
                                        Add
                                    </Button>
                                </List.Item>
                            )}
                        />
                    )}
                </Spin>
            </Card>

            <Card title="Model Performance Analytics" style={{ marginTop: '24px' }}>
                <Row gutter={16}>
                    {selectedModels.length > 0 ? (
                        selectedModels.map((modelId) => {
                            const model = models.find((m) => m.id === modelId);
                            return (
                                <Col key={modelId} xs={24} sm={12} lg={8}>
                                    <Card size="small">
                                        <div><strong>{model?.name}</strong></div>
                                        <div style={{ marginTop: '8px', fontSize: '12px' }}>
                                            Performance: {model?.performance}% <br />
                                            Accuracy: {model?.accuracy}% <br />
                                            Cost: ${model?.costPerRequest}/req
                                        </div>
                                    </Card>
                                </Col>
                            );
                        })
                    ) : (
                        <Empty description="No models selected" />
                    )}
                </Row>
            </Card>
        </div>
    );
};

export default AIModelSearch;
