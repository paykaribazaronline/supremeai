// AdminSystemWorkRules.tsx — SYSTEM WORK RULES AUTHORITY MATRIX
// Fetches: GET/POST/PUT/DELETE /api/admin/system-work-rules, POST /seed/defaults, POST /sync/{ruleKey}
import React, { useState, useEffect } from 'react';
import {
  Table, Tag, Space, Button, Typography, message, Modal, Form,
  Input, Select, Badge, Tooltip, Popconfirm, InputNumber
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SyncOutlined
} from '@ant-design/icons';
import { authUtils } from '../lib/authUtils';

const { Text } = Typography;
const { Option } = Select;

interface SystemWorkRuleRow {
  id: string;
  ruleKey: string;
  category: string;
  description: string;
  value: string;
  valueType: string;
  targetDoc: string;
  targetField: string;
  enabled: boolean;
  priority: number;
  lastSyncStatus: string;
  lastSyncedAt: string;
  changeNote: string;
}

const SYNC_COLOR: Record<string, string> = {
  'NO_CONFLICT': '#52c41a',
  'CONFLICT_RESOLVED': '#1890ff',
  'NO_TARGET': '#faad14',
  'DISABLED': '#d9d9d9',
  'FIRESTORE_UNAVAILABLE': '#ff4d4f',
  'PROPAGATION_FAILED': '#ff4d4f',
  'PROPAGATION_PARTIAL': '#faad14',
  'SYNCING': '#1890ff',
  'INVALID_TARGET': '#ff4d4f',
};

function fmtSync(status: string) {
  return (status || '—').replace(/_/g, ' ');
}

const AdminSystemWorkRules: React.FC = () => {
  const [rules, setRules] = useState<SystemWorkRuleRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncingId, setSyncingId] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<SystemWorkRuleRow | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    const init = async () => {
      await fetchRules();
    };
    init();
  }, []);

  // ─── Fetch all rules ──────────────────────────────────────────────────────

  async function fetchRules() {
    setLoading(true);
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/system-work-rules');
      const body = await res.json();
      if (body.success) {
        const mapped = (body.data || []).map((r: any) => ({
          id: r.id || r.ruleKey,
          ruleKey: r.ruleKey,
          category: r.category,
          description: r.description,
          value: r.value,
          valueType: r.valueType,
          targetDoc: r.targetDoc || '—',
          targetField: r.targetField || '—',
          enabled: r.enabled,
          priority: r.priority,
          lastSyncStatus: r.lastSyncStatus || 'NONE',
          lastSyncedAt: r.lastSyncedAt || '',
          changeNote: r.changeNote || '',
        })) as SystemWorkRuleRow[];
        setRules(mapped);
      }
    } catch (e) {
      console.error(e);
      message.error('Failed to load system work rules');
    } finally {
      setLoading(false);
    }
  }

  // ─── CRUD ─────────────────────────────────────────────────────────────────

  const handleSave = async (values: any) => {
    const payload = editing ? values : values;
    const method = editing ? 'PUT' : 'POST';
    const endpoint = editing
      ? `/api/admin/system-work-rules/${editing.id}`
      : '/api/admin/system-work-rules';

    try {
      const res = await authUtils.fetchWithAuth(endpoint, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error('Save failed');
      message.success(editing ? 'Rule updated' : 'Rule created');
      setModalVisible(false);
      setEditing(null);
      form.resetFields();
      fetchRules();
    } catch (e) {
      message.error('Failed to save rule');
    }
  };

  const handleDelete = async (rule: SystemWorkRuleRow) => {
    try {
      const res = await authUtils.fetchWithAuth(`/api/admin/system-work-rules/${rule.id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('Delete failed');
      message.success('Rule deleted');
      fetchRules();
    } catch (e) {
      message.error('Failed to delete rule');
    }
  };

  const handleSync = async (rule: SystemWorkRuleRow) => {
    setSyncingId(rule.id);
    try {
      const res = await authUtils.fetchWithAuth(`/api/admin/system-work-rules/sync/${rule.id}`, {
        method: 'POST'
      });
      if (!res.ok) throw new Error('Sync failed');
      const body = await res.json();
      const status = body.data?.lastSyncStatus || 'UNKNOWN';
      if (status === 'CONFLICT_RESOLVED') {
        message.success(`Conflict resolved: value synced to ${rule.targetDoc}`);
      } else {
        message.info(`Status: ${fmtSync(status)}`);
      }
      fetchRules();
    } catch (e) {
      message.error('Sync failed: ' + e);
    } finally {
      setSyncingId(null);
    }
  };

  const handleSeed = async () => {
    try {
      const res = await authUtils.fetchWithAuth('/api/admin/system-work-rules/seed/defaults', { method: 'POST' });
      if (!res.ok) throw new Error('Seed failed');
      message.success('Default rules seeded / verified');
      fetchRules();
    } catch (e) {
      message.error('Failed to seed defaults');
    }
  };

  const openCreateModal = () => { setEditing(null); form.resetFields(); setModalVisible(true); };
  const openEditModal = (record: SystemWorkRuleRow) => {
    setEditing(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  // ─── Columns ──────────────────────────────────────────────────────────────

  const columns = [
    {
      title: 'RULE_KEY',
      dataIndex: 'ruleKey' as const,
      key: 'ruleKey',
      width: 200,
      render: (id: string) => (
        <span className="text-[10px] font-black font-mono text-white">{id}</span>
      ),
    },
    {
      title: 'CATEGORY',
      dataIndex: 'category' as const,
      key: 'category',
      width: 110,
      render: (cat: string) => {
        const colorMap: Record<string, string> = {
          LEARNING: 'cyan', EXECUTION: 'blue', PROTOCOL: 'orange',
          SCHEDULING: 'purple', APPROVAL: 'magenta', GENERAL: 'gray',
          LOGGING: 'green', AUTOMATION: 'gold', SECURITY: 'red'
        };
        return <Tag color={(colorMap as any)[cat] || 'default'}>{cat}</Tag>;
      },
    },
    {
      title: 'DESCRIPTION',
      dataIndex: 'description' as const,
      key: 'description',
      ellipsis: true,
      render: (t: string) => <span className="text-[10px] text-white/60 truncate max-w-[200px]">{t}</span>,
    },
    {
      title: 'VALUE',
      dataIndex: 'value' as const,
      key: 'value',
      width: 130,
      render: (v: string, r: SystemWorkRuleRow) => (
        <Tooltip title={`Target: ${r.targetDoc} → ${r.targetField}`}>
          <Badge
            status={(r.lastSyncStatus === 'CONFLICT_RESOLVED' || r.lastSyncStatus === 'NO_CONFLICT') ? 'success' : 'default'}
            text={<span className="text-[10px] font-mono">{v}</span>}
          />
        </Tooltip>
      ),
    },
    {
      title: 'TARGET',
      dataIndex: 'targetDoc' as const,
      key: 'targetDoc',
      width: 200,
      render: (t: string) => (
        <span className="text-[9px] font-mono text-white/40 truncate max-w-[180px] block" title={t}>{t}</span>
      ),
    },
    {
      title: 'STATUS',
      dataIndex: 'lastSyncStatus' as const,
      key: 'lastSyncStatus',
      width: 170,
      render: (status: string) => {
        const color = SYNC_COLOR[status] || '#d9d9d9';
        return (
          <Tooltip title={`Last synced: ${status}`}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <div style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: color }} />
              <span className="text-[9px]" style={{ color, fontWeight: 700 }}>
                {fmtSync(status)}
              </span>
            </div>
          </Tooltip>
        );
      },
    },
    {
      title: 'ACTIONS',
      key: 'actions',
      width: 200,
      render: (_: any, record: SystemWorkRuleRow) => (
        <Space size="small">
          <Tooltip title="Manual Sync">
            <Button
              size="small"
              icon={<SyncOutlined spin={syncingId === record.id} />}
              onClick={() => handleSync(record)}
              style={{ background: 'rgba(0,243,255,0.08)', borderColor: 'rgba(0,243,255,0.2)', color: '#00f3ff' }}
            >
              Sync
            </Button>
          </Tooltip>
          <Tooltip title="Edit">
            <Button size="small" icon={<EditOutlined />} onClick={() => openEditModal(record)} />
          </Tooltip>
          <Popconfirm title="Delete this rule permanently?" okText="Delete" cancelText="Cancel" onConfirm={() => handleDelete(record)}>
            <Tooltip title="Delete">
              <Button size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      {/* ── Header ──────────────────────────────────────────────────────────── */}
      <div style={{
        marginBottom: 24,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center'
      }}>
        <div>
          <Typography.Title level={2} style={{ margin: 0, fontWeight: 800 }}>
            সিস্টেম ওয়ার্ক রুলস
          </Typography.Title>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Authoritative system work rules — supersede all per-module settings in real-time.
            Last updated rules automatically propagate to the matched Firestore document.
          </Text>
        </div>
        <Space>
          <Button
            type="primary"
            icon={<SyncOutlined />}
            onClick={handleSeed}
            style={{ background: '#722ed1', borderColor: '#722ed1' }}
          >
            Seed Defaults
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={openCreateModal}
          >
            New Rule
          </Button>
        </Space>
      </div>

      {/* ── Rule matrix table ────────────────────────────────────────────────── */}
      <div style={{
        background: 'rgba(255,255,255,0.02)',
        border: '1px solid rgba(255,255,255,0.06)',
        borderRadius: 12,
        overflow: 'hidden'
      }}>
        <Table
          loading={loading}
          columns={columns}
          dataSource={rules}
          rowKey="id"
          pagination={{ pageSize: 20 }}
          size="middle"
          scroll={{ x: 1100 }}
          style={{ background: 'transparent', color: '#fff' }}
        />
      </div>

      {/* ── Decision-audit panel for last selected rule ──────────────────────── */}
      {editing && rules.find(r => r.ruleKey === editing?.ruleKey)?.lastSyncStatus === 'CONFLICT_RESOLVED' && (
        <div style={{
          marginTop: 16,
          padding: 12,
          background: 'rgba(24,144,255,0.06)',
          border: '1px solid rgba(24,144,255,0.2)',
          borderRadius: 8,
        }}>
          <Text strong style={{ color: '#1890ff', fontSize: 11, textTransform: 'uppercase', letterSpacing: 1 }}>
            ✔ Conflict resolved — rule value has been propagated to the target Firestore document.
          </Text>
        </div>
      )}

      {/* ── Create / Edit modal ──────────────────────────────────────────────── */}
      <Modal
        title={editing ? 'Edit System Work Rule' : 'Create System Work Rule'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); setEditing(null); form.resetFields(); }}
        onOk={() => form.submit()}
        width={720}
        okText="Save"
        cancelText="Cancel"
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item label="RULE_KEY" name="ruleKey" required
            help="Unique machine identifier, e.g. LEARNING_AUTO_INTERVAL">
            <Input placeholder="RULE_KEY" disabled={!!editing} />
          </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item label="CATEGORY" name="category" required>
              <Select placeholder="Category">
                <Option value="LEARNING">LEARNING</Option>
                <Option value="EXECUTION">EXECUTION</Option>
                <Option value="PROTOCOL">PROTOCOL</Option>
                <Option value="SCHEDULING">SCHEDULING</Option>
                <Option value="APPROVAL">APPROVAL</Option>
                <Option value="LOGGING">LOGGING</Option>
                <Option value="AUTOMATION">AUTOMATION</Option>
                <Option value="SECURITY">SECURITY</Option>
                <Option value="GENERAL">GENERAL</Option>
              </Select>
            </Form.Item>
            <Form.Item label="PRIORITY" name="priority" initialValue={1}>
              <InputNumber min={1} max={10} className="w-full" />
            </Form.Item>
          </div>

          <Form.Item label="DESCRIPTION" name="description" required
            help="Human-readable description shown in this table">
            <Input placeholder="What this rule controls…" />
          </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
            <Form.Item label="VALUE" name="value" required
              help="The effective value the system must apply">
              <Input placeholder="e.g. 30min, true, high…" />
            </Form.Item>
            <Form.Item label="VALUE_TYPE" name="valueType">
              <Select placeholder="Type">
                <Option value="STRING">STRING</Option>
                <Option value="INTEGER">INTEGER</Option>
                <Option value="BOOLEAN">BOOLEAN</Option>
                <Option value="CRON">CRON</Option>
                <Option value="OBJECT">OBJECT</Option>
                <Option value="LIST">LIST</Option>
              </Select>
            </Form.Item>
            <Form.Item label="ENABLED" name="enabled" initialValue={true}>
              <Select style={{ width: '100%' }} allowClear={false}>
                <Option value={true}>Enabled</Option>
                <Option value={false}>Disabled</Option>
              </Select>
            </Form.Item>
            </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 12 }}>
            <Form.Item label="TARGET_DOC" name="targetDoc"
              help="Firestore doc this rule is written to on save. Format: collection/docId">
              <Input placeholder="system_configs/global_settings" />
            </Form.Item>
            <Form.Item label="TARGET_FIELD" name="targetField"
              help="Field inside the doc to update (leave blank = whole doc from structuredValue)">
              <Input placeholder="timeouts.auto_learning_interval_min" />
            </Form.Item>
          </div>

          <Form.Item label="CHANGE_NOTE" name="changeNote"
            help="Explanation of why this rule is being set or changed">
            <Input.TextArea rows={2} placeholder="e.g. Admin changed auto-learning from 2h to 30min…" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminSystemWorkRules;
