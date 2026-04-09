import React, { useState, useEffect, useCallback } from 'react';
import { authUtils } from '../lib/authUtils';

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

interface LearningSettings {
    maxTopicsPerCycle: number;
    cycleIntervalMinutes: number;
    dailyWriteLimit: number;
    dailyReadLimit: number;
    learningEnabled: boolean;
}

const LearningResearch: React.FC = () => {
    const [learningStats, setLearningStats] = useState<LearningStats | null>(null);
    const [researchStats, setResearchStats] = useState<ResearchStats | null>(null);
    const [criticalItems, setCriticalItems] = useState<CriticalItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [researching, setResearching] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [settings, setSettings] = useState<LearningSettings>({
        maxTopicsPerCycle: 3, cycleIntervalMinutes: 5, dailyWriteLimit: 18000, dailyReadLimit: 50000, learningEnabled: true,
    });
    const [draft, setDraft] = useState<LearningSettings>({ ...settings });
    const [savingSettings, setSavingSettings] = useState(false);
    const [settingsMsg, setSettingsMsg] = useState<string | null>(null);

    const getToken = () => authUtils.getToken();

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

            // Fetch learning settings
            try {
                const settingsRes = await fetch('/api/research/settings', { headers });
                if (settingsRes.ok) {
                    const s = await settingsRes.json();
                    setSettings(s);
                    setDraft(s);
                }
            } catch (_) { /* ignore */ }
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

    const saveSettings = async () => {
        setSavingSettings(true);
        setSettingsMsg(null);
        try {
            const token = getToken();
            const res = await fetch('/api/research/settings', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
                body: JSON.stringify(draft),
            });
            if (res.ok) {
                const data = await res.json();
                setSettings({ ...settings, ...data });
                setSettingsMsg('Settings saved successfully!');
            } else {
                const data = await res.json().catch(() => ({}));
                setSettingsMsg(data.error || 'Failed to save');
            }
        } catch (_) {
            setSettingsMsg('Network error');
        } finally {
            setSavingSettings(false);
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

            {/* Learning Settings Control Panel */}
            <div style={limitPanelStyle}>
                <h3 style={{ margin: '0 0 16px 0', fontSize: 16 }}>⚙️ Learning Settings</h3>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 16 }}>
                    <SettingField
                        label="Topics per Cycle"
                        hint="1-50 — how many topics learned each cycle"
                        value={draft.maxTopicsPerCycle}
                        onChange={v => setDraft({ ...draft, maxTopicsPerCycle: v })}
                        min={1} max={50}
                    />
                    <SettingField
                        label="Cycle Interval (min)"
                        hint="1-1440 — minutes between research cycles"
                        value={draft.cycleIntervalMinutes}
                        onChange={v => setDraft({ ...draft, cycleIntervalMinutes: v })}
                        min={1} max={1440}
                    />
                    <SettingField
                        label="Daily Write Limit"
                        hint="Firebase daily write quota cap"
                        value={draft.dailyWriteLimit}
                        onChange={v => setDraft({ ...draft, dailyWriteLimit: v })}
                        min={100} max={1000000}
                    />
                    <SettingField
                        label="Daily Read Limit"
                        hint="Firebase daily read quota cap"
                        value={draft.dailyReadLimit}
                        onChange={v => setDraft({ ...draft, dailyReadLimit: v })}
                        min={100} max={1000000}
                    />
                </div>
                <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
                    <button onClick={saveSettings} disabled={savingSettings} style={{ ...btnStyle, ...primaryBtnStyle }}>
                        {savingSettings ? 'Saving...' : 'Save Settings'}
                    </button>
                    <button onClick={() => setDraft({ ...settings })} style={btnStyle}>Reset</button>
                    {settingsMsg && (
                        <span style={{ fontSize: 13, color: settingsMsg.includes('success') ? '#059669' : '#DC2626' }}>
                            {settingsMsg}
                        </span>
                    )}
                </div>
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

const SettingField: React.FC<{
    label: string; hint: string; value: number;
    onChange: (v: number) => void; min: number; max: number;
}> = ({ label, hint, value, onChange, min, max }) => (
    <div style={{ background: '#fff', borderRadius: 8, padding: 12, border: '1px solid #e5e7eb' }}>
        <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 4 }}>{label}</div>
        <input
            type="number" min={min} max={max} value={value}
            onChange={e => onChange(Number(e.target.value))}
            style={limitInputStyle}
        />
        <div style={{ fontSize: 11, color: '#9CA3AF', marginTop: 4 }}>{hint}</div>
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
const limitPanelStyle: React.CSSProperties = {
    marginTop: 24, background: '#F0F0FF', border: '1px solid #C7D2FE', borderRadius: 8, padding: 16,
};
const limitInputStyle: React.CSSProperties = {
    width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 14, boxSizing: 'border-box',
};

export default LearningResearch;
