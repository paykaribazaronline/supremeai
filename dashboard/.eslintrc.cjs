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
        'react/jsx-no-undef': 'error',
        'react/no-unknown-property': 'off',
        '@typescript-eslint/no-unused-vars': 'off',
        '@typescript-eslint/no-explicit-any': 'off',
        'react-hooks/exhaustive-deps': 'off',
        'no-empty': 'off',
        'import/no-unresolved': ['off'],
        'import/order': 'off',
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