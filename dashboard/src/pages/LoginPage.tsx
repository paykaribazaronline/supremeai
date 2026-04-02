// LoginPage.tsx — Firebase-Auth-first login for the React admin dashboard
import React, { useState } from 'react';
import { Form, Input, Button, Alert, Card, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { firebaseSignIn } from '../lib/firebase';

const { Title, Text } = Typography;

interface LoginPageProps {
  onLoginSuccess: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (values: { usernameOrEmail: string; password: string }) => {
    setLoading(true);
    setError(null);
    const { usernameOrEmail, password } = values;

    let token: string | null = null;
    let refreshToken = '';
    let userData: Record<string, unknown> = {};

    // ── Step 1: Try Firebase Auth (requires an email address) ───────────────
    const isEmail = usernameOrEmail.includes('@');
    if (isEmail) {
      try {
        const result = await firebaseSignIn(usernameOrEmail, password);
        token = result.token;
        refreshToken = result.refreshToken;
        userData = result.user;
      } catch (fbErr) {
        console.warn('Firebase Auth failed, trying direct login:', fbErr);
      }
    }

    // ── Step 2: Fall back to direct backend JWT login ────────────────────────
    if (!token) {
      try {
        const resp = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: usernameOrEmail, password }),
        });
        const data = await resp.json() as {
          token?: string;
          refreshToken?: string;
          user?: Record<string, unknown>;
          message?: string;
        };
        if (!resp.ok) throw new Error(data.message ?? 'Login failed');
        token = data.token ?? null;
        refreshToken = data.refreshToken ?? '';
        userData = data.user ?? {};
      } catch (apiErr) {
        const msg = apiErr instanceof Error ? apiErr.message : 'Login failed';
        setError(msg);
        setLoading(false);
        return;
      }
    }

    // ── Save session ─────────────────────────────────────────────────────────
    if (token) {
      localStorage.setItem('supremeai_token', token);
      localStorage.setItem('supremeai_refresh_token', refreshToken);
      localStorage.setItem('supremeai_user', JSON.stringify(userData));
      onLoginSuccess();
    } else {
      setError('Authentication succeeded but no token was returned');
    }
    setLoading(false);
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)',
    }}>
      <Card style={{ width: 380, borderRadius: 12, boxShadow: '0 10px 40px rgba(0,0,0,0.2)' }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={2} style={{ color: '#1e3c72', marginBottom: 4 }}>
            Supreme<span style={{ color: '#4299e1' }}>AI</span>
          </Title>
          <Text type="secondary">Admin Dashboard</Text>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            closable
            onClose={() => setError(null)}
          />
        )}

        <Form layout="vertical" onFinish={handleSubmit} autoComplete="on">
          <Form.Item
            name="usernameOrEmail"
            rules={[{ required: true, message: 'Enter your username or email' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Username or email"
              size="large"
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Enter your password' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password"
              size="large"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
            >
              Sign In
            </Button>
          </Form.Item>
        </Form>

        <div style={{
          background: '#e3f2fd',
          borderLeft: '4px solid #4299e1',
          padding: 12,
          borderRadius: 4,
          fontSize: 12,
          color: '#1565c0',
        }}>
          <strong>Auth flow:</strong> Firebase Auth → backend JWT<br />
          Username login uses backend JWT directly.
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;
