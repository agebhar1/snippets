import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import fs from 'node:fs/promises'
import { harFromMessages } from 'chrome-har'
import puppeteer from 'puppeteer'

let browser
let client
let events
let page
let promises

const observe = [
  // https://chromedevtools.github.io/devtools-protocol/tot/Network/
  'Network.dataReceived',
  'Network.eventSourceMessageReceived',
  'Network.loadingFailed',
  'Network.loadingFinished',
  'Network.requestServedFromCache',
  'Network.requestWillBeSent',
  'Network.responseReceived',
  'Network.webSocketClosed',
  'Network.webSocketCreated',
  'Network.webSocketFrameError',
  'Network.webSocketFrameReceived',
  'Network.webSocketFrameSent',
  'Network.webSocketHandshakeResponseReceived',
  'Network.webSocketWillSendHandshakeRequest',
  'Network.webTransportClosed',
  'Network.webTransportConnectionEstablished',
  'Network.webTransportCreated',
  'Network.policyUpdated', // Experimental
  'Network.reportingApiEndpointsChangedForOrigin', // Experimental
  'Network.reportingApiReportAdded', // Experimental
  'Network.reportingApiReportUpdated', // Experimental
  'Network.requestWillBeSentExtraInfo', // Experimental
  'Network.resourceChangedPriority', // Experimental
  'Network.responseReceivedEarlyHints', // Experimental
  'Network.responseReceivedExtraInfo', // Experimental
  'Network.signedExchangeReceived', // Experimental
  'Network.subresourceWebBundleInnerResponseError', // Experimental
  'Network.subresourceWebBundleInnerResponseParsed', // Experimental
  'Network.subresourceWebBundleMetadataError', // Experimental
  'Network.subresourceWebBundleMetadataReceived', // Experimental
  'Network.trustTokenOperationDone', // Experimental
  // https://chromedevtools.github.io/devtools-protocol/tot/Page/
  'Page.domContentEventFired',
  'Page.fileChooserOpened',
  'Page.frameAttached',
  'Page.frameDetached',
  'Page.frameNavigated',
  'Page.interstitialHidden',
  'Page.interstitialShown',
  'Page.javascriptDialogClosed',
  'Page.javascriptDialogOpening',
  'Page.lifecycleEvent',
  'Page.loadEventFired',
  'Page.windowOpen',
  'Page.backForwardCacheNotUsed', // Experimental
  'Page.compilationCacheProduced', // Experimental
  'Page.documentOpened', // Experimental
  'Page.frameRequestedNavigation', // Experimental
  'Page.frameResized', // Experimental
  'Page.frameStartedLoading', // Experimental
  'Page.frameStoppedLoading', // Experimental
  'Page.frameSubtreeWillBeDetached', // Experimental
  'Page.navigatedWithinDocument', // Experimental
  'Page.screencastFrame', // Experimental
  'Page.screencastVisibilityChanged', // Experimental
]

async function startTrace() {
  events = []
  promises = []
  client = await page.target().createCDPSession()
  // https://chromedevtools.github.io/devtools-protocol/tot/Network/#method-enable
  await client.send('Network.enable')
  // https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-enable
  await client.send('Page.enable')

  // register event listeners
  observe.forEach((method) => {
    client.on(method, (params) => {
      events.push({ method, params })
      // https://github.com/Everettss/puppeteer-har/blob/977fe74a5e952478e968d28267cf6a1ff8d0f1fa/lib/PuppeteerHar.js#L72-L97
      if (method === 'Network.responseReceived') {
        const { requestId, response: { status } } = params
        if (status !== 204) {
          const promise = client
            .send('Network.getResponseBody', { requestId })
            .then(({ body, base64Encoded }) => {
              params.response.body = body
              params.response.encoding = base64Encoded ? 'base64' : undefined
            }, () => {
              // ProtocolError: Protocol error (Network.getResponseBody): No data found for resource with given identifier
            })
          promises.push(promise)
        }
      }
    })
  })
}

async function stopTrace() {
  await client.send('Network.disable')
  await client.send('Page.disable')
}

async function writeTrace(file) {
  await Promise.all(promises)
  const har = harFromMessages(events, {
    includeResourcesFromDiskCache: true,
    includeTextFromResponseBody: true,
  })
  return fs.writeFile(file, JSON.stringify(har))
}

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
  await startTrace()
})

afterEach(async () => {
  await stopTrace()

  const client = await page.target().createCDPSession()
  await client.send('Network.clearBrowserCache')
  await client.send('Network.clearBrowserCookies')

  page.close()
  page = null
  events = null
})

describe('NGINX', () => {
  async function loginWithKeycloak() {
    const login = await page.waitForSelector('#kc-login')
    expect(login).not.toBeNull()

    const username = await page.$('#username')
    expect(username).not.toBeNull()

    const password = await page.$('#password')
    expect(password).not.toBeNull()

    await username.type('admin@example.com')
    await password.type('password')
    await login.click()

    return page.waitForNavigation()
  }

  describe('ngx_http_auth_request_module', () => {
    describe('auth_request', () => {
      it('protects resource by Keycloak (OAuth2)', async () => {
        await page.goto('http://localhost:8080/')

        await loginWithKeycloak()

        await writeTrace('oauth2.har')

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

    describe('auth_request_set', () => {
      it('provide authorization response header "X-Auth-Request-Access-Token" to request variable', async () => {
        await page.goto('http://localhost:8080/userinfo/access-token')

        const response = await loginWithKeycloak()
        const data = await response.json()

        expect(data).toEqual({
          email: 'admin@example.com',
          preferred_username: 'admin@example.com',
          sub: '3356c0a0-d4d5-4436-9c5a-2299c71c08ec',
        })
      })
    })
  })
})
