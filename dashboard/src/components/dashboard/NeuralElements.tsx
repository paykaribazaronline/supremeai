import React from 'react';

export const NeuralCore = () => (
  <div className="neural-node pulsing">
    <svg viewBox="0 0 200 200">
      <defs>
        <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style={{ stopColor: '#00f3ff', stopOpacity: 1 }} />
          <stop offset="100%" style={{ stopColor: '#bc13fe', stopOpacity: 1 }} />
        </linearGradient>
      </defs>
      <circle cx="100" cy="100" r="80" fill="none" stroke="url(#grad1)" strokeWidth="0.5" strokeDasharray="10 5" className="rotating" />
      <circle cx="100" cy="100" r="60" fill="none" stroke="url(#grad1)" strokeWidth="1" strokeDasharray="5 5" className="rotating" style={{ animationDirection: 'reverse' }} />
      <path d="M100 40 L100 160 M40 100 L160 100" stroke="url(#grad1)" strokeWidth="0.5" opacity="0.3" />
      <circle cx="100" cy="100" r="10" fill="url(#grad1)" className="pulsing" />
    </svg>
  </div>
);

export const NeuralGraph = () => (
  <svg width="100%" height="150" viewBox="0 0 400 150">
    <circle cx="50" cy="75" r="5" fill="var(--neon-blue)" className="pulsing" />
    <circle cx="150" cy="40" r="4" fill="var(--neon-purple)" className="pulsing" />
    <circle cx="150" cy="110" r="4" fill="var(--neon-purple)" className="pulsing" />
    <circle cx="250" cy="75" r="5" fill="var(--neon-blue)" className="pulsing" />
    <circle cx="350" cy="75" r="8" fill="var(--neon-blue)" className="pulsing" />
    
    <line x1="50" y1="75" x2="150" y2="40" className="neural-line" />
    <line x1="50" y1="75" x2="150" y2="110" className="neural-line" />
    <line x1="150" y1="40" x2="250" y2="75" className="neural-line" />
    <line x1="150" y1="110" x2="250" y2="75" className="neural-line" />
    <line x1="250" y1="75" x2="350" y2="75" className="neural-line" />
  </svg>
);
