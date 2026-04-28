// AdminAPIKeys.tsx - API Key Management & Model Discovery

import React from 'react';
import AdminLayout from '../components/AdminLayout';
import APIKeysManager from '../components/APIKeysManager';

const AdminAPIKeys: React.FC = () => {
  return (
    <AdminLayout title="API Keys & Model Discovery">
      <APIKeysManager />
    </AdminLayout>
  );
};

export default AdminAPIKeys;
