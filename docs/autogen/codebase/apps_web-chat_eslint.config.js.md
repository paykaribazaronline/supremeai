# 📄 ফাইল: apps/web-chat/eslint.config.js

**প্রকার:** .js  
**সাইজ:** 1,934 বাইট  
**আপডেট:** 2026-07-11T17:37:52.697189

---

## কোড

```js
import js from "@eslint/js";
import tsParser from "@typescript-eslint/parser";
import tsPlugin from "@typescript-eslint/eslint-plugin";
import globals from "globals";

export default [
  js.configs.recommended,
  {
    files: ["**/*.ts", "**/*.tsx"],
    languageOptions: {
      parser: tsParser,
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
      },
      // Required for strict type-checked rules
      parserOptions: {
        project: ["./tsconfig.app.json", "./tsconfig.node.json"],
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      "@typescript-eslint": tsPlugin,
    },
    rules: {
      // Base rules
      "no-unused-vars": "off",
      "@typescript-eslint/no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
      
      // Strict Type Safety (Anti `any` Rules)
      "@typescript-eslint/no-explicit-any": "error", // No explicit any
      "@typescript-eslint/no-unsafe-assignment": "error", // No assigning any
      "@typescript-eslint/no-unsafe-member-access": "error", // No property access on any
      "@typescript-eslint/no-unsafe-call": "error", // No function calls on any
      "@typescript-eslint/no-unsafe-return": "error", // No returning any
      "@typescript-eslint/no-unsafe-argument": "error", // No passing any as argument
      "@typescript-eslint/explicit-function-return-type": ["warn", { allowExpressions: true }],
      
      // Strict Error Handling (Anti-Silent Error Rules)
      "no-empty": ["error", { allowEmptyCatch: false }], // No empty try/catch blocks
      "@typescript-eslint/no-floating-promises": "error", // Promises must be handled
      "@typescript-eslint/no-misused-promises": "error",

      // Anti-Billing Leak / Performance
      "no-constant-condition": ["error", { "checkLoops": true }], // Prevent while(true)
    },
  },
  {
    ignores: ["dist", "node_modules"],
  },
];

```