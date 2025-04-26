module.exports = {
  moduleFileExtensions: ['js', 'json', 'ts'],
  rootDir: 'src',
  testEnvironment: 'node',
  testRegex: '.*\\.spec\\.ts$',
  transform: {
    '^.+\\.(t|j)s$': 'ts-jest',
  },
  collectCoverageFrom: ['**/*.(t|j)s'],
  coverageReporters:
    process.env.CI === 'true' || process.env.CI === true
      ? ['text-summary', 'cobertura']
      : ['lcov'],
  coveragePathIgnorePatterns: ['<rootDir>/node_modules/', '@types', '\.dto\.(t|j)s'],
  coverageDirectory: '../coverage',
};
