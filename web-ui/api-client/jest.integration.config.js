module.exports = {
  collectCoverage: true,
  collectCoverageFrom: [
    'src/**/*.js',
  ],
  coverageReporters: [
    'html-spa',
    'text-summary',
  ],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  reporters: [
    [
      'jest-html-reporter',
      {
        outputPath: './reports/test-report.html',
        pageTitle: 'Test Report',
        includeFailureMsg: true,
        includeConsoleLog: true,
      },
    ],
  ],
  testEnvironment: 'jsdom',
  testRegex: '(/tests/.*\\.test\\.js)$',
  transform: {
    '^.+\\.js$': 'babel-jest',
  },
};
