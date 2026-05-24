import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Typography, Row, Col, Progress, Table, List, Badge, Card, Statistic, Space } from 'antd';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip,
  ResponsiveContainer, BarChart, Bar, Cell, LineChart, Line
} from 'recharts';
import {
  BellOutlined, GlobalOutlined, DeploymentUnitOutlined,
  SafetyCertificateOutlined, ThunderboltOutlined,
  ScanOutlined, EllipsisOutlined, RiseOutlined,
  DotChartOutlined, BoxPlotOutlined, FireOutlined
} from '@ant-design/icons';

const { Title, Text } = Typography;

interface DashboardHomeProps {
  isAdmin: boolean;
  setActiveKey: (key: string) => void;
}

const networkData = [
  { time: '10:00', load: 30, traffic: 40, latency: 12 },
  { time: '10:05', load: 45, traffic: 50, latency: 15 },
  { time: '10:10', load: 60, traffic: 80, latency: 10 },
  { time: '10:15', load: 50, traffic: 65, latency: 18 },
  { time: '10:20', load: 85, traffic: 95, latency: 12 },
  { time: '10:25', load: 70, traffic: 85, latency: 14 },
  { time: '10:30', load: 90, traffic: 110, latency: 11 },
];

const processData = [
  { name: 'Core Engine', value: 85, color: '#00f3ff' },
  { name: 'Neural Link', value: 40, color: '#bc13fe' },
  { name: 'Data Nexus', value: 65, color: '#00f3ff' },
  { name: 'ML Matrix', value: 95, color: '#bc13fe' },
  { name: 'Edge API', value: 55, color: '#00f3ff' },
  { name: 'Ghost Web', value: 75, color: '#bc13fe' },
];

const eventColumns = [
  {
    title: 'TIMESTAMP',
    dataIndex: 'time',
    key: 'time',
    render: (text: string) => <Text style={{ color: 'var(--text-dim)', fontFamily: 'JetBrains Mono', fontSize: '11px' }}>{text}</Text>
  },
  {
    title: 'SOURCE',
    dataIndex: 'source',
    key: 'source',
    render: (text: string) => <Text style={{ color: '#fff', fontWeight: 600, fontSize: '12px', letterSpacing: '0.5px' }}>{text}</Text>
  },
  {
    title: 'ACTIVITY',
    dataIndex: 'activity',
    key: 'activity',
    render: (text: string) => <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: '12px' }}>{text}</Text>
  },
  {
    title: 'STATUS',
    dataIndex: 'status',
    key: 'status',
    render: (text: string) => (
      <Badge
        status={text === 'Completed' ? 'success' : (text === 'Failed' ? 'error' : 'processing')}
        text={<span style={{
          color: text === 'Completed' ? 'var(--success)' : (text === 'Failed' ? '#ff4d4f' : 'var(--neon-blue)'),
          fontWeight: 700,
          fontSize: '11px',
          textTransform: 'uppercase'
        }}>{text}</span>}
      />
    )
  },
];

const eventData = [
  { key: '1', time: '12.03.2023 13:24:02', source: 'INFRA-CORE-X', activity: 'Initializing neural handshake.', status: 'Inter' },
  { key: '2', time: '12.03.2023 13:22:00', source: 'DB-SYNC-G7', activity: 'Synchronizing encrypted shards.', status: 'Inter' },
  { key: '3', time: '12.03.2023 12:02:00', source: 'NEUROLYNX-E1', activity: 'Deep learning weights updated.', status: 'Completed' },
  { key: '4', time: '12.03.2023 11:45:12', source: 'API-GW-PRIME', activity: 'Burst traffic mitigation active.', status: 'Failed' },
];

const alertData = [
  { icon: <FireOutlined />, color: '#ff4d4f', title: 'Critical Anomaly', desc: 'Sector 7 memory leak detected.' },
  { icon: <ThunderboltOutlined />, color: 'var(--neon-blue)', title: 'Power Surge', desc: 'Distributed nodes rebalancing.' },
  { icon: <SafetyCertificateOutlined />, color: 'var(--success)', title: 'Security Patch', desc: 'Kernel-level firewall updated.' },
];

export const DashboardHome: React.FC<DashboardHomeProps> = ({ isAdmin, setActiveKey }) => {
  const [activeMetric, setActiveMetric] = useState('load');

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
      className="dashboard-home-container"
      style={{ padding: '0 24px 24px', maxWidth: '1600px', margin: '0 auto' }}
    >
      {/* Cinematic Header Section */}
      <div style={{ marginBottom: 32, position: 'relative' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', borderBottom: '1px solid rgba(0, 243, 255, 0.1)', paddingBottom: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 4 }}>
              <Badge status="processing" color="var(--neon-blue)" />
              <Text style={{ color: 'var(--neon-blue)', letterSpacing: 2, fontWeight: 800, fontSize: 12 }}>SYSTEM ONLINE</Text>
            </div>
            <Title level={2} style={{ color: '#fff', margin: 0, fontWeight: 800, fontSize: 32, letterSpacing: -1 }}>
              Command <span className="text-gradient">Center</span>
            </Title>
            <Text style={{ color: 'var(--text-dim)', fontSize: 14 }}>Welcome back, Commander. Neural systems are at 98.4% efficiency.</Text>
          </div>

          <div style={{ textAlign: 'right', display: 'none' }}>
             {/* Large screen stats */}
             <Space size={40}>
               <Statistic
                 title={<span style={{ color: 'var(--text-dim)', fontSize: 12 }}>LATENCY</span>}
                 value={12}
                 suffix="ms"
                 valueStyle={{ color: 'var(--neon-blue)', fontWeight: 800 }}
               />
               <Statistic
                 title={<span style={{ color: 'var(--text-dim)', fontSize: 12 }}>THROUGHPUT</span>}
                 value={1.2}
                 suffix="GB/s"
                 valueStyle={{ color: 'var(--neon-purple)', fontWeight: 800 }}
               />
             </Space>
          </div>
        </div>
      </div>

      <Row gutter={[24, 24]}>
        {/* Main Intelligence Display */}
        <Col xs={24} xl={16}>
          <div className="glass-card" style={{ height: '480px', padding: 0, display: 'flex', flexDirection: 'column' }}>
            <div style={{ padding: '24px 24px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Title level={5} style={{ color: '#fff', margin: 0 }}>Global Neural Activity</Title>
                <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>Real-time stream analysis</Text>
              </div>
              <Space>
                <Button
                  size="small"
                  className={activeMetric === 'load' ? 'cyber-button' : 'glass-action-button'}
                  onClick={() => setActiveMetric('load')}
                >Load</Button>
                <Button
                  size="small"
                  className={activeMetric === 'traffic' ? 'cyber-button' : 'glass-action-button'}
                  onClick={() => setActiveMetric('traffic')}
                >Traffic</Button>
              </Space>
            </div>
            
            <div style={{ flex: 1, marginTop: 20 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={networkData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="mainGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={activeMetric === 'load' ? 'var(--neon-blue)' : 'var(--neon-purple)'} stopOpacity={0.3}/>
                      <stop offset="95%" stopColor={activeMetric === 'load' ? 'var(--neon-blue)' : 'var(--neon-purple)'} stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(255,255,255,0.03)" />
                  <XAxis dataKey="time" hide />
                  <YAxis hide />
                  <RechartsTooltip 
                    contentStyle={{ backgroundColor: 'rgba(13, 13, 18, 0.95)', border: '1px solid var(--neon-blue)', borderRadius: 8, backdropFilter: 'blur(10px)' }}
                    itemStyle={{ color: '#fff' }}
                  />
                  <Area
                    type="monotone"
                    dataKey={activeMetric}
                    stroke={activeMetric === 'load' ? 'var(--neon-blue)' : 'var(--neon-purple)'}
                    fillOpacity={1}
                    fill="url(#mainGradient)"
                    strokeWidth={3}
                    animationDuration={2000}
                  />
                  <Line type="monotone" dataKey="latency" stroke="#fff" strokeWidth={1} strokeDasharray="5 5" dot={false} />
                </AreaChart>
              </ResponsiveContainer>
            </div>

            <div style={{ padding: 24, background: 'rgba(0,0,0,0.2)', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
              <Row gutter={48}>
                <Col span={6}>
                  <Text type="secondary" style={{ fontSize: 11, display: 'block' }}>UPTIME</Text>
                  <Text style={{ color: '#fff', fontSize: 20, fontWeight: 700 }}>99.99%</Text>
                </Col>
                <Col span={6}>
                  <Text type="secondary" style={{ fontSize: 11, display: 'block' }}>ACTIVE NODES</Text>
                  <Text style={{ color: '#fff', fontSize: 20, fontWeight: 700 }}>1,240</Text>
                </Col>
                <Col span={6}>
                  <Text type="secondary" style={{ fontSize: 11, display: 'block' }}>THREAT LEVEL</Text>
                  <Text style={{ color: 'var(--success)', fontSize: 20, fontWeight: 700 }}>MINIMAL</Text>
                </Col>
                <Col span={6}>
                  <Text type="secondary" style={{ fontSize: 11, display: 'block' }}>SYNC STATUS</Text>
                  <Text style={{ color: 'var(--neon-blue)', fontSize: 20, fontWeight: 700 }}>STABLE</Text>
                </Col>
              </Row>
            </div>
          </div>
        </Col>

        {/* System Health Pulse */}
        <Col xs={24} xl={8}>
          <div className="glass-card" style={{ height: '480px', display: 'flex', flexDirection: 'column' }}>
            <div className="glass-card-title">
              Core Vitality
              <DotChartOutlined style={{ color: 'var(--neon-blue)' }} />
            </div>

            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 32 }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13 }}>Neural Stability</Text>
                  <Text style={{ color: 'var(--neon-blue)', fontWeight: 700 }}>98%</Text>
                </div>
                <Progress percent={98} strokeColor="var(--neon-blue)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={6} />
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13 }}>Quantum Throughput</Text>
                  <Text style={{ color: 'var(--neon-purple)', fontWeight: 700 }}>85%</Text>
                </div>
                <Progress percent={85} strokeColor="var(--neon-purple)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={6} />
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13 }}>Synaptic Latency</Text>
                  <Text style={{ color: 'var(--success)', fontWeight: 700 }}>Optimized</Text>
                </div>
                <Progress percent={100} strokeColor="var(--success)" trailColor="rgba(255,255,255,0.05)" showInfo={false} strokeWidth={6} />
              </div>
            </div>

            <div style={{ marginTop: 'auto', padding: '20px', background: 'rgba(255,255,255,0.02)', borderRadius: 12, border: '1px solid rgba(255,255,255,0.05)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div className="pulsing" style={{ width: 12, height: 12, borderRadius: '50%', background: 'var(--success)', boxShadow: '0 0 10px var(--success)' }} />
                <div>
                  <Text style={{ color: '#fff', fontSize: 14, fontWeight: 600, display: 'block' }}>All Systems Operational</Text>
                  <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>Last sweep: 2 seconds ago</Text>
                </div>
              </div>
            </div>
          </div>
        </Col>

        {/* Data Distribution */}
        <Col xs={24} lg={12}>
          <div className="glass-card" style={{ height: '400px' }}>
            <div className="glass-card-title">
              Synaptic Distribution
              <BoxPlotOutlined style={{ color: 'var(--neon-purple)' }} />
            </div>
            <div style={{ height: '300px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={processData} layout="vertical" margin={{ left: 40, right: 40 }}>
                  <XAxis type="number" hide />
                  <YAxis
                    dataKey="name"
                    type="category"
                    stroke="rgba(255,255,255,0.5)"
                    fontSize={12}
                    axisLine={false}
                    tickLine={false}
                  />
                  <RechartsTooltip cursor={{ fill: 'rgba(255,255,255,0.05)' }} contentStyle={{ background: '#000', border: '1px solid var(--neon-blue)' }} />
                  <Bar dataKey="value" radius={[0, 4, 4, 0]} barSize={12}>
                    {processData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </Col>

        {/* Live Event Stream */}
        <Col xs={24} lg={12}>
          <div className="glass-card" style={{ height: '400px', display: 'flex', flexDirection: 'column' }}>
            <div className="glass-card-title">
              System Event Stream
              <Badge count={eventData.length} style={{ backgroundColor: 'var(--neon-blue)' }} />
            </div>
            <div style={{ flex: 1, overflow: 'auto' }}>
              <Table
                dataSource={eventData}
                columns={eventColumns}
                pagination={false}
                size="small"
                style={{ background: 'transparent' }}
                rowClassName="cyber-table-row"
              />
            </div>
          </div>
        </Col>

        {/* Alert Cards */}
        {alertData.map((alert, index) => (
          <Col xs={24} md={8} key={index}>
            <motion.div
              whileHover={{ scale: 1.02 }}
              className="glass-card"
              style={{ padding: 20, borderLeft: `4px solid ${alert.color}` }}
            >
              <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
                <div style={{
                  width: 40, height: 40, borderRadius: 8, background: `${alert.color}15`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: alert.color, fontSize: 20
                }}>
                  {alert.icon}
                </div>
                <div>
                  <Text style={{ color: '#fff', fontWeight: 700, fontSize: 14, display: 'block' }}>{alert.title}</Text>
                  <Text style={{ color: 'var(--text-dim)', fontSize: 12 }}>{alert.desc}</Text>
                </div>
              </div>
            </motion.div>
          </Col>
        ))}
      </Row>

      {/* Footer Decoration */}
      <div style={{ marginTop: 48, textAlign: 'center', opacity: 0.3 }}>
        <Text style={{ color: 'var(--neon-blue)', letterSpacing: 4, fontSize: 10 }}>SUPREME AI ENGINE // BUILD 4.0.2 // QUANTUM READY</Text>
      </div>
    </motion.div>
  );
};
