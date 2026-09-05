import { URL, fileURLToPath } from 'node:url'

import { configDefaults } from 'vitest/config'
import { defineConfig } from 'vite'
import { playwright } from '@vitest/browser-playwright'

import ui from '@nuxt/ui/vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        // https://rolldown.rs/reference/OutputOptions.codeSplitting
        codeSplitting: {
          groups: [
            { name: 'vue', test: /node_modules\/@?vue/, priority: 20 },
            { name: 'nuxt', test: /node_modules\/@nuxt/, priority: 15, includeDependenciesRecursively: false },
            { name: 'vendor', test: /node_modules/, priority: 10 },
          ],
        },
      },
    },
    sourcemap: true,
  },
  plugins: [
    vue(),
    ui({
      // https://ui.nuxt.com/docs/getting-started/integrations/icons/vue#iconify-dataset
      experimental: {
        componentDetection: true,
      },
      // https://ui.nuxt.com/docs/getting-started/integrations/icons/vue#iconify-dataset
      icon: {
        clientBundle: {
          scan: true,
        },
      },
      router: true,
    }),
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
        { browser: 'chromium', viewport: { width: 1920, height: 1080 } },
      ],
    },
    exclude: [...configDefaults.exclude, 'e2e/**'],
    root: fileURLToPath(new URL('./', import.meta.url)),
  },
})
