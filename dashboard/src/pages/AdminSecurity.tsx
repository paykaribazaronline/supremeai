import React, { useState, useEffect } from 'react';
import { Typography, Space, Row, Col, message, Spin, Badge, Button, Breadcrumb, Card } from 'antd';
import { 
  ReloadOutlined, 
  DashboardOutlined, 
  SafetyCertificateOutlined,
  SecurityScanOutlined
} from '@ant-design/icons';
import { fetchWithAuth } from '../lib/authUtils';

// Modular Components
import HealthScoreCard from '../components/security/HealthScoreCard';
import SelfHealingPanel from '../components/security/SelfHealingPanel';
import CyberLearningPanel from '../components/security/CyberLearningPanel';
import SystemAuditPanel from '../components/security/SystemAuditPanel';
import SurveillancePanel from '../components/security/SurveillancePanel';

const { Title, Text } = Typography;

const AdminSecurity: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [healingStatus, setHealingStatus] = useState<any>(null);
  const [systemStats, setSystemStats] = useState<any>(null);
  const [testError, setTestError] = useState('');
  const [fixing, setFixing] = useState(false);
  const [fixResult, setFixResult] = useState<any>(null);
  const [cyberSkills, setCyberSkills] = useState<any[]>([]);
  const [protections, setProtections] = useState<any[]>([]);
  const [auditing, setAuditing] = useState(false);
  const [auditReport, setAuditReport] = useState<any>(null);
  const [learning, setLearning] = useState(false);
  const [learnTopic, setLearnTopic] = useState('');
  const [autoConfig, setAutoConfig] = useState({
    autonomousLearningEnabled: false,
    autonomousAuditEnabled: false
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [healingRes, contractRes, skillsRes, protectRes] = await Promise.all([
        fetchWithAuth('/api/self-healing/status'),
        fetchWithAuth('/api/admin/dashboard/contract'),
        fetchWithAuth('/api/admin/security/cyber/skills'),
        fetchWithAuth('/api/admin/security/cyber/protections')
      ]);

      if (healingRes.ok) {
        setHealingStatus(await healingRes.json());
      }
      if (contractRes.ok) {
        const data = await contractRes.json();
        setSystemStats(data.data?.stats);
      }
      if (skillsRes.ok) {
        const data = await skillsRes.json();
        setCyberSkills(data.data || []);
      }
      if (protectRes.ok) {
        const data = await protectRes.json();
        setProtections(data.data || []);
      }

      const configRes = await fetchWithAuth('/api/admin/security/cyber/config');
      if (configRes.ok) {
        const data = await configRes.json();
        setAutoConfig(data.data);
      }
    } catch (error) {
      console.error('Error fetching security data:', error);
      message.error('ডাটা লোড করতে ব্যর্থ হয়েছে');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 30000); // Auto-refresh every 30s
    return () => clearInterval(interval);
  }, []);

  const handleTestFix = async () => {
    if (!testError.trim()) {
      message.warning('অনুগ্রহ করে একটি এরর মেসেজ লিখুন');
      return;
    }

    setFixing(true);
    try {
      const response = await fetchWithAuth('/api/self-healing/detect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ error: testError })
      });

      if (response.ok) {
        const result = await response.json();
        setFixResult(result);
        message.success('সিস্টেম এররটি বিশ্লেষণ করেছে');
      } else {
        message.error('এরর ডিটেকশন ব্যর্থ হয়েছে');
      }
    } catch (error) {
      message.error('সার্ভার ত্রুটি');
    } finally {
      setFixing(false);
    }
  };

  const handleRunAudit = async () => {
    setAuditing(true);
    try {
      const response = await fetchWithAuth('/api/admin/security/cyber/audit', { method: 'POST' });
      if (response.ok) {
        const result = await response.json();
        setAuditReport(result.data);
        message.success('সেলফ-অডিট সফলভাবে সম্পন্ন হয়েছে');
      }
    } catch (error) {
      message.error('অডিট ব্যর্থ হয়েছে');
    } finally {
      setAuditing(false);
    }
  };

  const handleStartLearning = async () => {
    if (!learnTopic.trim()) return;
    setLearning(true);
    try {
      const response = await fetchWithAuth('/api/admin/security/cyber/learn', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ topic: learnTopic })
      });
      if (response.ok) {
        message.success(`সিস্টেম ডিফেন্স লার্নিং শুরু করেছে: ${learnTopic}`);
        setLearnTopic('');
        fetchData();
      }
    } catch (error) {
      message.error('লার্নিং সাইকেল ব্যর্থ হয়েছে');
    } finally {
      setLearning(false);
    }
  };

  const handleToggleAutonomous = async (type: 'learning' | 'audit', enabled: boolean) => {
    try {
      const payload = type === 'learning' 
        ? { autonomousLearningEnabled: enabled }
        : { autonomousAuditEnabled: enabled };
        
      const response = await fetchWithAuth('/api/admin/security/cyber/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      
      if (response.ok) {
        setAutoConfig(prev => ({
          ...prev,
          ...(type === 'learning' ? { autonomousLearningEnabled: enabled } : { autonomousAuditEnabled: enabled })
        }));
        message.success(`${type === 'learning' ? 'Autonomous Learning' : 'Autonomous Audit'} ${enabled ? 'সক্রিয়' : 'নিষ্ক্রিয়'} করা হয়েছে`);
      }
    } catch (error) {
      message.error('কনফিগারেশন আপডেট করতে ব্যর্থ হয়েছে');
    }
  };

   if (loading && !systemStats) {
     return (
       <div className="loading-fallback">
         <Spin size="large" tip="সিকিউরিটি ডাটা লোড হচ্ছে..." />
       </div>
     );
   }

   const healthScore = systemStats?.systemHealthScore || 100;
   const healthStatus = systemStats?.systemHealthStatus || 'healthy';
   const healthReason = systemStats?.systemHealthReason || "All systems operational";

   return (
     <div className="admin-page">
       {/* Header Section */}
       <div className="admin-header">
         <Breadcrumb separator=">" style={{ marginBottom: 'var(--space-2)', opacity: 0.7 }}>
           <Breadcrumb.Item href=""><DashboardOutlined /> ড্যাশবোর্ড</Breadcrumb.Item>
           <Breadcrumb.Item><SafetyCertificateOutlined /> সিস্টেম প্রোটেকশন</Breadcrumb.Item>
           <Breadcrumb.Item>সিকিউরিটি & রেজিলিয়েন্স</Breadcrumb.Item>
         </Breadcrumb>
         
         <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
           <div>
             <Title level={2} className="admin-title">
               নিরাপত্তা ও স্থিতিশীলতা <span className="admin-badge" style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--success)', borderColor: 'rgba(16, 185, 129, 0.3)' }}>CYBER GUARD</span>
             </Title>
             <Text className="admin-subtitle">
               AI-চালিত স্ব-সura classifiers এবং রেজিলিয়েন্সoutes
             </Text>
           </div>
           <Button 
             type="primary" 
             icon={<ReloadOutlined />} 
             onClick={fetchData}
             loading={loading}
             className="admin-btn-primary"
             style={{ 
               background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
               border: 'none',
               fontWeight: 600,
               boxShadow: '0 4px clamp(12px, 2vw, 20px) rgba(16, 185, 129, 0.3)'
             }}
           >
             রিফ্রেশ
           </Button>
         </div>
        </div>
        {/* Sub-header info */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-4)', flexWrap: 'wrap', gap: 'var(--space-3)' }}>
          <Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 'var(--text-sm)' }}>
            সিস্টেমের নিরাপত্তা স্তর মনিটর করুন এবং অটোমেটেড ডিফেন্স পরিচালনা করুন
          </Text>
          <Space>
            <Badge 
              status="processing" 
              color="#10b981"
              text={<Text style={{ color: '#10b981', fontWeight: 600, fontSize: 'var(--text-xs)' }}>Cyber Guard Active</Text>} 
            />
            <Button 
              type="primary" 
              icon={<ReloadOutlined />} 
              onClick={fetchData} 
              loading={loading}
              className="admin-btn-primary"
              style={{ 
                background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', 
                border: 'none',
                fontWeight: 600,
                boxShadow: '0 4px clamp(12px, 2vw, 20px) rgba(59, 130, 246, 0.3)'
              }}
            >
              রিফ্রেশ ডাটা
            </Button>
          </Space>
        </div>

      {/* Suggestions moved to centralized Approvals tab */}

       <Row gutter={[16, 16]} style={{ marginTop: 'var(--space-5)' }}>
        {/* System Health Score */}
        <Col xs={24} lg={8}>
          <HealthScoreCard 
            healthScore={healthScore} 
            healthStatus={healthStatus} 
            healthReason={healthReason} 
          />
        </Col>

        {/* Self-Healing Status */}
        <Col xs={24} lg={16}>
          <SelfHealingPanel 
            healingStatus={healingStatus}
            testError={testError}
            setTestError={setTestError}
            handleTestFix={handleTestFix}
            fixing={fixing}
            fixResult={fixResult}
          />
        </Col>

        {/* Cyber Learning & Hacking Defense */}
        <Col xs={24} lg={12}>
          <CyberLearningPanel 
            learnTopic={learnTopic}
            setLearnTopic={setLearnTopic}
            onStartLearning={handleStartLearning}
            learning={learning}
            cyberSkills={cyberSkills}
            autonomousLearningEnabled={autoConfig.autonomousLearningEnabled}
            onToggleAutonomous={(enabled) => handleToggleAutonomous('learning', enabled)}
          />
        </Col>

        {/* System Self-Audit */}
        <Col xs={24} lg={12}>
          <SystemAuditPanel 
            onRunAudit={handleRunAudit}
            auditing={auditing}
            auditReport={auditReport}
            protections={protections}
            autonomousAuditEnabled={autoConfig.autonomousAuditEnabled}
            onToggleAutonomous={(enabled) => handleToggleAutonomous('audit', enabled)}
          />
        </Col>

        {/* Surveillance Panel */}
        <Col xs={24}>
          <SurveillancePanel />
        </Col>
      </Row>

      <style>{`
        .glass-card {
          border-radius: 24px !important;
          background: rgba(255,255,255,0.02) !important;
          border: 1px solid rgba(255,255,255,0.08) !important;
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          box-shadow: 0 10px 30px rgba(0,0,0,0.2) !important;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
          overflow: hidden;
        }
        
        .glass-card:hover {
          transform: translateY(-4px);
          background: rgba(255,255,255,0.04) !important;
          border-color: rgba(255,255,255,0.15) !important;
          box-shadow: 0 20px 40px rgba(0,0,0,0.4) !important;
        }

        .ant-breadcrumb-link {
          color: rgba(255,255,255,0.45) !important;
          font-size: 13px !important;
        }
        
        .ant-breadcrumb-link a {
          color: rgba(255,255,255,0.45) !important;
        }
        
        .ant-breadcrumb-link a:hover {
          color: #3b82f6 !important;
        }

        .ant-breadcrumb-separator {
          color: rgba(255,255,255,0.2) !important;
        }

        .ant-typography {
          color: #fff !important;
        }

        /* Sub-component style overrides */
        .ant-card-head {
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          padding: 16px 24px !important;
        }

        .ant-card-head-title {
          font-size: 16px !important;
          font-weight: 700 !important;
          letter-spacing: 0.5px !important;
          text-transform: uppercase !important;
        }

        .ant-statistic-title {
          color: rgba(255,255,255,0.45) !important;
          font-size: 12px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
          margin-bottom: 12px !important;
        }

        .ant-input, .ant-input-affix-wrapper, .ant-input-number {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          color: #fff !important;
          padding: 8px 16px !important;
          transition: all 0.3s ease !important;
        }

        .ant-input:focus, .ant-input-affix-wrapper-focused {
          border-color: #3b82f6 !important;
          box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .ant-btn-primary {
          border-radius: 12px !important;
          font-weight: 600 !important;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
        }

        .ant-btn-primary:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 20px rgba(59, 130, 246, 0.4) !important;
        }
      `}</style>
    </div>
  );
};

export default AdminSecurity;

