import { describe, expect, test } from 'vitest'
import { render } from 'vitest-browser-vue'

import MyComponent from '../../../src/components/MyComponent.vue'
import ui from '@nuxt/ui/vue-plugin'

import { createRouter, createWebHistory } from 'vue-router'

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

    await screen.getByRole('button').click()
    await screen.getByRole('button').click()

    await expect(screen.baseElement).toMatchScreenshot()
  })

})
