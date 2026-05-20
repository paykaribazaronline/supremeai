import React, { useState, useEffect, useRef } from 'react';
import { 
  Typography, Space, Row, Col, Button, notification, Card
} from 'antd';
import { 
  MonitorOutlined,
  PauseCircleOutlined,
  ThunderboltOutlined,
  ChromeOutlined,
  ReloadOutlined,
  ArrowRightOutlined,
  RocketOutlined,
  BulbOutlined,
  FileSearchOutlined,
  RobotOutlined,
  LeftOutlined,
  RightOutlined,
  SafetyOutlined,
  GlobalOutlined,
  SearchOutlined,
  EnterOutlined,
  KeyOutlined,
  CodeOutlined,
  InfoCircleOutlined,
  HistoryOutlined,
  SettingOutlined,
  ExpandOutlined,
  LockOutlined
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import axios from 'axios';
import { useRole } from '../contexts/RoleContext';

// Modular Components
import BrowserViewport from '../components/browser/BrowserViewport';
import BrowserToolbar from '../components/browser/BrowserToolbar';
import MissionProtocol from '../components/browser/MissionProtocol';
import IntelligenceFeed from '../components/browser/IntelligenceFeed';
import BrowserSafetyDrawer from '../components/browser/BrowserSafetyDrawer';
import StructureTreeDrawer from '../components/browser/StructureTreeDrawer';
import BrowserHeader from '../components/browser/BrowserHeader';
import BrowserDirectCommand from '../components/browser/BrowserDirectCommand';
import IntelligenceTabs from '../components/browser/IntelligenceTabs';

const { Title, Text } = Typography;

const AdminBrowser: React.FC = () => {
  const { isGuest } = useRole();
  const [url, setUrl] = useState('https://www.google.com');
  const [displayUrl, setDisplayUrl] = useState('https://www.google.com');
  const [goal, setGoal] = useState('');
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [navigating, setNavigating] = useState(false);
  const [stepping, setStepping] = useState(false);
  const [activities, setActivities] = useState<any[]>([]);
  const [tasks, setTasks] = useState<any[]>([]);
  const [currentTask, setCurrentTask] = useState<any>(null);
  const [findings, setFindings] = useState<any[]>([]);
  const [isAutoMode, setIsAutoMode] = useState(false);
  const [showDom, setShowDom] = useState(false);
  const [domTree, setDomTree] = useState<any>(null);
  const [keyInput, setKeyInput] = useState('');
  const [browserStatus, setBrowserStatus] = useState('Idle');
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [showSettings, setShowSettings] = useState(false);
  const [showConsole, setShowConsole] = useState(true);
  const [allowedUrls, setAllowedUrls] = useState<any[]>([]);
  const [deniedUrls, setDeniedUrls] = useState<any[]>([]);
  const [credentials, setCredentials] = useState<any[]>([]);
  const [newUrlPattern, setNewUrlPattern] = useState('');
  const [newUrlType, setNewUrlType] = useState('allowed');
  const [newCred, setNewCred] = useState({ website: '', username: '', password: '', token: '' });
  const [learningStatus, setLearningStatus] = useState<any>(null);
  const [isLearning, setIsLearning] = useState(true);
  const [votingDetails, setVotingDetails] = useState<any[]>([]);
  const [lastAiAction, setLastAiAction] = useState<any>(null);

  const browserRef = useRef<HTMLImageElement>(null);

  const fetchScreenshot = async () => {
    try {
      const response = await axios.get('/api/browser/surf/screenshot');
      if (response.data.screenshot) {
        setScreenshot(`data:image/png;base64,${response.data.screenshot}`);
        setBrowserStatus('Streaming Live');
      }
    } catch (error) {
      console.error('Failed to fetch screenshot:', error);
      setBrowserStatus('Connection Lost');
    }
  };

  const fetchDom = async () => {
    try {
      const response = await axios.get('/api/browser/surf/accessibility');
      setDomTree(response.data.tree);
    } catch (error) {
      console.error('Failed to fetch DOM:', error);
    }
  };

  const fetchData = async () => {
    try {
      const [actRes, taskRes] = await Promise.all([
        axios.get('/api/browser/activity/recent'),
        axios.get('/api/browser/tasks')
      ]);
      setActivities(actRes.data.activities || []);
      const activeTasks = taskRes.data.tasks || [];
      setTasks(activeTasks);
      if (activeTasks.length > 0 && !currentTask) setCurrentTask(activeTasks[0]);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    }
  };

  const fetchFindings = async (taskId: string) => {
    try {
      const response = await axios.get(`/api/browser/tasks/${taskId}/findings`);
      setFindings(response.data.findings || []);
    } catch (error) {
      console.error('Failed to fetch findings:', error);
    }
  };

  const fetchPermissions = async () => {
    try {
      const [allowed, denied] = await Promise.all([
        axios.get('/api/browser/urls/allowed'),
        axios.get('/api/browser/urls/denied')
      ]);
      setAllowedUrls(allowed.data.urls || []);
      setDeniedUrls(denied.data.urls || []);
    } catch (error) {
      console.error('Failed to fetch permissions:', error);
    }
  };

  const fetchCredentials = async () => {
    try {
      const response = await axios.get('/api/browser/credentials');
      setCredentials(response.data.credentials || []);
    } catch (error) {
      console.error('Failed to fetch credentials:', error);
    }
  };

  const fetchLearningStatus = async () => {
    try {
      const response = await axios.get('/api/browser/system-learning');
      setLearningStatus(response.data);
      setIsLearning(response.data.autoLearnEnabled);
      setVotingDetails([
        { model: 'Gemini 1.5 Flash', vote: 'CLICK', confidence: 0.95, reasoning: 'Identified primary action button via visual cues.' },
        { model: 'GPT-4o', vote: 'CLICK', confidence: 0.92, reasoning: 'Matches standard login pattern structure.' },
        { model: 'DeepSeek-V4', vote: 'WAIT', confidence: 0.45, reasoning: 'Detected ongoing network activity.' }
      ]);
    } catch (error) {
      console.error('Failed to fetch learning status:', error);
    }
  };

  const toggleLearning = async (enabled: boolean) => {
    if (isGuest) return;
    try {
      await axios.post('/api/browser/system-learning/toggle', { enabled });
      setIsLearning(enabled);
      fetchLearningStatus();
    } catch (error) {
      console.error('Failed to toggle learning:', error);
    }
  };

  useEffect(() => {
    fetchData();
    fetchPermissions();
    fetchCredentials();
    fetchLearningStatus();
    const interval = setInterval(() => {
      fetchScreenshot();
      fetchData();
    }, 1500); 
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (currentTask) fetchFindings(currentTask.id);
  }, [currentTask, activities]);

  const handleNavigate = async (targetUrl?: string) => {
    const finalUrl = targetUrl || url;
    if (!finalUrl || isGuest) return;
    setNavigating(true);
    try {
      await axios.post('/api/browser/surf/navigate', { url: finalUrl });
      if (targetUrl) setUrl(targetUrl);
      setDisplayUrl(finalUrl);
      fetchScreenshot();
    } catch (error) {
      console.error('Navigation failed:', error);
    } finally {
      setNavigating(false);
    }
  };

  const handleCreateTask = async () => {
    if (!goal || isGuest) return;
    try {
      const res = await axios.post('/api/browser/tasks', { goal });
      setCurrentTask(res.data);
      setGoal('');
      setIsAutoMode(true);
      fetchData();
    } catch (error) {
      console.error('Task creation failed:', error);
    }
  };

  const handleStep = async () => {
    if (!currentTask || isGuest) return;
    setStepping(true);
    try {
      await axios.post(`/api/browser/tasks/${currentTask.id}/step`);
      fetchData();
    } catch (error) {
      console.error('Autonomous step failed:', error);
    } finally {
      setStepping(false);
    }
  };

  const handleBrowserClick = async (x: number, y: number) => {
    if (isGuest || navigating || stepping) return;
    try {
      setBrowserStatus('Clicking...');
      await axios.post('/api/browser/surf/click-at', { x, y });
      setTimeout(fetchScreenshot, 500);
    } catch (error) {
      console.error('Click failed:', error);
    }
  };

  const handleTypeKey = async (key?: string) => {
    const finalKey = key || keyInput;
    if (!finalKey || isGuest) return;
    try {
      setBrowserStatus('Typing...');
      await axios.post('/api/browser/surf/type-key', { key: finalKey });
      setKeyInput('');
      setTimeout(fetchScreenshot, 500);
    } catch (error) {
      console.error('Key type failed:', error);
    }
  };

  const handleSavePermission = async () => {
    if (!newUrlPattern || isGuest) return;
    try {
      const endpoint = newUrlType === 'allowed' ? '/api/browser/urls/allowed' : '/api/browser/urls/denied';
      await axios.post(endpoint, { url: newUrlPattern, pattern: newUrlPattern });
      setNewUrlPattern('');
      fetchPermissions();
      notification.success({ message: 'Policy Updated' });
    } catch (error) {
      notification.error({ message: 'Update Failed' });
    }
  };

  const handleSaveCredential = async () => {
    if (!newCred.website || isGuest) return;
    try {
      await axios.post('/api/browser/credentials', newCred);
      setNewCred({ website: '', username: '', password: '', token: '' });
      fetchCredentials();
      notification.success({ message: 'Vault Updated' });
    } catch (error) {
      notification.error({ message: 'Storage Error' });
    }
  };

  const handleDeleteUrl = async (id: string) => {
    if (isGuest) return;
    try {
      await axios.delete(`/api/browser/urls/${id}`);
      fetchPermissions();
    } catch (error) {
      console.error('Failed to delete permission:', error);
    }
  };

  const handleDeleteCredential = async (id: string) => {
    if (isGuest) return;
    try {
      await axios.delete(`/api/browser/credentials/${id}`);
      fetchCredentials();
    } catch (error) {
      console.error('Failed to delete credential:', error);
    }
  };

  return (
    <div className="admin-page">
      <Row gutter={[16, 16]}>
        <BrowserHeader isAutoMode={isAutoMode} setIsAutoMode={setIsAutoMode} />

        <Col xs={24} lg={16}>
          <div className="glass-card browser-container" style={{ boxShadow: '0 clamp(30px, 4vw, 60px) rgba(0,0,0,0.5)', overflow: 'hidden', border: '1px solid rgba(0, 243, 255, 0.12)' }}>
            <BrowserToolbar 
              url={url}
              setUrl={setUrl}
              handleNavigate={handleNavigate}
              fetchScreenshot={fetchScreenshot}
              handleTypeKey={handleTypeKey}
              fetchDom={fetchDom}
              setShowDom={setShowDom}
              showDom={showDom}
              showConsole={showConsole}
              setShowConsole={setShowConsole}
              setShowSettings={setShowSettings}
              showSettings={showSettings}
              deniedUrls={deniedUrls}
            />
            
            <BrowserViewport 
              screenshot={screenshot}
              displayUrl={displayUrl}
              mousePos={mousePos}
              setMousePos={setMousePos}
              handleBrowserClick={(e: any) => {
                const rect = e.currentTarget.getBoundingClientRect();
                const x = Math.round((e.clientX - rect.left) * (1280 / rect.width));
                const y = Math.round((e.clientY - rect.top) * (720 / rect.height));
                handleBrowserClick(x, y);
              }}
              lastAiAction={lastAiAction}
              showConsole={showConsole}
              votingDetails={votingDetails}
              stepping={stepping}
              navigating={navigating}
              browserRef={browserRef}
            />

            <BrowserDirectCommand 
              keyInput={keyInput}
              setKeyInput={setKeyInput}
              handleTypeKey={handleTypeKey}
            />
          </div>

          <IntelligenceFeed activities={activities} findings={findings} />
        </Col>

        <Col xs={24} lg={8}>
          <IntelligenceTabs activities={activities} findings={findings} />
        </Col>
      </Row>

      <StructureTreeDrawer 
        open={showDom}
        onClose={() => setShowDom(false)}
        domTree={domTree}
      />

      <BrowserSafetyDrawer 
        open={showSettings}
        onClose={() => setShowSettings(false)}
        newUrlPattern={newUrlPattern}
        setNewUrlPattern={setNewUrlPattern}
        newUrlType={newUrlType}
        setNewUrlType={setNewUrlType}
        handleSavePermission={handleSavePermission}
        allowedUrls={allowedUrls}
        deniedUrls={deniedUrls}
        handleDeleteUrl={handleDeleteUrl}
        newCred={newCred}
        setNewCred={setNewCred}
        handleSaveCredential={handleSaveCredential}
        credentials={credentials}
        handleDeleteCredential={handleDeleteCredential}
        isLearning={isLearning}
        toggleLearning={toggleLearning}
        learningStatus={learningStatus}
      />
    </div>
  );
};

export default AdminBrowser;

