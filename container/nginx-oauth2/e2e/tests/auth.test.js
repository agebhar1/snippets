import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import puppeteer from 'puppeteer'

let browser
let page

beforeAll(async () => {
  browser = await puppeteer.launch({
    headless: true,
  })
})

afterAll(async () => {
  await browser.close()
})

beforeEach(async () => {
  page = await browser.newPage()
  await page.setViewport({
    width: 1920,
    height: 1080,
    deviceScaleFactor: 1,
  })
})

afterEach(async () => {
  page.close()
})

describe('NGINX', () => {
  it('protects resource by Keycloak (OAuth2)', async () => {
    await page.goto('http://localhost:8080/')

    const login = await page.waitForSelector('#kc-login')
    expect(login).not.toBeNull()

    const username = await page.$('#username')
    expect(username).not.toBeNull()

    const password = await page.$('#password')
    expect(password).not.toBeNull()

    await username.type('admin@example.com')
    await password.type('password')
    await login.click()

    await page.waitForNavigation()

    expect(page.url()).toStrictEqual('http://localhost:8080/')

    const text = await page.$eval('body', el => el.innerText)
    expect(text).toStrictEqual(
      `Welcome to nginx!

If you see this page, the nginx web server is successfully installed and working. Further configuration is required.

For online documentation and support please refer to nginx.org.
Commercial support is available at nginx.com.

Thank you for using nginx.`)
  })
})
