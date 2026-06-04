import { Card, Form, Switch, Button, message } from "antd";
import React from "react";

interface NotificationSettingsCardProps {
  form: any;
  onFinish: (values: any) => void;
  saving: boolean;
}

const NotificationSettingsCard: React.FC<NotificationSettingsCardProps> = ({
  form,
  onFinish,
  saving,
}) => {
  return (
    <Card
      className="glass-card"
      style={{ marginTop: 16, borderRadius: "12px" }}
      title="Communication Preferences"
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "20px",
            marginBottom: "32px",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div>
              <div style={{ fontWeight: 500 }}>Email Notifications</div>
              <div style={{ fontSize: "12px", opacity: 0.6 }}>
                Send system updates and security alerts via email
              </div>
            </div>
            <Form.Item
              name="emailNotifications"
              valuePropName="checked"
              style={{ marginBottom: 0 }}
            >
              <Switch />
            </Form.Item>
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div>
              <div style={{ fontWeight: 500 }}>SMS Critical Alerts</div>
              <div style={{ fontSize: "12px", opacity: 0.6 }}>
                Immediate mobile alerts for system failures
              </div>
            </div>
            <Form.Item
              name="smsAlerts"
              valuePropName="checked"
              style={{ marginBottom: 0 }}
            >
              <Switch />
            </Form.Item>
          </div>
        </div>

        <Form.Item style={{ marginTop: 24, marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={saving}
            style={{
              borderRadius: "8px",
              paddingLeft: "32px",
              paddingRight: "32px",
            }}
          >
            Save Preferences
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default NotificationSettingsCard;
