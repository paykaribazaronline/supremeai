import React, { useState, useRef } from 'react';
import { Card, Button, Typography, Space, message, Upload, Divider, Progress, Tag, Spin } from 'antd';
import { AudioOutlined, UploadOutlined, PlayCircleOutlined, StopOutlined, CloudUploadOutlined } from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';

const { Title, Text, Paragraph } = Typography;

export default function AdminVoiceStudio() {
  const [recording, setRecording] = useState(false);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [transcription, setTranscription] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const mediaRecorder = useRef<MediaRecorder | null>(null);
  const audioChunks = useRef<Blob[]>([]);

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaRecorder.current = new MediaRecorder(stream);
      audioChunks.current = [];

      mediaRecorder.current.ondataavailable = (event) => {
        audioChunks.current.push(event.data);
      };

      mediaRecorder.current.onstop = () => {
        const audioBlob = new Blob(audioChunks.current, { type: 'audio/wav' });
        const url = URL.createObjectURL(audioBlob);
        setAudioUrl(url);
      };

      mediaRecorder.current.start();
      setRecording(true);
      message.info('রেকর্ডিং শুরু হয়েছে...');
    } catch (err) {
      console.error("Error accessing microphone:", err);
      message.error('মাইক্রোফোন অ্যাক্সেস করা সম্ভব হয়নি।');
    }
  };

  const stopRecording = () => {
    if (mediaRecorder.current) {
      mediaRecorder.current.stop();
      setRecording(false);
      message.success('রেকর্ডিং শেষ হয়েছে।');
    }
  };

  const handleTranscribe = async () => {
    if (!audioChunks.current.length) {
      message.warning('প্রথমে কিছু রেকর্ড করুন অথবা আপলোড করুন।');
      return;
    }

    setLoading(true);
    try {
      const audioBlob = new Blob(audioChunks.current, { type: 'audio/wav' });
      const formData = new FormData();
      formData.append('file', audioBlob, 'voice.wav');

      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/voice/transcribe`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) throw new Error('Transcription failed');

      const data = await response.json();
      setTranscription(data.text || 'কোনো টেক্সট পাওয়া যায়নি।');
      message.success('ট্রান্সক্রিপশন সফল হয়েছে!');
    } catch (err) {
      console.error(err);
      message.error('ভয়েস প্রসেস করতে সমস্যা হয়েছে।');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="voice-studio-container" style={{ maxWidth: 1200, margin: '0 auto', padding: '20px' }}>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 30 }}>
          <div>
            <Title level={2} className="neon-text" style={{ margin: 0 }}>ভয়েস স্টুডিও (VOICE_STUDIO)</Title>
            <Text style={{ color: 'var(--text-dim)' }}>নিউরাল ভয়েস প্রসেসিং এবং ট্রান্সক্রিপশন হাব</Text>
          </div>
          <Tag color="cyan" style={{ fontSize: 14, padding: '4px 12px' }}>CLOUD_SYNC_ACTIVE</Tag>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 30 }}>
          {/* Audio Input Section */}
          <Card 
            className="glass-panel" 
            style={{ borderRadius: 16, border: '1px solid rgba(0, 243, 255, 0.1)' }}
          >
            <Title level={4} style={{ color: 'var(--neon-blue)' }}>
              <AudioOutlined /> ভয়েস ইনপুট
            </Title>
            <Paragraph style={{ color: 'var(--text-dim)' }}>
              আপনার কণ্ঠস্বর রেকর্ড করুন অথবা একটি অডিও ফাইল আপলোড করুন।
            </Paragraph>

            <div style={{ 
              height: 200, 
              background: 'rgba(0,0,0,0.3)', 
              borderRadius: 12, 
              display: 'flex', 
              flexDirection: 'column',
              alignItems: 'center', 
              justifyContent: 'center',
              border: '1px dashed rgba(0,243,255,0.3)',
              marginBottom: 20,
              position: 'relative',
              overflow: 'hidden'
            }}>
              {recording && (
                <motion.div 
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ repeat: Infinity, duration: 1 }}
                  style={{ 
                    width: 80, 
                    height: 80, 
                    borderRadius: '50%', 
                    background: 'rgba(255, 0, 0, 0.2)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  <div style={{ width: 40, height: 40, borderRadius: '50%', background: '#ff4d4f' }} />
                </motion.div>
              )}
              
              {!recording && !audioUrl && (
                <Text style={{ color: '#555' }}>অডিও ডেটা নেই</Text>
              )}

              {audioUrl && !recording && (
                <audio src={audioUrl} controls style={{ width: '90%' }} />
              )}
            </div>

            <Space size="middle" style={{ width: '100%', justifyContent: 'center' }}>
              {!recording ? (
                <Button 
                  type="primary" 
                  shape="round" 
                  icon={<AudioOutlined />} 
                  size="large"
                  onClick={startRecording}
                  className="cyber-button"
                >
                  রেকর্ড শুরু করুন
                </Button>
              ) : (
                <Button 
                  danger 
                  shape="round" 
                  icon={<StopOutlined />} 
                  size="large"
                  onClick={stopRecording}
                >
                  থামান
                </Button>
              )}

              <Upload 
                showUploadList={false} 
                beforeUpload={(file) => {
                  const url = URL.createObjectURL(file);
                  setAudioUrl(url);
                  audioChunks.current = [file];
                  return false;
                }}
              >
                <Button icon={<UploadOutlined />} shape="round" size="large" ghost>ফাইল আপলোড</Button>
              </Upload>
            </Space>
          </Card>

          {/* Transcription Result Section */}
          <Card 
            className="glass-panel" 
            style={{ borderRadius: 16, border: '1px solid rgba(188, 122, 250, 0.1)' }}
          >
            <Title level={4} style={{ color: 'var(--neon-purple)' }}>
              <CloudUploadOutlined /> নিউরাল ট্রান্সক্রিপশন
            </Title>
            <Paragraph style={{ color: 'var(--text-dim)' }}>
              Voicebox AI ব্যবহার করে অডিও থেকে টেক্সটে রূপান্তর করুন।
            </Paragraph>

            <div style={{ 
              minHeight: 180, 
              background: 'rgba(255,255,255,0.02)', 
              padding: 20, 
              borderRadius: 12,
              border: '1px solid rgba(255,255,255,0.05)',
              marginBottom: 20,
              fontSize: 16,
              lineHeight: 1.6,
              color: transcription ? '#fff' : '#444'
            }}>
              {loading ? (
                <div style={{ textAlign: 'center', paddingTop: 40 }}>
                  <Spin tip="AI প্রসেস করছে..." />
                </div>
              ) : (
                transcription || 'ফলাফল এখানে প্রদর্শিত হবে...'
              )}
            </div>

            <Button 
              type="primary" 
              block 
              size="large" 
              icon={<PlayCircleOutlined />} 
              onClick={handleTranscribe}
              loading={loading}
              disabled={!audioUrl}
              style={{ background: 'linear-gradient(90deg, #bc7afa, #00f3ff)', border: 'none' }}
            >
              ট্রান্সক্রাইব করুন
            </Button>
          </Card>
        </div>

        {/* Synthesis Section (Future) */}
        <Card 
          className="glass-panel" 
          style={{ marginTop: 30, borderRadius: 16 }}
        >
          <div style={{ display: 'flex', alignItems: 'center', opacity: 0.5 }}>
            <Title level={4} style={{ margin: 0, marginRight: 10 }}>ভয়েস সিন্থেসিস (TTS)</Title>
            <Tag>SOON</Tag>
          </div>
          <Divider style={{ margin: '15px 0' }} />
          <Text italic style={{ color: '#666' }}>Voicebox TTS মডিউল কনফিগার করা হচ্ছে। পরবর্তী আপডেটে এটি পাওয়া যাবে।</Text>
        </Card>
      </motion.div>
    </div>
  );
}
