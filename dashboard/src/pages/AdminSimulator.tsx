import React, { useState, useEffect } from 'react';
import { Typography, Card, Button, message, Row, Col, Drawer, Alert } from 'antd';
import { ReloadOutlined, RocketOutlined, MobileOutlined } from '@ant-design/icons';
import { fetchWithAuth } from '../lib/authUtils';
import { useRole } from '../contexts/RoleContext';
import SimulatorPreview from '../components/SimulatorPreview';
import SimulatorStats from '../components/simulator/SimulatorStats';
import SimulationControlCard from '../components/simulator/SimulationControlCard';
import DeploymentHistoryTable from '../components/simulator/DeploymentHistoryTable';
import QuotaAdminCard from '../components/simulator/QuotaAdminCard';
import { DeploymentRecord, Project, SimulatorStatsData } from '../components/simulator/types';

const { Title } = Typography;

const AdminSimulator: React.FC = () => {
  const { isAdmin, isGuest, user } = useRole();
  const [loading, setLoading] = useState(false);
  const [deployments, setDeployments] = useState<DeploymentRecord[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedAppId, setSelectedAppId] = useState<string | undefined>();
  const [previewOpen, setPreviewOpen] = useState(false);
  const [stats, setStats] = useState<SimulatorStatsData>({
    totalDeployments: 0,
    activeSessions: 0
  });

  const fetchData = async () => {
    if (isGuest) {
      setLoading(false);
      return;
    }
    
    setLoading(true);
    try {
      const endpoint = isAdmin ? '/api/simulator/admin/usage' : '/api/simulator/installed';
      const usageRes = await fetchWithAuth(endpoint);
      
      if (usageRes.ok) {
        const data = await usageRes.json();
        if (isAdmin) {
          const records = data.deployments || [];
          setDeployments(records);
          setStats({
            totalDeployments: data.totalDeployments || 0,
            activeSessions: records.filter((d: any) => d.status === 'RUNNING').length
          });
        } else {
          const apps = data.installedApps || [];
          const mapped = apps.map((a: any) => ({
            appId: a.appId,
            deviceType: a.deviceType || 'UNKNOWN',
            previewUrl: a.previewUrl,
            status: a.status,
            deployedAt: a.installedAt
          }));
          setDeployments(mapped);
          setStats({
            totalDeployments: apps.length,
            activeSessions: mapped.filter((d: any) => d.status === 'RUNNING').length
          });
        }
      }

      const projectsRes = await fetchWithAuth('/api/projects');
      if (projectsRes.ok) {
        const data = await projectsRes.json();
        setProjects(data.projects || []);
      }
    } catch (error) {
      console.error('Error fetching simulator data:', error);
      message.error('সার্ভারের সাথে যোগাযোগ বিচ্ছিন্ন');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [isAdmin, isGuest]);

  const handleSimulate = (appId: string) => {
    setSelectedAppId(appId);
    setPreviewOpen(true);
  };

  const handleSetQuota = async (userId: string, quota: number) => {
    try {
      const response = await fetchWithAuth(`/api/simulator/admin/set-quota/${userId}?quota=${quota}`, {
        method: 'POST'
      });
      if (response.ok) {
        message.success('কোটা সফলভাবে আপডেট করা হয়েছে');
        fetchData();
      } else {
        message.error('কোটা আপডেট করতে ব্যর্থ হয়েছে');
      }
    } catch (error) {
      message.error('সার্ভার ত্রুটি');
    }
  };

  return (
    <div style={{ padding: 24, background: '#0a0a0c', minHeight: '100vh' }}>
      {isGuest && (
        <Alert
          message="গেস্ট মোড প্রিভিউ"
          description="সিমুলেটর ম্যানেজমেন্টের অ্যাডভান্সড ফিচারগুলো ব্যবহারের জন্য অনুগ্রহ করে লগইন করুন।"
          type="warning"
          showIcon
          style={{ marginBottom: 24, borderRadius: 12, background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.2)' }}
          action={
            <Button size="small" type="primary" onClick={() => window.location.href = '/login'}>
              লগইন করুন
            </Button>
          }
        />
      )}

      <Row gutter={[24, 24]}>
        <Col span={16}>
          <Title level={2} style={{ marginBottom: 24, fontWeight: 700, color: '#fff' }}>
            সিমুলেটর ম্যানেজমেন্ট
          </Title>

          <SimulatorStats stats={stats} />

          <SimulationControlCard 
            projects={projects}
            selectedAppId={selectedAppId}
            onSelectAppId={setSelectedAppId}
            onOpenSimulator={() => setPreviewOpen(true)}
          />

          <Card
            className="glass-card"
            style={{ 
              borderRadius: 16, 
              marginTop: 24,
              background: 'rgba(255,255,255,0.02)', 
              border: '1px solid rgba(255,255,255,0.1)'
            }}
            title={<span style={{ color: '#fff' }}><MobileOutlined /> রিসেন্ট সেশনসমূহ</span>}
            extra={<Button icon={<ReloadOutlined />} onClick={fetchData} loading={loading} type="text" style={{ color: '#fff' }}>রিফ্রেশ</Button>}
          >
            <DeploymentHistoryTable 
              deployments={deployments}
              loading={loading}
              onRefresh={fetchData}
              onSimulate={handleSimulate}
            />
          </Card>
        </Col>

        <Col span={8}>
          <SimulatorPreview appId={selectedAppId} />
          
          {isAdmin && (
            <QuotaAdminCard onSetQuota={handleSetQuota} />
          )}
        </Col>
      </Row>

      <Drawer
        title={<span style={{ color: '#fff' }}>সিমুলেটর প্রিভিউ: {selectedAppId}</span>}
        placement="right"
        width="80%"
        onClose={() => setPreviewOpen(false)}
        open={previewOpen}
        bodyStyle={{ background: '#000', padding: 0 }}
        headerStyle={{ background: '#1a1a1a', borderBottom: '1px solid rgba(255,255,255,0.1)' }}
      >
        <div style={{ height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center', background: '#000' }}>
          <SimulatorPreview appId={selectedAppId} />
        </div>
      </Drawer>

      <style>{`
        .admin-table-dark .ant-table {
          background: transparent !important;
          color: #fff !important;
        }
        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.03) !important;
          color: rgba(255,255,255,0.45) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
        }
        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
        }
        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.05) !important;
        }
        .glass-card {
          transition: transform 0.2s ease, box-shadow 0.2s ease;
          box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }
        .glass-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 24px rgba(0,0,0,0.2);
        }
      `}</style>
    </div>
  );
};

export default AdminSimulator;

