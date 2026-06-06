import { Result, Button, Typography } from "antd";
import { motion } from "framer-motion";
import { RefreshCcw, Home } from "lucide-react";
import { Component, ErrorInfo, ReactNode } from "react";

const { Text } = Typography;

interface Props {
  children?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error:", error, errorInfo);
    // Auto-recover from stale chunk errors after deployment
    if (
      error.message?.includes("Failed to fetch dynamically imported module") ||
      error.message?.includes("Loading chunk") ||
      error.message?.includes("Loading CSS chunk")
    ) {
      const hasReloaded = sessionStorage.getItem("chunk_error_reload");
      if (!hasReloaded) {
        sessionStorage.setItem("chunk_error_reload", "1");
        window.location.reload();
        return;
      }
      sessionStorage.removeItem("chunk_error_reload");
    }
  }

  public render() {
    if (this.state.hasError) {
      return (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          style={{
            padding: "60px 20px",
            background: "rgba(10, 10, 15, 0.8)",
            borderRadius: "24px",
            margin: "20px",
            border: "1px solid rgba(239, 68, 68, 0.2)",
            backdropFilter: "blur(20px)",
            textAlign: "center",
          }}
        >
          <Result
            status="error"
            title={
              <span
                style={{ color: "#fff", fontSize: "24px", fontWeight: 800 }}
              >
                কম্পোনেন্ট লোড করতে সমস্যা হয়েছে
              </span>
            }
            subTitle={
              <div style={{ marginTop: "16px" }}>
                <Text
                  style={{ color: "rgba(255,255,255,0.6)", fontSize: "16px" }}
                >
                  দুঃখিত, এই অংশটি রেন্ডার করার সময় একটি ত্রুটি ঘটেছে।
                </Text>
                <br />
                <code
                  style={{
                    display: "inline-block",
                    marginTop: "12px",
                    padding: "8px 16px",
                    background: "rgba(239, 68, 68, 0.1)",
                    color: "#ef4444",
                    borderRadius: "8px",
                    fontSize: "12px",
                  }}
                >
                  {this.state.error?.message || "Unknown System Error"}
                </code>
              </div>
            }
            extra={[
              <Button
                type="primary"
                key="reload"
                icon={<RefreshCcw size={16} />}
                onClick={() => window.location.reload()}
                style={{
                  background: "linear-gradient(135deg, #ef4444, #f59e0b)",
                  border: "none",
                  height: "40px",
                  borderRadius: "8px",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "8px",
                }}
              >
                ড্যাশবোর্ড রিলোড করুন
              </Button>,
              <Button
                key="reset"
                icon={<Home size={16} />}
                onClick={() => this.setState({ hasError: false })}
                style={{
                  background: "rgba(255,255,255,0.05)",
                  color: "#fff",
                  border: "1px solid rgba(255,255,255,0.1)",
                  height: "40px",
                  borderRadius: "8px",
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "8px",
                }}
              >
                আবার চেষ্টা করুন
              </Button>,
            ]}
          />
        </motion.div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
