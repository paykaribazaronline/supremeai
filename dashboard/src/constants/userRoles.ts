// User role constants for consistent styling across the application
export const USER_ROLE_COLORS = {
  ADMIN: 'red',
  PRO: 'blue',
  ENTERPRISE: 'purple',
  USER: 'default',
  FREE: 'default'
} as const;

export type UserRole = keyof typeof USER_ROLE_COLORS;

export const getUserRoleColor = (role: string): string => {
  const upperRole = role.toUpperCase() as UserRole;
  return USER_ROLE_COLORS[upperRole] || USER_ROLE_COLORS.USER;
};