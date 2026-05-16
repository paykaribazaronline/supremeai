import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Space } from 'antd';
import { Project } from './types';

const { Option } = Select;

interface ProjectModalProps {
  visible: boolean;
  editingProject: Project | null;
  onCancel: () => void;
  onSubmit: (values: any) => void;
}

const ProjectModal: React.FC<ProjectModalProps> = ({
  visible,
  editingProject,
  onCancel,
  onSubmit
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      if (editingProject) {
        form.setFieldsValue(editingProject);
      } else {
        form.resetFields();
      }
    }
  }, [visible, editingProject, form]);

  const handleFinish = (values: any) => {
    onSubmit(values);
  };

  return (
    <Modal
      title={editingProject ? 'Edit Project' : 'Create New Project'}
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={500}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item 
          name="name" 
          label="Project Name" 
          rules={[{ required: true, message: 'Please input project name!' }]}
        >
          <Input placeholder="My AI Project" />
        </Form.Item>
        
        <Form.Item name="description" label="Description">
          <Input.TextArea rows={3} placeholder="Brief description of the project..." />
        </Form.Item>
        
        {editingProject && (
          <Form.Item name="status" label="Status" rules={[{ required: true }]}>
            <Select>
              <Option value="ACTIVE">Active</Option>
              <Option value="PAUSED">Paused</Option>
              <Option value="COMPLETED">Completed</Option>
              <Option value="FAILED">Failed</Option>
            </Select>
          </Form.Item>
        )}
        
        <Form.Item style={{ marginTop: 24, marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={onCancel}>Cancel</Button>
            <Button type="primary" htmlType="submit">
              {editingProject ? 'Update Project' : 'Create Project'}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ProjectModal;
