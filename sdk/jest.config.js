/** @type {import('ts-jest').JestConfigWithTsJest} */
export default {
  roots: [
    '.',
  ],
  preset: 'ts-jest',
  testEnvironment: 'node',
  collectCoverage: true,
  collectCoverageFrom: [
    'index.ts',
    'lib/**',
  ],
  setupFilesAfterEnv: [
    'jest-extended/all',
  ],
  transformIgnorePatterns: [
      'node_modules/(?!(string-width|strip-ansi|ansi-regex|test-json-import)/)',
  ],
  'transform': {
    '^.+\\.(ts|tsx)$': 'ts-jest',
  },
};
