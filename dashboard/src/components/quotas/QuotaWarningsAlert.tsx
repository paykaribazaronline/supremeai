import React from 'react';
import { Alert, Button, message } from 'antd';
import { WarningOutlined } from '@ant-design/icons';

interface QuotaWarningsAlertProps {
  warningsCount: number;
}

const QuotaWarningsAlert: React.FC<QuotaWarningsAlertProps> = ({ warningsCount }) => {
  if (warningsCount === 0) return null;

  return (
    <Alert
      message="কোটা সতর্কতা"
      description={`${warningsCount} জন ইউজার তাদের মাসিক কোটা সীমার কাছাকাছি পৌঁছেছেন।`}
      type="warning"
      showIcon
      icon={<WarningOutlined />}
      style={{ 
        marginBottom: 24, 
        borderRadius: 12, 
        background: 'rgba(245, 158, 11, 0.1)', 
        border: '1px solid rgba(245, 158, 11, 0.2)',
        color: '#f59e0b'
      }}
      action={
        <Button size="small" ghost onClick={() => message.info('সতর্কতা তালিকা চেক করুন')}>
          বিস্তারিত
        </Button>
      }
    />
  );
};

export default QuotaWarningsAlert;
