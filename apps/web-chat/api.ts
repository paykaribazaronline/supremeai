import axios from 'axios';

// JWT Integration using Axios
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  timeout: 5000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const api = {
  async executeTask(task: string, messages: any[], taskType: string = 'general') {
    try {
      const response = await apiClient.post('/task/execute', {
        task,
        task_type: taskType,
        messages
      });
      return response.data;
    } catch (error: any) {
      console.error('Task Execution Failed:', error);
      return { result: null, error: error.message };
    }
  },

  async fetchQuota() {
    return new Promise((resolve) => setTimeout(() => resolve({ remaining: 87 }), 500));
  }
};
