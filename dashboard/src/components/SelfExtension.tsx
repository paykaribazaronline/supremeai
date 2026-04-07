import React, { useEffect, useState } from 'react';

interface ExtensionStatus {
    ready: boolean;
    moduleCount?: number;
    lastExtension?: string;
    [key: string]: unknown;
}

interface BatchResult {
    total: number;
    success: number;
    failed: number;
    details?: { requirement: string; status: string }[];
}

const SelfExtension: React.FC = () => {
    const [status, setStatus] = useState<ExtensionStatus | null>(null);
    const [requirement, setRequirement] = useState('');
    const [batchText, setBatchText] = useState('');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<string | null>(null);
    const [batchResult, setBatchResult] = useState<BatchResult | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<'single' | 'batch' | 'status'>('status');

    const getHeaders = () => {
        const token = localStorage.getItem('supremeai_token') || localStorage.getItem('authToken');
        return {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        };
    };

    const fetchStatus = async () => {
        try {
            const res = await fetch('/api/extend/status', { headers: getHeaders() });
            const data = await res.json();
            setStatus(data);
        } catch {
            setStatus(null);
        }
    };

    useEffect(() => { fetchStatus(); }, []);

    const submitRequirement = async () => {
        if (!requirement.trim()) return;
        setLoading(true);
        setResult(null);
        setError(null);
        try {
            const res = await fetch('/api/extend/requirement', {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ requirement }),
            });
            const data = await res.json();
            if (res.ok) {
                setResult(JSON.stringify(data, null, 2));
                setRequirement('');
                fetchStatus();
            } else {
                setError(data.message || data.error || 'Failed');
            }
        } catch {
            setError('Network error');
        } finally {
            setLoading(false);
        }
    };

    const submitBatch = async () => {
        const lines = batchText.split('\n').map((l) => l.trim()).filter(Boolean);
        if (lines.length === 0) return;
        setLoading(true);
        setBatchResult(null);
        setError(null);
        try {
            const res = await fetch('/api/extend/batch', {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ requirements: lines }),
            });
            const data = await res.json();
            if (res.ok) {
                setBatchResult(data);
                setBatchText('');
                fetchStatus();
            } else {
                setError(data.message || data.error || 'Batch failed');
            }
        } catch {
            setError('Network error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: 24 }}>
            <h2>Self-Extension Engine</h2>
            <p style={{ color: '#6B7280', marginBottom: 16 }}>
                Submit requirements — SupremeAI generates its own code and extends itself.
            </p>

            {/* Tab bar */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                {(['status', 'single', 'batch'] as const).map((tab) => (
                    <button
                        key={tab}
                        onClick={() => { setActiveTab(tab); setError(null); }}
                        style={{
                            padding: '10px 20px',
                            borderRadius: 6,
                            border: activeTab === tab ? '2px solid #6366F1' : '1px solid #d1d5db',
                            background: activeTab === tab ? '#EEF2FF' : '#fff',
                            color: activeTab === tab ? '#6366F1' : '#374151',
                            fontWeight: activeTab === tab ? 600 : 400,
                            cursor: 'pointer',
                            textTransform: 'capitalize',
                        }}
                    >
                        {tab === 'single' ? 'Submit Requirement' : tab === 'batch' ? 'Batch Submit' : 'Status'}
                    </button>
                ))}
            </div>

            {/* Status Tab */}
            {activeTab === 'status' && (
                <div style={cardStyle}>
                    <h3>Extension Engine Status</h3>
                    {status ? (
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16, marginTop: 12 }}>
                            <StatBox label="Ready" value={status.ready ? 'Yes' : 'No'} color={status.ready ? '#10B981' : '#EF4444'} />
                            {status.moduleCount !== undefined && <StatBox label="Modules" value={String(status.moduleCount)} color="#6366F1" />}
                            {status.lastExtension && <StatBox label="Last Extension" value={status.lastExtension} color="#F59E0B" />}
                        </div>
                    ) : (
                        <p style={{ color: '#9CA3AF' }}>Loading status...</p>
                    )}
                    <button onClick={fetchStatus} style={{ ...btnStyle, marginTop: 16 }}>Refresh Status</button>
                </div>
            )}

            {/* Single Requirement Tab */}
            {activeTab === 'single' && (
                <div style={cardStyle}>
                    <h3>Submit Single Requirement</h3>
                    <p style={{ color: '#6B7280', fontSize: 13, marginBottom: 12 }}>
                        Example: "create UserAuditService with methods: audit, log, export"
                    </p>
                    <textarea
                        value={requirement}
                        onChange={(e) => setRequirement(e.target.value)}
                        placeholder="Describe the service or feature you need..."
                        rows={4}
                        style={{ ...inputStyle, marginBottom: 12 }}
                    />
                    <button onClick={submitRequirement} disabled={loading} style={{ ...btnStyle, opacity: loading ? 0.6 : 1 }}>
                        {loading ? 'Generating...' : 'Submit Requirement'}
                    </button>
                    {result && (
                        <pre style={codeStyle}>{result}</pre>
                    )}
                </div>
            )}

            {/* Batch Tab */}
            {activeTab === 'batch' && (
                <div style={cardStyle}>
                    <h3>Batch Submit Requirements</h3>
                    <p style={{ color: '#6B7280', fontSize: 13, marginBottom: 12 }}>
                        One requirement per line. SupremeAI will process all of them.
                    </p>
                    <textarea
                        value={batchText}
                        onChange={(e) => setBatchText(e.target.value)}
                        placeholder={'create PaymentService with methods: charge, refund\ncreate ReportService with methods: generate, export, schedule'}
                        rows={6}
                        style={{ ...inputStyle, marginBottom: 12 }}
                    />
                    <button onClick={submitBatch} disabled={loading} style={{ ...btnStyle, opacity: loading ? 0.6 : 1 }}>
                        {loading ? 'Processing Batch...' : 'Submit Batch'}
                    </button>
                    {batchResult && (
                        <div style={{ marginTop: 16 }}>
                            <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
                                <StatBox label="Total" value={String(batchResult.total)} color="#6366F1" />
                                <StatBox label="Success" value={String(batchResult.success)} color="#10B981" />
                                <StatBox label="Failed" value={String(batchResult.failed)} color="#EF4444" />
                            </div>
                            {batchResult.details && (
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                                    <thead>
                                        <tr style={{ background: '#F3F4F6' }}>
                                            <th style={thStyle}>Requirement</th>
                                            <th style={thStyle}>Status</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {batchResult.details.map((d, i) => (
                                            <tr key={i}>
                                                <td style={tdStyle}>{d.requirement}</td>
                                                <td style={tdStyle}>
                                                    <span style={{ color: d.status === 'success' ? '#10B981' : '#EF4444', fontWeight: 600 }}>
                                                        {d.status}
                                                    </span>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    )}
                </div>
            )}

            {error && <div style={{ background: '#FEE2E2', color: '#991B1B', padding: 12, borderRadius: 6, marginTop: 16 }}>{error}</div>}
        </div>
    );
};

const StatBox: React.FC<{ label: string; value: string; color: string }> = ({ label, value, color }) => (
    <div style={{ background: '#F9FAFB', border: '1px solid #e5e7eb', borderRadius: 8, padding: 16, textAlign: 'center' }}>
        <div style={{ fontSize: 12, color: '#6B7280', marginBottom: 4 }}>{label}</div>
        <div style={{ fontSize: 20, fontWeight: 700, color }}>{value}</div>
    </div>
);

const cardStyle: React.CSSProperties = { background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, padding: 24 };
const inputStyle: React.CSSProperties = { width: '100%', padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' };
const btnStyle: React.CSSProperties = { padding: '10px 24px', background: '#6366F1', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' };
const codeStyle: React.CSSProperties = { background: '#1e1e1e', color: '#d4d4d4', padding: 16, borderRadius: 6, overflowX: 'auto', fontSize: 13, maxHeight: 400, marginTop: 16 };
const thStyle: React.CSSProperties = { textAlign: 'left', padding: '8px 12px', borderBottom: '1px solid #e5e7eb' };
const tdStyle: React.CSSProperties = { padding: '8px 12px', borderBottom: '1px solid #e5e7eb' };

export default SelfExtension;
