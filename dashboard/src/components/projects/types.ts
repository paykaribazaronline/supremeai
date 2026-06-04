export interface Project {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface GenerationForm {
  name: string;
  description: string;
  platform: string;
  database: string;
  useAI?: boolean;
}

export type GenerationStatus = "idle" | "generating" | "success" | "error";

export type ProjectSortField = "name" | "status" | "createdAt";
