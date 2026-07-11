# 📄 ফাইল: apps/studio-client/eslint.config.js

**প্রকার:** .js  
**সাইজ:** 1,273 বাইট  
**আপডেট:** 2026-07-11T17:37:52.662365

---

## কোড

```js
// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
import storybook from "eslint-plugin-storybook";

import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'


export default tseslint.config({ ignores: ['dist'] }, {
  extends: [
    js.configs.recommended,
    ...tseslint.configs.recommended,
  ],
  files: ['**/*.{ts,tsx}'],
  languageOptions: {
    ecmaVersion: 2020,
    globals: globals.browser,
  },
  plugins: {
    'react-hooks': reactHooks,
    'react-refresh': reactRefresh,
  },
  rules: {
    ...reactHooks.configs.recommended.rules,
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true, allowExportNames: ['useTheme', 'useToast', 'useI18n', 'useThemeSync', 'useSwarmStream', 'setSujonState', 'useSujonState', 'SUJON_STATE_EVENT', 'SujonState', 'Theme', 'THEME_ORDER', 'ToastType', 'Toast', 'globalShowToastRef'] },
    ],
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-unused-vars': 'off',
    'react-hooks/set-state-in-effect': 'off',
  },
}, storybook.configs["flat/recommended"]);
```