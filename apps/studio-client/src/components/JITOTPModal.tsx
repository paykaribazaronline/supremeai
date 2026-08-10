// apps/studio-client/src/components/JITOTPModal.tsx
// বাংলা মন্তব্য: JIT OTP ভেরিফিকেশন মোডাল কম্পোনেন্ট — সেনসিটিভ অপারেশনের সময় ৬ ডিজিটের অন-স্পট ওটিপি ভেরিফিকেশনের সুন্দর ইন্টারফেস।

import { useState, useEffect } from 'react';
import type { FC } from 'react';
import { apiClient } from '../services/apiClient';

interface JITOTPModalProps {
  isOpen: boolean;
  onClose: () => void;
  onVerify: (resultData: any) => void;
  actionDescription?: string;
  targetEndpoint?: string;
  actionPayload?: any;
}

export const JITOTPModal: FC<JITOTPModalProps> = ({
  isOpen,
  onClose,
  onVerify,
  actionDescription = 'sensitive operation',
  targetEndpoint = '/api/secure/action',
  actionPayload = {},
}) => {
  const [otp, setOtp] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(300); // 5 minutes
  const [attempts, setAttempts] = useState(0);
  const maxAttempts = 3;

  useEffect(() => {
    if (isOpen && timeLeft > 0) {
      const timer = setInterval(() => {
        setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [isOpen, timeLeft]);

  if (!isOpen) return null;

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const handleVerify = async () => {
    if (!otp || otp.length !== 6) {
      setError('Please enter a valid 6-digit OTP code.');
      return;
    }

    if (attempts >= maxAttempts) {
      setError('Maximum verification attempts exceeded.');
      return;
    }

    setIsVerifying(true);
    setError('');

    try {
      const result: any = await apiClient.performSensitiveAction(
        targetEndpoint,
        actionPayload,
        otp
      );

      if (result && !result.requiresOTP) {
        onVerify(result);
        onClose();
      } else {
        setAttempts((prev) => prev + 1);
        setError(result?.message || 'OTP verification failed. Please check the code.');
      }
    } catch (err: any) {
      setAttempts((prev) => prev + 1);
      setError(err?.message || 'Verification request failed. Try again.');
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: '#111827',
          border: '1px solid #374151',
          borderRadius: '16px',
          padding: '28px',
          maxWidth: '420px',
          width: '90%',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
          color: '#f9fafb',
          fontFamily: 'system-ui, -apple-system, sans-serif',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            🔐 Just-In-Time Verification
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#9ca3af',
              fontSize: '20px',
              cursor: 'pointer',
            }}
          >
            ×
          </button>
        </div>

        <p style={{ fontSize: '14px', color: '#9ca3af', marginTop: 0, marginBottom: '20px' }}>
          This operation (<strong style={{ color: '#e5e7eb' }}>{actionDescription}</strong>) requires JIT OTP authorization.
        </p>

        <div style={{ marginBottom: '20px' }}>
          <input
            type="text"
            maxLength={6}
            value={otp}
            onChange={(e) => {
              const val = e.target.value.replace(/\D/g, '');
              setOtp(val);
              if (val.length === 6) {
                setTimeout(handleVerify, 100);
              }
            }}
            placeholder="Enter 6-digit OTP"
            style={{
              width: '100%',
              padding: '12px 16px',
              fontSize: '20px',
              letterSpacing: '6px',
              textAlign: 'center',
              borderRadius: '8px',
              background: '#1f2937',
              border: '1px solid #4b5563',
              color: '#ffffff',
              outline: 'none',
              boxSizing: 'border-box',
            }}
            disabled={isVerifying || attempts >= maxAttempts}
            autoFocus
          />
        </div>

        {error && (
          <div style={{ color: '#f87171', fontSize: '13px', marginBottom: '16px', textAlign: 'center' }}>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px', color: '#6b7280', marginBottom: '20px' }}>
          <span>⏱️ Expires in: {formatTime(timeLeft)}</span>
          <span>Attempts: {attempts}/{maxAttempts}</span>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={handleVerify}
            disabled={isVerifying || otp.length !== 6 || attempts >= maxAttempts}
            style={{
              flex: 1,
              padding: '10px 16px',
              borderRadius: '8px',
              background: isVerifying || otp.length !== 6 ? '#374151' : '#3b82f6',
              color: '#ffffff',
              border: 'none',
              fontWeight: 600,
              cursor: isVerifying || otp.length !== 6 ? 'not-allowed' : 'pointer',
            }}
          >
            {isVerifying ? 'Verifying...' : 'Verify & Continue'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default JITOTPModal;
