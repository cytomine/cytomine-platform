module.exports = {
  'env': {
    es6: true,
    browser: true,
    node: true
  },
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: 'module'
  },
  extends: [
    'eslint:recommended',
    'plugin:jest/recommended',
  ],
  rules: {
    'indent': ['error', 2],
    'quotes': ['error', 'single', {'avoidEscape': true}],
    'array-bracket-spacing': ['error', 'never'],
    'camelcase': ['error', {'properties': 'always'}],
    'semi': ['error', 'always'],
    'jest/expect-expect': 'off',
    'jest/no-commented-out-tests': 'off',
    'jest/no-disabled-tests': 'off',
  }
}
