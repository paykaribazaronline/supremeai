// constants.ts - API Key Providers & Popular Models
// All provider endpoints and model lists are now fetched dynamically from the backend.
// These constants serve only as fallback defaults during initial load.

import { PopularModel } from "./types";

export const POPULAR_MODELS: PopularModel[] = [];

export const PROVIDER_ENDPOINTS: Record<string, string> = {};

export const getProviderEndpoint = (providerName: string): string => {
  const lower = providerName.toLowerCase();
  return PROVIDER_ENDPOINTS[lower] || "";
};
