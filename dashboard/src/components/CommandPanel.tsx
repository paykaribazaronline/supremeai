import React, { useState } from 'react';

const CommandPanel: React.FC = () => {
  const [command, setCommand] = useState('');
  const [output, setOutput] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!command.trim()) return;
    try {
      const token = localStorage.getItem('supremeai_token') || localStorage.getItem('authToken');
      const res = await fetch('/api/admin/command', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ command }),
      });
      const data = await res.json();
      setOutput(JSON.stringify(data, null, 2));
    } catch (err) {
      setOutput('Error executing command');
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <h2>Command Panel</h2>
      <form onSubmit={handleSubmit} style={{ marginBottom: 16 }}>
        <input
          type="text"
          value={command}
          onChange={(e) => setCommand(e.target.value)}
          placeholder="Enter admin command…"
          style={{ width: '80%', padding: '8px 12px', marginRight: 8 }}
        />
        <button type="submit">Run</button>
      </form>
      {output && (
        <pre style={{ background: '#1e1e1e', color: '#d4d4d4', padding: 16, borderRadius: 4, overflowX: 'auto' }}>
          {output}
        </pre>
      )}
    </div>
  );
};

export default CommandPanel;
