import { useState } from 'react';
import { apiClient, ApiError } from '../services/apiClient';

export const useBudgetCheck = () => {
  const [isChecking, setIsChecking] = useState(false);
  const [budgetError, setBudgetError] = useState<string | null>(null);

  const checkBudget = async (estimatedCost: number = 0): Promise<boolean> => {
    setIsChecking(true);
    setBudgetError(null);
    try {
      // In a real implementation, you might pass the estimated cost or operation type
      await apiClient.get(`/api/admin/metrics/cost?estimated=${estimatedCost}`);
      setIsChecking(false);
      return true;
    } catch (err: any) {
      setIsChecking(false);
      if (err instanceof ApiError && err.status === 402) {
        setBudgetError(err.message || 'Insufficient budget for this operation.');
      } else {
        setBudgetError('Failed to verify budget.');
      }
      return false;
    }
  };

  return { checkBudget, isChecking, budgetError };
};
