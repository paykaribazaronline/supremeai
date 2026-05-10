import React, { useEffect, useRef } from 'react';

const NeuralNetworkFlow: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let width = canvas.width = canvas.offsetWidth;
    let height = canvas.height = canvas.offsetHeight;

    const particles: Particle[] = [];
    const particleCount = 40;
    const connectionDistance = 150;

    class Particle {
      x: number;
      y: number;
      vx: number;
      vy: number;
      size: number;

      constructor() {
        this.x = Math.random() * width;
        this.y = Math.random() * height;
        this.vx = (Math.random() - 0.5) * 0.5;
        this.vy = (Math.random() - 0.5) * 0.5;
        this.size = Math.random() * 2 + 1;
      }

      update() {
        this.x += this.vx;
        this.y += this.vy;

        if (this.x < 0 || this.x > width) this.vx *= -1;
        if (this.y < 0 || this.y > height) this.vy *= -1;
      }

      draw() {
        if (!ctx) return;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fillStyle = '#10b981';
        ctx.fill();
        
        // Glow effect
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#10b981';
      }
    }

    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle());
    }

    const animate = () => {
      ctx.clearRect(0, 0, width, height);
      ctx.shadowBlur = 0; // Reset shadow for lines

      particles.forEach((p, i) => {
        p.update();
        p.draw();

        for (let j = i + 1; j < particles.length; j++) {
          const p2 = particles[j];
          const dx = p.x - p2.x;
          const dy = p.y - p2.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < connectionDistance) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.strokeStyle = `rgba(16, 185, 129, ${1 - dist / connectionDistance})`;
            ctx.lineWidth = 0.5;
            ctx.stroke();
          }
        }
      });

      animationFrameId = requestAnimationFrame(animate);
    };

    const handleResize = () => {
      width = canvas.width = canvas.offsetWidth;
      height = canvas.height = canvas.offsetHeight;
    };

    window.addEventListener('resize', handleResize);
    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return (
    <div className="w-full h-full relative overflow-hidden bg-black/20 rounded-2xl border border-white/5">
      <div className="absolute top-4 left-6 z-10">
        <div className="flex flex-col gap-1">
          <span className="text-[10px] font-black text-emerald-500 uppercase tracking-[0.3em]">Neural Network Operational Flow</span>
          <span className="text-[8px] text-white/30 uppercase tracking-widest font-bold">Real-time Node Synchronization // ACTIVE</span>
        </div>
      </div>
      <canvas ref={canvasRef} className="w-full h-full" />
      
      {/* Decorative Overlays */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-0 left-0 w-full h-full bg-[radial-gradient(circle_at_50%_50%,rgba(16,185,129,0.05),transparent_70%)]"></div>
        <div className="absolute bottom-4 right-6 flex items-center gap-4">
           <div className="flex flex-col items-end">
              <span className="text-[8px] text-white/20 uppercase font-bold">Sync Integrity</span>
              <span className="text-[10px] font-mono text-emerald-500">99.98%</span>
           </div>
           <div className="w-[1px] h-4 bg-white/10"></div>
           <div className="flex flex-col items-end">
              <span className="text-[8px] text-white/20 uppercase font-bold">Active Nodes</span>
              <span className="text-[10px] font-mono text-emerald-500">1,842</span>
           </div>
        </div>
      </div>
    </div>
  );
};

export default NeuralNetworkFlow;
