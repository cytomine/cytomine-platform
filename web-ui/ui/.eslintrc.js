module.exports = {
  'root': true,
  'env': {
    'node': true
  },
  'extends': [
    'eslint:recommended',
    'plugin:jest/recommended',
    'plugin:vue/essential'
  ],
  rules: {
    'array-bracket-spacing': ['error', 'never'],
    'brace-style': ['error', '1tbs'],
    'camelcase': ['error', {allow: ['$_veeValidate']}],
    'curly': ['error', 'all'],
    'eqeqeq': ['error', 'always'],
    'indent': ['error', 2, {'SwitchCase': 1, 'ignoredNodes': ['TemplateLiteral']}],
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
  },
};
