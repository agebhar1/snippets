// @ts-check

import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import { globalIgnores } from 'eslint/config'
import pluginVue from 'eslint-plugin-vue'
import stylistic from '@stylistic/eslint-plugin'
import tseslint from 'typescript-eslint'
import vitest from '@vitest/eslint-plugin'

export default defineConfigWithVueTs(
  pluginVue.configs['flat/recommended'],
  stylistic.configs['disable-legacy'],
  stylistic.configs.recommended,
  tseslint.configs.stylistic,
  vitest.configs.all,
  vueTsConfigs.recommended,
  {
    name: 'config',
    rules: {
      'vitest/no-done-callback': 0,
    },
  },
  globalIgnores([
    'dist/**/*',
  ]),
)
