import { expect, test, beforeAll, afterAll } from 'vitest'
import { mount } from '@vue/test-utils'
import { render, config } from 'vitest-browser-vue'
import { commands, locators, page } from '@vitest/browser/context'
import App from '@/App.vue'
import HelloWorld from '../../src/components/HelloWorld.vue'
import { Quasar } from 'quasar'
import cloneDeep from 'lodash/cloneDeep'

import '@quasar/extras/material-icons/material-icons.css'
import '@quasar/extras/material-icons-outlined/material-icons-outlined.css'
import 'quasar/src/css/index.sass'
import { nextTick } from 'vue'

// locators.extend({
//   getByNative(selector) { return selector }
// })

declare module '@vitest/browser/context' {
  interface LocatorSelectors {
    getByNative(selector: string): Locator
  }
}

const waitRequestAnimationFrame = () =>
  new Promise<DOMHighResTimeStamp>((resolve) => {
    console.log('request animation frame...')
    requestAnimationFrame(resolve)
  })

// const globalConfigBackup = cloneDeep(config.global)
//
// beforeAll(() => {
//   console.log('beforeAll')
//   config.global.plugins.unshift([Quasar, {
//     install(app) {
//       console.log("Plugin");
//     }
//   }])
// })
//
// afterAll(() => {
//   console.log('afterAll')
//   config.global = globalConfigBackup
// })

test.runIf(() => true)('renders name', async () => {
  // await page.viewport(1920, 720)

  // const wrapper = mount(HelloWorld, {
  //   props: {
  //     msg: 'HelloWorld'
  //   },
  //   global: {
  //     plugins: [Quasar, { install(app) { console.log("Plugin"); } }]
  //   }
  // });
  //
  // expect(wrapper.exists()).toBe(true)
  //
  // console.log(wrapper.html())

  const screen = render(App, {
    props: {
      msg: 'Hello Quasar',
    },
    global: {
      plugins: [Quasar, {
        install(app) {
          console.log('Plugin » install')
        },
      }],
    },
  })

  await screen.getByRole('button').click()
  await screen.getByRole('button').click()

  await screen.getByTestId('1').click()

  const option = screen
    .getByRole('listbox')
    .getByRole('option', { name: 'orange' })
  // .nth(2)

  console.log(option)

  await option.hover()

  // while (true) {
  //   await waitRequestAnimationFrame()
  //   let xs = option.elements()
  //   console.log(xs)
  //   if (xs.length > 0) break
  // }

  await expect.element(option, { timeout: 1 }).toHaveTextContent('orange')

  await option.click()

  await page.screenshot({
    save: true,
  })
})
