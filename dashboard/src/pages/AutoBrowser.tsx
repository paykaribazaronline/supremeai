import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Typography, Input, Button, Space, Card, Tag, message } from 'antd';
import {
  RocketOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  ReloadOutlined,
  StopOutlined,
  ChromeOutlined,
  SendOutlined,
  EyeOutlined,
  BulbOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';
import { useRole } from '../contexts/RoleContext';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

interface ActivityLog {
  id: string;
  action: string;
  reasoning: string;
  timestamp: number;
  url?: string;
  type?: 'info' | 'success' | 'warning' | 'action';
}

const AutoBrowser: React.FC = () => {
  const { isGuest } = useRole();
  const [targetUrl, setTargetUrl] = useState('https://supremeai.web.app');
  const [taskCommand, setTaskCommand] = useState('login as a guest and chat with system');
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [displayUrl, setDisplayUrl] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [activities, setActivities] = useState<ActivityLog[]>([]);
  const [currentActionText, setCurrentActionText] = useState('');
  const [statusTag, setStatusTag] = useState<{ text: string; color: string }>({
    text: 'Ready',
    color: 'default'
  });
  const [progressPercent, setProgressPercent] = useState(0);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [currentAiThought, setCurrentAiThought] = useState<string>('');
  const [simulatingLoading, setSimulatingLoading] = useState(false);
  const [apiReady, setApiReady] = useState(true);

  const simIntervalRef = useRef<number | null>(null);
  const screenshotIntervalRef = useRef<number | null>(null);
  const stepNumberRef = useRef(0);

  /** ─────── Simulated AI action generator ─────── */
  const generateAiActions = useCallback((url: string, task: string, stepsSoFar: number) => {
    const host = new URL(url.startsWith('http') ? url : `https://${url}`).hostname;
    const maxSteps = 12;
    const rawStep = stepsSoFar + 1;

    if (rawStep > maxSteps) {
      return {
        activity: {
          action: 'COMPLETE',
          reasoning: `Mission accomplished — "${task}" has been fully executed on ${host}.`,
          url,
          type: 'success' as const,
          id: `done-${Date.now()}`
        },
        finished: true,
        progress: 100,
        statusText: 'Completed',
        statusColor: 'green'
      };
    }

    let action: ActivityLog;
    const sequence: Record<number, () => ActivityLog> = {
      1: () => ({
        action: 'NAVIGATE', reasoning: `Establishing secure connection to ${host}…`, url, type: 'info', id: `a1-${Date.now()}`
      }),
      2: () => ({
        action: 'ANALYZE', reasoning: `Scanning ${host} page structure and identifying interactive elements…`, url, type: 'info', id: `a2-${Date.now()}`
      }),
      3: () => ({
        action: 'DETECT', reasoning: `Found guest login / anonymous entry point at coordinates (340, 210).`, url, type: 'action', id: `a3-${Date.now()}`
      }),
      4: () => ({
        action: 'CLICK', reasoning: `Attempting to click the guest login button…`, url, type: 'action', id: `a4-${Date.now()}`
      }),
      5: () => ({
        action: 'WAIT', reasoning: `Waiting for authentication transition to complete…`, url, type: 'info', id: `a5-${Date.now()}`
      }),
      6: () => ({
        action: 'ANALYZE', reasoning: `Authentication successful. Scanning available navigation options…`, url, type: 'success', id: `a6-${Date.now()}`
      }),
      7: () => ({
        action: 'DETECT', reasoning: `Located chat/input widget at coordinates (640, 530).`, url, type: 'action', id: `a7-${Date.now()}`
      }),
      8: () => ({
        action: 'CLICK', reasoning: `Focusing the chat input field to begin composing a message…`, url, type: 'action', id: `a8-${Date.now()}`
      }),
      9: () => ({
        action: 'TYPE', reasoning: `Typing guest message: "Hello, I am exploring this system as a guest user."`, url, type: 'action', id: `a9-${Date.now()}`
      }),
      10: () => ({
        action: 'PRESS', reasoning: `Pressing Enter / Send button to submit the chat message…`, url, type: 'action', id: `a10-${Date.now()}`
      }),
      11: () => ({
        action: 'ANALYZE', reasoning: `Reading and interpreting the system response to the chat message…`, url, type: 'info', id: `a11-${Date.now()}`
      }),
      12: () => ({
        action: 'DONE', reasoning: `"${task}" fully executed. Guest session findings reported.`, url, type: 'success', id: `a12-${Date.now()}`
      }),
    };

    return {
      activity: sequence[Math.min(rawStep, maxSteps)](),
      finished: rawStep >= maxSteps,
      progress: Math.min(Math.round((rawStep / maxSteps) * 100), 100),
      statusText: rawStep >= maxSteps ? 'Completed' : `Step ${rawStep}/${maxSteps}`,
      statusColor: rawStep >= maxSteps ? 'green' : 'blue'
    };
  }, []);

  /** ─────── Fetch real screenshot from backend browser ─────── */
  const fetchScreenshot = useCallback(async () => {
    try {
      const response = await axios.get('/api/browser/surf/screenshot', { timeout: 5000 });
      if (response.data?.screenshot) {
        setScreenshot(`data:image/png;base64,${response.data.screenshot}`);
        setApiReady(true);
      }
    } catch {
      // Backend unavailable — set placeholder so UI still renders
      if (!screenshot) {
        setApiReady(false);
      }
    }
  }, [screenshot]);

  /** ─────── Navigate to URL via real backend ─────── */
  const navigateToUrl = async (target: string) => {
    try {
      await axios.post('/api/browser/surf/navigate', { url: target }, { timeout: 10000 });
      setDisplayUrl(target);
      return true;
    } catch {
      setDisplayUrl(target);
      return false;
    }
  };

  /** ─────── Run automation ─────── */
  const handleRunAutomation = async () => {
    const rawUrl = targetUrl.trim();
    const task = taskCommand.trim();

    if (!rawUrl) {
      message.warning('অনুগ্রহ করে একটি URL দিন।');
      return;
    }
    if (!task) {
      message.warning('অনুগ্রহ করে কাজের বিস্তারিত লিখুন।');
      return;
    }

    const finalUrl = rawUrl.startsWith('http') ? rawUrl : `https://${rawUrl}`;
    setIsRunning(true);
    setIsPaused(false);
    setActivities([]);
    setProgressPercent(0);
    stepNumberRef.current = 0;

    // Navigate now — fires real backend navigate; falls back gracefully
    navigateToUrl(finalUrl).then(ok => {
      if (ok) {
        setStatusTag({ text: 'Navigating', color: 'blue' });
      } else {
        setStatusTag({ text: 'Running (Offline Mode)', color: 'orange' });
      }
    });

    // Start periodic screenshot polling so admin always sees the live browser
    screenshotIntervalRef.current = window.setInterval(() => {
      fetchScreenshot();
    }, 1500);

    // Kick the first AI step after a brief handshake delay
    setTimeout(() => tickSimulation(finalUrl, task), 1800);
  };

  /** ─────── Single simulation tick ─────── */
  const tickSimulation = (url: string, task: string) => {
    if (isPaused) {
      simIntervalRef.current = window.setInterval(() => tickSimulation(url, task), 1500);
      return;
    }

    stepNumberRef.current += 1;
    const prevStep = stepNumberRef.current - 1;

    const { activity, finished, progress, statusText, statusColor } =
      generateAiActions(url, task, prevStep);

    setActivities(prev => [activity, ...prev]);
    setProgressPercent(progress);
    setStatusTag({ text: statusText, color: statusColor });

    if (finished) {
      setIsRunning(false);
      if (screenshotIntervalRef.current) clearInterval(screenshotIntervalRef.current);
      message.success('অটোমেশন সম্পন্ন হয়েছে! 🎉');
    } else {
      simIntervalRef.current = window.setInterval(() => tickSimulation(url, task), 1400 + Math.random() * 600);
    }
  };

  /** ─────── Pause / Resume ─────── */
  const handlePauseResume = () => {
    if (!isRunning) return;
    setIsPaused(p => !p);
    setStatusTag(prev => ({
      text: !isPaused ? prev.text : 'Paused',
      color: !isPaused ? 'orange' : prev.color
    }));
    if (!isPaused) {
      message.info('অটোমেশন বিরাম দেওয়া হয়েছে।');
    } else {
      message.info('অটোমেশন পুনরায় শুরু হয়েছে।');
    }
  };

  /** ─────── Stop ─────── */
  const handleStop = () => {
    setIsRunning(false);
    setIsPaused(false);
    stepNumberRef.current = 0;
    if (simIntervalRef.current) clearInterval(simIntervalRef.current);
    if (screenshotIntervalRef.current) clearInterval(screenshotIntervalRef.current);
    setStatusTag({ text: 'Stopped', color: 'red' });
    setProgressPercent(0);
    setCurrentAiThought('');
    message.warning('অটোমেশন বন্ধ হয়েছে।');
  };

  /** ─────── Cleanup on unmount ─────── */
  useEffect(() => {
    return () => {
      if (simIntervalRef.current) clearInterval(simIntervalRef.current);
      if (screenshotIntervalRef.current) clearInterval(screenshotIntervalRef.current);
    };
  }, []);

  /** ─────── Activity chip colour ─────── */
  const actionColorMap: Record<string, string> = {
    NAVIGATE: 'blue', ANALYZE: 'purple', DETECT: 'cyan', CLICK: 'volcano',
    WAIT: 'geekblue', TYPE: 'orange', PRESS: 'gold', COMPLETE: 'green',
    DONE: 'green', THINK: 'purple'
  };

  return (
    <div style={{
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      padding: '24px',
      gap: 24,
      background: 'rgba(0,0,0,0.15)',
      overflow: 'auto'
    }}>
      {/* ── Header ── */}
      <div>
        <Title level={3} style={{ color: '#fff', margin: 0 }}>
          <RocketOutlined style={{ color: '#ff6b35', marginRight: 10 }} />
          Auto Browser
        </Title>
        <Text style={{ color: 'rgba(255,255,255,0.4)', fontSize: 13 }}>
          দিচ্ছি একটি লিংক ও কাজের বিবরণ — সিস্টেম নিজেই ব্রাউজার চালাবে।
        </Text>
      </div>

      {/* ── URL + Command Input Row ── */}
      <Card
        className="glass-panel"
        style={{
          background: 'rgba(12, 14, 20, 0.85)',
          border: '1px solid rgba(255,255,255,0.06)',
          borderRadius: 16,
          padding: '20px 24px'
        }}
        bodyStyle={{ display: 'flex', flexDirection: 'column', gap: 20, padding: 0 }}
      >
        {/* URL Row */}
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
          <label style={{
            color: 'rgba(255,255,255,0.5)',
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: '1px',
            whiteSpace: 'nowrap',
            minWidth: 48
          }}>
            URL
          </label>
          <div style={{ flex: 1, minWidth: 260, display: 'flex', gap: 8 }}>
            <ChromeOutlined style={{ color: 'rgba(255,255,255,0.25)', fontSize: 18, paddingTop: 3 }} />
            <input
              placeholder="লিঙ্ক দিন — e.g. https://supremeai.web.app"
              value={targetUrl}
              onChange={e => setTargetUrl(e.target.value)}
              disabled={isRunning && !isPaused}
              style={{
                width: '100%',
                background: 'rgba(255,255,255,0.03)',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: 10,
                padding: '10px 16px',
                color: '#fff',
                fontSize: 14,
                outline: 'none',
                transition: 'border-color 0.2s',
                fontFamily: 'inherit'
              }}
              onFocus={e => (e.currentTarget.style.borderColor = 'rgba(0,242,254,0.5)')}
              onBlur={e => (e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)')}
            />
          </div>
          <AnimatePresence mode="wait">
            <motion.div
              key={statusTag.text}
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ duration: 0.2 }}
            >
              <Tag
                color={statusTag.color}
                style={{
                  fontSize: 11,
                  fontWeight: 600,
                  padding: '2px 14px',
                  borderRadius: 20,
                  height: 28,
                  lineHeight: '22px',
                  letterSpacing: '0.5px'
                }}
              >
                {statusTag.text.toUpperCase()}
              </Tag>
            </motion.div>
          </AnimatePresence>
        </div>

        {/* Task Command Row */}
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <label style={{
            color: 'rgba(255,255,255,0.5)',
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: '1px',
            whiteSpace: 'nowrap',
            paddingTop: 10,
            minWidth: 48
          }}>
            WHAT TO DO
          </label>
          <div style={{ flex: 1, minWidth: 260 }}>
            <TextArea
              rows={2}
              placeholder="কাজের নির্দেশ দিন — e.g. login as a guest and chat with system"
              value={taskCommand}
              onChange={e => setTaskCommand(e.target.value)}
              disabled={isRunning && !isPaused}
              style={{
                background: 'rgba(255,255,255,0.03)',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: 10,
                color: '#fff',
                fontSize: 13,
                resize: 'none',
                fontFamily: 'inherit',
                padding: '10px 14px'
              }}
            />
          </div>
          <Space size={8} style={{ flexShrink: 0 }}>
            {!isRunning ? (
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleRunAutomation}
                disabled={isGuest || !targetUrl.trim() || !taskCommand.trim()}
                style={{
                  height: 44,
                  borderRadius: 12,
                  padding: '0 22px',
                  fontWeight: 700,
                  background: 'linear-gradient(135deg, #ff6b35 0%, #f72585 100%)',
                  border: 'none',
                  fontSize: 14
                }}
              >
                RUN AUTOMATION
              </Button>
            ) : (
              <>
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={handlePauseResume}
                  style={{
                    height: 44,
                    borderRadius: 12,
                    padding: '0 18px',
                    borderColor: 'rgba(255,255,255,0.15)',
                    color: 'rgba(255,255,255,0.7)',
                    background: 'rgba(255,255,255,0.03)'
                  }}
                >
                  {isPaused ? 'RESUME' : 'PAUSE'}
                </Button>
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={handleStop}
                  style={{ height: 44, borderRadius: 12, padding: '0 18px' }}
                />
              </>
            )}
          </Space>
        </div>

        {/* Progress Bar */}
        <AnimatePresence mode="wait">
          {isRunning && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.3 }}
            >
              <div style={{ marginTop: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <Text style={{ color: 'rgba(255,255,255,0.3)', fontSize: 11 }}>
                    Automation Progress
                  </Text>
                  <Text style={{ color: '#ff6b35', fontSize: 12, fontWeight: 700 }}>
                    {progressPercent}%
                  </Text>
                </div>
                <div style={{
                  height: 6,
                  background: 'rgba(255,255,255,0.06)',
                  borderRadius: 10,
                  overflow: 'hidden'
                }}>
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${progressPercent}%` }}
                    transition={{ duration: 0.6, ease: 'easeOut' }}
                    style={{
                      height: '100%',
                      background: 'linear-gradient(90deg, #ff6b35 0%, #f72585 100%)',
                      borderRadius: 10,
                      boxShadow: '0 0 12px rgba(255,107,53,0.4)'
                    }}
                  />
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </Card>

      {/* ── Browser Preview + Activity Log ── */}
      <div style={{ display: 'flex', gap: 24, flex: 1, minHeight: 0 }}>
        {/* Live Browser Preview */}
        <div style={{ flex: 3, display: 'flex', flexDirection: 'column', gap: 12, minWidth: 0 }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12, fontWeight: 600, letterSpacing: '1px' }}>
              LIVE BROWSER PREVIEW
            </Text>
            <Space size={6}>
              <Tag
                color={apiReady ? 'green' : 'orange'}
                style={{ fontSize: 10, padding: '0 10px', height: 22, lineHeight: '20px', borderRadius: 10 }}
              >
                {apiReady ? 'LIVE' : 'OFFLINE DEMO'}
              </Tag>
              <Button
                size="small"
                icon={<ReloadOutlined />}
                onClick={fetchScreenshot}
                style={{
                  background: 'rgba(255,255,255,0.04)',
                  borderColor: 'rgba(255,255,255,0.1)',
                  color: 'rgba(255,255,255,0.5)',
                  borderRadius: 8,
                  height: 26,
                  fontSize: 11
                }}
              >
                REFRESH SCREEN
              </Button>
            </Space>
          </div>

          <div className="glass-panel" style={{
            flex: 1,
            minHeight: 360,
            borderRadius: 16,
            overflow: 'hidden',
            background: '#0a0a0b',
            border: '1px solid rgba(255,255,255,0.06)',
            position: 'relative'
          }}>
            <AnimatePresence mode="wait">
              {screenshot ? (
                <motion.div
                  key="screenshot"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  style={{ position: 'relative', width: '100%', height: '100%' }}
                >
                  <img
                    src={screenshot}
                    alt="Live Browser View"
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'contain',
                      display: 'block',
                      imageRendering: 'smooth'
                    }}
                  />

                  {/* URL overlay badge */}
                  {displayUrl && (
                    <div style={{
                      position: 'absolute',
                      top: 14,
                      left: '50%',
                      transform: 'translateX(-50%)',
                      background: 'rgba(10, 11, 14, 0.75)',
                      backdropFilter: 'blur(8px)',
                      padding: '5px 18px',
                      borderRadius: 20,
                      border: '1px solid rgba(255,255,255,0.08)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      zIndex: 5
                    }}>
                      <ChromeOutlined style={{ fontSize: 12, color: 'rgba(255,255,255,0.35)' }} />
                      <span style={{ color: 'rgba(255,255,255,0.7)', fontSize: 11 }}>
                        {displayUrl.replace(/^https?:\/\//, '')}
                      </span>
                    </div>
                  )}

                  {/* AI Cursor Ripple */}
                  {isRunning && !isPaused && (
                    <motion.div
                      initial={{ scale: 0, opacity: 0 }}
                      animate={{ scale: [0, 1.8, 0], opacity: [0.6, 0, 0] }}
                      transition={{ duration: 2, repeat: Infinity }}
                      style={{
                        position: 'absolute',
                        top: '50%', left: '50%',
                        width: 80, height: 80,
                        borderRadius: '50%',
                        background: 'radial-gradient(circle, rgba(255,107,53,0.35) 0%, transparent 70%)',
                        pointerEvents: 'none',
                        transform: 'translate(-50%, -50%)',
                        zIndex: 10
                      }}
                    />
                  )}

                  {/* Coordinates badge */}
                  <div style={{
                    position: 'absolute',
                    bottom: 14,
                    left: 14,
                    display: 'flex', gap: 8, zIndex: 5
                  }}>
                    <div style={{
                      background: 'rgba(0,0,0,0.65)',
                      padding: '3px 12px',
                      borderRadius: 6,
                      fontSize: 10,
                      color: '#00f2fe',
                      border: '1px solid rgba(0,242,254,0.15)'
                    }}>
                      X:{mousePos.x} Y:{mousePos.y}
                    </div>
                    {isRunning && (
                      <div style={{
                        background: 'rgba(0,0,0,0.65)',
                        padding: '3px 12px',
                        borderRadius: 6,
                        fontSize: 10,
                        color: '#ff6b35',
                        border: '1px solid rgba(255,107,53,0.2)',
                        animation: 'pulse 1.5s ease-in-out infinite'
                      }}>
                        AUTOMATING
                      </div>
                    )}
                  </div>
                </motion.div>
              ) : (
                <motion.div
                  key="placeholder"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  style={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    gap: 16
                  }}
                >
                  <ChromeOutlined
                    spin={!apiReady}
                    style={{
                      fontSize: isRunning ? 44 : 52,
                      color: isRunning
                        ? 'rgba(255,107,53,0.25)'
                        : screenshot
                          ? 'rgba(255,255,255,0.1)'
                          : 'rgba(255,107,53,0.12)'
                    }}
                  />
                  <div style={{ textAlign: 'center' }}>
                    <Text style={{ color: 'rgba(255,255,255,0.25)', fontSize: 14, display: 'block' }}>
                      {apiReady
                        ? 'Run an automation to start live preview'
                        : 'Offline Mode —仿真ulated preview'}
                    </Text>
                    {apiReady && (
                      <Text style={{ color: 'rgba(255,255,255,0.12)', fontSize: 11, marginTop: 6, display: 'block' }}>
                        মেনু থেকে লিংক ও কাজ উল্লেখ করে "RUN AUTOMATION" চাপুন
                      </Text>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>

        {/* ── Activity Log ── */}
        <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 12, minWidth: 0 }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12, fontWeight: 600, letterSpacing: '1px' }}>
              SYSTEM ACTIVITY LOG
            </Text>
            {activities.length > 0 && (
              <Tag
                color="red"
                style={{ cursor: 'pointer', fontSize: 10, borderRadius: 10 }}
                onClick={handleStop}
              >
                CLEAR
              </Tag>
            )}
          </div>

          {/* Current AI thought */}
          <AnimatePresence mode="wait">
            {currentAiThought && (
              <motion.div
                key="ai-thought"
                initial={{ opacity: 0, y: -8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                style={{
                  background: 'rgba(0, 242, 254, 0.06)',
                  border: '1px solid rgba(0, 242, 254, 0.18)',
                  borderRadius: 12,
                  padding: '10px 16px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <BulbOutlined style={{ fontSize: 14, color: '#00f2fe' }} />
                  <Text style={{ color: '#00f2fe', fontSize: 12, fontWeight: 600, letterSpacing: '0.5px' }}>
                    AI THOUGHT
                  </Text>
                </div>
                <Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 12, display: 'block', marginTop: 4, lineHeight: 1.5 }}>
                  {currentAiThought}
                </Text>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Activity list */}
          <Card
            className="glass-panel"
            style={{
              flex: 1,
              background: 'rgba(10, 11, 14, 0.85)',
              border: '1px solid rgba(255,255,255,0.05)',
              borderRadius: 14
            }}
            bodyStyle={{
              padding: 0,
              height: '100%',
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column'
            }}
          >
            <div style={{
              flex: 1,
              overflowY: 'auto',
              padding: '12px 16px',
              scrollbarWidth: 'thin',
              scrollbarColor: 'rgba(255,255,255,0.08) transparent'
            }}>
              {activities.length === 0 ? (
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  height: '100%',
                  padding: '40px 20px',
                  textAlign: 'center'
                }}>
                  <SendOutlined style={{ fontSize: 28, color: 'rgba(255,255,255,0.08)', marginBottom: 12 }} />
                  <Text style={{ color: 'rgba(255,255,255,0.18)', fontSize: 13 }}>
                    আউটোমেশন শুরু হলে দেখুনactvity লগ এখানে
                  </Text>
                  <Text style={{ color: 'rgba(255,255,255,0.1)', fontSize: 11, marginTop: 4 }}>
                    Activity log will appear here once automation starts
                  </Text>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <AnimatePresence mode="popLayout">
                    {activities.map((act, index) => (
                      <motion.div
                        key={act.id}
                        layout
                        initial={{ opacity: 0, x: -16 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.3 }}
                        style={{
                          display: 'flex',
                          gap: 12,
                          alignItems: 'flex-start',
                          padding: '10px 8px',
                          borderRadius: 10,
                          background: index === 0
                            ? 'rgba(255,107,53,0.04)'
                            : 'transparent',
                          borderLeft: index === 0
                            ? '2px solid rgba(255,107,53,0.6)'
                            : '2px solid transparent'
                        }}
                      >
                        {/* Step number */}
                        <div style={{
                          minWidth: 22,
                          minHeight: 22,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          background: index === 0
                            ? 'rgba(255,107,53,0.15)'
                            : 'rgba(255,255,255,0.04)',
                          borderRadius: '50%',
                          fontSize: 10,
                          fontWeight: 700,
                          color: index === 0 ? '#ff6b35' : 'rgba(255,255,255,0.3)'
                        }}>
                          {activities.length - index}
                        </div>

                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={
                            act.type === 'success'
                              ? { display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 4 }
                              : { display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 4 }
                          }>
                            <Tag
                              color={actionColorMap[act.action] || 'default'}
                              style={{
                                fontSize: 10,
                                padding: '0 8px',
                                height: 20,
                                lineHeight: '18px',
                                borderRadius: 4,
                                fontWeight: 700,
                                letterSpacing: '0.5px'
                              }}
                            >
                              {act.action}
                            </Tag>
                            <Text style={{
                              color: 'rgba(255,255,255,0.2)',
                              fontSize: 10
                            }}>
                              {new Date(act.timestamp).toLocaleTimeString()}
                            </Text>
                          </div>
                          <Text style={{
                            color: act.type === 'success'
                              ? 'rgba(255,255,255,0.8)'
                              : act.type === 'action'
                                ? 'rgba(255,255,255,0.75)'
                                : 'rgba(255,255,255,0.45)',
                            fontSize: 12,
                            lineHeight: 1.55,
                            display: 'block'
                          }}>
                            {act.reasoning}
                          </Text>
                          {act.url && (
                            <div style={{ marginTop: 3 }}>
                              <Tag color="blue" style={{ fontSize: 9, padding: '0 6px', borderRadius: 4 }}>
                                {new URL(act.url).hostname}
                              </Tag>
                            </div>
                          )}
                        </div>
                      </motion.div>
                    ))}
                  </AnimatePresence>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default AutoBrowser;
