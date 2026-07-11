# 📄 ফাইল: packages/shared-types/src/auth.types.ts

**প্রকার:** .ts  
**সাইজ:** 940 বাইট  
**আপডেট:** 2026-07-11T18:21:34.881281

---

## কোড

```ts
import { z } from 'zod';

export const AuthStateEnum = z.enum([
  'UNINITIALIZED',
  'LOGGED_OUT',
  'LOGGED_IN'
]);

export const AuthTransitionEventEnum = z.enum([
  'AUTH_INIT',
  'LOGIN_SUCCESS',
  'LOGIN_FAILURE',
  'SESSION_RESTORE',
  'LOGOUT',
  'SESSION_EXPIRE',
  'WORKSPACE_SWITCH'
]);

export const WorkspaceStateEnum = z.enum([
  'DASHBOARD',
  'PROJECT_SPACE',
  'SETTINGS',
  'COLLABORATION'
]);

export const AuthStateSchema = z.object({
  status: AuthStateEnum,
  user: z.object({
    id: z.string(),
    email: z.string().email(),
    role: z.string()
  }).nullable(),
  activeWorkspaceId: z.string().nullable(),
  workspaceState: WorkspaceStateEnum.nullable()
});

export type AuthState = z.infer<typeof AuthStateEnum>;
export type AuthTransitionEvent = z.infer<typeof AuthTransitionEventEnum>;
export type WorkspaceState = z.infer<typeof WorkspaceStateEnum>;
export type AuthStateData = z.infer<typeof AuthStateSchema>;

```