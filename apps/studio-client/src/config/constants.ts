export const AppDefaults = {
  adminEmail: import.meta.env.VITE_DEFAULT_ADMIN_EMAIL || '',
  maxConcurrency: 3,
  features: {
    selfHealing: false,
    costGuard: true
  }
};