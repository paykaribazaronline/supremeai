import React from 'react';
import { Tabs, Typography, Timeline, Empty, List, Tag, Alert, Card, Button } from 'antd';
import { 
  HistoryOutlined, 
  BulbOutlined, 
  CodeOutlined, 
  RocketOutlined 
} from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Text, Paragraph, Title } = Typography;
const { TabPane } = Tabs;

interface IntelligenceTabsProps {
  activities: any[];
  findings: any[];
}

const IntelligenceTabs: React.FC<IntelligenceTabsProps> = ({ activities, findings }) => {
  return (
    <Tabs defaultActiveKey="stream" className="custom-tabs-sidebar">
      <TabPane tab={<span><HistoryOutlined /> Intelligence Feed</span>} key="stream">
        <div style={{ maxHeight: '400px', overflowY: 'auto', paddingRight: 8 }}>
          <Timeline
            style={{ marginTop: 16 }}
            items={activities.slice(0, 15).map((act, i) => ({
              color: i === 0 ? '#00f2fe' : 'rgba(255,255,255,0.1)',
              children: (
                <motion.div 
                  initial={{ opacity: 0, x: -10 }} 
                  animate={{ opacity: 1, x: 0 }}
                  style={{ marginBottom: 16 }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span className="action-chip">{act.action.toUpperCase()}</span>
                    <Text style={{ color: 'rgba(255,255,255,0.2)', fontSize: 10 }}>
                      {new Date(act.timestamp).toLocaleTimeString()}
                    </Text>
                  </div>
                  <Paragraph style={{ color: '#fff', fontSize: 13, margin: '8px 0 4px', lineHeight: '1.4' }}>
                    {act.reasoning}
                  </Paragraph>
                  {act.url && <Text style={{ color: '#00f2fe', fontSize: 10, opacity: 0.5 }}>@{new URL(act.url).hostname}</Text>}
                </motion.div>
              )
            }))}
          />
          {activities.length === 0 && <Empty description={<span style={{ color: 'rgba(255,255,255,0.2)' }}>Scanning for activity...</span>} />}
        </div>
      </TabPane>

      <TabPane tab={<span><BulbOutlined /> Data Collected</span>} key="data">
        <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
          {findings.length > 0 ? (
            <List
              dataSource={findings}
              renderItem={(item: any) => (
                <div className="thought-bubble">
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <Text strong style={{ color: '#00f2fe', fontSize: 12 }}>{item.type}</Text>
                    <Tag color="blue" style={{ fontSize: 9, margin: 0 }}>{ (item.confidence * 100).toFixed(0) }% MATCH</Tag>
                  </div>
                  <Paragraph style={{ color: '#fff', margin: 0, fontSize: 13 }}>{item.content}</Paragraph>
                </div>
              )}
            />
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={<span style={{ color: 'rgba(255,255,255,0.2)' }}>No data extracted yet</span>} />
          )}
        </div>
      </TabPane>

      <TabPane tab={<span><CodeOutlined /> Strategic Insights</span>} key="strategic">
        <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
          <Alert 
            message="Autonomous Strategy Engine"
            description="Synthesized blueprints for automated app generation and voting logic."
            type="success"
            showIcon
            style={{ marginBottom: 16, background: 'rgba(39, 201, 63, 0.05)', border: '1px solid rgba(39, 201, 63, 0.2)', color: 'rgba(255,255,255,0.8)' }}
          />
          
          {findings.filter(f => f.type === 'APP_GENERATION' || f.tags?.includes('blueprint')).length > 0 ? (
            <List
              dataSource={findings.filter(f => f.type === 'APP_GENERATION' || f.tags?.includes('blueprint'))}
              renderItem={(item: any) => (
                <Card size="small" style={{ background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(0, 242, 254, 0.1)', marginBottom: 12, borderRadius: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <Title level={5} style={{ color: '#00f2fe', margin: 0, fontSize: 14 }}>{item.title || 'Blueprint Node'}</Title>
                    <Tag color="purple" style={{ fontSize: 9 }}>VOTING READY</Tag>
                  </div>
                  <Paragraph style={{ color: '#fff', fontSize: 12, marginTop: 8 }}>{item.content}</Paragraph>
                  
                  <div style={{ marginTop: 12, padding: '8px 12px', background: 'rgba(255,255,255,0.02)', borderRadius: 8, border: '1px solid rgba(255,255,255,0.05)' }}>
                    <Text style={{ fontSize: 10, color: 'rgba(255,255,255,0.3)', display: 'block', marginBottom: 4 }}>EXECUTION COMMAND</Text>
                    <code style={{ fontSize: 11, color: '#faad14' }}>supremeai start --template {item.title?.toLowerCase().replace(/\s+/g, '-')} --auto</code>
                  </div>

                  <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 12 }}>
                    {item.tags?.map((tag: string) => <Tag key={tag} color="blue" style={{ fontSize: 9, borderRadius: 10 }}>{tag}</Tag>)}
                  </div>
                </Card>
              )}
            />
          ) : (
            <div style={{ textAlign: 'center', padding: '40px 20px' }}>
              <RocketOutlined style={{ fontSize: 32, color: 'rgba(255,255,255,0.1)', marginBottom: 16 }} />
              <Text style={{ color: 'rgba(255,255,255,0.2)', display: 'block' }}>Complete a mission to synthesize strategic blueprints</Text>
              <Button type="link" style={{ marginTop: 8, color: '#00f2fe' }}>Learn more about our Autonomous Engine</Button>
            </div>
          )}
        </div>
      </TabPane>
    </Tabs>
  );
};

export default IntelligenceTabs;
