import { vi } from "vitest";
import "@testing-library/jest-dom/vitest";

// Mock localStorage and sessionStorage globally to prevent JSDOM test environment failures
if (typeof window !== "undefined") {
  const mockStorage = () => {
    let store: Record<string, string> = {};
    return {
      getItem: (key: string) => store[key] || null,
      setItem: (key: string, value: string) => {
        store[key] = value.toString();
      },
      removeItem: (key: string) => {
        delete store[key];
      },
      clear: () => {
        store = {};
      },
      length: 0,
      key: (index: number) => "",
    };
  };

  Object.defineProperty(window, "localStorage", { value: mockStorage() });
  Object.defineProperty(window, "sessionStorage", { value: mockStorage() });
}
