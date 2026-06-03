export interface DeploymentRecord {
  appId: string;
  deviceType: string;
  previewUrl: string;
  status: string;
  deployedAt: string;
}

export interface Project {
  id: string;
  name: string;
  type: string;
}

export interface SimulatorStatsData {
  totalDeployments: number;
  activeSessions: number;
}
