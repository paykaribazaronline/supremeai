// AdminAPIKeys.tsx - API Key Management & Model Discovery

import React from 'react';
import AdminLayout from '../components/AdminLayout';
import APIManagement from '../components/APIManagement';

const AdminAPIKeys: React.FC = () => {
  return (
    <AdminLayout title="API Keys & Model Discovery">
      <APIManagement />
    </AdminLayout>
  );
};

export default AdminAPIKeys;
