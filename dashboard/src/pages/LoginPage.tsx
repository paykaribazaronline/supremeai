// LoginPage.tsx - SupremeAI Authentication Portal (Redesigned)
import {
  UserOutlined,
  LockOutlined,
  RobotOutlined,
  LoginOutlined,
  MailOutlined,
  UserAddOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import {
  Form,
  Input,
  Button,
  Card,
  Typography,
  message,
  Space,
  Divider,
} from "antd";
import { motion } from "framer-motion";
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { useRole } from "../contexts/RoleContext";
import { authUtils } from "../lib/authUtils";
import { firebaseSignIn } from "../lib/firebase";

const { Title, Text } = Typography;

interface LoginForm {
  email: string;
  password: string;
}

interface CreateUserForm {
  email: string;
  password: string;
  confirmPassword: string;
  fullName: string;
}

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [createUserLoading, setCreateUserLoading] = useState(false);
  const [showRegisterForm, setShowRegisterForm] = useState(false);
  const [form] = Form.useForm<LoginForm>();
  const [createUserForm] = Form.useForm<CreateUserForm>();
  const { refreshUser } = useRole();
  const navigate = useNavigate();

  useEffect(() => {
    if (authUtils.isAuthenticated()) {
      if (authUtils.isAdmin()) {
        navigate("/admin/dashboard");
      } else {
        navigate("/user/dashboard");
      }
    }
  }, [navigate]);

  if (authUtils.isAuthenticated()) {
    return (
      <div style={{ height: "100vh", display: "flex", justifyContent: "center", alignItems: "center", background: "#0a0a0a" }}>
        <Typography.Title level={3} style={{ color: "var(--neon-blue)" }}>Redirecting to Dashboard...</Typography.Title>
      </div>
    );
  }

  const handleLogin = async (values: LoginForm) => {
    setLoading(true);
    try {
      // ✅ Real Firebase Authentication
      const result = await firebaseSignIn(values.email, values.password);

      authUtils.setToken(result.token);
      authUtils.setCurrentUser(result.user);

      message.success(
        `স্বাগতম, ${result.user.displayName || result.user.email}!`,
      );
      refreshUser();

      setTimeout(() => {
        if (authUtils.isAdmin()) {
          window.location.href = "/admin/dashboard";
        } else {
          window.location.href = "/user/dashboard";
        }
      }, 1500);
    } catch (error: any) {
      message.error(error.message || "লগইন ব্যর্থ হয়েছে!");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async (values: CreateUserForm) => {
    setCreateUserLoading(true);
    try {
      if (values.password !== values.confirmPassword) {
        throw new Error("পাসওয়ার্ড মিলছে না!");
      }

      const API_BASE = import.meta.env.VITE_API_URL || "";
      const resp = await fetch(`${API_BASE}/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: values.email,
          password: values.password,
          displayName: values.fullName,
        }),
      });

      if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.message || "রেজিস্ট্রেশন ব্যর্থ হয়েছে!");
      }

      const result = await resp.json();
      message.success(
        result.data?.message ||
          "অ্যাকাউন্ট তৈরি সফল! এখন লগইন করার চেষ্টা করুন।",
      );
      setShowRegisterForm(false);
      createUserForm.resetFields();

      // Switch to login mode and fill email automatically
      form.setFieldsValue({ email: values.email });
    } catch (error: any) {
      message.error(error.message || "ইউজার তৈরি করতে ব্যর্থ হয়েছে!");
    } finally {
      setCreateUserLoading(false);
    }
  };

  const handleGuestLogin = async () => {
    setLoading(true);
    try {
      // Guest mode - limited read-only access
      authUtils.setToken("GUEST_MODE");
      authUtils.setCurrentUser({
        id: "guest",
        uid: "guest",
        email: null,
        displayName: "Guest User",
        username: "guest",
        role: "user",
        tier: "guest",
      });
      message.success("গেস্ট মোডে প্রবেশ করা হয়েছে!");
      refreshUser();
      setTimeout(() => {
        window.location.href = "/user/dashboard";
      }, 1000);
    } catch (error: any) {
      message.error(error.message || "গেস্ট এক্সেস ব্যর্থ হয়েছে!");
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    const email = form.getFieldValue("email");
    if (!email) {
      message.warning("অনুগ্রহ করে আগে ইমেইল প্রদান করুন!");
      return;
    }

    try {
      const API_BASE = import.meta.env.VITE_API_URL || "";
      const resp = await fetch(`${API_BASE}/api/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      if (resp.ok) {
        message.info(
          "পাসওয়ার্ড রিসেট লিঙ্ক আপনার ইমেইলে পাঠানো হয়েছে (যদি ইমেইলটি রেজিস্টার্ড থাকে)।",
        );
      }
    } catch (error) {
      message.error("পাসওয়ার্ড রিসেট রিকোয়েস্ট পাঠাতে ব্যর্থ হয়েছে।");
    }
  };

  return (
    <div
      className="login-container"
      style={{
        minHeight: "100vh",
        background:
          "linear-gradient(135deg, #0a0a0c 0%, #1a1a1f 50%, #0a0a0c 100%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "20px",
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* Background Effects */}
      <div className="bg-grid" />
      <div className="hex-grid" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8 }}
        style={{ width: "100%", maxWidth: "440px", zIndex: 10 }}
      >
        {/* Header */}
        <div style={{ textAlign: "center", marginBottom: "32px" }}>
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="glass-card"
            style={{
              padding: "24px",
              borderRadius: "20px",
              border: "1px solid rgba(0, 243, 255, 0.25)",
              boxShadow:
                "0 20px 40px rgba(0, 0, 0, 0.6), 0 0 30px rgba(0, 243, 255, 0.1)",
              marginBottom: "24px",
            }}
          >
            <RobotOutlined
              className="pulsing"
              style={{
                fontSize: "56px",
                color: "var(--neon-blue)",
                filter: "drop-shadow(0 0 12px var(--neon-blue))",
                marginBottom: "16px",
              }}
            />
            <Title
              level={2}
              style={{
                color: "#fff",
                margin: 0,
                fontWeight: 900,
                fontSize: "32px",
                letterSpacing: "2px",
              }}
            >
              SUPREME <span className="text-gradient">AI</span>
            </Title>
            <Text
              style={{
                color: "var(--text-dim)",
                fontSize: "13px",
                letterSpacing: "1px",
                textTransform: "uppercase",
                display: "block",
                marginTop: "6px",
              }}
            >
              কমান্ড সেন্টার অথেন্টিকেশন
            </Text>
          </motion.div>
        </div>

        {/* Main Login Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <Card
            className="glass-panel"
            style={{
              background: "rgba(8, 8, 16, 0.85)",
              border: "1px solid rgba(0, 243, 255, 0.2)",
              borderRadius: "20px",
              boxShadow: `
                0 30px 60px rgba(0, 0, 0, 0.8),
                0 0 50px rgba(0, 243, 255, 0.08),
                inset 0 1px 0 rgba(255, 255, 255, 0.05)
              `,
              overflow: "hidden",
              position: "relative",
            }}
          >
            {/* Neon top border animation */}
            <div
              style={{
                position: "absolute",
                top: 0,
                left: 0,
                right: 0,
                height: "2px",
                background:
                  "linear-gradient(90deg, var(--neon-blue), var(--neon-purple))",
              }}
            />

            <div style={{ padding: "24px" }}>
              {/* Title */}
              <div style={{ textAlign: "center", marginBottom: "28px" }}>
                <Text
                  style={{
                    color: "var(--text-dim)",
                    fontSize: "13px",
                    letterSpacing: "0.5px",
                  }}
                >
                  সিস্টেম অ্যাক্সেস করতে লগইন করুন
                </Text>
              </div>

              {/* Login Form */}
              <Form
                form={form}
                layout="vertical"
                onFinish={handleLogin}
                size="large"
              >
                <Form.Item
                  name="email"
                  rules={[
                    { required: true, message: "ইমেইল প্রয়োজন!" },
                    { type: "email", message: "সঠিক ইমেইল ফরম্যাট দিন!" },
                  ]}
                >
                  <Input
                    prefix={
                      <MailOutlined
                        style={{
                          color: "var(--neon-blue)",
                          marginRight: "8px",
                        }}
                      />
                    }
                    placeholder="ইমেইল অ্যাড্রেস"
                    autoComplete="email"
                    className="cyber-input-field"
                    style={{
                      background: "rgba(0, 0, 0, 0.4)",
                      border: "1px solid rgba(0, 243, 255, 0.15)",
                      color: "#fff",
                      borderRadius: "8px",
                      height: "50px",
                    }}
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[{ required: true, message: "পাসওয়ার্ড প্রয়োজন!" }]}
                >
                  <Input.Password
                    prefix={
                      <LockOutlined
                        style={{
                          color: "var(--neon-blue)",
                          marginRight: "8px",
                        }}
                      />
                    }
                    placeholder="পাসওয়ার্ড"
                    autoComplete="current-password"
                    className="cyber-input-field"
                    style={{
                      background: "rgba(0, 0, 0, 0.4)",
                      border: "1px solid rgba(0, 243, 255, 0.15)",
                      color: "#fff",
                      borderRadius: "8px",
                      height: "50px",
                    }}
                  />
                </Form.Item>

                {/* Primary Action Buttons */}
                <Space
                  direction="vertical"
                  style={{ width: "100%", gap: "14px", marginTop: "8px" }}
                >
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    icon={<LoginOutlined />}
                    className="cyber-button"
                    style={{
                      width: "100%",
                      background:
                        "linear-gradient(135deg, var(--neon-blue), var(--neon-purple))",
                      border: "none",
                      height: "50px",
                      fontWeight: 800,
                      fontSize: "15px",
                      borderRadius: "8px",
                      letterSpacing: "1px",
                      textTransform: "uppercase",
                      color: "#000",
                      boxShadow: "0 8px 24px rgba(0, 243, 255, 0.35)",
                    }}
                    block
                  >
                    Login
                  </Button>
                </Space>
              </Form>

              {/* Forgot Password */}
              <div style={{ textAlign: "center", marginTop: "16px" }}>
                <Button
                  type="link"
                  size="small"
                  onClick={handleForgotPassword}
                  style={{ color: "var(--neon-blue)", padding: 0 }}
                >
                  পাসওয়ার্ড ভুলে গেছেন?
                </Button>
              </div>

              {/* Divider */}
              <Divider
                style={{
                  margin: "24px 0 16px 0",
                  borderColor: "rgba(255,255,255,0.1)",
                }}
              >
                <Text style={{ color: "var(--text-dim)", fontSize: "12px" }}>
                  বা
                </Text>
              </Divider>

              {/* Secondary Actions (Register & Guest) */}
              <Space
                direction="vertical"
                style={{ width: "100%", gap: "12px" }}
              >
                {!showRegisterForm ? (
                  <Button
                    type="link"
                    icon={<UserAddOutlined />}
                    onClick={() => setShowRegisterForm(true)}
                    style={{
                      width: "100%",
                      color: "var(--text-main)",
                      border: "1px dashed rgba(255,255,255,0.2)",
                      borderRadius: "8px",
                      height: "48px",
                    }}
                  >
                    Create New Account
                  </Button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    style={{
                      background: "rgba(0, 243, 255, 0.05)",
                      border: "1px solid rgba(0, 243, 255, 0.2)",
                      borderRadius: "12px",
                      padding: "20px",
                      marginTop: "16px",
                    }}
                  >
                    <Form
                      form={createUserForm}
                      layout="vertical"
                      onFinish={handleCreateUser}
                      size="large"
                    >
                      <Form.Item
                        name="fullName"
                        rules={[
                          { required: true, message: "পূর্ণ নাম প্রয়োজন!" },
                        ]}
                      >
                        <Input
                          prefix={
                            <UserOutlined
                              style={{ color: "var(--neon-blue)" }}
                            />
                          }
                          placeholder="পূর্ণ নাম"
                          style={{
                            background: "rgba(255,255,255,0.05)",
                            border: "1px solid rgba(255,255,255,0.1)",
                            color: "var(--text-main)",
                            borderRadius: "8px",
                          }}
                        />
                      </Form.Item>

                      <Form.Item
                        name="email"
                        rules={[
                          { required: true, message: "ইমেইল প্রয়োজন!" },
                          { type: "email", message: "সঠিক ইমেইল ফরম্যাট দিন!" },
                        ]}
                      >
                        <Input
                          prefix={
                            <MailOutlined
                              style={{ color: "var(--neon-blue)" }}
                            />
                          }
                          placeholder="ইমেইল অ্যাড্রেস"
                          style={{
                            background: "rgba(255,255,255,0.05)",
                            border: "1px solid rgba(255,255,255,0.1)",
                            color: "var(--text-main)",
                            borderRadius: "8px",
                          }}
                        />
                      </Form.Item>

                      <Form.Item
                        name="password"
                        rules={[
                          { required: true, message: "পাসওয়ার্ড প্রয়োজন!" },
                          {
                            min: 6,
                            message: "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে!",
                          },
                        ]}
                      >
                        <Input.Password
                          prefix={
                            <LockOutlined
                              style={{ color: "var(--neon-blue)" }}
                            />
                          }
                          placeholder="পাসওয়ার্ড (অন্তত ৬ অক্ষর)"
                          style={{
                            background: "rgba(255,255,255,0.05)",
                            border: "1px solid rgba(255,255,255,0.1)",
                            color: "var(--text-main)",
                            borderRadius: "8px",
                          }}
                        />
                      </Form.Item>

                      <Form.Item
                        name="confirmPassword"
                        rules={[
                          {
                            required: true,
                            message: "পাসওয়ার্ড কনফার্ম করুন!",
                          },
                          ({ getFieldValue }) => ({
                            validator(_, value) {
                              if (
                                !value ||
                                getFieldValue("password") === value
                              ) {
                                return Promise.resolve();
                              }
                              return Promise.reject(
                                new Error("পাসওয়ার্ড মিলছে না!"),
                              );
                            },
                          }),
                        ]}
                      >
                        <Input.Password
                          prefix={
                            <CheckCircleOutlined
                              style={{ color: "var(--neon-blue)" }}
                            />
                          }
                          placeholder="পাসওয়ার্ড আবার লিখুন"
                          style={{
                            background: "rgba(255,255,255,0.05)",
                            border: "1px solid rgba(255,255,255,0.1)",
                            color: "var(--text-main)",
                            borderRadius: "8px",
                          }}
                        />
                      </Form.Item>

                      <Button
                        type="primary"
                        htmlType="submit"
                        loading={createUserLoading}
                        icon={<UserAddOutlined />}
                        block
                        style={{
                          background:
                            "linear-gradient(135deg, var(--success), var(--neon-blue))",
                          border: "none",
                          height: "48px",
                          fontWeight: 700,
                          fontSize: "16px",
                          borderRadius: "8px",
                          marginBottom: "8px",
                        }}
                      >
                        Create Account
                      </Button>

                      <Button
                        type="text"
                        onClick={() => {
                          setShowRegisterForm(false);
                          createUserForm.resetFields();
                        }}
                        style={{ padding: 0, height: "auto" }}
                      >
                        Cancel
                      </Button>
                    </Form>
                  </motion.div>
                )}

                <Button
                  type="default"
                  onClick={handleGuestLogin}
                  loading={loading}
                  icon={<RobotOutlined />}
                  className="glass-action-button"
                  style={{
                    width: "100%",
                    height: "48px",
                    fontWeight: 700,
                    borderRadius: "8px",
                    border: "1px solid rgba(0, 243, 255, 0.35)",
                    color: "var(--neon-blue)",
                    background: "rgba(0, 243, 255, 0.06)",
                  }}
                >
                  Continue as Guest
                </Button>
              </Space>

              {/* Info Text */}
              <div
                style={{
                  marginTop: showRegisterForm ? "12px" : "24px",
                  textAlign: "center",
                }}
              >
                <Text
                  style={{
                    color: "var(--text-dim)",
                    fontSize: "11px",
                    lineHeight: "1.6",
                  }}
                >
                  একাউন্ট তৈরি করলে আপনি SupremeAI-এর সব ফিচার ব্যবহার করতে
                  পারবেন। আপনার ইমেইল ভিত্তিক অ্যাক্সেস লেভেল স্বয়ংক্রিয়ভাবে
                  নির্ধারিত হবে।
                </Text>
              </div>
            </div>
          </Card>
        </motion.div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
          style={{ textAlign: "center", marginTop: "24px" }}
        >
          <Text style={{ color: "var(--text-dim)", fontSize: "12px" }}>
            SupremeAI Command Center v4.2.0
          </Text>
        </motion.div>
        <style>{`
          .cyber-input-field {
            background: rgba(8, 8, 16, 0.6) !important;
            border: 1px solid rgba(0, 243, 255, 0.15) !important;
            color: #fff !important;
            border-radius: 8px !important;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
          }
          .cyber-input-field:hover {
            border-color: rgba(0, 243, 255, 0.45) !important;
            box-shadow: 0 0 10px rgba(0, 243, 255, 0.15) !important;
          }
          .cyber-input-field-focused, .cyber-input-field:focus, .cyber-input-field:focus-within {
            border-color: var(--neon-blue) !important;
            box-shadow: 0 0 20px rgba(0, 243, 255, 0.45), inset 0 2px 4px rgba(0,0,0,0.6) !important;
            background: rgba(0, 0, 0, 0.6) !important;
          }
          .cyber-input-field input {
            color: #fff !important;
            background: transparent !important;
          }
          .cyber-input-field input::placeholder {
            color: rgba(255, 255, 255, 0.35) !important;
          }
        `}</style>
      </motion.div>
    </div>
  );
};

export default LoginPage;
