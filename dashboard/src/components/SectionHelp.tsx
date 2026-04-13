// SectionHelp.tsx - Contextual help component for admin dashboard sections

import React, { useState } from 'react';
import {
    Alert,
    Button,
    Card,
    Popover,
    Space,
    Tag,
    Collapse,
    Tooltip,
} from 'antd';
import {
    QuestionCircleOutlined,
    BulbOutlined as LightbulbOutlined,
    PlayCircleOutlined,
    BookOutlined,
} from '@ant-design/icons';

interface SectionHelpProps {
    title: string;
    tips: string[];
    steps?: string[];
    bestPractices?: string[];
    warnings?: string[];
    relatedSections?: string[];
    difficulty?: 'beginner' | 'intermediate' | 'advanced';
}

const SectionHelp: React.FC<SectionHelpProps> = ({
    title,
    tips,
    steps,
    bestPractices,
    warnings,
    relatedSections,
    difficulty = 'intermediate',
}) => {
    const [showHelp, setShowHelp] = useState(false);

    const getDifficultyInfo = () => {
        const difficultyMap = {
            beginner: { color: 'green', emoji: '👶', text: 'Easy - New users can do this' },
            intermediate: { color: 'blue', emoji: '📚', text: 'Intermediate - Some experience needed' },
            advanced: { color: 'red', emoji: '🚀', text: 'Advanced - Expert users only' },
        };
        return difficultyMap[difficulty];
    };

    const difficultyInfo = getDifficultyInfo();

    const helpContent = (
        <div style={{ maxWidth: 400, maxHeight: 500, overflow: 'auto' }}>
            <Space direction="vertical" style={{ width: '100%' }} size="large">
                {/* Difficulty Badge */}
                <div>
                    <Tag color={difficultyInfo.color}>
                        {difficultyInfo.emoji} {difficultyInfo.text}
                    </Tag>
                </div>

                {/* Quick Tips */}
                {tips.length > 0 && (
                    <div>
                        <h4>
                            <LightbulbOutlined style={{ marginRight: 8, color: '#faad14' }} />
                            Quick Tips
                        </h4>
                        <ul style={{ fontSize: 12, opacity: 0.9 }}>
                            {tips.map((tip, idx) => (
                                <li key={idx} style={{ marginBottom: 4 }}>
                                    {tip}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}

                {/* Step-by-Step Guide */}
                {steps && steps.length > 0 && (
                    <div>
                        <h4>
                            <PlayCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                            How to Use
                        </h4>
                        <ol style={{ fontSize: 12, opacity: 0.9 }}>
                            {steps.map((step, idx) => (
                                <li key={idx} style={{ marginBottom: 4 }}>
                                    {step}
                                </li>
                            ))}
                        </ol>
                    </div>
                )}

                {/* Best Practices */}
                {bestPractices && bestPractices.length > 0 && (
                    <div>
                        <h4>✔️ Best Practices</h4>
                        <ul style={{ fontSize: 12, opacity: 0.9 }}>
                            {bestPractices.map((practice, idx) => (
                                <li key={idx} style={{ marginBottom: 4 }}>
                                    {practice}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}

                {/* Warnings */}
                {warnings && warnings.length > 0 && (
                    <div>
                        <h4 style={{ color: '#d4380d' }}>⚠️ Important Warnings</h4>
                        <ul style={{ fontSize: 12, opacity: 0.9 }}>
                            {warnings.map((warning, idx) => (
                                <li key={idx} style={{ marginBottom: 4, color: '#d4380d' }}>
                                    {warning}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}

                {/* Related Sections */}
                {relatedSections && relatedSections.length > 0 && (
                    <div>
                        <h4>📖 Related Sections</h4>
                        <Space>
                            {relatedSections.map((section, idx) => (
                                <Tag key={idx} color="blue">
                                    {section}
                                </Tag>
                            ))}
                        </Space>
                    </div>
                )}
            </Space>
        </div>
    );

    return (
        <Popover
            content={helpContent}
            title={
                <Space>
                    <QuestionCircleOutlined />
                    <span>{title} - Help</span>
                </Space>
            }
            trigger="click"
            placement="left"
            open={showHelp}
            onOpenChange={setShowHelp}
        >
            <Tooltip title={`Click for help on ${title}`}>
                <Button
                    type="text"
                    icon={<QuestionCircleOutlined />}
                    size="small"
                    style={{ color: '#1890ff', marginLeft: 8 }}
                />
            </Tooltip>
        </Popover>
    );
};

// Helper component to add help section to cards
export const HelpfulCard: React.FC<{
    title: string;
    children: React.ReactNode;
    help: SectionHelpProps;
    [key: string]: any;
}> = ({ title, children, help, ...cardProps }) => {
    return (
        <Card
            title={
                <Space>
                    <span>{title}</span>
                    <SectionHelp {...help} />
                </Space>
            }
            {...cardProps}
        >
            {children}
        </Card>
    );
};

export default SectionHelp;
