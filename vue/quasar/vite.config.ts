/// <reference types="vitest/config" />
/// <reference types="@vitest/browser/providers/playwright" />
import * as path from 'node:path'

import { quasar, transformAssetUrls } from '@quasar/vite-plugin'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { removeDataTestid } from './src/plugins'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue({ template: { transformAssetUrls } }),
    quasar({
      sassVariables: path.resolve('src/styles/quasar.sass'),
    }),
    removeDataTestid(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src/'),
    },
  },
  test: {
    projects: [{
      extends: true,
      test: {
        include: ['tests/node/**/*.{spec,test}.{ts,js}'],
        name: { label: 'node', color: 'yellow' },
        environment: 'node',
      },
    }, {
      extends: true,
      test: {
        browser: {
          enabled: true,
          headless: false,
          provider: 'playwright',
          // https://vitest.dev/guide/browser/playwright
          instances: [
            { browser: 'chromium'/* , viewport: {height: 1280, width: 720} */ },
            // { browser: 'firefox' }
          ],
        },
        include: ['tests/browser/**/*.{spec,test}.{ts,js}'],
        name: { label: 'browser', color: 'green' },
        environment: 'browser',
      },
    }],
  },
})
