// 3D Dashboard Core - Three.js Implementation
import * as THREE from 'https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js';
import { OrbitControls } from 'https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/controls/OrbitControls.js';

export class Dashboard3D {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(75, this.container.clientWidth / this.container.clientHeight, 0.1, 1000);
        this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        
        this.init();
        this.animate();
    }

    init() {
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.container.appendChild(this.renderer.domElement);

        this.controls = new OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;

        this.camera.position.set(0, 5, 10);
        this.scene.background = new THREE.Color(0x0a0a0f);

        // Add lighting
        const ambientLight = new THREE.AmbientLight(0x404040, 0.6);
        const pointLight = new THREE.PointLight(0x00ff88, 1, 100);
        pointLight.position.set(10, 10, 10);
        this.scene.add(ambientLight, pointLight);

        // Create agent nodes
        this.createAgentNodes();
        
        window.addEventListener('resize', () => this.onResize());
    }

    createAgentNodes() {
        const agentGeometry = new THREE.SphereGeometry(0.3, 32, 32);
        const agentMaterial = new THREE.MeshStandardMaterial({ color: 0x00ff88, emissive: 0x00ff44, emissiveIntensity: 0.3 });
        
        for (let i = 0; i < 10; i++) {
            const agent = new THREE.Mesh(agentGeometry, agentMaterial.clone());
            const angle = (i / 10) * Math.PI * 2;
            agent.position.x = Math.cos(angle) * 4;
            agent.position.z = Math.sin(angle) * 4;
            agent.position.y = 0.5;
            agent.userData.id = i;
            this.scene.add(agent);
        }
    }

    animate() {
        requestAnimationFrame(() => this.animate());
        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }

    onResize() {
        this.camera.aspect = this.container.clientWidth / this.container.clientHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
    }
}
