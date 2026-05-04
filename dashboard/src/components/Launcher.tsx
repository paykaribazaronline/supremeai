import React, { useState } from 'react';
import { Card, Button, Input, Select, Badge } from 'antd';

interface LauncherApp {
  id: string;
  name: string;
  description: string;
  category: 'ai-model' | 'agent' | 'tool' | 'workflow';
  skillMd?: string;
  installed: boolean;
  installing: boolean;
}

const initialApps: LauncherApp[] = [
  {
    id: 'reverse-engineer',
    name: 'Website Reverse Engineer',
    description: 'Reverse engineer any website and generate API connectors',
    category: 'tool',
    installed: false,
    installing: false
  },
  {
    id: 'dynamic-agent',
    name: 'Dynamic AI Agent',
    description: 'Execute tasks with dynamic AI agent selection (Plan 1)',
    category: 'agent',
    installed: true,
    installing: false
  }
];

export const Launcher: React.FC = () => {
  const [apps, setApps] = useState<LauncherApp[]>(initialApps);
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [search, setSearch] = useState('');

  const filteredApps = apps.filter(app => {
    const matchesCategory = categoryFilter === 'all' || app.category === categoryFilter;
    const matchesSearch = app.name.toLowerCase().includes(search.toLowerCase()) ||
                          app.description.toLowerCase().includes(search.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const handleInstall = async (appId: string) => {
    setApps(prev => prev.map(app => 
      app.id === appId ? { ...app, installing: true } : app
    ));

    // Simulate install (replace with actual API call)
    setTimeout(() => {
      setApps(prev => prev.map(app => 
        app.id === appId ? { ...app, installed: true, installing: false } : app
      ));
    }, 2000);
  };

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">App Launcher</h1>
      
      {/* Filters */}
      <div className="flex gap-4 mb-6">
        <Input
          placeholder="Search apps..."
          value={search}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
          style={{ minWidth: 200 }}
        />
        <Select value={categoryFilter} onChange={(v) => setCategoryFilter(v)}>
          <option value="all">All Categories</option>
          <option value="ai-model">AI Models</option>
          <option value="agent">Agents</option>
          <option value="tool">Tools</option>
          <option value="workflow">Workflows</option>
        </Select>
      </div>

      {/* App Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredApps.map(app => (
          <Card key={app.id} className="p-4">
            <div className="flex justify-between items-start mb-2">
              <h3 className="font-semibold">{app.name}</h3>
              {app.installed && <Badge status="success">Installed</Badge>}
            </div>
            <p className="text-sm text-gray-600 mb-4">{app.description}</p>
            <div className="flex gap-2">
              {!app.installed && (
                <Button
                  onClick={() => handleInstall(app.id)}
                  disabled={app.installing}
                >
                  {app.installing ? 'Installing...' : 'Install'}
                </Button>
              )}
              <Button >Details</Button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
};
