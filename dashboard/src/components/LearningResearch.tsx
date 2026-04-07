import React, { useState, useEffect, useCallback } from 'react';

interface LearningStats {
    totalLearnings: number;
    errorsResolved: number;
    patternsLearned: number;
    techniquesKnown: number;
}

interface ResearchStats {
    totalResearches: number;
    sourcesScanned: number;
    lastResearchTime: string;
    trendsFound: number;
}

interface CriticalItem {
    id: string;
    requirement: string;
    severity: string;
    category: string;
}

const LearningResearch: React.FC = () => {
    const [learningStats, setLearningStats] = useState<LearningStats | null>(null);
    const [researchStats, setResearchStats] = useState<ResearchStats | null>(null);
    const [criticalItems, setCriticalItems] = useState<CriticalItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [researching, setResearching] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const getToken = () => localStorage.getItem('supremeai_token') || localStorage.getItem('authToken');

    const fetchAll = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const token = getToken();
            const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};

            const [statsRes, researchRes, criticalRes] = await Promise.all([
                fetch('/api/learning/stats', { headers }),
                fetch('/api/learning/research-stats', { headers }),
                fetch('/api/learning/critical', { headers }),
            ]);

            if (statsRes.ok) {
                const data = await statsRes.json();
                setLearningStats(data);
            }
            if (researchRes.ok) {
                const data = await researchRes.json();
                setResearchStats(data);
            }
            if (criticalRes.ok) {
                const data = await criticalRes.json();
                setCriticalItems(Array.isArray(data) ? data : []);
            }
        } catch (err) {
            setError('Failed to load learning data');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchAll();
    }, [fetchAll]);

    const triggerResearch = async () => {
        setResearching(true);
        try {
            const token = getToken();
            const res = await fetch('/api/learning/research-now', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
            });
            if (res.ok) {
                setTimeout(fetchAll, 3000);
            }
        } catch (err) {
            // ignore
        } finally {
            setResearching(false);
        }
    };

    if (loading) {
        return <div style={{ padding: 24, textAlign: 'center' }}>Loading...</div>;
    }

    return (
        <div style={{ padding: 24 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
                <h2 style={{ margin: 0 }}>Learning & Research</h2>
                <div>
                    <button onClick={fetchAll} style={btnStyle}>Refresh</button>
                    <button onClick={triggerResearch} disabled={researching} style={{ ...btnStyle, ...primaryBtnStyle, marginLeft: 8 }}>
                        {researching ? 'Researching...' : 'Research Now'}
                    </button>
                </div>
            </div>

            {error && <div style={errorBanner}>{error}</div>}

            <div style={gridStyle}>
                <StatCard title="Total Learnings" value={learningStats?.totalLearnings ?? 0} color="#6366F1" />
                <StatCard title="Errors Resolved" value={learningStats?.errorsResolved ?? 0} color="#EF4444" />
                <StatCard title="Patterns Learned" value={learningStats?.patternsLearned ?? 0} color="#10B981" />
                <StatCard title="Techniques Known" value={learningStats?.techniquesKnown ?? 0} color="#F59E0B" />
            </div>

            <h3 style={{ marginTop: 24 }}>Internet Research Stats</h3>
            <div style={gridStyle}>
                <StatCard title="Total Researches" value={researchStats?.totalResearches ?? 0} color="#8B5CF6" />
                <StatCard title="Sources Scanned" value={researchStats?.sourcesScanned ?? 0} color="#06B6D4" />
                <StatCard title="Trends Found" value={researchStats?.trendsFound ?? 0} color="#EC4899" />
                <StatCard title="Last Research" value={researchStats?.lastResearchTime ?? 'N/A'} color="#64748B" />
            </div>

            <h3 style={{ marginTop: 24 }}>Critical Requirements ({criticalItems.length})</h3>
            {criticalItems.length === 0 ? (
                <p style={{ color: '#999' }}>No critical items found.</p>
            ) : (
                <table style={tableStyle}>
                    <thead>
                        <tr>
                            <th style={thStyle}>Requirement</th>
                            <th style={thStyle}>Category</th>
                            <th style={thStyle}>Severity</th>
                        </tr>
                    </thead>
                    <tbody>
                        {criticalItems.map((item, i) => (
                            <tr key={item.id || i}>
                                <td style={tdStyle}>{item.requirement}</td>
                                <td style={tdStyle}>{item.category}</td>
                                <td style={tdStyle}>
                                    <span style={{
                                        ...tagStyle,
                                        backgroundColor: item.severity === 'CRITICAL' ? '#FEE2E2' : '#FEF3C7',
                                        color: item.severity === 'CRITICAL' ? '#991B1B' : '#92400E',
                                    }}>
                                        {item.severity}
                                    </span>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
};

const StatCard: React.FC<{ title: string; value: string | number; color: string }> = ({ title, value, color }) => (
    <div style={{
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: 8,
        padding: 16,
        borderLeft: `4px solid ${color}`,
    }}>
        <div style={{ fontSize: 24, fontWeight: 'bold', color }}>{value}</div>
        <div style={{ fontSize: 13, color: '#6B7280', marginTop: 4 }}>{title}</div>
    </div>
);

const gridStyle: React.CSSProperties = { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 };
const btnStyle: React.CSSProperties = { padding: '8px 16px', borderRadius: 6, border: '1px solid #d1d5db', cursor: 'pointer', background: '#fff' };
const primaryBtnStyle: React.CSSProperties = { background: '#6366F1', color: '#fff', border: 'none' };
const errorBanner: React.CSSProperties = { background: '#FEE2E2', color: '#991B1B', padding: 12, borderRadius: 6, marginBottom: 16 };
const tableStyle: React.CSSProperties = { width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: 8, overflow: 'hidden' };
const thStyle: React.CSSProperties = { textAlign: 'left', padding: '10px 12px', borderBottom: '2px solid #e5e7eb', background: '#f9fafb', fontSize: 13 };
const tdStyle: React.CSSProperties = { padding: '10px 12px', borderBottom: '1px solid #f3f4f6', fontSize: 13 };
const tagStyle: React.CSSProperties = { padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600 };

export default LearningResearch;
