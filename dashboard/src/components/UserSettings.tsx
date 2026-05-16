import React from 'react';
import { Card, Form, Select, Switch, Typography, Space, Divider, message, Button, Avatar, Badge } from 'antd';
import { SaveOutlined, UserOutlined, KeyOutlined, SafetyOutlined, SettingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useRole } from '../contexts/RoleContext';
import { auth } from '../lib/firebase';
import { sendPasswordResetEmail } from 'firebase/auth';

const { Title, Text } = Typography;
const { Option } = Select;

interface UserSettingsProps {
  darkMode: boolean;
  setDarkMode: (value: boolean) => void;
  chatFont: string;
  setChatFont: (value: string) => void;
}

const UserSettings: React.FC<UserSettingsProps> = ({ darkMode, setDarkMode, chatFont, setChatFont }) => {
  const { t, i18n } = useTranslation();
  const { user, isAdmin, isAuthenticated } = useRole();
  const [form] = Form.useForm();
  const [resetLoading, setResetLoading] = React.useState(false);

  const handlePasswordReset = async () => {
    if (!user?.email) return;
    setResetLoading(true);
    try {
      await sendPasswordResetEmail(auth, user.email);
      message.success(t('settings.reset_email_sent', 'Password reset email sent! Check your inbox.'));
    } catch (error) {
      message.error(t('settings.reset_error', 'Failed to send reset email.'));
    } finally {
      setResetLoading(false);
    }
  };

  const handleSave = (values: any) => {
    // Save language preference
    if (values.language) {
      i18n.changeLanguage(values.language);
      localStorage.setItem('preferredLanguage', values.language);
    }

    // Save theme preference
    if (typeof values.darkMode !== 'undefined') {
      setDarkMode(values.darkMode);
      localStorage.setItem('darkMode', String(values.darkMode));
    }

    // Save other preferences
    if (values.notifications !== undefined) {
      localStorage.setItem('notificationsEnabled', String(values.notifications));
    }
    if (values.focusMode !== undefined) {
      localStorage.setItem('focusMode', String(values.focusMode));
    }

    // Save chat font preference
    if (values.chatFont) {
      setChatFont(values.chatFont);
      localStorage.setItem('chatFont', values.chatFont);
    }

    message.success('Settings saved successfully');
  };

  React.useEffect(() => {
    const savedLanguage = localStorage.getItem('preferredLanguage') || i18n.language || 'en';
    const savedDarkMode = localStorage.getItem('darkMode') !== 'false';

    form.setFieldsValue({
      language: savedLanguage,
      darkMode: savedDarkMode,
      notifications: localStorage.getItem('notificationsEnabled') !== 'false',
      focusMode: localStorage.getItem('focusMode') === 'true',
      chatFont: localStorage.getItem('chatFont') || 'font-mono',
    });
  }, [form, i18n.language]);

  return (
    <div style={{ padding: 24 }}>
      <Title level={2} style={{ marginBottom: 24, fontWeight: 700 }}>
        {t('settings.userPreferences', 'User Settings')}
      </Title>

      {/* User Profile Card */}
      <Card
        style={{
          background: 'rgba(0, 243, 255, 0.03)',
          border: '1px solid rgba(0, 243, 255, 0.1)',
          borderRadius: 12,
          marginBottom: 24
        }}
        bodyStyle={{ padding: 24 }}
      >
        <Space size="large" align="start">
          <Avatar 
            size={64} 
            src={user?.photoURL} 
            icon={<UserOutlined />} 
            style={{ 
              background: 'rgba(0, 243, 255, 0.1)', 
              border: '2px solid var(--neon-blue)',
              boxShadow: '0 0 15px rgba(0, 243, 255, 0.3)'
            }} 
          />
          <div>
            <Title level={4} style={{ margin: 0, color: '#fff' }}>{user?.displayName || (isAuthenticated ? 'Authorized User' : 'Guest Entity')}</Title>
            <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>{user?.email || 'No email associated'}</Text>
            <Space>
              <Badge 
                status={isAdmin ? "processing" : "default"} 
                color={isAdmin ? "var(--neon-blue)" : "var(--neon-purple)"}
                text={<span style={{ color: isAdmin ? "var(--neon-blue)" : "var(--neon-purple)", fontWeight: 700, fontSize: 10, textTransform: 'uppercase' }}>
                  {user?.role || user?.tier || 'GUEST'}
                </span>}
              />
              <Text style={{ fontSize: 10, color: 'rgba(255,255,255,0.3)', textTransform: 'uppercase' }}>
                ID: {user?.uid?.substring(0, 8) || '----'}
              </Text>
            </Space>
          </div>
        </Space>
      </Card>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 24, marginBottom: 24 }}>
        {/* Security Card */}
        <Card
          title={<Space><SafetyOutlined /> {t('settings.security', 'Security & Access')}</Space>}
          style={{
            background: 'rgba(255,255,255,0.03)',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: 12,
          }}
          bodyStyle={{ padding: 24 }}
        >
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 16 }}>
            {t('settings.security_desc', 'Manage your account security and authentication methods.')}
          </Text>
          <Button 
            icon={<KeyOutlined />} 
            onClick={handlePasswordReset} 
            loading={resetLoading}
            disabled={!isAuthenticated}
            style={{ width: '100%', borderRadius: 6 }}
          >
            {t('settings.change_password', 'Change Password')}
          </Button>
        </Card>

        {/* Preferences Card */}
        <Card
          title={<Space><SettingOutlined /> {t('settings.preferences', 'System Preferences')}</Space>}
          style={{
            background: 'rgba(255,255,255,0.03)',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: 12,
          }}
          bodyStyle={{ padding: 24 }}
        >
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSave}
          >
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Form.Item name="language" label={t('settings.language', 'Language')} style={{ marginBottom: 12 }}>
                <Select style={{ width: '100%' }}>
                  <Option value="en">English</Option>
                  <Option value="bn">বাংলা (Bengali)</Option>
                </Select>
              </Form.Item>

              <Form.Item name="chatFont" label={t('settings.chatFont', 'AI Chat Font Style')} style={{ marginBottom: 12 }}>
                <Select style={{ width: '100%' }}>
                  <Option value="font-mono">Standard Terminal (Mono)</Option>
                  <Option value="font-doodle">Doodle Art (Handwritten)</Option>
                  <Option value="font-floral">Floral Elegant (Cursive)</Option>
                  <Option value="font-cloudy">Cloudy Soft (Bold)</Option>
                  <Option value="font-bubble">Bubble Letters (Playful)</Option>
                  <Option value="font-sketch">Pencil Sketch (Artistic)</Option>
                </Select>
              </Form.Item>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Text>{t('settings.darkMode', 'Dark Mode')}</Text>
                <Form.Item name="darkMode" valuePropName="checked" noStyle>
                  <Switch />
                </Form.Item>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Text>{t('settings.notifications', 'Notifications')}</Text>
                <Form.Item name="notifications" valuePropName="checked" noStyle>
                  <Switch />
                </Form.Item>
              </div>

              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} style={{ width: '100%', marginTop: 8 }}>
                {t('settings.save', 'Save Settings')}
              </Button>
            </Space>
          </Form>
        </Card>
      </div>

      <Card
        style={{
          background: 'rgba(16,185,129,0.05)',
          border: '1px solid rgba(16,185,129,0.2)',
          borderRadius: 12
        }}
        bodyStyle={{ padding: 24 }}
      >
        <Title level={5} style={{ color: '#10b981', marginBottom: 12 }}>
          {t('settings.about', 'About')}
        </Title>
        <Text type="secondary" style={{ fontSize: 12 }}>
          SupremeAI Dashboard v6.0.0
          <br />
          © 2026 SupremeAI. All rights reserved.
        </Text>
      </Card>
    </div>
  );
};

export default UserSettings;
