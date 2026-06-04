import {
  SafetyOutlined,
  GlobalOutlined,
  KeyOutlined,
  BulbOutlined,
  ReloadOutlined,
  CodeOutlined,
} from "@ant-design/icons";
import {
  Drawer,
  Tabs,
  Input,
  Space,
  Row,
  Col,
  Button,
  List,
  Badge,
  Typography,
  Popconfirm,
  Card,
  Tag,
  Descriptions,
  Alert,
  Progress,
} from "antd";
import { motion } from "framer-motion";
import React from "react";

const { TabPane } = Tabs;
const { Text, Title, Paragraph } = Typography;

interface BrowserSafetyDrawerProps {
  open: boolean;
  onClose: () => void;
  newUrlPattern: string;
  setNewUrlPattern: (val: string) => void;
  newUrlType: string;
  setNewUrlType: (val: string) => void;
  handleSavePermission: () => void;
  allowedUrls: any[];
  deniedUrls: any[];
  handleDeleteUrl: (id: string) => void;
  newCred: any;
  setNewCred: (cred: any) => void;
  handleSaveCredential: () => void;
  credentials: any[];
  handleDeleteCredential: (id: string) => void;
  isLearning: boolean;
  toggleLearning: (val: boolean) => void;
  learningStatus: any;
}

const BrowserSafetyDrawer: React.FC<BrowserSafetyDrawerProps> = ({
  open,
  onClose,
  newUrlPattern,
  setNewUrlPattern,
  newUrlType,
  setNewUrlType,
  handleSavePermission,
  allowedUrls,
  deniedUrls,
  handleDeleteUrl,
  newCred,
  setNewCred,
  handleSaveCredential,
  credentials,
  handleDeleteCredential,
  isLearning,
  toggleLearning,
  learningStatus,
}) => {
  return (
    <Drawer
      title={
        <span style={{ color: "#fff" }}>
          <SafetyOutlined style={{ marginRight: 10 }} /> Browser Safety &
          Credentials
        </span>
      }
      placement="left"
      width={500}
      onClose={onClose}
      open={open}
      styles={{
        header: {
          background: "#1a1b1e",
          borderBottom: "1px solid rgba(255,255,255,0.1)",
        },
        body: { background: "#08090a", padding: 24 },
      }}
      closeIcon={<span style={{ color: "rgba(255,255,255,0.5)" }}>×</span>}
    >
      <Tabs defaultActiveKey="permissions" className="custom-tabs">
        <TabPane
          tab={
            <span>
              <SafetyOutlined /> URL Control
            </span>
          }
          key="permissions"
        >
          <div
            style={{
              background: "rgba(255,255,255,0.03)",
              padding: 20,
              borderRadius: 16,
              border: "1px solid rgba(255,255,255,0.05)",
              marginBottom: 24,
            }}
          >
            <Text
              style={{
                color: "#fff",
                fontWeight: 600,
                display: "block",
                marginBottom: 16,
              }}
            >
              Policy Configuration
            </Text>
            <Space direction="vertical" style={{ width: "100%" }} size="middle">
              <Input
                placeholder="domain.com (e.g. facebook.com)"
                value={newUrlPattern}
                onChange={(e) => setNewUrlPattern(e.target.value)}
                prefix={
                  <GlobalOutlined style={{ color: "rgba(255,255,255,0.3)" }} />
                }
                style={{
                  background: "rgba(0,0,0,0.3)",
                  color: "#fff",
                  height: 45,
                  borderRadius: 10,
                  border: "1px solid rgba(255,255,255,0.1)",
                }}
              />
              <Row gutter={12}>
                <Col span={12}>
                  <Button
                    block
                    onClick={() => setNewUrlType("allowed")}
                    style={{
                      height: 40,
                      borderRadius: 10,
                      background:
                        newUrlType === "allowed"
                          ? "rgba(39, 201, 63, 0.1)"
                          : "transparent",
                      borderColor:
                        newUrlType === "allowed"
                          ? "#27c93f"
                          : "rgba(255,255,255,0.1)",
                      color:
                        newUrlType === "allowed"
                          ? "#27c93f"
                          : "rgba(255,255,255,0.4)",
                    }}
                  >
                    ALLOW LIST
                  </Button>
                </Col>
                <Col span={12}>
                  <Button
                    block
                    onClick={() => setNewUrlType("denied")}
                    style={{
                      height: 40,
                      borderRadius: 10,
                      background:
                        newUrlType === "denied"
                          ? "rgba(255, 77, 79, 0.1)"
                          : "transparent",
                      borderColor:
                        newUrlType === "denied"
                          ? "#ff4d4f"
                          : "rgba(255,255,255,0.1)",
                      color:
                        newUrlType === "denied"
                          ? "#ff4d4f"
                          : "rgba(255,255,255,0.4)",
                    }}
                  >
                    DENY LIST
                  </Button>
                </Col>
              </Row>
              <Button
                type="primary"
                block
                onClick={handleSavePermission}
                style={{ height: 45, borderRadius: 10, fontWeight: 600 }}
              >
                DEPLOY SECURITY RULE
              </Button>
            </Space>
          </div>

          <List
            dataSource={[...allowedUrls, ...deniedUrls]}
            renderItem={(item: any) => (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <div
                  style={{
                    background: "rgba(255,255,255,0.02)",
                    padding: "12px 16px",
                    borderRadius: 12,
                    marginBottom: 10,
                    border: "1px solid rgba(255,255,255,0.05)",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Space>
                    <Badge
                      color={item.type === "allowed" ? "#27c93f" : "#ff4d4f"}
                    />
                    <Text
                      style={{ color: "#fff", fontSize: 13, fontWeight: 500 }}
                    >
                      {item.pattern}
                    </Text>
                  </Space>
                  <Popconfirm
                    title="Purge Security Rule?"
                    description="This will immediately remove the URL restriction. Proceed?"
                    onConfirm={() => handleDeleteUrl(item.id)}
                    okText="Purge"
                    cancelText="Retain"
                    okButtonProps={{ danger: true }}
                  >
                    <Button
                      type="text"
                      danger
                      icon={
                        <ReloadOutlined
                          style={{ transform: "rotate(45deg)" }}
                        />
                      }
                    />
                  </Popconfirm>
                </div>
              </motion.div>
            )}
          />
        </TabPane>

        <TabPane
          tab={
            <span>
              <KeyOutlined /> Neural Vault
            </span>
          }
          key="credentials"
        >
          <div
            style={{
              background: "rgba(0,242,254,0.03)",
              padding: 20,
              borderRadius: 16,
              border: "1px solid rgba(0,242,254,0.1)",
              marginBottom: 24,
            }}
          >
            <Text
              style={{
                color: "#00f2fe",
                fontWeight: 600,
                display: "block",
                marginBottom: 16,
              }}
            >
              Store Encrypted Secrets
            </Text>
            <Space direction="vertical" style={{ width: "100%" }} size="small">
              <Input
                placeholder="Website Domain (e.g. amazon.com)"
                value={newCred.website}
                onChange={(e) =>
                  setNewCred({ ...newCred, website: e.target.value })
                }
                style={{
                  background: "rgba(0,0,0,0.3)",
                  color: "#fff",
                  height: 40,
                  borderRadius: 8,
                  border: "1px solid rgba(255,255,255,0.1)",
                }}
              />
              <Input
                placeholder="Username / Email"
                value={newCred.username}
                onChange={(e) =>
                  setNewCred({ ...newCred, username: e.target.value })
                }
                style={{
                  background: "rgba(0,0,0,0.3)",
                  color: "#fff",
                  height: 40,
                  borderRadius: 8,
                  border: "1px solid rgba(255,255,255,0.1)",
                }}
              />
              <Input.Password
                placeholder="Secure Password"
                value={newCred.password}
                onChange={(e) =>
                  setNewCred({ ...newCred, password: e.target.value })
                }
                style={{
                  background: "rgba(0,0,0,0.3)",
                  color: "#fff",
                  height: 40,
                  borderRadius: 8,
                  border: "1px solid rgba(255,255,255,0.1)",
                }}
              />
              <Input
                placeholder="Access Token (Optional)"
                value={newCred.token}
                onChange={(e) =>
                  setNewCred({ ...newCred, token: e.target.value })
                }
                style={{
                  background: "rgba(0,0,0,0.3)",
                  color: "#fff",
                  height: 40,
                  borderRadius: 8,
                  border: "1px solid rgba(255,255,255,0.1)",
                }}
              />
              <Button
                type="primary"
                block
                onClick={handleSaveCredential}
                style={{
                  height: 45,
                  borderRadius: 10,
                  marginTop: 12,
                  background:
                    "linear-gradient(135deg, #00f2fe 0%, #4facfe 100%)",
                  border: "none",
                  fontWeight: 600,
                }}
              >
                LOCK IN VAULT
              </Button>
            </Space>
          </div>

          <List
            dataSource={credentials}
            renderItem={(item: any) => (
              <Card
                size="small"
                style={{
                  background: "rgba(255,255,255,0.02)",
                  border: "1px solid rgba(255,255,255,0.05)",
                  marginBottom: 12,
                  borderRadius: 12,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Space>
                    <div
                      style={{
                        background: "rgba(0,242,254,0.1)",
                        padding: 8,
                        borderRadius: 8,
                      }}
                    >
                      <GlobalOutlined style={{ color: "#00f2fe" }} />
                    </div>
                    <div>
                      <Text
                        style={{
                          color: "#fff",
                          fontWeight: 600,
                          display: "block",
                        }}
                      >
                        {item.website}
                      </Text>
                      <Text
                        style={{ color: "rgba(255,255,255,0.3)", fontSize: 11 }}
                      >
                        {item.username}
                      </Text>
                    </div>
                  </Space>
                  <Space>
                    <Tag
                      color="geekblue"
                      style={{
                        fontSize: 10,
                        borderRadius: 4,
                        margin: 0,
                        background: "rgba(47, 84, 235, 0.1)",
                        border: "1px solid rgba(47, 84, 235, 0.2)",
                      }}
                    >
                      AES-256
                    </Tag>
                    <Popconfirm
                      title="Delete Encrypted Secret?"
                      description="This action is permanent and cannot be undone."
                      onConfirm={() => handleDeleteCredential(item.id)}
                      okText="Delete"
                      cancelText="Keep"
                      okButtonProps={{ danger: true }}
                    >
                      <Button
                        type="text"
                        danger
                        size="small"
                        icon={
                          <ReloadOutlined
                            style={{ transform: "rotate(45deg)" }}
                          />
                        }
                      />
                    </Popconfirm>
                  </Space>
                </div>
              </Card>
            )}
          />
        </TabPane>

        <TabPane
          tab={
            <span>
              <BulbOutlined /> Learning Mode
            </span>
          }
          key="learning"
        >
          <div style={{ padding: 12 }}>
            <div
              style={{
                background: "rgba(255,255,255,0.03)",
                padding: 20,
                borderRadius: 16,
                border: "1px solid rgba(255,255,255,0.05)",
                marginBottom: 24,
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: 16,
                }}
              >
                <Title level={5} style={{ color: "#fff", margin: 0 }}>
                  System Autodidact
                </Title>
                <Button
                  type={isLearning ? "primary" : "default"}
                  onClick={() => toggleLearning(!isLearning)}
                  danger={isLearning}
                  style={{ borderRadius: 8 }}
                >
                  {isLearning ? "Disable" : "Enable"}
                </Button>
              </div>
              <Paragraph
                style={{ color: "rgba(255,255,255,0.4)", fontSize: 12 }}
              >
                When enabled, the system will automatically synthesize patterns,
                UI components, and business logic from every website visited to
                improve its app generation capabilities.
              </Paragraph>

              {learningStatus && (
                <Descriptions column={1} size="small" style={{ marginTop: 20 }}>
                  <Descriptions.Item
                    label={
                      <span style={{ color: "rgba(255,255,255,0.3)" }}>
                        Patterns Learned
                      </span>
                    }
                  >
                    <Text style={{ color: "#00f2fe" }}>
                      {learningStatus.patternsCount || 0}
                    </Text>
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={
                      <span style={{ color: "rgba(255,255,255,0.3)" }}>
                        Knowledge Depth
                      </span>
                    }
                  >
                    <Progress
                      percent={Math.min(
                        100,
                        (learningStatus.patternsCount || 0) * 2,
                      )}
                      size="small"
                      strokeColor="#00f2fe"
                    />
                  </Descriptions.Item>
                </Descriptions>
              )}
            </div>

            <div
              style={{
                background: "rgba(255,255,255,0.02)",
                padding: 16,
                borderRadius: 12,
                border: "1px solid rgba(255,255,255,0.05)",
                marginBottom: 20,
              }}
            >
              <Text
                style={{
                  color: "#faad14",
                  fontSize: 11,
                  fontWeight: 700,
                  display: "block",
                  marginBottom: 12,
                }}
              >
                <CodeOutlined style={{ marginRight: 8 }} /> SYNTHESIZED
                AUTOMATION LOGIC
              </Text>
              <div
                style={{
                  background: "#000",
                  padding: 12,
                  borderRadius: 8,
                  fontFamily: "monospace",
                  fontSize: 11,
                }}
              >
                <div style={{ color: "rgba(255,255,255,0.3)" }}>
                  // Autogenerated script for navigation pattern
                </div>
                <div style={{ color: "#52c41a" }}>function</div>{" "}
                <div style={{ color: "#00f2fe", display: "inline" }}>
                  onPageLoad
                </div>
                () {"{"}
                <div style={{ paddingLeft: 16, color: "#fff" }}>
                  await detectPrimaryAction();
                  <br />
                  const elements = findByAriaRole('button');
                  <br />
                  mapVisualHierarchy(elements);
                </div>
                {"}"}
              </div>
              <Text
                style={{
                  color: "rgba(255,255,255,0.2)",
                  fontSize: 10,
                  marginTop: 12,
                  display: "block",
                }}
              >
                সিস্টেম প্রতিটি ভিজিট থেকে এই ধরনের অটোমেশন স্ক্রিপ্ট তৈরি করে
                যা পরবর্তীতে নতুন অ্যাপ তৈরিতে সাহায্য করে।
              </Text>
            </div>

            <Alert
              message="Neural Feedback"
              description="The system has identified 12 new UI patterns in the last session."
              type="info"
              showIcon
              style={{
                background: "rgba(0, 242, 254, 0.05)",
                border: "1px solid rgba(0, 242, 254, 0.2)",
                color: "rgba(255,255,255,0.8)",
              }}
            />
          </div>
        </TabPane>
      </Tabs>
    </Drawer>
  );
};

export default BrowserSafetyDrawer;
