import { describe, it, expect, beforeEach, vi } from "vitest";

import { authUtils, fetchWithAuth } from "../src/lib/authUtils";

describe("authUtils", () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("returns GUEST_MODE when no token present", () => {
    expect(authUtils.getToken()).toBe("GUEST_MODE");
    expect(authUtils.isAuthenticated()).toBe(false);
    expect(authUtils.isGuest()).toBe(true);
  });

  it("obfuscate and deobfuscate roundtrip", () => {
    const raw = "Bearer abc123.eyJzdWIiOiIxMjM0In0.abcdef";
    const obfuscated = authUtils.setToken(raw);
    // setToken also stores in sessionStorage, but we can read the stored value via deobfuscation
    const stored = sessionStorage.getItem("supremeai_token");
    expect(stored).toBeTruthy();
    // getToken should recover the original token
    expect(authUtils.getToken()).toBe(raw);
  });

  it("setCurrentUser / getCurrentUser roundtrip", () => {
    const user = { uid: "u1", email: "test@example.com", role: "admin" };
    authUtils.setCurrentUser(user);
    expect(authUtils.isAdmin()).toBe(true);
    expect(authUtils.getCurrentUser()?.email).toBe("test@example.com");
  });

  it("clearAuth removes all stored tokens", () => {
    authUtils.setToken("tok");
    authUtils.setCurrentUser({ uid: "u1" });
    authUtils.clearAuth();
    expect(authUtils.getToken()).toBe("GUEST_MODE");
    expect(authUtils.getCurrentUser()).toBeNull();
  });

  it("isAuthenticated returns true for a saved token", () => {
    authUtils.setToken("Bearer real.jwt.token");
    expect(authUtils.isAuthenticated()).toBe(true);
    expect(authUtils.isGuest()).toBe(false);
  });
});
