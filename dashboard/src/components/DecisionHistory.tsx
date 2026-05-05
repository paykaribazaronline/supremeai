/**
 * DecisionHistory Component
 * Displays decision history with timeline visualization
 * Integrates with AgentDecisionLogger backend
 */

import React, { useState } from 'react';
import TimelineVisualization from './TimelineVisualization';

const DecisionHistory: React.FC = () => {
    const [selectedProject, setSelectedProject] = useState<string>('all');
    const [view, setView] = useState<'timeline' | 'compact'>('timeline');

    const handleDrillDown = (decisionId: string) => {
        console.log(`Drill-down into decision: ${decisionId}`);
        // Would navigate to decision detail view
    };

    return (
        <div className="decision-history">
            <div className="decision-history-header">
                <h2>Decision History</h2>
                
                <div className="decision-history-controls">
                    <div className="control-group">
                        <label htmlFor="project-select">Project:</label>
                        <select 
                            id="project-select"
                            value={selectedProject}
                            onChange={(e) => setSelectedProject(e.target.value)}
                        >
                            <option value="all">All Projects</option>
                            <option value="demo">Demo Project</option>
                            <option value="phase6">Phase 6</option>
                        </select>
                    </div>
                    
                    <div className="control-group">
                        <label htmlFor="view-select">View:</label>
                        <select 
                            id="view-select"
                            value={view}
                            onChange={(e) => setView(e.target.value as 'timeline' | 'compact')}
                        >
                            <option value="timeline">Timeline</option>
                            <option value="compact">Compact</option>
                        </select>
                    </div>
                </div>
            </div>

            <TimelineVisualization 
                projectId={selectedProject === 'all' ? undefined : selectedProject}
                maxEntries={50}
                onDrillDown={handleDrillDown}
                compact={view === 'compact'}
            />

            <style>{`
                .decision-history {
                    padding: 20px;
                }

                .decision-history-header {
                    margin-bottom: 30px;
                }

                .decision-history-header h2 {
                    font-size: 28px;
                    color: #2c3e50;
                    margin-bottom: 15px;
                }

                .decision-history-controls {
                    display: flex;
                    gap: 20px;
                    align-items: flex-end;
                    flex-wrap: wrap;
                }

                .control-group {
                    display: flex;
                    flex-direction: column;
                    gap: 5px;
                }

                .control-group label {
                    font-size: 0.9rem;
                    color: #7f8c8d;
                    font-weight: 600;
                    text-transform: uppercase;
                }

                .control-group select {
                    padding: 8px 12px;
                    border: 1px solid #e0e0e0;
                    border-radius: 4px;
                    font-size: 14px;
                    background: white;
                    cursor: pointer;
                    transition: all 0.3s ease;
                }

                .control-group select:hover {
                    border-color: #3498db;
                }

                .control-group select:focus {
                    outline: none;
                    border-color: #3498db;
                    box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
                }
            `}</style>
        </div>
    );
};

export default DecisionHistory;
