import React, { useEffect, useState } from 'react';
import { Avatar, Tooltip, Badge, Space } from 'antd';
import { UserOutlined } from '@ant-design/icons';

const CollaborationBadge: React.FC = () => {
    const [activeUsers, setActiveUsers] = useState<any[]>([]);

    useEffect(() => {
        // Simulate real-time collaboration presence
        setActiveUsers([
            { name: 'Developer A', color: '#4285F4', active: true },
            { name: 'AI Assistant', color: '#34A853', active: true },
            { name: 'Admin', color: '#EA4335', active: true },
        ]);
    }, []);

    return (
        <Space size={-8}>
            {activeUsers.map((user, idx) => (
                <Tooltip key={idx} title={user.name}>
                    <Badge dot status="success" offset={[-4, 32]}>
                        <Avatar 
                            style={{ 
                                backgroundColor: user.color, 
                                border: '2px solid white',
                                cursor: 'pointer'
                            }} 
                            icon={<UserOutlined />} 
                        />
                    </Badge>
                </Tooltip>
            ))}
            <div style={{ marginLeft: '12px', fontSize: '12px', color: '#6B7280', fontWeight: 500 }}>
                {activeUsers.length} active in Studio
            </div>
        </Space>
    );
};

export default CollaborationBadge;
