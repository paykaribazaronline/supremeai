import React, { useState, useEffect } from 'react';

export const DataStream = () => {
  const [streams, setStreams] = useState<any[]>([]);
  
  useEffect(() => {
    const newStreams = Array.from({ length: 20 }).map((_, i) => ({
      id: i,
      left: `${Math.random() * 100}%`,
      delay: `${Math.random() * 5}s`,
      duration: `${5 + Math.random() * 10}s`,
      content: Math.random().toString(2).substring(2, 10)
    }));
    setStreams(newStreams);
  }, []);

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none', zIndex: 0 }}>
      {streams.map(s => (
        <div 
          key={s.id} 
          className="data-stream" 
          style={{ left: s.left, animationDelay: s.delay, animationDuration: s.duration }}
        >
          {s.content}
        </div>
      ))}
    </div>
  );
};

export const Waveform = () => (
  <div className="waveform-container">
    {[...Array(12)].map((_, i) => (
      <div 
        key={i} 
        className="wave-bar" 
        style={{ animationDelay: `${i * 0.1}s` }} 
      />
    ))}
  </div>
);
