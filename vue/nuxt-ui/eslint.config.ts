import { vueTsConfigs, withVueTs } from '@vue/eslint-config-typescript'
import { globalIgnores } from 'eslint/config'

import pluginVitest from '@vitest/eslint-plugin'
import pluginVue from 'eslint-plugin-vue'
import stylistic from '@stylistic/eslint-plugin'

export default withVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
    rules: {
      // https://eslint.org/docs/latest/rules/sort-imports
      'sort-imports': ['error', {
        allowSeparatedGroups: true,
      }],
    },
  },
  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**']),

  ...pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  stylistic.configs.recommended,
)
