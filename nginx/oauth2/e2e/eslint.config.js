import { defineConfig } from 'eslint/config'
import js from '@eslint/js'
import stylistic from '@stylistic/eslint-plugin'
import vitest from '@vitest/eslint-plugin'

export default defineConfig([
  js.configs.recommended,
  stylistic.configs['recommended'],
  vitest.configs.recommended,
  {
    rules: {
      'sort-imports': 'error',
    },
  },
])
