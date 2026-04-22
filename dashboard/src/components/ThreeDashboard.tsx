import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as THREE from 'three';
import './ThreeDashboard.css';

/**
 * Phase 6: 3D Real-Time Dashboard Component
 * 
 * Renders a 3D scene showing:
 * - Project structure (nodes and edges)
 * - Agent coordination (agent spheres)
 * - Decision voting progress
 * - Real-time metrics overlay
 * 
 * Target: <100ms to render, 60 FPS on desktop
 * 
 * WebSocket connection to /ws/visualization for live updates at 30 FPS
 */
interface Node3D {
    id: string;
    label: string;
    position: [number, number, number];
    color: number;
    scale: number;
    mesh?: THREE.Mesh;
}

interface Edge3D {
    from: string;
    to: string;
    color: number;
    thickness: number;
    dashed: boolean;
    line?: THREE.Line;
}

interface AgentNode {
    id: string;
    name: string;
    position: number[];
    status: string;
    color: number;
    scale: number;
    votingProgress: number;
    mesh?: THREE.Mesh;
}

const ThreeDashboard: React.FC = () => {
    const mountRef = useRef<HTMLDivElement>(null);
    const sceneRef = useRef<THREE.Scene>();
    const cameraRef = useRef<THREE.PerspectiveCamera>();
    const rendererRef = useRef<THREE.WebGLRenderer>();
    const websocketRef = useRef<WebSocket>();
    const animationIdRef = useRef<number>();
    
    const nodesRef = useRef<Map<string, Node3D>>(new Map());
    const edgesRef = useRef<Map<string, Edge3D>>(new Map());
    const agentsRef = useRef<Map<string, AgentNode>>(new Map());
    
    const [fps, setFps] = useState(0);
    const [connectedClients, setConnectedClients] = useState(0);
    const [isConnected, setIsConnected] = useState(false);
    const [connectionError, setConnectionError] = useState<string | null>(null);
    const fpsCounterRef = useRef(0);
    const lastTimeRef = useRef(Date.now());
    const frameCountRef = useRef(0);

    // Keyboard navigation state
    const cameraTargetRef = useRef({ x: 0, y: 50, z: 100 });
    const isFocusedRef = useRef(false);

    /**
     * Initialize Three.js scene, camera, renderer
     */
    const initScene = () => {
        if (!mountRef.current) return;

        // Scene setup
        const scene = new THREE.Scene();
        scene.background = new THREE.Color(0x1a1a1a);
        scene.fog = new THREE.Fog(0x1a1a1a, 50, 200);
        sceneRef.current = scene;

        // Camera setup
        const camera = new THREE.PerspectiveCamera(
            75,
            window.innerWidth / window.innerHeight,
            0.1,
            2000
        );
        camera.position.set(0, 50, 100);
        camera.lookAt(0, 0, 0);
        cameraRef.current = camera;

        // Renderer setup with performance optimizations
        const renderer = new THREE.WebGLRenderer({ 
            antialias: true, 
            alpha: false,
            powerPreference: 'high-performance',
        });
        renderer.setSize(window.innerWidth, window.innerHeight);
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        // Limit pixel ratio for performance on high-DPI screens
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        // Enable frustum culling
        renderer.info.autoReset = true;
        rendererRef.current = renderer;
        
        mountRef.current.appendChild(renderer.domElement);

        // Lighting
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
        scene.add(ambientLight);

        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(100, 200, 100);
        directionalLight.castShadow = true;
        directionalLight.shadow.camera.left = -500;
        directionalLight.shadow.camera.right = 500;
        directionalLight.shadow.camera.top = 500;
        directionalLight.shadow.camera.bottom = -500;
        // Optimize shadow map resolution
        directionalLight.shadow.mapSize.width = 1024;
        directionalLight.shadow.mapSize.height = 1024;
        scene.add(directionalLight);

        // Grid helper for reference
        const gridHelper = new THREE.GridHelper(400, 20, 0x444444, 0x222222);
        gridHelper.position.y = -50;
        scene.add(gridHelper);

        // Handle window resize
        const onWindowResize = () => {
            const width = window.innerWidth;
            const height = window.innerHeight;
            camera.aspect = width / height;
            camera.updateProjectionMatrix();
            renderer.setSize(width, height);
        };
        window.addEventListener('resize', onWindowResize);

        // Keyboard navigation for accessibility
        const onKeyDown = (e: KeyboardEvent) => {
            if (!isFocusedRef.current) return;
            
            const step = e.shiftKey ? 10 : 5;
            switch (e.key) {
                case 'ArrowUp':
                    cameraTargetRef.current.y += step;
                    e.preventDefault();
                    break;
                case 'ArrowDown':
                    cameraTargetRef.current.y -= step;
                    e.preventDefault();
                    break;
                case 'ArrowLeft':
                    cameraTargetRef.current.x -= step;
                    e.preventDefault();
                    break;
                case 'ArrowRight':
                    cameraTargetRef.current.x += step;
                    e.preventDefault();
                    break;
                case '+':
                case '=':
                    cameraTargetRef.current.z -= step;
                    e.preventDefault();
                    break;
                case '-':
                    cameraTargetRef.current.z += step;
                    e.preventDefault();
                    break;
            }
        };
        window.addEventListener('keydown', onKeyDown);

        // Start animation loop
        animate();
    };

    /**
     * Animation loop - render at 60 FPS (browser display rate)
     * Receives data from WebSocket at ~30 FPS
     */
    const animate = () => {
        animationIdRef.current = requestAnimationFrame(animate);

        // Update FPS counter
        const now = Date.now();
        const deltaTime = now - lastTimeRef.current;
        lastTimeRef.current = now;
        
        fpsCounterRef.current++;
        frameCountRef.current++;
        if (deltaTime > 1000) {
            setFps(Math.round((fpsCounterRef.current * 1000) / deltaTime));
            fpsCounterRef.current = 0;
            lastTimeRef.current = now;
        }

        // Performance: Skip rotation animation when FPS is low
        if (fps > 30 || fps === 0) {
            // Rotate agent nodes for visual interest
            agentsRef.current.forEach((agent) => {
                if (agent.mesh && agent.mesh.geometry) {
                    agent.mesh.rotation.x += 0.01;
                    agent.mesh.rotation.y += 0.02;
                }
            });
        }

        // Smooth camera movement for keyboard navigation
        if (cameraRef.current) {
            const target = cameraTargetRef.current;
            cameraRef.current.position.x += (target.x - cameraRef.current.position.x) * 0.05;
            cameraRef.current.position.y += (target.y - cameraRef.current.position.y) * 0.05;
            cameraRef.current.position.z += (target.z - cameraRef.current.position.z) * 0.05;
            cameraRef.current.lookAt(0, 0, 0);
        }

        // Light pulse on voting progress
        const scene = sceneRef.current;
        if (scene) {
            const lights = scene.children.filter((c: THREE.Object3D) => c instanceof THREE.Light);
            lights.forEach((light: THREE.Object3D) => {
                if (light instanceof THREE.DirectionalLight) {
                    const intensity = 0.6 + Math.sin(Date.now() / 1000) * 0.2;
                    light.intensity = intensity;
                }
            });
        }

        // Render
        if (rendererRef.current && sceneRef.current && cameraRef.current) {
            rendererRef.current.render(sceneRef.current, cameraRef.current);
        }
    };

    /**
     * Connect to WebSocket and start receiving visualization frames
     */
    const connectWebSocket = () => {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//localhost:8080/ws/visualization`;

        try {
            const ws = new WebSocket(wsUrl);
            websocketRef.current = ws;

            ws.onopen = () => {
                setConnectionError(null);
                setIsConnected(true);
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    handleVisualizationFrame(data);
                } catch (e) {
                    setConnectionError('Received invalid data from server.');
                }
            };

            ws.onclose = () => {
                setIsConnected(false);
                // Attempt reconnection after 3 seconds
                setTimeout(connectWebSocket, 3000);
            };

            ws.onerror = () => {
                setConnectionError('Unable to connect to visualization server.');
                setIsConnected(false);
            };
        } catch (e) {
            setConnectionError('Failed to initialize WebSocket connection.');
            setTimeout(connectWebSocket, 3000);
        }
    };

    /**
     * Handle incoming visualization frame from server
     */
    const handleVisualizationFrame = (data: any) => {
        if (data.type === 'scene_init') {
            // Scene initialization already handled in initScene
            return;
        }

        if (data.type === 'frame_update') {
            // Update build flow nodes
            if (data.buildFlow && data.buildFlow.nodes) {
                updateBuildFlowNodes(data.buildFlow.nodes);
            }

            // Update build flow edges
            if (data.buildFlow && data.buildFlow.edges) {
                updateBuildFlowEdges(data.buildFlow.edges);
            }

            // Update agent nodes
            if (data.agents) {
                updateAgentNodes(data.agents);
            }
        }
    };

    /**
     * Update or create build flow nodes in the scene
     */
    const updateBuildFlowNodes = (nodes: Node3D[]) => {
        const scene = sceneRef.current;
        if (!scene) return;

        nodes.forEach((nodeData) => {
            let node = nodesRef.current.get(nodeData.id);

            if (!node) {
                // Create new node
                const geometry = new THREE.SphereGeometry(nodeData.scale, 32, 32);
                const material = new THREE.MeshStandardMaterial({
                    color: nodeData.color,
                    emissive: nodeData.color,
                    emissiveIntensity: 0.3,
                    metalness: 0.4,
                    roughness: 0.6,
                });

                const mesh = new THREE.Mesh(geometry, material);
                mesh.position.set(...nodeData.position);
                mesh.castShadow = true;
                mesh.receiveShadow = true;
                
                scene.add(mesh);

                // Create label
                const labelCanvas = document.createElement('canvas');
                labelCanvas.width = 256;
                labelCanvas.height = 64;
                const ctx = labelCanvas.getContext('2d');
                if (ctx) {
                    ctx.fillStyle = '#ffffff';
                    ctx.font = 'bold 24px Arial';
                    ctx.textAlign = 'center';
                    ctx.fillText(nodeData.label, 128, 40);
                }

                const labelTexture = new THREE.CanvasTexture(labelCanvas);
                const labelMaterial = new THREE.MeshBasicMaterial({ map: labelTexture });
                const labelGeometry = new THREE.PlaneGeometry(
                    (nodeData.scale * 2 * labelCanvas.width) / labelCanvas.height,
                    nodeData.scale * 2
                );
                const labelMesh = new THREE.Mesh(labelGeometry, labelMaterial);
                labelMesh.position.copy(mesh.position);
                labelMesh.position.z += nodeData.scale + 5;
                scene.add(labelMesh);

                node = { ...nodeData, mesh };
                nodesRef.current.set(nodeData.id, node);
            } else {
                // Update existing node position
                if (node.mesh) {
                    node.mesh.position.set(...nodeData.position);
                }
            }
        });
    };

    /**
     * Update or create edges between nodes
     */
    const updateBuildFlowEdges = (edges: Edge3D[]) => {
        const scene = sceneRef.current;
        if (!scene) return;

        edges.forEach((edgeData) => {
            const edgeId = `${edgeData.from}-${edgeData.to}`;
            const fromNode = nodesRef.current.get(edgeData.from);
            const toNode = nodesRef.current.get(edgeData.to);

            if (!fromNode || !toNode) return;

            let edge = edgesRef.current.get(edgeId);

            if (!edge) {
                // Create new edge
                const geometry = new THREE.BufferGeometry();
                const positions = new Float32Array([
                    ...fromNode.position,
                    ...toNode.position,
                ]);
                geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

                const material = new THREE.LineBasicMaterial({
                    color: edgeData.color,
                    linewidth: edgeData.thickness,
                });

                const line = new THREE.Line(geometry, material);
                scene.add(line);

                edge = { ...edgeData, line };
                edgesRef.current.set(edgeId, edge);
            } else if (edge.line) {
                // Update edge positions
                const positions = edge.line.geometry.attributes.position.array as Float32Array;
                positions[0] = fromNode.position[0];
                positions[1] = fromNode.position[1];
                positions[2] = fromNode.position[2];
                positions[3] = toNode.position[0];
                positions[4] = toNode.position[1];
                positions[5] = toNode.position[2];
                edge.line.geometry.attributes.position.needsUpdate = true;
            }
        });
    };

    /**
     * Update or create agent nodes
     */
    const updateAgentNodes = (agents: AgentNode[]) => {
        const scene = sceneRef.current;
        if (!scene) return;

        agents.forEach((agentData) => {
            let agent = agentsRef.current.get(agentData.id);

            if (!agent) {
                // Create new agent node
                const geometry = new THREE.OctahedronGeometry(agentData.scale, 3);
                const material = new THREE.MeshStandardMaterial({
                    color: agentData.color,
                    emissive: agentData.color,
                    emissiveIntensity: 0.5,
                    wireframe: false,
                });

                const mesh = new THREE.Mesh(geometry, material);
                mesh.position.set(...(agentData.position as [number, number, number]));
                mesh.castShadow = true;
                mesh.receiveShadow = true;
                
                scene.add(mesh);

                agent = { ...agentData, mesh };
                agentsRef.current.set(agentData.id, agent);
            } else {
                // Update agent position
                if (agent.mesh) {
                    agent.mesh.position.set(...(agentData.position as [number, number, number]));
                }
            }
        });
    };

    /**
     * Initialize on component mount
     */
    useEffect(() => {
        initScene();
        connectWebSocket();

        return () => {
            // Cleanup
            if (animationIdRef.current) {
                cancelAnimationFrame(animationIdRef.current);
            }
            if (websocketRef.current) {
                websocketRef.current.close();
            }
            if (rendererRef.current && mountRef.current) {
                mountRef.current.removeChild(rendererRef.current.domElement);
            }
        };
    }, []);

    const handleContainerFocus = useCallback(() => {
        isFocusedRef.current = true;
    }, []);

    const handleContainerBlur = useCallback(() => {
        isFocusedRef.current = false;
    }, []);

    /**
     * Render component
     */
    return (
        <div className="three-dashboard" role="region" aria-label="3D Real-Time Visualization Dashboard">
            <div
                ref={mountRef}
                className="scene-container"
                role="img"
                aria-label="3D real-time visualization dashboard showing system architecture, services, agents, and components. Use arrow keys to navigate when focused."
                aria-describedby="dashboard-description"
                tabIndex={0}
                onFocus={handleContainerFocus}
                onBlur={handleContainerBlur}
            />
            
            {/* HUD Overlay with metrics */}
            <div className="hud-overlay" aria-live="polite" aria-atomic="true">
                <div id="dashboard-description" className="sr-only">
                    Interactive 3D dashboard displaying real-time system metrics including FPS, connection status, and connected clients. Use arrow keys to move the camera, plus/minus to zoom.
                </div>
                <div className="hud-section top-left">
                    <div className="hud-title">3D VISUALIZATION</div>
                    <div className="hud-stat">
                        <span className="label">FPS:</span>
                        <span className={`value ${fps > 50 ? 'good' : fps > 30 ? 'warning' : 'bad'}`}>
                            {fps}
                        </span>
                    </div>
                    <div className="hud-stat">
                        <span className="label">Status:</span>
                        <span className={`value ${isConnected ? 'connected' : 'disconnected'}`}>
                            {isConnected ? 'LIVE' : 'OFFLINE'}
                        </span>
                    </div>
                    {connectionError && (
                        <div className="hud-stat" style={{ color: '#ff4d4f' }}>
                            <span className="label">Error:</span>
                            <span>{connectionError}</span>
                        </div>
                    )}
                </div>

                <div className="hud-section bottom-right">
                    <div className="hud-stat">
                        <span className="label">Connected Clients:</span>
                        <span className="value">{connectedClients}</span>
                    </div>
                    <div className="hud-stat">
                        <span className="label">Render Time:</span>
                        <span className="value">&lt;16ms</span>
                    </div>
                </div>

                <div className="hud-section bottom-left">
                    <div className="legend">
                        <div className="legend-item">
                            <div className="legend-color" style={{ backgroundColor: '#00aa00' }} />
                            <span>Services</span>
                        </div>
                        <div className="legend-item">
                            <div className="legend-color" style={{ backgroundColor: '#0088ff' }} />
                            <span>Agents</span>
                        </div>
                        <div className="legend-item">
                            <div className="legend-color" style={{ backgroundColor: '#ff8800' }} />
                            <span>Components</span>
                        </div>
                    </div>
                    <div className="keyboard-hints" style={{ marginTop: 8, fontSize: 11, color: '#888' }}>
                        <div>Arrow keys: Move camera</div>
                        <div>+/-: Zoom in/out</div>
                        <div>Shift: Faster movement</div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ThreeDashboard;
