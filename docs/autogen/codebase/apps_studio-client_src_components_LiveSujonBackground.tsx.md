# 📄 ফাইল: apps/studio-client/src/components/LiveSujonBackground.tsx

**প্রকার:** .tsx  
**সাইজ:** 12,325 বাইট  
**আপডেট:** 2026-07-07T11:15:52.149698

---

## কোড

```tsx
import React, { useEffect, useRef, useState } from 'react';
import { useSessionCockpitStore, type SujonState } from '../store/sessionCockpitStore';

// Re-export for DashboardShell
export type { SujonState };
export const SUJON_STATE_EVENT = 'supremeai:sujon-state';

export function setSujonState(state: SujonState): void {
  window.dispatchEvent(new CustomEvent<SujonState>(SUJON_STATE_EVENT, { detail: state }));
}

export function useSujonState(): SujonState {
  const [state, setState] = useState<SujonState>('idle');
  useEffect(() => {
    const onState = (e: Event) => setState((e as CustomEvent<SujonState>).detail);
    window.addEventListener(SUJON_STATE_EVENT, onState);
    return () => window.removeEventListener(SUJON_STATE_EVENT, onState);
  }, []);
  return state;
}

const vertexShaderSource = `#version 300 es
precision highp float;
in vec2 a_position;
in vec2 a_texCoord;
out vec2 v_texCoord;
void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
}
`;

const fragmentShaderSource = `#version 300 es
precision highp float;
in vec2 v_texCoord;
out vec4 outColor;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec3 u_baseColor;
uniform float u_intensity;
uniform int u_stateId;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution.xy;
    vec2 pos = (uv - 0.5) * 2.0;
    pos.x *= u_resolution.x / u_resolution.y;
    
    vec3 color = u_baseColor;
    float n = 0.0;
    
    if (u_stateId == 0) { // Idle
        n = noise(pos * 3.0 + u_time * 0.2) * 0.5;
        color *= (0.5 + n);
    } else if (u_stateId == 1) { // Scanning
        float scan = sin(uv.y * 50.0 + u_time * 5.0) * 0.5 + 0.5;
        float sweep = step(0.9, fract(uv.x * 2.0 - u_time));
        color *= (0.5 + scan * 0.5 + sweep * u_intensity);
    } else if (u_stateId == 2) { // Executing / Processing
        n = noise(vec2(pos.x * 10.0 - u_time * 5.0, pos.y * 2.0));
        color *= step(0.6, n) * 1.5;
    } else if (u_stateId == 3) { // Self-Healing
        n = noise(pos * 5.0 + floor(u_time * 10.0) * 0.1);
        color *= (0.5 + n);
    } else if (u_stateId == 4) { // CircuitOpen
        float dist = length(pos);
        float vignette = smoothstep(1.5, 0.5, dist);
        float pulse = sin(u_time * 2.0) * 0.2 + 0.8;
        color *= vignette * pulse;
    } else if (u_stateId == 5) { // AwaitingHuman
        float dist = length(pos);
        float pulse = sin(u_time * 3.0 - dist * 5.0) * 0.5 + 0.5;
        color *= (0.5 + pulse * 0.5);
    } else if (u_stateId == 6) { // Success
        float wave = sin(u_time * 10.0 - length(pos) * 10.0);
        color *= smoothstep(0.0, 1.0, wave);
    } else if (u_stateId == 7) { // Failed
        float wave = sin(length(pos) * 20.0 + u_time * 10.0);
        color *= smoothstep(0.0, 1.0, wave);
    }

    outColor = vec4(color, 0.3); // Kept subtle
}
`;

function createShader(gl: WebGL2RenderingContext, type: number, source: string) {
    const shader = gl.createShader(type);
    if (!shader) return null;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.error(gl.getShaderInfoLog(shader));
        gl.deleteShader(shader);
        return null;
    }
    return shader;
}

function createProgram(gl: WebGL2RenderingContext, vertexShader: WebGLShader, fragmentShader: WebGLShader) {
    const program = gl.createProgram();
    if (!program) return null;
    gl.attachShader(program, vertexShader);
    gl.attachShader(program, fragmentShader);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        console.error(gl.getProgramInfoLog(program));
        gl.deleteProgram(program);
        return null;
    }
    return program;
}

const getStateId = (state: string) => {
    switch(state) {
        case 'idle': return 0;
        case 'scanning': return 1;
        case 'executing': 
        case 'processing': return 2;
        case 'self_healing': return 3;
        case 'circuit_open': return 4;
        case 'awaiting_human': return 5;
        case 'success': return 6;
        case 'failed': return 7;
        default: return 0;
    }
};

const getBaseColor = (state: string): [number, number, number] => {
    switch(state) {
        case 'idle': return [0.17, 0.19, 0.23];
        case 'scanning': return [0.1, 0.5, 0.9];
        case 'executing':
        case 'processing': return [0.1, 0.8, 0.3];
        case 'self_healing': return [0.9, 0.7, 0.1];
        case 'circuit_open': return [0.7, 0.1, 0.1];
        case 'awaiting_human': return [0.5, 0.2, 0.8];
        case 'success': return [0.1, 0.9, 0.4];
        case 'failed': return [0.8, 0.1, 0.2];
        default: return [0.17, 0.19, 0.23];
    }
};

interface LiveSujonBackgroundProps {
  state?: SujonState;
}

export function LiveSujonBackground({ state: forcedState }: LiveSujonBackgroundProps) {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const eventState = useSujonState();
    const sessionState = useSessionCockpitStore((state) => state.agentState);
    const sessionId = useSessionCockpitStore((state) => state.sessionId);
    
    // If we are in a session, prefer session state over ambient event state
    const effectiveState = forcedState ?? (sessionId ? sessionState : eventState);

    const animationRef = useRef<number>(0);
    const glRef = useRef<WebGL2RenderingContext | null>(null);
    const programRef = useRef<WebGLProgram | null>(null);
    const bufferRef = useRef<WebGLBuffer | null>(null);
    const texCoordBufferRef = useRef<WebGLBuffer | null>(null);
    const vaoRef = useRef<WebGLVertexArrayObject | null>(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const gl = canvas.getContext('webgl2', { antialias: false, alpha: true, depth: false });
        if (!gl) {
            console.error('WebGL2 not supported');
            return;
        }
        glRef.current = gl;

        const vShader = createShader(gl, gl.VERTEX_SHADER, vertexShaderSource);
        const fShader = createShader(gl, gl.FRAGMENT_SHADER, fragmentShaderSource);
        if (!vShader || !fShader) return;

        const program = createProgram(gl, vShader, fShader);
        if (!program) return;
        programRef.current = program;

        gl.deleteShader(vShader);
        gl.deleteShader(fShader);

        const positionAttributeLocation = gl.getAttribLocation(program, 'a_position');
        const texCoordAttributeLocation = gl.getAttribLocation(program, 'a_texCoord');
        
        const positionBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
        const positions = new Float32Array([
            -1, -1,
             1, -1,
            -1,  1,
            -1,  1,
             1, -1,
             1,  1,
        ]);
        gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW);
        bufferRef.current = positionBuffer;

        const vao = gl.createVertexArray();
        gl.bindVertexArray(vao);
        vaoRef.current = vao;
        
        gl.enableVertexAttribArray(positionAttributeLocation);
        gl.vertexAttribPointer(positionAttributeLocation, 2, gl.FLOAT, false, 0, 0);

        const texCoordBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
            0, 0,
            1, 0,
            0, 1,
            0, 1,
            1, 0,
            1, 1,
        ]), gl.STATIC_DRAW);
        texCoordBufferRef.current = texCoordBuffer;
        
        gl.enableVertexAttribArray(texCoordAttributeLocation);
        gl.vertexAttribPointer(texCoordAttributeLocation, 2, gl.FLOAT, false, 0, 0);

        const timeLocation = gl.getUniformLocation(program, 'u_time');
        const resolutionLocation = gl.getUniformLocation(program, 'u_resolution');
        const colorLocation = gl.getUniformLocation(program, 'u_baseColor');
        const intensityLocation = gl.getUniformLocation(program, 'u_intensity');
        const stateIdLocation = gl.getUniformLocation(program, 'u_stateId');

        let startTime = performance.now();
        let isVisible = document.visibilityState === 'visible';

        const handleVisibilityChange = () => {
            isVisible = document.visibilityState === 'visible';
            if (isVisible) {
                startTime = performance.now() - (animationRef.current || 0) * 1000;
                render(performance.now());
            }
        };
        document.addEventListener('visibilitychange', handleVisibilityChange);

        const resizeCanvas = () => {
            if (!canvas) return;
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
            gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);
        };
        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();



        const render = (now: number) => {
            if (!isVisible || !glRef.current) return;
            
            const time = (now - startTime) * 0.001;

            gl.useProgram(program);
            gl.bindVertexArray(vao);

            gl.uniform1f(timeLocation, time);
            gl.uniform2f(resolutionLocation, gl.canvas.width, gl.canvas.height);
            
            // Note: In React, accessing effectiveState directly in this closure might hold stale value unless we ref it.
            // But since this effect re-runs on nothing (dependencies array []), we need to use a ref to get latest state, 
            // OR include effectiveState in dependencies. But rebuilding WebGL context on state change is bad.
            // Instead, we will use a ref for the effectiveState. Let's fix that.
            
            gl.clearColor(0, 0, 0, 0);
            gl.clear(gl.COLOR_BUFFER_BIT);
            gl.drawArrays(gl.TRIANGLES, 0, 6);

            animationRef.current = requestAnimationFrame(render);
        };

        animationRef.current = requestAnimationFrame(render);

        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
            window.removeEventListener('resize', resizeCanvas);
            if (animationRef.current) cancelAnimationFrame(animationRef.current);
            if (glRef.current) {
                const glCtx = glRef.current;
                if (programRef.current) glCtx.deleteProgram(programRef.current);
                if (bufferRef.current) glCtx.deleteBuffer(bufferRef.current);
                if (texCoordBufferRef.current) glCtx.deleteBuffer(texCoordBufferRef.current);
                if (vaoRef.current) glCtx.deleteVertexArray(vaoRef.current);
                const ext = glCtx.getExtension('WEBGL_lose_context');
                if (ext) ext.loseContext();
            }
        };
    }, []); // Run once to initialize context

    // Update uniforms when state changes
    useEffect(() => {
        if (!glRef.current || !programRef.current) return;
        const gl = glRef.current;
        gl.useProgram(programRef.current);
        const colorLocation = gl.getUniformLocation(programRef.current, 'u_baseColor');
        const intensityLocation = gl.getUniformLocation(programRef.current, 'u_intensity');
        const stateIdLocation = gl.getUniformLocation(programRef.current, 'u_stateId');
        
        const color = getBaseColor(effectiveState);
        gl.uniform3f(colorLocation, color[0], color[1], color[2]);
        gl.uniform1f(intensityLocation, 1.0);
        gl.uniform1i(stateIdLocation, getStateId(effectiveState));
    }, [effectiveState]);

    return (
        <canvas
            ref={canvasRef}
            className="fixed inset-0 z-[-1] pointer-events-none w-full h-full opacity-60 transition-opacity duration-1000"
            style={{ contain: 'strict' }}
        />
    );
}

```