// LoginPage.tsx — Firebase-Auth-first login for the React admin dashboard
import React, { useState } from 'react';
import { Form, Input, Button, Alert, Card, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { firebaseSignIn } from '../lib/firebase';
import { authUtils } from '../lib/authUtils';

const { Title, Text } = Typography;

interface LoginPageProps {
  onLoginSuccess: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (values: { email: string; password: string }) => {
    setLoading(true);
    setError(null);
    const { email, password } = values;

    try {
      const result = await firebaseSignIn(email, password);
      const token = result.token;
      const userData = result.user;

      // Store using unified key 'authToken' for consistency across all screens
      authUtils.setToken(token);
      authUtils.setCurrentUser(userData);
      
      onLoginSuccess();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Login failed';
      setError(msg);
    } finally {
      setLoading(false);
    }
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
            name="email"
            rules={[{ required: true, message: 'Enter your Firebase email' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Email"
              size="large"
              type="email"
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
          <strong>Auth flow:</strong> Firebase Auth email login → backend JWT.
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;
