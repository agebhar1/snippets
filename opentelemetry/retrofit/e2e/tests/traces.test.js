import { afterAll, beforeAll, describe, expect, it } from 'vitest'
import { execFileSync } from 'node:child_process' // https://nodejs.org/api/child_process.html#child_processexecfilesyncfile-args-options
import path from 'node:path'

const sleep = timeout => new Promise(resolve => setTimeout(resolve, timeout))

const arrayContainingObject = expected => expect.arrayContaining([expect.objectContaining(expected)])

const composeFilePath = path.resolve(__dirname, '..')

beforeAll(async () => {
})

afterAll(async () => {
})

describe('PoC gateway', () => {
  it('should propagate traceID on middleware read/write operation(s)', async () => {
    const start = Math.floor(new Date().getTime() / 1000)

    execFileSync('docker', ['compose', 'exec', '--no-tty', '--env', 'OTEL_SERVICE_NAME=clientOne', 'middleware', 'client', '-w', 'one'], {
      cwd: composeFilePath,
      input: 'a',
      stdio: ['pipe', 'ignore', 'ignore'],
      encoding: 'utf8',
    })

    const traces = []
    do {
      const end = Math.floor(new Date().getTime() / 1000)
      try {
        const stdout = execFileSync('docker', ['compose', 'exec', 'middleware', 'curl', '--silent', '--fail', `http://tempo:3200/api/search?start=${start}&end=${end}`], {
          cwd: composeFilePath,
          stdio: ['ignore', 'pipe', 'ignore'],
          encoding: 'utf8',
        })
        traces.push(...JSON.parse(stdout).traces)
      }
      catch {
        await sleep(1000)
      }
    } while (traces.length === 0)

    const trace = traces.pop()
    expect(trace).toEqual({
      traceID: expect.anything(),
      rootServiceName: 'clientOne',
      rootTraceName: 'produce one',
      startTimeUnixNano: expect.anything(),
      durationMs: expect.toSatisfy(value => value >= 1000),
    })

    const stdout = execFileSync('docker', ['compose', 'exec', 'middleware', 'curl', '--silent', '--fail', `http://tempo:3200/api/traces/${trace.traceID}`], {
      cwd: composeFilePath,
      stdio: ['ignore', 'pipe', 'ignore'],
      encoding: 'utf8',
    })

    expect(JSON.parse(stdout)).toEqual({
      batches: expect.arrayContaining([
        expect.objectContaining({
          resource: { attributes: arrayContainingObject({ key: 'service.name', value: { stringValue: 'clientOne' } }) },
          scopeSpans: arrayContainingObject({
            scope: { name: 'go.opentelemetry.io/otel/sdk/tracer' },
            spans: arrayContainingObject({
              name: 'produce one',
            }),
          }),
        }),
        expect.objectContaining({
          resource: { attributes: arrayContainingObject({ key: 'service.name', value: { stringValue: 'clientTwo' } }) },
          scopeSpans: arrayContainingObject({
            scope: { name: 'go.opentelemetry.io/otel/sdk/tracer' },
            spans: arrayContainingObject({
              name: 'produce two',
            }),
          }),
        }),
        expect.objectContaining({
          resource: { attributes: arrayContainingObject({ key: 'service.name', value: { stringValue: 'clientThree' } }) },
          scopeSpans: arrayContainingObject({
            scope: { name: 'go.opentelemetry.io/otel/sdk/tracer' },
            spans: arrayContainingObject({
              name: 'produce three',
            }),
          }),
        }),
      ]),
    })
  })
})
