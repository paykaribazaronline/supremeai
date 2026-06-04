import { Modal, Form, Input, Select, Space, Button } from "antd";
import React from "react";

import { User } from "./types";

const { Option } = Select;

interface UserModalProps {
  open: boolean;
  editingUser: User | null;
  onCancel: () => void;
  onFinish: (values: any) => void;
  loading: boolean;
  form: any;
}

const UserModal: React.FC<UserModalProps> = ({
  open,
  editingUser,
  onCancel,
  onFinish,
  loading,
  form,
}) => {
  return (
    <Modal
      title={editingUser ? "Edit User Profile" : "Register New User"}
      open={open}
      onCancel={onCancel}
      footer={null}
      width={500}
      centered
      className="glass-modal"
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        requiredMark={false}
      >
        <Form.Item
          name="email"
          label="Email Address"
          rules={[
            {
              required: true,
              type: "email",
              message: "Please enter a valid email",
            },
          ]}
        >
          <Input
            placeholder="user@example.com"
            disabled={!!editingUser}
            style={{ borderRadius: "8px" }}
          />
        </Form.Item>

        <Form.Item name="displayName" label="Display Name">
          <Input placeholder="John Doe" style={{ borderRadius: "8px" }} />
        </Form.Item>

        {!editingUser && (
          <Form.Item
            name="password"
            label="Initial Password"
            rules={[
              {
                required: true,
                min: 6,
                message: "Password must be at least 6 characters",
              },
            ]}
          >
            <Input.Password
              placeholder="Secure password"
              style={{ borderRadius: "8px" }}
            />
          </Form.Item>
        )}

        <Form.Item
          name="tier"
          label="Subscription Tier / Role"
          rules={[{ required: true, message: "Please select a tier" }]}
        >
          <Select placeholder="Select tier" style={{ borderRadius: "8px" }}>
            <Option value="free">Free Tier</Option>
            <Option value="pro">Pro Subscriber</Option>
            <Option value="enterprise">Enterprise</Option>
            <Option value="admin">Administrator</Option>
          </Select>
        </Form.Item>

        <Form.Item
          style={{ marginTop: 32, marginBottom: 0, textAlign: "right" }}
        >
          <Space>
            <Button onClick={onCancel} style={{ borderRadius: "8px" }}>
              Cancel
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              style={{
                borderRadius: "8px",
                paddingLeft: "24px",
                paddingRight: "24px",
              }}
            >
              {editingUser ? "Save Changes" : "Create User"}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default UserModal;
