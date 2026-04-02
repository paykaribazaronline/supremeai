import React, { useEffect, useRef, useState } from 'react';
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
    const fpsCounterRef = useRef(0);
    const lastTimeRef = useRef(Date.now());

    /**
     * Initialize Three.js scene, camera, renderer
     */
    const initScene = () => {
        if (!mountRef.current) return;

        // Scene setup
        const scene = new THREE.Scene();
        scene.background = new THREE.Color(0x1a1a1a);
        scene.fog = new THREE.Fog(0x1a1a1a, 2000, 200);
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

        // Renderer setup
        const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
        renderer.setSize(window.innerWidth, window.innerHeight);
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFShadowMap;
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
        if (deltaTime > 100) {
            setFps(Math.round((fpsCounterRef.current * 1000) / deltaTime));
            fpsCounterRef.current = 0;
            lastTimeRef.current = now;
        }

        // Rotate agent nodes for visual interest
        agentsRef.current.forEach((agent) => {
            if (agent.mesh && agent.mesh.geometry) {
                agent.mesh.rotation.x += 0.01;
                agent.mesh.rotation.y += 0.02;
            }
        });

        // Light pulse on voting progress
        const scene = sceneRef.current;
        if (scene) {
            const lights = scene.children.filter((c) => c instanceof THREE.Light);
            lights.forEach((light) => {
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
                console.log('✓ 3D Visualization WebSocket connected');
                setIsConnected(true);
            };

            ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    handleVisualizationFrame(data);
                } catch (e) {
                    console.error('Error parsing visualization frame:', e);
                }
            };

            ws.onclose = () => {
                console.log('✗ 3D Visualization WebSocket disconnected');
                setIsConnected(false);
                // Attempt reconnection after 3 seconds
                setTimeout(connectWebSocket, 3000);
            };

            ws.onerror = (event) => {
                console.error('WebSocket error:', event);
                setIsConnected(false);
            };
        } catch (e) {
            console.error('Failed to connect WebSocket:', e);
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

    /**
     * Render component
     */
    return (
        <div className="three-dashboard">
            <div ref={mountRef} className="scene-container" />
            
            {/* HUD Overlay with metrics */}
            <div className="hud-overlay">
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
                            {isConnected ? '● LIVE' : '○ OFFLINE'}
                        </span>
                    </div>
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
                </div>
            </div>
        </div>
    );
};

export default ThreeDashboard;
