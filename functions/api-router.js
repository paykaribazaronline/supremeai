// api-router.js - Central API router for long-term smart system
// Handles all /api/* paths with CORS + graceful defaults

const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

// Health
app.get('/health', (req, res) => res.json({ status: 'ok', mode: 'emulator' }));

// Providers - support both REST and function-style paths
const providerList = [
  { id: 'firebase', name: 'Firebase (supremeai-a)', type: 'firebase', deploymentSource: 'gcloud', status: 'active', models: ['gemini-pro', 'gemini-1.5-pro'] },
  { id: 'vertex', name: 'Vertex AI - Google Cloud', type: 'vertex', deploymentSource: 'gcloud', status: 'active', models: ['gemini-1.5-pro', 'gemini-1.5-flash'] }
];

app.get(['/admin/providers/configured', '/getConfiguredProviders'], (req, res) => {
  res.json({ 
    success: true, 
    data: { 
      providers: providerList,
      total: providerList.length,
      active: providerList.length 
    } 
  });
});

app.get(['/admin/providers/health-stats', '/getProviderHealthStats'], (req, res) => {
  res.json({ success: true, data: { total: 2, active: 2, error: 0 } });
});

// Rules, Plans, Chat (graceful empty responses for now)
app.get('/admin/rules', (req, res) => res.json({ success: true, data: [] }));
app.get('/admin/plans', (req, res) => res.json({ success: true, data: [] }));
app.get('/admin/chat/actions/pending', (req, res) => res.json({ success: true, data: [] }));
app.post('/chat/send', (req, res) => res.json({ success: true, message: 'Received in emulator' }));

// Projects (Deployment tab)
app.get(['/projects', '/api/projects'], (req, res) => {
  res.json({ success: true, data: [] });
});

// Smart catch-all for unknown admin routes (returns sensible defaults)
app.use('/admin/*', (req, res) => {
  const path = req.path;
  let data = null;

  if (path.includes('users')) data = [];
  if (path.includes('rules')) data = [];
  if (path.includes('plans')) data = [];
  if (path.includes('chat')) data = [];
  if (path.includes('logs')) data = [];
  if (path.includes('quotas')) data = { usage: 0, limit: 1000 };

  res.json({ success: true, data, note: 'Emulator stub - real implementation pending' });
});

app.use((req, res) => {
  res.status(404).json({ error: 'Not found in emulator' });
});

module.exports = app;
