/**
 * Phase 6 Week 5-6: Timeline Visualization React Component
 * Interactive decision timeline with color-coded outcomes
 * 
 * Features:
 * - Real-time timeline updates
 * - Color-coded visualization (green=SUCCESS, red=FAILED, yellow=PARTIAL, grey=PENDING)
 * - Drill-down capability for decision details
 * - Statistics dashboard integration
 * - Responsive design
 */

import React, { useState, useEffect } from 'react';
import './TimelineVisualization.css';

interface TimelineEntry {
  decisionId: string;
  agent: string;
  decision: string;
  reasoning: string;
  confidence: number;
  timestamp: number;
  outcome: 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'PENDING';
  color: 'green' | 'red' | 'yellow' | 'grey';
  formattedTime: string;
  successMetric?: number;
}

interface TimelineStats {
  total: number;
  successful: number;
  failed: number;
  partial: number;
  successRate: number;
  averageConfidence: number;
}

interface TimelineVisualizationProps {
  projectId?: string;
  agentName?: string;
  maxEntries?: number;
  onDrillDown?: (decisionId: string) => void;
  compact?: boolean;
}

export const TimelineVisualization: React.FC<TimelineVisualizationProps> = ({
  projectId,
  agentName,
  maxEntries = 50,
  onDrillDown,
  compact = false,
}) => {
  const [timeline, setTimeline] = useState<TimelineEntry[]>([]);
  const [stats, setStats] = useState<TimelineStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<TimelineEntry | null>(null);

  // Fetch timeline data
  useEffect(() => {
    const fetchTimeline = async () => {
      try {
        setLoading(true);
        setError(null);

        let url = '/api/v1/timeline';
        
        if (projectId) {
          url += `/project/${projectId}?limit=${maxEntries}`;
        } else if (agentName) {
          url += `/agent/${agentName}?limit=${maxEntries}`;
        }

        const response = await fetch(url);
        
        if (!response.ok) {
          throw new Error(`Failed to fetch timeline: ${response.statusText}`);
        }

        const data = await response.json();
        
        if (data.timeline) {
          setTimeline(data.timeline);
        }
        
        if (data.stats) {
          setStats(data.stats);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    fetchTimeline();
    
    // Refresh every 30 seconds
    const interval = setInterval(fetchTimeline, 30000);
    
    return () => clearInterval(interval);
  }, [projectId, agentName, maxEntries]);

  const handleEntryClick = (entry: TimelineEntry) => {
    setSelectedEntry(entry);
    if (onDrillDown) {
      onDrillDown(entry.decisionId);
    }
  };

  if (loading) {
    return <div className="timeline-loading">Loading timeline...</div>;
  }

  if (error) {
    return <div className="timeline-error">Error: {error}</div>;
  }

  if (compact) {
    return renderCompactTimeline();
  }

  return (
    <div className="timeline-container">
      {stats && renderStatsDashboard()}
      {renderMainTimeline()}
      {selectedEntry && renderDetailPanel()}
    </div>
  );

  // ==================== Render Functions ====================

  function renderStatsDashboard() {
    return (
      <div className="timeline-stats-dashboard">
        <div className="stat-card">
          <h4>Total Decisions</h4>
          <p className="stat-value">{stats!.total}</p>
        </div>
        
        <div className="stat-card success">
          <h4>Successful</h4>
          <p className="stat-value">{stats!.successful}</p>
          <p className="stat-percentage">{stats!.successRate.toFixed(1)}%</p>
        </div>
        
        <div className="stat-card failed">
          <h4>Failed</h4>
          <p className="stat-value">{stats!.failed}</p>
          <p className="stat-percentage">{((stats!.failed / stats!.total) * 100).toFixed(1)}%</p>
        </div>
        
        <div className="stat-card partial">
          <h4>Partial</h4>
          <p className="stat-value">{stats!.partial}</p>
        </div>
        
        <div className="stat-card">
          <h4>Avg Confidence</h4>
          <p className="stat-value">{(stats!.averageConfidence * 100).toFixed(0)}%</p>
        </div>
      </div>
    );
  }

  function renderMainTimeline() {
    return (
      <div className="timeline-view">
        <h3 className="timeline-title">Decision Timeline</h3>
        
        <div className="timeline">
          <div className="timeline-axis" />
          
          {timeline.map((entry, index) => (
            <div
              key={entry.decisionId}
              className={`timeline-entry timeline-${entry.color}`}
              onClick={() => handleEntryClick(entry)}
            >
              <div className={`timeline-marker marker-${entry.color}`} />
              
              <div className="timeline-content">
                <h4 className="timeline-agent">{entry.agent}</h4>
                <p className="timeline-decision">{entry.decision}</p>
                <p className="timeline-reasoning">{entry.reasoning}</p>
                
                <div className="timeline-meta">
                  <span className={`timeline-outcome outcome-${entry.color}`}>
                    {entry.outcome}
                  </span>
                  <span className="timeline-confidence">
                    Confidence: {(entry.confidence * 100).toFixed(0)}%
                  </span>
                  <span className="timeline-time" title={new Date(entry.timestamp).toISOString()}>
                    {formatTimeAgo(entry.timestamp)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
        
        {timeline.length === 0 && (
          <div className="timeline-empty">
            No decisions recorded yet
          </div>
        )}
      </div>
    );
  }

  function renderCompactTimeline() {
    return (
      <div className="timeline-compact">
        <h4>Timeline</h4>
        <div className="compact-entries">
          {timeline.slice(0, 10).map((entry) => (
            <div
              key={entry.decisionId}
              className={`compact-entry entry-${entry.color}`}
              title={`${entry.agent}: ${entry.decision}`}
              onClick={() => handleEntryClick(entry)}
            />
          ))}
        </div>
      </div>
    );
  }

  function renderDetailPanel() {
    return (
      <div className="timeline-detail-panel" onClick={(e) => e.stopPropagation()}>
        <button 
          className="detail-close"
          onClick={() => setSelectedEntry(null)}
        >
          ✕
        </button>
        
        <div className="detail-content">
          <h3>Decision Details</h3>
          
          <div className="detail-row">
            <label>Agent:</label>
            <span>{selectedEntry!.agent}</span>
          </div>
          
          <div className="detail-row">
            <label>Decision:</label>
            <span>{selectedEntry!.decision}</span>
          </div>
          
          <div className="detail-row">
            <label>Reasoning:</label>
            <span className="detail-reasoning">{selectedEntry!.reasoning}</span>
          </div>
          
          <div className="detail-row">
            <label>Outcome:</label>
            <span className={`outcome outcome-${selectedEntry!.color}`}>
              {selectedEntry!.outcome}
            </span>
          </div>
          
          <div className="detail-row">
            <label>Confidence:</label>
            <span className="confidence-bar">
              <div 
                className="confidence-fill"
                style={{ width: `${selectedEntry!.confidence * 100}%` }}
              />
              <span className="confidence-text">{(selectedEntry!.confidence * 100).toFixed(0)}%</span>
            </span>
          </div>
          
          {selectedEntry!.successMetric !== undefined && (
            <div className="detail-row">
              <label>Success Metric:</label>
              <span>{(selectedEntry!.successMetric * 100).toFixed(2)}%</span>
            </div>
          )}
          
          <div className="detail-row">
            <label>Timestamp:</label>
            <span>{new Date(selectedEntry!.timestamp).toLocaleString()}</span>
          </div>
        </div>
      </div>
    );
  }

  function formatTimeAgo(timestamp: number): string {
    const now = Date.now();
    const diff = now - timestamp;
    
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (minutes > 0) return `${minutes}m ago`;
    return `${seconds}s ago`;
  }
};

export default TimelineVisualization;
