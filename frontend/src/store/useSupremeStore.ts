import { create } from 'zustand';
import { createUserSlice } from './slices/userSlice';
import { createWorkspaceSlice } from './slices/workspaceSlice';
import { createUiSlice } from './slices/uiSlice';
import { createApiSlice } from './slices/apiSlice';

export const useSupremeStore = create((...a) => ({
  ...createUserSlice(...a),
  ...createWorkspaceSlice(...a),
  ...createUiSlice(...a),
  ...createApiSlice(...a),
}));
