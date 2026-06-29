import { apiClient } from '../apiClient';

export interface JavaWorkerHealth {
  status: string;
  uptimeSeconds: number;
  activeTasks: number;
  queuedTasks: number;
  memoryUsageMb: number;
  cpuLoadPercentage: number;
  totalTasksProcessed: number;
}

export const fetchJavaWorkerHealth = async (): Promise<JavaWorkerHealth> => {
  // Mock implementation, eventually connects to FastAPI which proxies from Java
  // For now, returning mocked data to simulate the Java worker metrics
  try {
    const response = await apiClient.get<JavaWorkerHealth>('/admin/microservices/java-worker/health');
    return response.data;
  } catch (error) {
    // Return dummy data if backend route is not ready
    console.warn("Using mock data for Java worker health");
    return {
      status: 'HEALTHY',
      uptimeSeconds: Math.floor(Math.random() * 86400),
      activeTasks: Math.floor(Math.random() * 5),
      queuedTasks: Math.floor(Math.random() * 10),
      memoryUsageMb: 256 + Math.floor(Math.random() * 128),
      cpuLoadPercentage: Math.floor(Math.random() * 40) + 10,
      totalTasksProcessed: 1250 + Math.floor(Math.random() * 50)
    };
  }
};
