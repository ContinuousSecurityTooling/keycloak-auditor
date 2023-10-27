/* eslint-env node */
module.exports = {
  env: {
    node: true,
    commonjs: true,
  },
  extends: ['eslint:recommended', 'plugin:@typescript-eslint/recommended'],
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  root: true,
  rules: {
    quotes: [2, 'single', { avoidEscape: true }],
    'comma-dangle': ['error', 'only-multiline'],
  },
};
