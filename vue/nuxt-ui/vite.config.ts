import { URL, fileURLToPath } from 'node:url'

import { configDefaults } from 'vitest/config'
import { defineConfig } from 'vite'
import { playwright } from '@vitest/browser-playwright'
import ui from '@nuxt/ui/vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    ui({ router: true }),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    setupFiles: 'tests/browser/setup.ts',
    browser: {
      enabled: true,
      headless: true,
      provider: playwright(),
      instances: [
        { browser: 'chromium', viewport: { height: 1080, width: 1920 } },
      ],
    },
    exclude: [...configDefaults.exclude, 'e2e/**'],
    root: fileURLToPath(new URL('./', import.meta.url)),
  },
})
