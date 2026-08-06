import React, { useState, useEffect } from 'react';
import useSupremeStore from '../../store/useSupremeStore';
import WebSocketManager from '../../services/realtime/WebSocketManager';
import { getWebSocketBaseUrl } from '../../utils/api';

interface SujonCoreCockpitProps {
  authToken: string;
}

const SujonCoreCockpit: React.FC<SujonCoreCockpitProps> = ({ authToken }) => {
  const [activeTab, setActiveTab] = useState<'fileTree' | 'executionShell' | 'agentLog'>('fileTree');
  const [files, setFiles] = useState<string[]>([]);
  const [shellHistory, setShellHistory] = useState<string[]>([]);
  const [agentLogs, setAgentLogs] = useState<any[]>([]);
  const [currentCommand, setCurrentCommand] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [executionState, setExecutionState] = useState<'idle' | 'running' | 'paused' | 'error' | 'completed'>('idle');
  const [timelinePosition, setTimelinePosition] = useState(0);
  const [agentState, setAgentState] = useState<'thinking' | 'planning' | 'executing' | 'reviewing' | 'communicating' | 'waiting' | 'analyzing' | 'learning'>('thinking');

  // Initialize WebSocket for real-time log streaming
  useEffect(() => {
    // বাংলা মন্তব্য: ফায়ারবেস ওয়েব অ্যাপে স্ট্যাটিক হোস্ট বাইপাস করে রেন্ডার WSS সকেটে সংযোগ
    const baseUrl = getWebSocketBaseUrl();
    const wsUrl = `${baseUrl}/api/ws/dashboard?token=${authToken}&channels=logs.stream,metrics.update`;
    const wsManager = new WebSocketManager(wsUrl, {
      onOpen: () => {
        console.warn('Connected to Sujon Core WebSocket');
        setIsConnected(true);
      },
      onClose: () => {
        console.warn('Disconnected from Sujon Core WebSocket');
        setIsConnected(false);
      },
      onMessage: (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'log_entry') {
            setAgentLogs(prev => [...prev.slice(-49), { ...data.payload, timestamp: new Date().toISOString() }]);
          } else if (data.type === 'execution_state') {
            setExecutionState(data.payload.state);
          } else if (data.type === 'agent_state') {
            setAgentState(data.payload.state);
          }
        } catch (e) {
          console.error('Error parsing WebSocket message:', e);
        }
      }
    });

    wsManager.connect();

    // Cleanup on unmount
    return () => {
      wsManager.disconnect();
    };
  }, [authToken]);

  // Mock file tree data
  useEffect(() => {
    setFiles([
      'src/',
      '  ├── components/',
      '  │   ├── dashboard/',
      '  │   │   ├── SujonCoreCockpit.tsx',
      '  │   │   └── DashboardErrorBoundary.tsx',
      '  │   └── admin/',
      '  │       ├── UserManagement.tsx',
      '  │       └── SystemHealth.tsx',
      '  ├── store/',
      '  │   └── useSupremeStore.ts',
      '  └── services/',
      '      └── realtime/',
      '          └── WebSocketManager.ts',
      'backend/',
      '  ├── api/',
      '  │   └── routes/',
      '  │       ├── realtime_dashboard.py',
      '  │       └── websocket_agent.py',
      '  └── core/',
      '      └── swarm_pubsub.py',
      'scripts/',
      '  └── colab_merge_pipeline.py',
      'docs/',
      '  └── FINAL_ROADMAP.md'
    ]);
  }, []);

  const handleExecuteCommand = () => {
    if (!currentCommand.trim()) return;

    setShellHistory(prev => [...prev, `$ ${currentCommand}`]);
    setExecutionState('running');

    // Simulate command execution
    setTimeout(() => {
      const output = `Command executed: ${currentCommand}\nOperation completed successfully.`;
      setShellHistory(prev => [...prev, output]);
      setExecutionState('completed');
    }, 1500);
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleExecuteCommand();
    }
  };

  const agentStates = [
    { name: 'thinking', label: 'Thinking', color: 'bg-blue-500' },
    { name: 'planning', label: 'Planning', color: 'bg-purple-500' },
    { name: 'executing', label: 'Executing', color: 'bg-green-500' },
    { name: 'reviewing', label: 'Reviewing', color: 'bg-yellow-500' },
    { name: 'communicating', label: 'Communicating', color: 'bg-indigo-500' },
    { name: 'waiting', label: 'Waiting', color: 'bg-gray-500' },
    { name: 'analyzing', label: 'Analyzing', color: 'bg-orange-500' },
    { name: 'learning', label: 'Learning', color: 'bg-pink-500' }
  ];

  return (
    <div className="flex flex-col h-full bg-gray-900 text-white">
      {/* Header */}
      <div className="flex items-center justify-between p-4 bg-gray-800 border-b border-gray-700">
        <div className="flex items-center space-x-4">
          <h1 className="text-xl font-bold text-cyan-400">Sujon Core - Autonomous AI Engineer</h1>
          <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
          <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
        </div>
        <div className="flex items-center space-x-4">
          <div className="text-sm">
            Execution: <span className={`font-mono ${executionState === 'running' ? 'text-green-400' : executionState === 'error' ? 'text-red-400' : 'text-gray-400'}`}>
              {executionState.toUpperCase()}
            </span>
          </div>
          <div className="text-sm">
            Agent: <span className="font-mono text-purple-400">{agentState}</span>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Panel - File Tree */}
        <div className={`w-1/3 border-r border-gray-700 flex flex-col ${activeTab === 'fileTree' ? 'block' : 'hidden md:block'}`}>
          <div className="p-2 bg-gray-800 border-b border-gray-700">
            <h2 className="font-semibold text-cyan-300">File Explorer</h2>
          </div>
          <div className="flex-1 overflow-y-auto p-2 bg-gray-850">
            <ul className="text-sm">
              {files.map((file, index) => (
                <li
                  key={index}
                  className={`py-1 px-2 hover:bg-gray-750 rounded cursor-pointer ${
                    file.trim().endsWith('.tsx') || file.trim().endsWith('.py') ? 'text-green-400' :
                    file.trim().endsWith('/') ? 'text-blue-400 font-medium' : 'text-gray-300'
                  }`}
                >
                  {file}
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Center Panel - Execution Shell */}
        <div className={`w-1/3 border-r border-gray-700 flex flex-col ${activeTab === 'executionShell' ? 'block' : 'hidden md:block'}`}>
          <div className="p-2 bg-gray-800 border-b border-gray-700">
            <h2 className="font-semibold text-cyan-300">Execution Shell</h2>
          </div>
          <div className="flex-1 overflow-y-auto p-2 font-mono text-sm bg-black bg-opacity-30">
            <div className="h-full flex flex-col">
              <div className="flex-1 overflow-y-auto mb-2">
                {shellHistory.map((entry, index) => (
                  <div
                    key={index}
                    className={`py-1 ${entry.startsWith('$') ? 'text-green-400' : 'text-gray-300'}`}
                  >
                    {entry}
                  </div>
                ))}
              </div>
              <div className="flex items-center mt-auto">
                <span className="text-green-400 mr-2">$</span>
                <input
                  type="text"
                  value={currentCommand}
                  onChange={(e) => setCurrentCommand(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="Enter command..."
                  className="flex-1 bg-gray-800 text-white px-2 py-1 rounded border border-gray-600 focus:outline-none focus:border-cyan-500"
                />
                <button
                  onClick={handleExecuteCommand}
                  className="ml-2 px-3 py-1 bg-cyan-600 hover:bg-cyan-700 rounded text-sm"
                >
                  Run
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Right Panel - Agent Log */}
        <div className={`w-1/3 flex flex-col ${activeTab === 'agentLog' ? 'block' : 'hidden md:block'}`}>
          <div className="p-2 bg-gray-800 border-b border-gray-700 flex justify-between items-center">
            <h2 className="font-semibold text-cyan-300">Agent Log</h2>
            <button
              onClick={() => setAgentLogs([])}
              className="text-xs bg-red-600 hover:bg-red-700 px-2 py-1 rounded"
            >
              Clear
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-2 font-mono text-xs bg-black bg-opacity-30">
            {agentLogs.map((log, index) => (
              <div key={index} className={`py-1 border-b border-gray-800 ${log.level === 'ERROR' ? 'text-red-400' : log.level === 'WARN' ? 'text-yellow-400' : 'text-gray-300'}`}>
                <span className="text-gray-500 mr-2">[{new Date(log.timestamp).toLocaleTimeString()}]</span>
                <span>[{log.level || 'INFO'}]</span> {log.message || log.msg}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Timeline Scrubber */}
      <div className="p-2 bg-gray-800 border-t border-gray-700">
        <div className="flex items-center">
          <span className="text-sm mr-2">Timeline:</span>
          <input
            type="range"
            min="0"
            max="100"
            value={timelinePosition}
            onChange={(e) => setTimelinePosition(parseInt(e.target.value))}
            className="flex-1"
          />
          <span className="text-sm ml-2 w-12">{timelinePosition}%</span>
        </div>
      </div>

      {/* Agent State Display */}
      <div className="p-2 bg-gray-800 border-t border-gray-700">
        <div className="flex flex-wrap gap-2">
          {agentStates.map((state) => (
            <div
              key={state.name}
              className={`px-3 py-1 rounded-full text-xs font-medium flex items-center ${
                agentState === state.name
                  ? `${state.color} text-white ring-2 ring-offset-2 ring-offset-gray-800 ring-white`
                  : 'bg-gray-700 text-gray-300'
              }`}
            >
              <span className={`w-2 h-2 rounded-full mr-2 ${agentState === state.name ? 'bg-white' : state.color}`}></span>
              {state.label}
            </div>
          ))}
        </div>
      </div>

      {/* Bottom Tab Navigation */}
      <div className="flex border-t border-gray-700 bg-gray-800">
        <button
          className={`flex-1 py-2 text-center ${activeTab === 'fileTree' ? 'bg-cyan-900 text-cyan-300' : 'hover:bg-gray-700'}`}
          onClick={() => setActiveTab('fileTree')}
        >
          File Tree
        </button>
        <button
          className={`flex-1 py-2 text-center ${activeTab === 'executionShell' ? 'bg-cyan-900 text-cyan-300' : 'hover:bg-gray-700'}`}
          onClick={() => setActiveTab('executionShell')}
        >
          Execution Shell
        </button>
        <button
          className={`flex-1 py-2 text-center ${activeTab === 'agentLog' ? 'bg-cyan-900 text-cyan-300' : 'hover:bg-gray-700'}`}
          onClick={() => setActiveTab('agentLog')}
        >
          Agent Log
        </button>
      </div>
    </div>
  );
};

export default SujonCoreCockpit;
