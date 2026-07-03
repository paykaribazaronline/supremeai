/**
 * In-memory admin token store.
 * This intentionally keeps admin tokens out of browser-local storage
 * to reduce exposure from XSS and persistent storage.
 */
let adminToken = '';

export const setAdminToken = (token: string) => {
  adminToken = token;
};

export const getAdminToken = () => adminToken;

export const clearAdminToken = () => {
  adminToken = '';
};
