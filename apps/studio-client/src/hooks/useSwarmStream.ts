import { useContext } from 'react';
import { SwarmHealthContext } from '../providers/MockSwarmProvider';
import { SwarmContextState } from '../types/swarm';

export const useSwarmStream = (): SwarmContextState => {
  const context = useContext(SwarmHealthContext);
  if (!context) {
    throw new Error('useSwarmStream must be used within a SwarmProvider');
  }
  return context;
};
