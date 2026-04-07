import React, { useState } from 'react';

interface TabContent {
    label: string;
    endpoint: string;
    fields: { name: string; label: string; placeholder: string; multiline?: boolean }[];
}

const tabs: TabContent[] = [
    {
        label: 'Create App',
        endpoint: '/api/teaching/create-app',
        fields: [
            { name: 'appName', label: 'App Name', placeholder: 'e.g., UserManagement' },
            { name: 'requirements', label: 'Requirements', placeholder: 'Describe what the app should do...', multiline: true },
            { name: 'techStack', label: 'Tech Stack', placeholder: 'e.g., Spring Boot + React' },
        ],
    },
    {
        label: 'Solve Error',
        endpoint: '/api/teaching/solve-error',
        fields: [
            { name: 'error', label: 'Error Message', placeholder: 'Paste the error message here...', multiline: true },
            { name: 'context', label: 'Context', placeholder: 'What were you doing when this error occurred?' },
            { name: 'language', label: 'Language/Framework', placeholder: 'e.g., Java, Python, Flutter' },
        ],
    },
    {
        label: 'Seed Technique',
        endpoint: '/api/teaching/seed-technique',
        fields: [
            { name: 'technique', label: 'Technique Name', placeholder: 'e.g., Circuit Breaker Pattern' },
            { name: 'description', label: 'Description', placeholder: 'Explain the technique in detail...', multiline: true },
            { name: 'category', label: 'Category', placeholder: 'e.g., resilience, performance, security' },
        ],
    },
];

const Teaching: React.FC = () => {
    const [activeTab, setActiveTab] = useState(0);
    const [formData, setFormData] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const currentTab = tabs[activeTab];

    const handleFieldChange = (fieldName: string, value: string) => {
        setFormData((prev) => ({ ...prev, [fieldName]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setResult(null);
        setError(null);

        try {
            const token = localStorage.getItem('supremeai_token') || localStorage.getItem('authToken');
            const res = await fetch(currentTab.endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
                body: JSON.stringify(formData),
            });

            const data = await res.json();
            if (res.ok) {
                setResult(JSON.stringify(data, null, 2));
                setFormData({});
            } else {
                setError(data.message || data.error || 'Request failed');
            }
        } catch (err) {
            setError('Network error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: 24 }}>
            <h2>Teaching Module</h2>
            <p style={{ color: '#6B7280', marginBottom: 16 }}>
                Teach SupremeAI new skills: create apps, solve errors, or seed techniques.
            </p>

            <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
                {tabs.map((tab, i) => (
                    <button
                        key={tab.label}
                        onClick={() => { setActiveTab(i); setResult(null); setError(null); }}
                        style={{
                            padding: '10px 20px',
                            borderRadius: 6,
                            border: i === activeTab ? '2px solid #6366F1' : '1px solid #d1d5db',
                            background: i === activeTab ? '#EEF2FF' : '#fff',
                            color: i === activeTab ? '#6366F1' : '#374151',
                            fontWeight: i === activeTab ? 600 : 400,
                            cursor: 'pointer',
                        }}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <form onSubmit={handleSubmit} style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, padding: 24 }}>
                {currentTab.fields.map((field) => (
                    <div key={field.name} style={{ marginBottom: 16 }}>
                        <label style={{ display: 'block', fontWeight: 600, marginBottom: 4, fontSize: 14 }}>
                            {field.label}
                        </label>
                        {field.multiline ? (
                            <textarea
                                value={formData[field.name] || ''}
                                onChange={(e) => handleFieldChange(field.name, e.target.value)}
                                placeholder={field.placeholder}
                                rows={4}
                                style={inputStyle}
                            />
                        ) : (
                            <input
                                type="text"
                                value={formData[field.name] || ''}
                                onChange={(e) => handleFieldChange(field.name, e.target.value)}
                                placeholder={field.placeholder}
                                style={inputStyle}
                            />
                        )}
                    </div>
                ))}

                <button type="submit" disabled={loading} style={{ ...submitBtnStyle, opacity: loading ? 0.6 : 1 }}>
                    {loading ? 'Processing...' : `Submit ${currentTab.label}`}
                </button>
            </form>

            {error && <div style={{ background: '#FEE2E2', color: '#991B1B', padding: 12, borderRadius: 6, marginTop: 16 }}>{error}</div>}

            {result && (
                <div style={{ marginTop: 16 }}>
                    <h3>Result</h3>
                    <pre style={{
                        background: '#1e1e1e',
                        color: '#d4d4d4',
                        padding: 16,
                        borderRadius: 6,
                        overflowX: 'auto',
                        fontSize: 13,
                        maxHeight: 400,
                    }}>
                        {result}
                    </pre>
                </div>
            )}
        </div>
    );
};

const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '10px 12px',
    border: '1px solid #d1d5db',
    borderRadius: 6,
    fontSize: 14,
    boxSizing: 'border-box',
};

const submitBtnStyle: React.CSSProperties = {
    padding: '10px 24px',
    background: '#6366F1',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
};

export default Teaching;
