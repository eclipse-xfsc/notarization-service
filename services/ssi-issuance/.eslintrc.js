module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  parserOptions: {
    sourceType: 'module',
    ecmaVersion: 2020,
  },
  env: {
    node: true,
  },
  plugins: ['prettier', '@typescript-eslint/eslint-plugin', 'jest'],
  extends: [
    'prettier',
    'plugin:@typescript-eslint/recommended',
    'plugin:jest/recommended',
  ],
  ignorePatterns: ['.eslintrc.js'],
  overrides: [
    {
      files: ['*.js', '*.jsx'],
      rules: {
        '@typescript-eslint/no-var-requires': 'off',
      },
    },
    {
      files: ['*.dto.ts'],
      rules: {
        '@typescript-eslint/no-inferrable-types': 'off'
      }
    },
    {
      files: ['*.spec.ts'],
      rules: {
        '@typescript-eslint/no-explicit-any': 'off'
      },
    },
    {
      files: ['*.e2e-spec.ts'],
      rules: {
        'jest/no-done-callback': 'off',
      },
    },
  ],
  settings: {
    jest: {
      version: '27'
    },
  },
  rules: {
    "no-unused-vars": 0,
    "@typescript-eslint/no-unused-vars": [1, { "argsIgnorePattern": "^_" }]
  },
};
