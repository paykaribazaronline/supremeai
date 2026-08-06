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
    return response.data || {
      status: 'OFFLINE',
      uptimeSeconds: 0,
      activeTasks: 0,
      queuedTasks: 0,
      memoryUsageMb: 0,
      cpuLoadPercentage: 0,
      totalTasksProcessed: 0
    };
  } catch (error) {
    // বাংলা মন্তব্য: মাইক্রোসার্ভিস সংযোগ ব্যর্থ হলে ফেক ডাটার পরিবর্তে অফলাইন স্ট্যাটাস দেওয়া হচ্ছে
    console.warn("Java worker health check failed:", error);
    return {
      status: 'OFFLINE',
      uptimeSeconds: 0,
      activeTasks: 0,
      queuedTasks: 0,
      memoryUsageMb: 0,
      cpuLoadPercentage: 0,
      totalTasksProcessed: 0
    };
  }
};
