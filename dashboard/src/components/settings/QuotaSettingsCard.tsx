import React from 'react';
import { Card, Table, Input, Tag } from 'antd';
import { SystemConfig } from './types';

interface QuotaSettingsCardProps {
  config: SystemConfig;
  onUpdateValue: (field: keyof SystemConfig, key: string, value: any) => void;
}

const QuotaSettingsCard: React.FC<QuotaSettingsCardProps> = ({ config, onUpdateValue }) => {
  const tiers = ['GUEST', 'FREE', 'BASIC', 'PRO', 'ENTERPRISE', 'ADMIN'];

  const quotaData = tiers.map((tier) => ({
    tier,
    quota: config.tierQuotas?.[tier] ?? 0,
    maxApis: config.tierMaxApis?.[tier] ?? 0,
  }));

  const columns = [
    {
      title: 'Subscription Tier',
      dataIndex: 'tier',
      key: 'tier',
      render: (tier: string) => <Tag color="cyan" style={{ borderRadius: '4px', fontWeight: 600 }}>{tier}</Tag>,
    },
    {
      title: 'Monthly Tokens/Quota',
      dataIndex: 'quota',
      key: 'quota',
      render: (quota: number, record: any) => (
        <Input
          type="number"
          defaultValue={quota}
          onBlur={(e) => onUpdateValue('tierQuotas', record.tier, Number(e.target.value))}
          style={{ width: 140, borderRadius: '6px' }}
        />
      ),
    },
    {
      title: 'Max API Keys',
      dataIndex: 'maxApis',
      key: 'maxApis',
      render: (maxApis: number, record: any) => (
        <Input
          type="number"
          defaultValue={maxApis}
          onBlur={(e) => onUpdateValue('tierMaxApis', record.tier, Number(e.target.value))}
          style={{ width: 120, borderRadius: '6px' }}
        />
      ),
    },
  ];

  return (
    <Card className="glass-card" style={{ marginTop: 16, borderRadius: '12px' }} title="Resource Quotas & Limits">
      <p style={{ opacity: 0.7, marginBottom: 20 }}>Manage operational limits for each user tier. Changes are applied automatically on blur.</p>
      <Table
        columns={columns}
        dataSource={quotaData}
        rowKey="tier"
        pagination={false}
        className="glass-table"
        size="middle"
      />
    </Card>
  );
};

export default QuotaSettingsCard;
