import { describe, expect, test, vi } from 'vitest'
import { render } from 'vitest-browser-vue'

import MyComponent from '../../../src/components/MyComponent.vue'
import ui from '@nuxt/ui/vue-plugin'

import { createRouter, createWebHistory } from 'vue-router'

const useToast = {
  add: vi.fn().mockName('useToast.add'),
}

// @ts-expect-error n/a
vi.mock(import('@nuxt/ui/composables'), () => ({
  useToast: () => useToast,
}))

const router = createRouter({
  routes: [
    { path: '/', component: MyComponent },
  ],
  history: createWebHistory(),
})

describe('MyComponent', () => {
  test('should match screenshot', async () => {
    const screen = await render(MyComponent, {
      props: {
        text: 'Hello World',
      },
      global: {
        plugins: [ui, router],
      },
    })

    await screen.getByRole('button', { name: '++' }).click()
    await screen.getByRole('button', { name: 'Toast' }).click()

    expect(useToast.add).toHaveBeenCalledWith(expect.objectContaining(
      { title: 'Toast title', description: 'Toast description' },
    ))
    await expect(screen.baseElement).toMatchScreenshot()
  })
})
