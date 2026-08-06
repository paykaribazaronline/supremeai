import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { CommandCenterRealtimeProvider } from './realtime/CommandCenterRealtimeProvider';
import { CommandCenterApp } from './shell/CommandCenterApp';
import { queryClient } from '../services/queryClient';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <CommandCenterRealtimeProvider>
        <CommandCenterApp />
      </CommandCenterRealtimeProvider>
    </QueryClientProvider>
  </React.StrictMode>,
);
