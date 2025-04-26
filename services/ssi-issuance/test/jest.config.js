module.exports = {
  moduleFileExtensions: ['js', 'json', 'ts'],
  rootDir: '..',
  testEnvironment: 'node',
  testMatch: ['**/test/**/*.e2e-spec.ts'],
  transform: {
    '^.+\\.(t|j)s$': 'ts-jest',
  },
  // globalSetup: './test/utils/_global_setup.ts',
  // globalTeardown: './test/utils/_global_teardown.ts',
  collectCoverageFrom: ['src/modules/**/*.(t|j)s'],
  coveragePathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '\.spec\.(t|j)s$'
  ],
  coverageDirectory: 'e2e-coverage',
  coverageReporters:
    process.env.CI === 'true' || process.env.CI === true
      ? ['text-summary', 'cobertura']
      : ['lcov'],
};
