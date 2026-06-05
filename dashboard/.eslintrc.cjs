module.exports = {
    root: true,
    env: { browser: true, es2020: true },
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:react/recommended',
        'plugin:react/jsx-runtime',
        'plugin:react-hooks/recommended',
        'plugin:import/recommended',
        'plugin:import/typescript',
        'plugin:prettier/recommended',
    ],
    ignorePatterns: ['dist', '.eslintrc.cjs'],
    parser: '@typescript-eslint/parser',
    plugins: ['react-refresh', 'import', 'react'],
    settings: {
        react: { version: 'detect' },
        'import/resolver': {
            node: {
                extensions: ['.js', '.jsx', '.ts', '.tsx'],
            },
        },
    },
    rules: {
        'react-refresh/only-export-components': [
            'warn',
            { allowConstantExport: true },
        ],
        // Ensures JSX components (like Ant Design's Space) are explicitly imported
        'react/jsx-no-undef': 'error',
        // React Three Fiber uses custom JSX properties
        'react/no-unknown-property': ['error', { ignore: ['intensity', 'position', 'angle', 'penumbra', 'args', 'attach', 'castShadow', 'receiveShadow', 'dispose', 'object', 'geometry', 'material', 'rotation', 'scale'] }],
        '@typescript-eslint/no-unused-vars': 'warn',
        // Validates import paths — ignore known local/dev packages
        'import/no-unresolved': ['error', {
            ignore: [
                '^vitest$',
                '^@testing-library/',
                '^@dataconnect/',
            ],
        }],
        // Automatically sorts imports for better readability
        'import/order': [
            'error',
            {
                groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index'],
                'newlines-between': 'always',
                alphabetize: {
                    order: 'asc',
                    caseInsensitive: true,
                },
            },
        ],
    },
    overrides: [
        {
            files: ['src/test/**/*.ts', 'src/test/**/*.tsx'],
            rules: {
                'import/no-unresolved': 'off',
                'import/namespace': 'off',
                'import/order': 'off',
                'import/no-duplicates': 'off',
                '@typescript-eslint/no-unused-vars': 'off',
            },
        },
    ],
}