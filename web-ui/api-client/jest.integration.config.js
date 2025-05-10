module.exports = {
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  testEnvironment: 'jsdom',
  testRegex: "(/tests/.*\\.test\\.js)$",
  transform: {
    '^.+\\.js$': 'babel-jest',
  },
};
