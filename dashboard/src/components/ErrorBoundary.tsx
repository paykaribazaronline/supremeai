import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Result, Button, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';

const { Paragraph, Text } = Typography;

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
    this.setState({
      error,
      errorInfo,
    });
  }

  private handleReload = () => {
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div style={{ padding: '50px', display: 'flex', justifyContent: 'center', minHeight: '100vh', alignItems: 'center' }}>
          <Result
            status="error"
            title="Something went wrong"
            subTitle="The application encountered an unexpected error."
            extra={[
              <Button type="primary" key="console" onClick={this.handleReload} icon={<ReloadOutlined />}>
                Reload Page
              </Button>
            ]}
          >
            <div className="desc">
              <Paragraph>
                <Text strong style={{ fontSize: 16 }}>
                  Error details:
                </Text>
              </Paragraph>
              <Paragraph style={{ background: '#f5f5f5', padding: '10px', borderRadius: '4px', fontFamily: 'monospace' }}>
                <Text type="danger">{this.state.error?.toString()}</Text>
              </Paragraph>
            </div>
          </Result>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
