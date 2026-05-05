// LoginPage.tsx — Simple Admin Login
import React, { useState } from 'react';
import { Form, Input, Button, Alert } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { firebaseSignIn } from '../lib/firebase';
import { authUtils } from '../lib/authUtils';

interface LoginPageProps {
  onLoginSuccess: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (values: { email: string; password: string }) => {
    setLoading(true);
    setError(null);

    try {
      const result = await firebaseSignIn(values.email, values.password);
      authUtils.setToken(result.token);
      authUtils.setCurrentUser(result.user);
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
      background: 'linear-gradient(135deg, #0F172A, #1E293B, #312E81)',
    }}>
      <div style={{
        width: 400,
        padding: '40px 32px',
        background: 'rgba(255,255,255,0.05)',
        backdropFilter: 'blur(16px)',
        borderRadius: 16,
        border: '1px solid rgba(255,255,255,0.1)',
      }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <h1 style={{ color: '#fff', margin: 0, fontSize: 28, fontWeight: 700 }}>
            Supreme<span style={{ color: '#A78BFA' }}>AI</span>
          </h1>
          <p style={{ color: 'rgba(255,255,255,0.5)', marginTop: 4, fontSize: 14 }}>
            Admin Login
          </p>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            closable
            onClose={() => setError(null)}
            style={{ marginBottom: 16, borderRadius: 8 }}
          />
        )}

        <Form layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="email"
            rules={[{ required: true, message: 'Enter your email' }]}
          >
            <Input
              prefix={<UserOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
              placeholder="Email"
              type="email"
              size="large"
              autoComplete="username"
              style={{
                height: 48,
                background: 'rgba(255,255,255,0.06)',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: 10,
                color: '#fff',
              }}
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Enter your password' }]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
              placeholder="Password"
              size="large"
              autoComplete="current-password"
              style={{
                height: 48,
                background: 'rgba(255,255,255,0.06)',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: 10,
                color: '#fff',
              }}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
              style={{
                height: 48,
                background: 'linear-gradient(135deg, #7C3AED, #A855F7)',
                border: 'none',
                borderRadius: 10,
                fontWeight: 600,
                fontSize: 15,
              }}
            >
              Sign In
            </Button>
          </Form.Item>
        </Form>
      </div>

      <style>{`
        input::placeholder { color: rgba(255,255,255,0.35) !important; }
      `}</style>
    </div>
  );
};

export default LoginPage;
