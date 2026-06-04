import {
  Sphere,
  MeshDistortMaterial,
  Float,
  MeshWobbleMaterial,
} from "@react-three/drei";
import { useFrame } from "@react-three/fiber";
import React, { useRef } from "react";
import * as THREE from "three";

export const CoreEngine = () => {
  const outerRef = useRef<THREE.Mesh>(null);
  const innerRef = useRef<THREE.Mesh>(null);

  useFrame((state) => {
    const time = state.clock.getElapsedTime();
    if (outerRef.current) {
      outerRef.current.rotation.y = time * 0.5;
      outerRef.current.rotation.z = time * 0.3;
    }
    if (innerRef.current) {
      innerRef.current.rotation.y = -time * 0.8;
    }
  });

  return (
    <group>
      <Float speed={2} rotationIntensity={0.5} floatIntensity={0.5}>
        {/* Outer Shield */}
        <Sphere ref={outerRef} args={[1.5, 64, 64]}>
          <MeshDistortMaterial
            color="#00f3ff"
            attach="material"
            distort={0.3}
            speed={2}
            roughness={0.1}
            metalness={0.8}
            transparent
            opacity={0.2}
            wireframe
          />
        </Sphere>

        {/* Inner Core */}
        <Sphere ref={innerRef} args={[0.8, 64, 64]}>
          <MeshWobbleMaterial
            color="#bc13fe"
            factor={0.4}
            speed={5}
            emissive="#bc13fe"
            emissiveIntensity={2}
          />
        </Sphere>

        {/* Core Glow */}
        <pointLight position={[0, 0, 0]} intensity={100} color="#00f3ff" />
        <pointLight position={[0, 0, 0]} intensity={50} color="#bc13fe" />
      </Float>

      {/* Orbiting Particles */}
      <OrbitingData color="#00f3ff" radius={2.5} speed={1} />
      <OrbitingData color="#ff00ff" radius={3.2} speed={-0.8} />
    </group>
  );
};

const OrbitingData = ({
  color,
  radius,
  speed,
}: {
  color: string;
  radius: number;
  speed: number;
}) => {
  const ref = useRef<THREE.Group>(null);

  useFrame((state) => {
    if (ref.current) {
      ref.current.rotation.y = state.clock.getElapsedTime() * speed;
    }
  });

  return (
    <group ref={ref}>
      {[...Array(5)].map((_, i) => (
        <mesh
          key={i}
          position={[
            Math.cos((i / 5) * Math.PI * 2) * radius,
            Math.sin(i * 1.5) * 0.5,
            Math.sin((i / 5) * Math.PI * 2) * radius,
          ]}
        >
          <boxGeometry args={[0.1, 0.1, 0.1]} />
          <meshStandardMaterial
            color={color}
            emissive={color}
            emissiveIntensity={5}
          />
        </mesh>
      ))}
    </group>
  );
};
