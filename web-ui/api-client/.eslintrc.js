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
    'array-bracket-spacing': ['error', 'never'],
    'brace-style': ['error', '1tbs'],
    'camelcase': ['error', {'properties': 'always'}],
    'curly': ['error', 'all'],
    'eqeqeq': ['error', 'always'],
    'indent': ['error', 2,{'SwitchCase': 1}],
    'keyword-spacing': ['error'],
    'no-console': ['off'],
    'no-redeclare': ['error'],
    'no-undef': ['error'],
    'no-unused-vars': ['error'],
    'no-var': ['error'],
    'object-curly-spacing': ['error'],
    'quotes': ['error', 'single', {'avoidEscape': true}],
    'semi': ['error', 'always'],
    'space-before-blocks': ['error', 'always'],
    'space-before-function-paren': ['error', {
      anonymous: 'always',
      named: 'never',
      asyncArrow: 'always',
    }],
    'space-infix-ops': ['error'],
    'space-in-parens': ['error'],
    'jest/expect-expect': 'off',
    'jest/no-commented-out-tests': 'off',
    'jest/no-disabled-tests': 'off',
  }
}
