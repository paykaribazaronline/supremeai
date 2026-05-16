import React from 'react';
import { Space, Button, Input, Tooltip, Badge, Typography } from 'antd';
import { 
  ChromeOutlined, LeftOutlined, RightOutlined, ReloadOutlined, 
  SafetyOutlined, CodeOutlined, ExpandOutlined, LockOutlined
} from '@ant-design/icons';
import AISuggestionInformer from '../AISuggestionInformer';

const { Text } = Typography;

interface BrowserToolbarProps {
  url: string;
  setUrl: (url: string) => void;
  deniedUrls: any[];
  showDom: boolean;
  setShowDom: (show: boolean) => void;
  showConsole: boolean;
  setShowConsole: (show: boolean) => void;
  showSettings: boolean;
  setShowSettings: (show: boolean) => void;
  fetchScreenshot: () => void;
  handleNavigate: () => void;
  handleTypeKey: (key: string) => void;
  fetchDom: () => void;
}

const BrowserToolbar: React.FC<BrowserToolbarProps> = ({
  url,
  setUrl,
  deniedUrls,
  showDom,
  setShowDom,
  showConsole,
  setShowConsole,
  showSettings,
  setShowSettings,
  fetchScreenshot,
  handleNavigate,
  handleTypeKey,
  fetchDom
}) => {
  return (
    <>
      <div className="browser-tab-bar">
        <div className="browser-tab">
          <ChromeOutlined style={{ color: '#4285F4' }} />
          <span>Remote Instance</span>
          <Badge status="success" style={{ marginLeft: 'auto' }} />
        </div>
        <Button type="text" icon={<ExpandOutlined />} style={{ color: 'rgba(255,255,255,0.3)', marginLeft: 'auto' }} />
      </div>
      
      <div className="browser-toolbar">
        <Space size="small">
          <div className="dot red" />
          <div className="dot yellow" />
          <div className="dot green" />
        </Space>
        
        <Space style={{ marginLeft: 16 }}>
          <Button type="text" icon={<LeftOutlined />} style={{ color: '#fff' }} onClick={() => handleTypeKey('Alt+ArrowLeft')} />
          <Button type="text" icon={<RightOutlined />} style={{ color: '#fff' }} onClick={() => handleTypeKey('Alt+ArrowRight')} />
          <Button type="text" icon={<ReloadOutlined />} style={{ color: '#fff' }} onClick={fetchScreenshot} />
        </Space>
        
        <div className="browser-address-bar">
          <div style={{ display: 'flex', alignItems: 'center', marginRight: 10 }}>
            {deniedUrls.some(p => url.includes(p.pattern)) ? (
              <Tooltip title="This URL is blocked by security policy">
                <LockOutlined style={{ color: '#ff4d4f', fontSize: 14 }} />
              </Tooltip>
            ) : (
              <Tooltip title="Secure Connection Established">
                <SafetyOutlined style={{ color: '#27c93f', fontSize: 14 }} />
              </Tooltip>
            )}
          </div>
          <Input 
            variant="borderless" 
            value={url} 
            onChange={(e) => setUrl(e.target.value)}
            onPressEnter={handleNavigate}
            style={{ color: '#fff', padding: 0, fontSize: 13, height: '100%' }}
            suffix={<AISuggestionInformer 
              context="browser_url" 
              onSelect={(val) => setUrl(val)} 
            />}
          />
          <Tooltip title="View Source Structure">
            <CodeOutlined 
              style={{ color: showDom ? '#00f2fe' : 'rgba(255,255,255,0.3)', cursor: 'pointer', marginLeft: 8 }} 
              onClick={() => { fetchDom(); setShowDom(true); }}
            />
          </Tooltip>
        </div>

        <Space>
          <Tooltip title="AI Thought Stream">
            <Button 
              type="text" 
              icon={<CodeOutlined />} 
              style={{ color: showConsole ? '#00f2fe' : 'rgba(255,255,255,0.3)' }} 
              onClick={() => setShowConsole(!showConsole)}
            />
          </Tooltip>
          <Button 
            type="text" 
            icon={<SafetyOutlined />} 
            style={{ color: showSettings ? '#00f2fe' : 'rgba(255,255,255,0.3)' }} 
            onClick={() => setShowSettings(true)}
          >
            <Text style={{ color: 'inherit', fontSize: 11, marginLeft: 4 }}>Safety & Auth</Text>
          </Button>
          <Button type="text" icon={<ReloadOutlined />} style={{ color: 'rgba(255,255,255,0.3)' }} onClick={fetchScreenshot} />
        </Space>
      </div>
    </>
  );
};

export default BrowserToolbar;
