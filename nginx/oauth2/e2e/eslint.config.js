import js from '@eslint/js'
import stylistic from '@stylistic/eslint-plugin'
import vitest from '@vitest/eslint-plugin'

// eslint.config.js
export default [
  js.configs.recommended,
  stylistic.configs['recommended-flat'],
  vitest.configs.recommended,
  {
    rules: {
      'sort-imports': 'error',
    },
  },
]
