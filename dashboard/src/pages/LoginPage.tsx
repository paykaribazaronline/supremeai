// LoginPage.tsx — Modern Glassmorphism Login
import React, { useState } from 'react';
import { Form, Input, Button, Alert, Typography, Spin } from 'antd';
import { LockOutlined, UserOutlined, RobotOutlined } from '@ant-design/icons';
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
      background: 'linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #312E81 100%)',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Animated background elements */}
      <div style={{
        position: 'absolute',
        width: '500px',
        height: '500px',
        background: 'radial-gradient(circle, rgba(124, 58, 237, 0.15) 0%, transparent 70%)',
        borderRadius: '50%',
        top: '-100px',
        right: '-100px',
        animation: 'pulse 4s ease-in-out infinite',
      }} />
      <div style={{
        position: 'absolute',
        width: '400px',
        height: '400px',
        background: 'radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%)',
        borderRadius: '50%',
        bottom: '-100px',
        left: '-100px',
        animation: 'pulse 6s ease-in-out infinite reverse',
      }} />
      <div style={{
        position: 'absolute',
        width: '300px',
        height: '300px',
        background: 'radial-gradient(circle, rgba(16, 185, 129, 0.1) 0%, transparent 70%)',
        borderRadius: '50%',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        animation: 'pulse 5s ease-in-out infinite',
      }} />

      {/* Glassmorphism card */}
      <div style={{
        width: '420px',
        padding: '48px 40px',
        background: 'rgba(255, 255, 255, 0.05)',
        backdropFilter: 'blur(20px)',
        borderRadius: '24px',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
        position: 'relative',
        zIndex: 10,
        transition: 'all 0.3s ease',
      }}>
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{
            width: '72px',
            height: '72px',
            background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
            borderRadius: '20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px',
            boxShadow: '0 10px 40px -10px rgba(124, 58, 237, 0.5)',
            animation: 'float 3s ease-in-out infinite',
          }}>
            <RobotOutlined style={{ fontSize: '36px', color: 'white' }} />
          </div>
          <Title level={2} style={{ 
            color: '#FFFFFF', 
            marginBottom: '8px',
            fontWeight: 700,
            letterSpacing: '-0.02em',
          }}>
            Supreme<span style={{ color: '#A78BFA' }}>AI</span>
          </Title>
          <Text style={{ color: 'rgba(255, 255, 255, 0.6)', fontSize: '15px' }}>
            Intelligent Platform Administration
          </Text>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            showIcon
            style={{ 
              marginBottom: '24px', 
              borderRadius: '12px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.2)',
              color: '#FCA5A5',
            }}
            closable
            onClose={() => setError(null)}
          />
        )}

        <Form layout="vertical" onFinish={handleSubmit} autoComplete="on">
          <Form.Item
            name="email"
            rules={[{ required: true, message: 'Enter your email address' }]}
          >
            <Input
              prefix={<UserOutlined style={{ color: 'rgba(255,255,255,0.5)' }} />}
              placeholder="Email Address"
              size="large"
              type="email"
              autoComplete="username"
              style={{
                height: '56px',
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '12px',
                color: '#FFFFFF',
                fontSize: '15px',
              }}
              placeholderStyle={{ color: 'rgba(255,255,255,0.4)' }}
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Enter your password' }]}
            style={{ marginBottom: '32px' }}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: 'rgba(255,255,255,0.5)' }} />}
              placeholder="Password"
              size="large"
              autoComplete="current-password"
              style={{
                height: '56px',
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '12px',
                color: '#FFFFFF',
                fontSize: '15px',
              }}
              placeholderStyle={{ color: 'rgba(255,255,255,0.4)' }}
              iconRender={(visible) => <span style={{ color: 'rgba(255,255,255,0.5)' }}>{visible ? '🙈' : '👁️'}</span>}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
              style={{
                height: '56px',
                background: 'linear-gradient(135deg, #7C3AED 0%, #A855F7 100%)',
                border: 'none',
                borderRadius: '12px',
                fontSize: '16px',
                fontWeight: 600,
                boxShadow: '0 10px 40px -10px rgba(124, 58, 237, 0.5)',
                transition: 'all 0.3s ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 15px 50px -10px rgba(124, 58, 237, 0.6)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 10px 40px -10px rgba(124, 58, 237, 0.5)';
              }}
            >
              {loading ? <Spin style={{ color: 'white' }} /> : 'Sign In to Dashboard'}
            </Button>
          </Form.Item>
        </Form>

        <div style={{
          marginTop: '32px',
          padding: '16px 20px',
          background: 'rgba(255, 255, 255, 0.03)',
          borderRadius: '12px',
          borderLeft: '3px solid #7C3AED',
          fontSize: '13px',
          color: 'rgba(255, 255, 255, 0.5)',
        }}>
          <span style={{ fontWeight: 600, color: 'rgba(255, 255, 255, 0.7)' }}>Security:</span> Firebase authenticated session with backend JWT validation
        </div>
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-8px); }
        }
        @keyframes pulse {
          0%, 100% { opacity: 0.4; transform: scale(1); }
          50% { opacity: 0.6; transform: scale(1.1); }
        }
        input::placeholder {
          color: rgba(255, 255, 255, 0.4) !important;
        }
      `}</style>
    </div>
  );
};

export default LoginPage;
