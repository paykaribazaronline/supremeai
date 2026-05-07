import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Result, Button } from 'antd';

interface Props {
  children?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: '40px', background: '#fff', borderRadius: '12px', margin: '20px' }}>
          <Result
            status="error"
            title="Dashboard Component Crashed"
            subTitle={this.state.error?.message || "An unexpected error occurred while rendering this component."}
            extra={[
              <Button type="primary" key="reload" onClick={() => window.location.reload()}>
                Reload Dashboard
              </Button>,
              <Button key="reset" onClick={() => this.setState({ hasError: false })}>
                Try Again
              </Button>,
            ]}
          />
        </div>
      );
    }

    return this.children;
  }
}

export default ErrorBoundary;
