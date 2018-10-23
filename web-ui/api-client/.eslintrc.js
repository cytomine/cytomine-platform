module.exports = {
    "env": {
        es6: true,
        browser: true,
        node: true
    },
    parserOptions: {
        ecmaVersion: 2018,
        sourceType: "module"
    },
    extends: "eslint:recommended",
    rules: {
        "indent": ["error", 4],
        "quotes": ["error", "double", {"avoidEscape": true}],
        "brace-style": ["error", "stroustrup"],
        "array-bracket-spacing": ["error", "never"],
        "camelcase": ["error", {"properties": "always"}],
        "semi": ["error", "always"]
    }
}
