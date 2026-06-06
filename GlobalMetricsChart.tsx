import React, { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const GlobalMetricsChart = () => {
    const [data, setData] = useState([]);

    useEffect(() => {
        fetch('/api/admin/metrics/summary')
            .then(res => res.json())
            .then(res => {
                const stats = res.data;
                const formattedData = [
                    { name: 'Learned', value: stats.patternsLearned, color: '#4caf50' },
                    { name: 'Edits', value: stats.codeEdits, color: '#2196f3' },
                    { name: 'Errors', value: stats.errorsReported, color: '#f44336' },
                    { name: 'Feedback', value: stats.feedbackGiven, color: '#ff9800' }
                ];
                setData(formattedData);
            });
    }, []);

    return (
        <div style={{ width: '100%', height: 300, background: '#1e1e1e', padding: '20px', borderRadius: '8px' }}>
            <h3 style={{ color: '#fff', marginBottom: '20px' }}>Global AI Learning Progress</h3>
            <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#444" />
                    <XAxis dataKey="name" stroke="#ccc" />
                    <YAxis stroke="#ccc" />
                    <Tooltip contentStyle={{ backgroundColor: '#333', border: 'none', color: '#fff' }} />
                    <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                        {data.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
};

export default GlobalMetricsChart;