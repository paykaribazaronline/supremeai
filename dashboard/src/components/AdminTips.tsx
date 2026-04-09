// AdminTips.tsx - Helpful tips and guidance for admin dashboard users

import React, { useState } from 'react';
import {
    Card,
    Collapse,
    Alert,
    Button,
    Modal,
    Space,
    Row,
    Col,
    Tag,
    Tooltip,
    Drawer,
    List,
    Divider,
} from 'antd';
import {
    QuestionCircleOutlined,
    LightbulbOutlined,
    BookOutlined,
    PlayCircleOutlined,
    CheckCircleOutlined,
} from '@ant-design/icons';

interface TipItem {
    title: string;
    description: string;
    icon?: React.ReactNode;
    category: string;
    difficulty: 'beginner' | 'intermediate' | 'advanced';
}

interface AdminTipsProps {
    section?: string;
}

const AdminTips: React.FC<AdminTipsProps> = ({ section }) => {
    const [showTips, setShowTips] = useState(false);
    const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

    const tips: TipItem[] = [
        // ========================================
        // DASHBOARD & OVERVIEW
        // ========================================
        {
            title: 'Understanding Your Dashboard Overview',
            description:
                'The Dashboard shows you at a glance: (1) How many AI agents are running, (2) How many tasks are in progress, (3) How many tasks are completed, (4) Your system health score (0-100, higher is better), and (5) Success rate of completed tasks. Refresh your data every 30 seconds to stay updated.',
            category: 'Dashboard',
            difficulty: 'beginner',
        },
        {
            title: 'System Health Status Colors',
            description:
                '🟢 GREEN (Healthy): Everything is working perfectly. 🟡 YELLOW (Warning): Some issues detected but system is still functioning. 🔴 RED (Critical): Immediate attention required. Action is needed to prevent system failure.',
            category: 'Dashboard',
            difficulty: 'beginner',
        },

        // ========================================
        // API KEYS & MODELS
        // ========================================
        {
            title: 'What are API Keys?',
            description:
                'API Keys are like passwords for AI services (OpenAI, Google, Hugging Face, etc.). They allow SupremeAI to use these services. Never share your keys publicly. Keep them secret and secure.',
            category: 'API Keys & Models',
            difficulty: 'beginner',
        },
        {
            title: 'How to Add a New API Key',
            description:
                'Step 1: Go to "API Keys & Models" → "Manage API Keys". Step 2: Click "Add New Key". Step 3: Select the provider (OpenAI, Claude, etc.). Step 4: Paste your API key from that service. Step 5: Set a usage limit (optional). Step 6: Click Save. Your system can now use that service!',
            category: 'API Keys & Models',
            difficulty: 'beginner',
        },
        {
            title: 'Discover New AI Models',
            description:
                'Go to "AI Models & Search" → "Search New Models" to find and add new AI models to your system. This helps you use the latest and most powerful AI systems available.',
            category: 'API Keys & Models',
            difficulty: 'intermediate',
        },
        {
            title: 'Understanding Usage Stats',
            description:
                'Usage Stats show you how much of your API quota you\'ve used. Most APIs charge based on usage (measured in tokens). Monitor this to avoid unexpected charges.',
            category: 'API Keys & Models',
            difficulty: 'beginner',
        },

        // ========================================
        // AI MODELS & ASSIGNMENTS
        // ========================================
        {
            title: 'What are AI Models?',
            description:
                'AI Models are the "brains" of your system. Different models are good at different things. For example: GPT-4 is great for complex reasoning, Claude is good for writing, Gemini is good for understanding images. You can use multiple models together!',
            category: 'AI Models & Search',
            difficulty: 'beginner',
        },
        {
            title: 'How to Assign AI to Tasks',
            description:
                'Step 1: Go to "AI Assignment" → "Assign AI to Tasks". Step 2: Select a task type (e.g., "writing", "analysis", "code review"). Step 3: Choose which AI models should handle it. Step 4: Save. Now your system will automatically use those AI models for those task types.',
            category: 'AI Assignment',
            difficulty: 'intermediate',
        },
        {
            title: 'Workload Balancing',
            description:
                'Workload Balancing ensures that no single AI agent gets overloaded with work. If one AI is busy, tasks are sent to another AI. This keeps your system running smoothly and prevents delays.',
            category: 'AI Assignment',
            difficulty: 'intermediate',
        },

        // ========================================
        // CHAT & COMMANDS
        // ========================================
        {
            title: 'How to Chat with AI',
            description:
                'Go to "Chat & Commands" and type what you want the AI to do. Be clear and specific. For example: "Write a Python function to parse JSON" works better than "write something".',
            category: 'Chat & Commands',
            difficulty: 'beginner',
        },
        {
            title: 'Chat Best Practices',
            description:
                '1. Be specific about what you need. 2. Provide context (e.g., "Write a Python function for" vs "Write for"). 3. Give examples if needed. 4. Ask follow-up questions if the answer isn\'t clear. 5. Rate responses to help the AI learn.',
            category: 'Chat & Commands',
            difficulty: 'beginner',
        },

        // ========================================
        // VOTING & DECISIONS
        // ========================================
        {
            title: 'What is Decision Voting?',
            description:
                'When your system makes important choices, it can ask multiple AIs to vote. This ensures the best decision is made. For example, 5 AIs might vote on whether a piece of code is safe. If 4 say yes, the decision is approved.',
            category: 'Decision & Voting',
            difficulty: 'intermediate',
        },
        {
            title: 'Understanding Vote History',
            description:
                'Vote History shows all past decisions made by your system through voting. Each entry shows what was voted on, how many AIs voted, and what the result was. Use this to understand how your system makes decisions.',
            category: 'Decision & Voting',
            difficulty: 'intermediate',
        },
        {
            title: 'Setting Decision Rules',
            description:
                'Decision Rules let you customize HOW votes are made. Examples: "Require 80% agreement before approving", "Only critical changes need voting", "Always vote on code changes". Rules help maintain safety and control.',
            category: 'Decision & Voting',
            difficulty: 'advanced',
        },

        // ========================================
        // KING MODE
        // ========================================
        {
            title: 'What is King Mode (Override)?',
            description:
                'King Mode lets you override AI decisions directly. Use this when: (1) The AI is stuck, (2) You need to force an action immediately, (3) The situation is critical. Be careful - this bypasses normal safety checks.',
            category: 'King Mode (Override)',
            difficulty: 'advanced',
        },
        {
            title: 'When to Use King Mode',
            description:
                'Use King Mode ONLY when absolutely necessary: Emergency situations, AI system failures, Critical security threats, Time-sensitive decisions. After using King Mode, investigate why the AI failed so it doesn\'t happen again.',
            category: 'King Mode (Override)',
            difficulty: 'advanced',
        },

        // ========================================
        // HEADLESS BROWSER
        // ========================================
        {
            title: 'What is Headless Browser?',
            description:
                'A Headless Browser is an automated web browser (like Chrome without the visible window). It can: Visit websites, Fill out forms, Click buttons, Take screenshots, Collect data. It\'s like a robot that uses the web.',
            category: 'Headless Browser',
            difficulty: 'intermediate',
        },
        {
            title: 'How to Use Web Scraping',
            description:
                'Step 1: Go to "Headless Browser" → "Scraping & Automation". Step 2: Enter the website URL. Step 3: Describe what data you want (e.g., "all product names and prices"). Step 4: The system will visit the site and collect the data. Step 5: Download the results.',
            category: 'Headless Browser',
            difficulty: 'intermediate',
        },
        {
            title: 'Taking Screenshots Automatically',
            description:
                'You can instruct the headless browser to visit any website and take a screenshot. Useful for: monitoring website changes, documenting web content, testing website layouts.',
            category: 'Headless Browser',
            difficulty: 'beginner',
        },

        // ========================================
        // VPN MANAGEMENT
        // ========================================
        {
            title: 'Why Use VPN from Admin Panel?',
            description:
                'VPNs mask your IP address to appear as if you\'re in a different location. Use VPN when: Testing geographically restricted services, Protecting privacy, Accessing blocked content. Always use VPN responsibly and legally.',
            category: 'VPN Management',
            difficulty: 'intermediate',
        },
        {
            title: 'How to Set Up a VPN Connection',
            description:
                'Step 1: Go to "VPN Management" → "VPN Connections". Step 2: Click "Add VPN". Step 3: Choose a region/country. Step 4: Set protocol (usually "OpenVPN"). Step 5: Click Connect. The system will now route through that VPN.',
            category: 'VPN Management',
            difficulty: 'intermediate',
        },

        // ========================================
        // GITHUB INTEGRATION
        // ========================================
        {
            title: 'What is GitHub Integration?',
            description:
                'GitHub Integration lets SupremeAI work directly with your GitHub repositories. It can: Review code, Create commits, Run workflows, Manage issues and pull requests. This automates your development workflow.',
            category: 'GitHub Integration',
            difficulty: 'intermediate',
        },
        {
            title: 'Setting Up GitHub Integration',
            description:
                'Step 1: Go to "GitHub Integration". Step 2: Click "Connect GitHub Account". Step 3: Authorize SupremeAI to access your repositories. Step 4: Select which repositories to integrate. Step 5: Done! Now the AI can work with your code.',
            category: 'GitHub Integration',
            difficulty: 'intermediate',
        },
        {
            title: 'Viewing Workflow Runs',
            description:
                'Go to "GitHub Integration" → "Workflows & Runs". Here you can see: All GitHub Actions workflows, Their current status (running, completed, failed), When they started and finished, The results and outputs.',
            category: 'GitHub Integration',
            difficulty: 'beginner',
        },

        // ========================================
        // PROGRESS TRACKING
        // ========================================
        {
            title: 'Understanding Work Progress',
            description:
                'Work Progress shows all tasks your system is working on: What tasks are being done, Who (which AI) is doing them, How much progress (0-100%), Estimated time remaining.',
            category: 'Progress Tracking',
            difficulty: 'beginner',
        },
        {
            title: 'Monitoring AI Improvements',
            description:
                'AI Improvements show how your AI system is getting better over time. Track: New capabilities learned, Performance improvements, Bug fixes, Overall AI version and quality score.',
            category: 'Progress Tracking',
            difficulty: 'beginner',
        },
        {
            title: 'Viewing AI Decisions',
            description:
                'AI Decisions shows every major decision your AI system made: When it was made, What decision was made, Why the AI chose that, The outcome (was it correct?). Use this to understand AI behavior.',
            category: 'Progress Tracking',
            difficulty: 'intermediate',
        },

        // ========================================
        // AUDIT & LOGS
        // ========================================
        {
            title: 'What are Audit Logs?',
            description:
                'Audit Logs record EVERYTHING that happens in your system. Who did what, when they did it, and what the result was. This is crucial for: Security, Troubleshooting, Compliance, Understanding what went wrong.',
            category: 'Audit & Logs',
            difficulty: 'beginner',
        },
        {
            title: 'How to Search Logs',
            description:
                'Go to "Audit & Logs". You can filter by: Date range, User, Action type, Result (success/failure), System component. Logs help you investigate problems and understand what happened.',
            category: 'Audit & Logs',
            difficulty: 'intermediate',
        },

        // ========================================
        // SYSTEM LEARNING
        // ========================================
        {
            title: 'What is System Learning?',
            description:
                'System Learning means your AI system learns from its experience. It improves by: Analyzing past decisions, Learning what works and what doesn\'t, Updating its knowledge, Getting smarter over time. This makes it more effective automatically.',
            category: 'AI System Learning',
            difficulty: 'intermediate',
        },

        // ========================================
        // GENERAL TIPS
        // ========================================
        {
            title: 'First Steps for New Admins',
            description:
                'Step 1: Review Dashboard to understand current status. Step 2: Set up API Keys for services you want to use. Step 3: Configure AI Assignments for your needs. Step 4: Test with Chat & Commands. Step 5: Monitor with Audit Logs. Step 6: Adjust settings as needed.',
            category: 'Getting Started',
            difficulty: 'beginner',
        },
        {
            title: 'System Maintenance Tips',
            description:
                '1. Check Dashboard daily for health issues. 2. Review Audit Logs weekly for suspicious activity. 3. Update API Keys before they expire. 4. Monitor cost/usage to avoid over-spending. 5. Backup important configurations. 6. Keep documentation updated.',
            category: 'Getting Started',
            difficulty: 'intermediate',
        },
        {
            title: 'Troubleshooting Common Issues',
            description:
                'Problem 1: "AI Taking Too Long" → Check workload balance and available models. Problem 2: "API Key Invalid" → Regenerate key from service provider. Problem 3: "Task Failed" → Check Audit Logs for error details. Problem 4: "System Slow" → Check health score and active agents.',
            category: 'Getting Started',
            difficulty: 'intermediate',
        },
    ];

    const categories = Array.from(new Set(tips.map((t) => t.category)));

    const getFilteredTips = () => {
        let filtered = tips;
        if (selectedCategory) {
            filtered = filtered.filter((t) => t.category === selectedCategory);
        }
        return filtered;
    };

    const getDifficultyColor = (difficulty: string) => {
        switch (difficulty) {
            case 'beginner':
                return 'green';
            case 'intermediate':
                return 'blue';
            case 'advanced':
                return 'red';
            default:
                return 'default';
        }
    };

    return (
        <div>
            {/* Floating Help Button */}
            <Tooltip title="Click for help and tips">
                <Button
                    type="primary"
                    shape="circle"
                    icon={<QuestionCircleOutlined />}
                    size="large"
                    onClick={() => setShowTips(true)}
                    style={{
                        position: 'fixed',
                        bottom: 30,
                        right: 30,
                        width: 60,
                        height: 60,
                        zIndex: 1000,
                        fontSize: 24,
                    }}
                />
            </Tooltip>

            {/* Tips Drawer */}
            <Drawer
                title={
                    <Space>
                        <LightbulbOutlined style={{ color: '#faad14' }} />
                        <span>Tips & Help Guide</span>
                    </Space>
                }
                placement="right"
                onClose={() => setShowTips(false)}
                open={showTips}
                width={600}
            >
                <Alert
                    message="Welcome to SupremeAI Admin Guide!"
                    description="This guide helps you understand every feature of the admin dashboard. Even if you're new to AI systems, you can master the administrator panel!"
                    type="info"
                    showIcon
                    icon={<BookOutlined />}
                    style={{ marginBottom: 16 }}
                />

                <Divider>Categories</Divider>

                <Space wrap style={{ marginBottom: 16 }}>
                    <Button
                        type={selectedCategory === null ? 'primary' : 'default'}
                        onClick={() => setSelectedCategory(null)}
                        size="small"
                    >
                        All Tips
                    </Button>
                    {categories.map((cat) => (
                        <Button
                            key={cat}
                            type={selectedCategory === cat ? 'primary' : 'default'}
                            onClick={() => setSelectedCategory(cat)}
                            size="small"
                        >
                            {cat}
                        </Button>
                    ))}
                </Space>

                <Divider />

                <List
                    dataSource={getFilteredTips()}
                    renderItem={(tip) => (
                        <List.Item style={{ marginBottom: 16, paddingBottom: 16, borderBottom: '1px solid #f0f0f0' }}>
                            <List.Item.Meta
                                avatar={<LightbulbOutlined style={{ fontSize: 20, color: '#faad14' }} />}
                                title={
                                    <Space>
                                        <span>{tip.title}</span>
                                        <Tag color={getDifficultyColor(tip.difficulty)}>{tip.difficulty}</Tag>
                                    </Space>
                                }
                                description={
                                    <div style={{ whiteSpace: 'pre-wrap', marginTop: 8, lineHeight: 1.6 }}>
                                        {tip.description}
                                    </div>
                                }
                            />
                        </List.Item>
                    )}
                />

                <Divider />

                <Alert
                    message="💡 Pro Tip"
                    description="Every section in the admin panel has a help icon. Click it to get section-specific tips without leaving your current page!"
                    type="success"
                    showIcon
                    icon={<CheckCircleOutlined />}
                />
            </Drawer>
        </div>
    );
};

export default AdminTips;
