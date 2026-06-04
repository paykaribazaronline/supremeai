export interface Job {
  jobId: string;
  url: string;
  status: string;
  progress: number;
  taskType?: string;
  instructions?: string;
  currentPhase?: string;
  submittedAt: string;
  startedAt?: string;
  completedAt?: string;
  error?: string;
  results?: {
    observation?: any;
    auth?: any;
    endpoints?: string[];
    connectors?: Record<
      string,
      { code: string; filename: string; status: string; validation?: any }
    >;
    alternative_suggestions?: AlternativeSuggestion[];
  };
}

export interface AlternativeSuggestion {
  url: string;
  reason: string;
  improvement: string;
}
