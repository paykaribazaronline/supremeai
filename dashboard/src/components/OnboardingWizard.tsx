import React, { useState } from 'react';
import { Modal, Steps, Button, Typography, Checkbox, Card, Row, Col } from 'antd';
import { RocketOutlined, RobotOutlined, DashboardOutlined, SafetyOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title, Paragraph, Text } = Typography;

interface OnboardingWizardProps {
  onComplete: (dontShowAgain: boolean) => void;
}

const OnboardingWizard: React.FC<OnboardingWizardProps> = ({ onComplete }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [dontShowAgain, setDontShowAgain] = useState(false);
  const { t } = useTranslation();

  const steps = [
    {
      title: t('onboarding.welcome', 'Welcome'),
      icon: <RocketOutlined />,
      content: (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <RocketOutlined style={{ fontSize: 64, color: '#1677ff', marginBottom: 16 }} />
          <Title level={3}>{t('onboarding.welcome', 'Welcome to SupremeAI!')}</Title>
          <Paragraph style={{ fontSize: 16, marginTop: 16 }}>
            {t('onboarding.welcome_desc', 'Your AI-powered Android app generation platform is ready to use.')}
          </Paragraph>
          <Paragraph type="secondary">
            {t('onboarding.tour_desc', "Let's take a quick tour to get you started.")}
          </Paragraph>
        </div>
      ),
    },
    {
      title: t('onboarding.agents_title', 'AI Agents'),
      icon: <RobotOutlined />,
      content: (
        <div>
          <Title level={4}>{t('onboarding.agents_title', '🤖 AI Agent Orchestration')}</Title>
          <Paragraph>
            {t('onboarding.agents_desc', 'SupremeAI uses multiple AI agents working together to build your Android apps:')}
          </Paragraph>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col span={12}>
              <Card size="small" title={t('agent.x_builder', 'X-Builder Agent')}>
                {t('agent.x_builder_desc', 'Designs app architecture & UI')}
              </Card>
            </Col>
            <Col span={12}>
              <Card size="small" title={t('agent.z_architect', 'Z-Architect Agent')}>
                {t('agent.z_architect_desc', 'Handles technical implementation')}
              </Card>
            </Col>
          </Row>
          <Paragraph style={{ marginTop: 16 }}>
            {t('onboarding.agents_voting', 'Agents use ')}
            <Text strong>consensus voting</Text> {t('onboarding.across', 'across multiple AI providers (OpenAI, Anthropic, Gemini, etc.) for better decisions.')}
          </Paragraph>
        </div>
      ),
    },
    {
      title: t('onboarding.dashboard_title', 'Dashboard'),
      icon: <DashboardOutlined />,
      content: (
        <div>
          <Title level={4}>{t('onboarding.dashboard_title', '📊 Your Command Center')}</Title>
          <Paragraph>
            {t('onboarding.dashboard_desc', 'The dashboard gives you real-time visibility into:')}
          </Paragraph>
          <ul style={{ textAlign: 'left', marginTop: 16 }}>
            <li><Text strong>System Health</Text> - {t('desc.system_health', 'Monitor AI agents & performance')}</li>
            <li><Text strong>Project Progress</Text> - {t('desc.project_status', 'Track app generation status')}</li>
            <li><Text strong>API Keys</Text> - {t('desc.api_keys', 'Manage your AI provider keys')}</li>
            <li><Text strong>Admin Controls</Text> - {t('desc.admin_controls', 'Approve/Reject AI decisions')}</li>
          </ul>
          <Paragraph style={{ marginTop: 16 }}>
            {t('onboarding.use_sidebar', 'Use the sidebar to navigate between sections.')}
          </Paragraph>
        </div>
      ),
    },
    {
      title: t('onboarding.security_title', 'Security'),
      icon: <SafetyOutlined />,
      content: (
        <div>
          <Title level={4}>{t('onboarding.security_title', '🔒 Security & Control')}</Title>
          <Paragraph>
            {t('onboarding.security_desc', 'SupremeAI provides multiple layers of security:')}
          </Paragraph>
          <ul style={{ textAlign: 'left', marginTop: 16 }}>
            <li><Text strong>King Mode</Text> - {t('desc.king_mode', 'AUTO / WAIT / FORCE_STOP controls')}</li>
            <li><Text strong>Admin Approval</Text> - {t('desc.admin_approval', 'Review AI decisions before execution')}</li>
            <li><Text strong>API Key Encryption</Text> - {t('desc.api_encryption', 'Your keys are encrypted at rest')}</li>
            <li><Text strong>Rate Limiting</Text> - {t('desc.rate_limiting', 'Protection against abuse')}</li>
          </ul>
          <Paragraph style={{ marginTop: 16 }}>
            {t('onboarding.start_wait', 'Start in ')}<Text strong>WAIT mode</Text> {t('onboarding.review_first', 'to review all AI actions first.')}
          </Paragraph>
        </div>
      ),
    },
    {
      title: t('onboarding.get_started_title', 'Get Started'),
      icon: <CheckCircleOutlined />,
      content: (
        <div style={{ textAlign: 'center' }}>
          <CheckCircleOutlined style={{ fontSize: 64, color: '#52c41a', marginBottom: 16 }} />
          <Title level={4}>{t('onboarding.get_started_title', "You're All Set!")}</Title>
          <Paragraph style={{ marginTop: 16 }}>
            {t('onboarding.get_started_desc', "Here's how to start building your first Android app:")}
          </Paragraph>
          <div style={{ textAlign: 'left', marginTop: 24, background: '#fafafa', padding: 16, borderRadius: 8 }}>
            <Paragraph><Text strong>1.</Text> {t('step.go_to', 'Go to')} <Text code>AI Agents</Text> {t('step.section', 'section')}</Paragraph>
            <Paragraph><Text strong>2.</Text> {t('step.describe', 'Describe your app idea in detail')}</Paragraph>
            <Paragraph><Text strong>3.</Text> {t('step.watch', 'Watch the AI agents build it!')}</Paragraph>
            <Paragraph><Text strong>4.</Text> {t('step.download', 'Download your APK when ready')}</Paragraph>
          </div>
          <Checkbox
            checked={dontShowAgain}
            onChange={(e) => setDontShowAgain(e.target.checked)}
            style={{ marginTop: 24 }}
          >
            {t('btn.dont_show', "Don't show this again")}
          </Checkbox>
        </div>
      ),
    },
  ];

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      onComplete(dontShowAgain);
    }
  };

  const handleSkip = () => {
    onComplete(dontShowAgain);
  };

  return (
    <Modal
      open={true}
      footer={null}
      closable={false}
      width={700}
      style={{ top: 20 }}
    >
      <div style={{ padding: '16px 0' }}>
        <Steps current={currentStep} items={steps.map(s => ({ title: s.title, icon: s.icon }))} style={{ marginBottom: 32 }} />
        <div style={{ minHeight: 300 }}>
          {steps[currentStep].content}
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 24 }}>
          <Button onClick={handleSkip}>
            Skip Tour
          </Button>
          <Button type="primary" onClick={handleNext}>
            {currentStep < steps.length - 1 ? 'Next' : 'Get Started'}
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default OnboardingWizard;
